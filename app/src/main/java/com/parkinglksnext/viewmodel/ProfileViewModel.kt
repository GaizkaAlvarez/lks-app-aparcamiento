package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.parkinglksnext.NotificationSettings
import com.parkinglksnext.UserProfile
import com.parkinglksnext.Vehicle
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.repository.UserRepository
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
class ProfileViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()
    private val reservationRepo = ReservationRepository()

    data class ProfileUiState(
        val userProfile: UserProfile? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _saveEvents = MutableSharedFlow<ProfileEvent>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<ProfileEvent> = _saveEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    userRepo.getUserProfile(firebaseUser.uid)
                } else {
                    _uiState.update { ProfileUiState() }
                    emptyFlow()
                }
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(userProfile = resource.data, isLoading = false, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isLoading = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun updateProfile(
        firstName: String,
        lastName: String,
        vehicles: List<Vehicle>,
        notificationSettings: NotificationSettings,
        oldPassword: String = "",
        newPassword: String = ""
    ) {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        val currentProfile = _uiState.value.userProfile ?: return
        val firebaseUser = authRepo.getCurrentUser() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            // 1. Change password if both fields provided
            if (oldPassword.isNotEmpty() && newPassword.isNotEmpty()) {
                val credential = EmailAuthProvider.getCredential(firebaseUser.email ?: "", oldPassword)
                val reauthResult = authRepo.reauthenticate(credential)
                if (reauthResult is Resource.Error) {
                    _uiState.update { it.copy(isSaving = false, error = reauthResult.message) }
                    return@launch
                }
                val pwdResult = authRepo.updatePassword(newPassword)
                if (pwdResult is Resource.Error) {
                    _uiState.update { it.copy(isSaving = false, error = pwdResult.message) }
                    return@launch
                }
            }

            // 2. Update Firestore profile
            val updatedProfile = currentProfile.copy(
                firstName = firstName,
                lastName = lastName,
                name = "$firstName $lastName",
                vehicles = vehicles,
                licensePlate = vehicles.firstOrNull()?.licensePlate ?: "",
                vehicleType = vehicles.firstOrNull()?.type ?: "comun",
                notificationSettings = notificationSettings
            )

            val result = userRepo.saveUserProfile(uid, updatedProfile)
            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false, error = null) }
                    _saveEvents.emit(ProfileEvent.SaveSuccess)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Save profile photo as base64 string directly in Firestore.
     * Called from MainActivity after converting the image URI.
     */
    fun saveProfilePhotoBase64(base64Image: String) {
        val uid = authRepo.getCurrentUser()?.uid ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val currentProfile = _uiState.value.userProfile ?: UserProfile()
            val updatedProfile = currentProfile.copy(profileImageBase64 = base64Image)
            val result = userRepo.saveUserProfile(uid, updatedProfile)

            when (result) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                }
                is Resource.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Cancel all active reservations for a specific vehicle.
     * Called when the user deletes a vehicle from their profile.
     */
    fun cancelReservationsForVehicle(vehicleId: String) {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            reservationRepo.getActiveReservations(uid).collect { resource ->
                if (resource is Resource.Success) {
                    resource.data
                        .filter { it.vehicleId == vehicleId && it.status == "active" }
                        .forEach { reservation ->
                            reservationRepo.cancelReservation(reservation.id)
                        }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class ProfileEvent {
    data object SaveSuccess : ProfileEvent()
}
