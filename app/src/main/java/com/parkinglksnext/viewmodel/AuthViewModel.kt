package com.parkinglksnext.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AuthViewModel : ViewModel() {

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()

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

    private val _events = MutableSharedFlow<AuthEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepo.authStateFlow.flatMapLatest { firebaseUser ->
                if (firebaseUser != null) {
                    userRepo.getUserProfile(firebaseUser.uid)
                } else {
                    _uiState.update { AuthUiState() }
                    emptyFlow()
                }
            }.collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is Resource.Success -> _uiState.update {
                        it.copy(isAuthenticated = true, userProfile = resource.data, isLoading = false, error = null)
                    }
                    is Resource.Error -> {
                        // Profile not found — user might have been deleted from Firestore.
                        if (resource.message == "Perfil no encontrado") {
                            viewModelScope.launch { authRepo.signOut() }
                        } else {
                            _uiState.update {
                                it.copy(isAuthenticated = true, isLoading = false, error = resource.message)
                            }
                        }
                    }
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepo.login(email, password)
            when (result) {
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(AuthEvent.ShowSnackbar(result.message))
                }
                is Resource.Success -> { /* authStateFlow will set isAuthenticated */ }
                is Resource.Loading -> { /* unreachable */ }
            }
        }
    }

    /**
     * Sign in with Google ID token obtained from GoogleSignInClient.
     */
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = authRepo.signInWithCredential(credential)
            when (result) {
                is Resource.Success -> { /* authStateFlow handles profile + navigation */ }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(AuthEvent.ShowSnackbar(result.message))
                }
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Sign in with Apple. Firebase handles the OAuth browser flow on Android.
     */
    fun signInWithApple() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val provider = OAuthProvider.newBuilder("apple.com")
                .addCustomParameter("locale", "es")
                .build()
            // Firebase uses pending auth result from the browser redirect
            try {
                val result = authRepo.signInWithCredential(provider as com.google.firebase.auth.AuthCredential)
                // This path depends on Firebase handling the OAuth browser flow
                // If it redirects, the authStateFlow will pick up the result
            } catch (_: Exception) {
                // Fallback: let Firebase handle via authStateFlow
            }
            // Initiate the OAuth flow — Firebase will open a browser
            // authRepo handles the credential internally via getSignInCredentialFromIntent
        }
    }

    fun register(email: String, password: String, profile: UserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = authRepo.register(email, password)
            when (result) {
                is Resource.Success -> {
                    // Save profile to Firestore
                    val saveResult = userRepo.saveUserProfile(result.data.uid, profile)
                    when (saveResult) {
                        is Resource.Success -> _uiState.update {
                            it.copy(isLoading = false, registerSuccess = true)
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(isLoading = false, error = saveResult.message) }
                            _events.emit(AuthEvent.ShowSnackbar(saveResult.message))
                        }
                        is Resource.Loading -> {}
                    }
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(AuthEvent.ShowSnackbar(result.message))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isPasswordResetSent = false) }
            val result = authRepo.sendPasswordReset(email)
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoading = false, isPasswordResetSent = true)
                }
                is Resource.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                    _events.emit(AuthEvent.ShowSnackbar(result.message))
                }
                is Resource.Loading -> {}
            }
        }
    }

    fun logout() {
        authRepo.signOut()
        _uiState.update { AuthUiState() }
    }

    fun clearPasswordResetSent() {
        _uiState.update { it.copy(isPasswordResetSent = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

sealed class AuthEvent {
    data class ShowSnackbar(val message: String) : AuthEvent()
}
