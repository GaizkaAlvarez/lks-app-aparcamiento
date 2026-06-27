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

/**
 * Manages reservation history (completed + cancelled) for the authenticated user.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {

    private val reservationRepo = ReservationRepository()
    private val authRepo = AuthRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class HistoryUiState(
        val reservations: List<Reservation> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        // Reactively load history when the user authenticates.
        // flatMapLatest cancels the previous snapshot listener on auth change.
        viewModelScope.launch {
            authRepo.authStateFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    reservationRepo.getReservationHistory(firebaseUser.uid)
                } else {
                    _uiState.update { HistoryUiState() }
                    emptyFlow()
                }
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(reservations = resource.data, isLoading = false, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }
}
