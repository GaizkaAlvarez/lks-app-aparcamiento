package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.UserProfile
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.UserRepository
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages authentication state: login, register, password reset, logout, session persistence.
 */
class AuthViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

    // ─── UI State ────────────────────────────────────────────────

    data class AuthUiState(
        val isAuthenticated: Boolean = false,
        val userProfile: UserProfile? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val isPasswordResetSent: Boolean = false,
        val registerSuccess: Boolean = false
    )

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ─── One-shot events (snackbar messages) ─────────────────────

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    // ─── Firebase auth state → session persistence ───────────────

    init {
        viewModelScope.launch {
            authRepo.authStateFlow.collect { firebaseUser ->
                if (firebaseUser != null) {
                    // Load user profile whenever a user is authenticated
                    userRepo.getUserProfile(firebaseUser.uid).collect { resource ->
                        when (resource) {
                            is Resource.Loading -> _uiState.update {
                                it.copy(isLoading = true)
                            }
                            is Resource.Success -> _uiState.update {
                                it.copy(
                                    isAuthenticated = true,
                                    userProfile = resource.data,
                                    isLoading = false,
                                    error = null
                                )
                            }
                            is Resource.Error -> _uiState.update {
                                it.copy(
                                    isAuthenticated = true, // still auth'd, just profile failed
                                    isLoading = false,
                                    error = resource.message
                                )
                            }
                        }
                    }
                } else {
                    _uiState.update {
                        AuthUiState() // reset to defaults on sign-out
                    }
                }
            }
        }
    }

    // ─── Actions ─────────────────────────────────────────────────

    fun login(email: String, password: String) {
        viewModelScope.launch {
            authRepo.login(email, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> { /* authStateFlow will handle the rest */ }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.message) }
                        _events.emit(AuthEvent.ShowSnackbar(resource.message))
                    }
                }
            }
        }
    }

    fun register(email: String, password: String, profile: UserProfile) {
        viewModelScope.launch {
            authRepo.register(email, password).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is Resource.Success -> {
                        val firebaseUser = resource.data
                        // Save the full profile to Firestore
                        userRepo.saveUserProfile(firebaseUser.uid, profile).collect { saveResult ->
                            when (saveResult) {
                                is Resource.Loading -> { /* already loading */ }
                                is Resource.Success -> {
                                    _uiState.update {
                                        it.copy(isLoading = false, registerSuccess = true, error = null)
                                    }
                                }
                                is Resource.Error -> {
                                    _uiState.update {
                                        it.copy(isLoading = false, error = saveResult.message)
                                    }
                                    _events.emit(AuthEvent.ShowSnackbar(saveResult.message))
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoading = false, error = resource.message) }
                        _events.emit(AuthEvent.ShowSnackbar(resource.message))
                    }
                }
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            authRepo.sendPasswordReset(email).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update {
                        it.copy(isLoading = true, error = null, isPasswordResetSent = false)
                    }
                    is Resource.Success -> _uiState.update {
                        it.copy(isLoading = false, isPasswordResetSent = true, error = null)
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = resource.message)
                        }
                        _events.emit(AuthEvent.ShowSnackbar(resource.message))
                    }
                }
            }
        }
    }

    fun logout() {
        authRepo.signOut()
        _uiState.update { AuthUiState() }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * One-shot events emitted by AuthViewModel (e.g., snackbar messages).
 */
sealed class AuthEvent {
    data class ShowSnackbar(val message: String) : AuthEvent()
}
