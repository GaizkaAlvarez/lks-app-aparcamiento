package com.parkinglksnext.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.ParkingWidgetProvider
import com.parkinglksnext.Reservation
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.util.NotificationHelper
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReservationsViewModel(application: Application) : AndroidViewModel(application) {

    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    data class ReservationsUiState(
        val currentReservations: List<Reservation> = emptyList(),
        val futureReservations: List<Reservation> = emptyList(),
        val spotReservations: List<Reservation> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReservationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ReservationEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    reservationRepo.getActiveReservations(firebaseUser.uid)
                } else {
                    _uiState.update { ReservationsUiState() }
                    emptyFlow()
                }
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> {
                        val (current, future) = categorizeActiveReservations(resource.data)
                        _uiState.update {
                            it.copy(
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
     * Split active reservations into current and future.
     * Past/expired active reservations are excluded — they only appear in History.
     */
    private fun categorizeActiveReservations(
        reservations: List<Reservation>
    ): Pair<List<Reservation>, List<Reservation>> {
        val today = LocalDate.now()
        val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute

        val current = mutableListOf<Reservation>()
        val future = mutableListOf<Reservation>()

        for (r in reservations) {
            if (r.status != "active") continue

            val resDate = try {
                LocalDate.parse(r.date)
            } catch (_: Exception) {
                continue
            }

            val startMin = r.startTime.toMinutes()
            val endMin = r.endTime.toMinutes()

            when {
                // Past: skip entirely (only shown in history)
                resDate < today -> { /* omit */ }
                resDate == today && endMin <= nowMinutes -> { /* omit */ }
                // Current: today and active now
                resDate == today && startMin <= nowMinutes && endMin > nowMinutes -> current.add(r)
                // Future: today not started yet, or future date
                resDate > today || (resDate == today && startMin > nowMinutes) -> future.add(r)
            }
        }

        return Pair(
            current.sortedBy { "${it.date}T${it.startTime}" },
            future.sortedBy { "${it.date}T${it.startTime}" }
        )
    }

    private fun String.toMinutes(): Int {
        val parts = this.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    }

    fun cancelReservation(id: String) {
        viewModelScope.launch {
            val result = reservationRepo.cancelReservation(id)
            when (result) {
                is Resource.Success -> {
                    _events.emit(ReservationEvent.CancelSuccess)
                    ParkingWidgetProvider.notifyDataChanged(getApplication())
                    val ctx = getApplication<Application>()
                    NotificationHelper.cancelReminders(ctx, id)
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateReservation(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            // Find reservation data for conflict check (search in both lists)
            val reservation = _uiState.value.currentReservations.find { it.id == id }
                ?: _uiState.value.futureReservations.find { it.id == id }

            // Validate max 8 hours
            val startMin = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            val endMin = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
            if (endMin - startMin > 480) {
                _uiState.update { it.copy(error = "La duración máxima es de 8 horas.") }
                return@launch
            }

            // Check for conflicting reservations (excluding the one being edited)
            if (reservation != null) {
                val hasConflict = reservationRepo.hasConflictingReservation(
                    spotId = reservation.spotId,
                    date = reservation.date,
                    startTime = startTime,
                    endTime = endTime,
                    excludeId = id
                )
                if (hasConflict) {
                    _uiState.update {
                        it.copy(error = "Ya existe una reserva en esa plaza para ese horario.")
                    }
                    return@launch
                }
            }

            val updates = mapOf<String, Any>(
                "startTime" to startTime,
                "endTime" to endTime
            )
            val result = reservationRepo.updateReservation(id, updates)
            when (result) {
                is Resource.Success -> {
                    _events.emit(ReservationEvent.UpdateSuccess)
                    ParkingWidgetProvider.notifyDataChanged(getApplication())
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun loadSpotReservations(spotId: String, date: String) {
        viewModelScope.launch {
            val reservations = reservationRepo.getReservationsForSpotAndDate(spotId, date)
            _uiState.update { it.copy(spotReservations = reservations) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class ReservationEvent {
    data object UpdateSuccess : ReservationEvent()
    data object CancelSuccess : ReservationEvent()
}
