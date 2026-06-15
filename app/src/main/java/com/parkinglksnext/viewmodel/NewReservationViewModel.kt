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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the two-step new reservation flow.
 * Step 1: select vehicle, date, start/end time.
 * Step 2: select a parking spot from filtered available spots and confirm.
 */
class NewReservationViewModel : ViewModel() {

    private val spotRepo = ParkingSpotRepository()
    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class NewReservationUiState(
        val step: Int = 1,                               // 1 = params, 2 = spot selection
        val selectedVehicle: Vehicle? = null,
        val selectedDate: String? = null,                // yyyy-MM-dd
        val startTime: String? = null,                   // HH:mm
        val endTime: String? = null,                     // HH:mm
        val selectedSpot: ParkingSpot? = null,
        val availableSpots: List<ParkingSpot> = emptyList(),
        val userVehicles: List<Vehicle> = emptyList(),
        val isLoading: Boolean = false,
        val isSuccess: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(NewReservationUiState())
    val uiState: StateFlow<NewReservationUiState> = _uiState.asStateFlow()

    // ─── Step 1 actions ──────────────────────────────────────────

    fun setUserVehicles(vehicles: List<Vehicle>) {
        _uiState.update {
            it.copy(userVehicles = vehicles)
        }
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
        val spots = spotRepo.getAvailableSpotsForVehicle(vehicle.type)
        _uiState.update {
            it.copy(step = 2, availableSpots = spots)
        }
    }

    // ─── Step 2 actions ──────────────────────────────────────────

    fun selectSpot(spot: ParkingSpot) {
        _uiState.update { it.copy(selectedSpot = spot) }
    }

    fun goBackToStep1() {
        _uiState.update { it.copy(step = 1, selectedSpot = null, availableSpots = emptyList()) }
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
            reservationRepo.createReservation(reservation).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, isSuccess = true, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                }
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
