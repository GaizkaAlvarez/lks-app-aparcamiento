package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.ParkingSpot
import com.parkinglksnext.Reservation
import com.parkinglksnext.Vehicle
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NewReservationViewModel : ViewModel() {

    private val spotRepo = ParkingSpotRepository()
    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    data class NewReservationUiState(
        val step: Int = 1,
        val selectedVehicle: Vehicle? = null,
        val selectedDate: String? = null,
        val startTime: String? = null,
        val endTime: String? = null,
        val selectedSpot: ParkingSpot? = null,
        val availableSpots: List<ParkingSpot> = emptyList(),
        val userVehicles: List<Vehicle> = emptyList(),
        val spotsLoading: Boolean = false,
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(NewReservationUiState())
    val uiState: StateFlow<NewReservationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<NewReservationEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NewReservationEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            spotRepo.spots.collect { _ ->
                val vehicle = _uiState.value.selectedVehicle
                if (vehicle != null && _uiState.value.step == 2) {
                    val available = spotRepo.getAvailableSpotsForVehicle(vehicle.type)
                    _uiState.update { it.copy(availableSpots = available, spotsLoading = false) }
                }
            }
        }
    }

    fun setUserVehicles(vehicles: List<Vehicle>) {
        _uiState.update { it.copy(userVehicles = vehicles) }
    }

    fun setVehicle(vehicle: Vehicle) {
        _uiState.update { it.copy(selectedVehicle = vehicle) }
    }

    fun setDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    fun setStartTime(time: String) {
        _uiState.update { it.copy(startTime = time) }
    }

    fun setEndTime(time: String) {
        _uiState.update { it.copy(endTime = time) }
    }

    fun goToStep2() {
        val vehicle = _uiState.value.selectedVehicle ?: return
        val date = _uiState.value.selectedDate ?: return
        val start = _uiState.value.startTime ?: return
        val end = _uiState.value.endTime ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(step = 2, spotsLoading = true, availableSpots = emptyList()) }

            // All spots compatible with vehicle type (static filter)
            val compatibleSpots = spotRepo.getAvailableSpotsForVehicle(vehicle.type)

            // Filter out spots that are already reserved for this date/time (dynamic filter)
            val conflictingIds = reservationRepo.getConflictingSpotIds(date, start, end)
            val libreSpots = compatibleSpots.filter { it.id !in conflictingIds }

            _uiState.update {
                it.copy(availableSpots = libreSpots.sortedBy { s -> s.number }, spotsLoading = false)
            }
        }
    }

    fun selectSpot(spot: ParkingSpot) {
        _uiState.update { it.copy(selectedSpot = spot) }
    }

    fun goBackToStep1() {
        _uiState.update { it.copy(step = 1, selectedSpot = null, availableSpots = emptyList(), spotsLoading = false) }
    }

    fun confirmReservation() {
        val state = _uiState.value
        val vehicle = state.selectedVehicle ?: return
        val spot = state.selectedSpot ?: return
        val date = state.selectedDate ?: return
        val start = state.startTime ?: return
        val end = state.endTime ?: return
        val uid = authRepo.getCurrentUser()?.uid ?: return

        val reservation = Reservation(
            userId = uid,
            vehicleId = vehicle.id,
            spotId = spot.id,
            spotNumber = spot.number,
            spotType = spot.type,
            date = date,
            startTime = start,
            endTime = end,
            status = "active"
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Check for conflicting reservations first
            val hasConflict = reservationRepo.hasConflictingReservation(
                spotId = spot.id, date = date, startTime = start, endTime = end
            )
            if (hasConflict) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Esta plaza ya está reservada en ese horario. Selecciona otra plaza u otro horario.")
                }
                return@launch
            }

            val result = reservationRepo.createReservation(reservation)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    _events.emit(NewReservationEvent.Success)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetState() {
        _uiState.update { NewReservationUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class NewReservationEvent {
    data object Success : NewReservationEvent()
}
