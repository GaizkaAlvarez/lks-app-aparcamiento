package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.Reservation
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ReservationRepository
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReservationsViewModel : ViewModel() {

    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    data class ReservationsUiState(
        val activeReservations: List<Reservation> = emptyList(),
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
                    is Resource.Success -> _uiState.update {
                        it.copy(activeReservations = resource.data, isLoading = false, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun cancelReservation(id: String) {
        viewModelScope.launch {
            val result = reservationRepo.cancelReservation(id)
            when (result) {
                is Resource.Success -> _events.emit(ReservationEvent.CancelSuccess)
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> {}
            }
        }
    }

    fun updateReservation(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            // Find reservation data for conflict check
            val reservation = _uiState.value.activeReservations.find { it.id == id }

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
                is Resource.Success -> _events.emit(ReservationEvent.UpdateSuccess)
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
