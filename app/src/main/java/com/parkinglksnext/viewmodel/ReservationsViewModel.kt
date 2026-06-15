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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages active reservations: real-time list, cancel, update.
 */
class ReservationsViewModel : ViewModel() {

    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class ReservationsUiState(
        val activeReservations: List<Reservation> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val isUpdateSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(ReservationsUiState())
    val uiState: StateFlow<ReservationsUiState> = _uiState.asStateFlow()

    init {
        loadActiveReservations()
    }

    private fun loadActiveReservations() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            reservationRepo.getActiveReservations(uid).collect { resource ->
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
            reservationRepo.cancelReservation(id).collect { resource ->
                when (resource) {
                    is Resource.Loading -> { /* handled by real-time listener */ }
                    is Resource.Success -> { /* real-time listener will update list */ }
                    is Resource.Error -> _uiState.update {
                        it.copy(error = resource.message)
                    }
                }
            }
        }
    }

    fun updateReservation(id: String, startTime: String, endTime: String) {
        viewModelScope.launch {
            val updates = mapOf<String, Any?>(
                "startTime" to startTime,
                "endTime" to endTime
            )
            reservationRepo.updateReservation(id, updates).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isUpdateSuccess = false) }
                    is Resource.Success -> _uiState.update { it.copy(isUpdateSuccess = true) }
                    is Resource.Error -> _uiState.update {
                        it.copy(error = resource.message, isUpdateSuccess = false)
                    }
                }
            }
        }
    }

    fun clearUpdateSuccess() {
        _uiState.update { it.copy(isUpdateSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
