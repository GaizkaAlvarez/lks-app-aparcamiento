package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.Reservation
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Manages reservation history for the authenticated user.
 * Shows ALL reservations divided into three sections: past, current, future.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {

    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class HistoryUiState(
        val pastReservations: List<Reservation> = emptyList(),
        val currentReservations: List<Reservation> = emptyList(),
        val futureReservations: List<Reservation> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Reactively load ALL reservations when the user authenticates.
        viewModelScope.launch {
            authRepo.authStateFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    reservationRepo.getAllReservations(firebaseUser.uid)
                } else {
                    _uiState.update { HistoryUiState() }
                    emptyFlow()
                }
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        val (past, current, future) = categorizeReservations(resource.data)
                        _uiState.update {
                            it.copy(
                                pastReservations = past,
                                currentReservations = current,
                                futureReservations = future,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    /**
     * Categorize reservations into past, current, and future.
     *
     * Past: completed/cancelled, OR active but date+endTime already passed
     * Current: active AND date=today AND time range includes now
     * Future: active AND (date>today OR (date=today AND startTime>now))
     */
    private fun categorizeReservations(
        reservations: List<Reservation>
    ): Triple<List<Reservation>, List<Reservation>, List<Reservation>> {
        val today = LocalDate.now()
        val now = LocalTime.now()
        val nowMinutes = now.hour * 60 + now.minute

        val past = mutableListOf<Reservation>()
        val current = mutableListOf<Reservation>()
        val future = mutableListOf<Reservation>()

        for (r in reservations) {
            // Completed or cancelled always go to past
            if (r.status == "completed" || r.status == "cancelled") {
                past.add(r)
                continue
            }

            // Active reservations: categorize by date/time
            val resDate = try {
                LocalDate.parse(r.date)
            } catch (_: Exception) {
                past.add(r) // unparseable dates go to past
                continue
            }

            val startMin = r.startTime.toMinutes()
            val endMin = r.endTime.toMinutes()

            when {
                // Date is in the past
                resDate < today -> past.add(r)
                // Today, but the reservation has already ended
                resDate == today && endMin <= nowMinutes -> past.add(r)
                // Today and currently active (within time range)
                resDate == today && startMin <= nowMinutes && endMin > nowMinutes -> current.add(r)
                // Today and hasn't started yet, or future date
                resDate > today || (resDate == today && startMin > nowMinutes) -> future.add(r)
                else -> future.add(r)
            }
        }

        // Sort: past by date descending, current by start time ascending, future by date ascending
        return Triple(
            past.sortedByDescending { "${it.date}T${it.startTime}" },
            current.sortedBy { "${it.date}T${it.startTime}" },
            future.sortedBy { "${it.date}T${it.startTime}" }
        )
    }

    private fun String.toMinutes(): Int {
        val parts = this.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }
}
