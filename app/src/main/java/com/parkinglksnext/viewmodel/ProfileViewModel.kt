package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.NotificationSettings
import com.parkinglksnext.UserProfile
import com.parkinglksnext.Vehicle
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.UserRepository
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages user profile: real-time loading, edit, and save.
 */
class ProfileViewModel : ViewModel() {

    private val userRepo = UserRepository()
    private val authRepo = AuthRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class ProfileUiState(
        val userProfile: UserProfile? = null,
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            userRepo.getUserProfile(uid).collect { resource ->
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

    /**
     * Save the full user profile (edit profile screen).
     */
    fun updateProfile(
        firstName: String,
        lastName: String,
        vehicles: List<Vehicle>,
        notificationSettings: NotificationSettings
    ) {
        val uid = authRepo.getCurrentUser()?.uid ?: return
        val currentProfile = _uiState.value.userProfile ?: return

        val updatedProfile = currentProfile.copy(
            firstName = firstName,
            lastName = lastName,
            name = "$firstName $lastName",
            vehicles = vehicles,
            licensePlate = vehicles.firstOrNull()?.licensePlate ?: "",
            vehicleType = vehicles.firstOrNull()?.type ?: "normal",
            notificationSettings = notificationSettings
        )

        viewModelScope.launch {
            userRepo.saveUserProfile(uid, updatedProfile).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isSaving = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isSaving = false, saveSuccess = true, error = null)
                    }
                    is Resource.Error -> _uiState.update {
                        it.copy(isSaving = false, error = resource.message)
                    }
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
