package com.parkinglksnext.repository

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps Firebase Authentication operations.
 * One-shot operations use suspend functions (no flow/emit deadlocks).
 * Only auth state uses callbackFlow for reactive observation.
 */
class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    /**
     * Real-time auth state: emits the current FirebaseUser (or null) on every auth change.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign in — suspend, no flow nesting.
     */
    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Resource.Success(it) }
                ?: Resource.Error("Usuario no encontrado")
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    /**
     * Create account — suspend, no flow nesting.
     */
    suspend fun register(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let { Resource.Success(it) }
                ?: Resource.Error("No se pudo crear la cuenta")
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    /**
     * Sign in with a credential from a federated provider (Google, Apple, etc.).
     */
    suspend fun signInWithCredential(credential: AuthCredential): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            result.user?.let { Resource.Success(it) }
                ?: Resource.Error("No se pudo iniciar sesión")
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    /**
     * Send password reset email — suspend.
     */
    suspend fun sendPasswordReset(email: String): Resource<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    /**
     * Re-authenticate the current user (required before password change).
     */
    suspend fun reauthenticate(credential: AuthCredential): Resource<Unit> {
        return try {
            auth.currentUser?.reauthenticate(credential)?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    /**
     * Update the current user's password (requires recent re-authentication).
     */
    suspend fun updatePassword(newPassword: String): Resource<Unit> {
        return try {
            auth.currentUser?.updatePassword(newPassword)?.await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(friendlyMessage(e))
        }
    }

    fun signOut() {
        auth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    // ─── Error translation ──────────────────────────────────────

    /**
     * Translate Firebase Auth exceptions to user-friendly Spanish messages.
     * Checks FirebaseAuthException.errorCode first (most reliable), then localizedMessage.
     */
    fun friendlyMessage(e: Exception): String {
        val code = (e as? FirebaseAuthException)?.errorCode ?: ""
        val msg = e.localizedMessage ?: ""
        return when {
            // Match by error code (precise)
            code == "ERROR_INVALID_EMAIL" ->
                "El formato del correo electrónico no es válido."
            code == "ERROR_WRONG_PASSWORD" ->
                "La contraseña es incorrecta."
            code == "ERROR_USER_NOT_FOUND" ->
                "No existe una cuenta con este correo electrónico."
            code == "ERROR_USER_DISABLED" ->
                "Esta cuenta ha sido deshabilitada."
            code == "ERROR_EMAIL_ALREADY_IN_USE" ->
                "Ya existe una cuenta con este correo electrónico."
            code == "ERROR_WEAK_PASSWORD" ->
                "La contraseña debe tener al menos 6 caracteres."
            code == "ERROR_INVALID_CREDENTIAL" ->
                "El correo electrónico o la contraseña son incorrectos."
            code == "ERROR_TOO_MANY_REQUESTS" ->
                "Demasiados intentos. Espera unos segundos e inténtalo de nuevo."
            code == "ERROR_NETWORK_REQUEST_FAILED" ->
                "Error de conexión. Comprueba tu conexión a Internet."
            code == "ERROR_REQUIRES_RECENT_LOGIN" ->
                "Para cambiar la contraseña necesitas volver a iniciar sesión."
            code == "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
                "Ya existe una cuenta con este correo usando otro método de inicio de sesión."
            // Fallback: match by message text
            msg.contains("invalid email", ignoreCase = true) ||
            msg.contains("badly formatted", ignoreCase = true) ->
                "El formato del correo electrónico no es válido."
            msg.contains("wrong password", ignoreCase = true) ||
            msg.contains("password is invalid", ignoreCase = true) ->
                "La contraseña es incorrecta."
            msg.contains("no user record", ignoreCase = true) ||
            msg.contains("user not found", ignoreCase = true) ->
                "No existe una cuenta con este correo electrónico."
            msg.contains("email already in use", ignoreCase = true) ->
                "Ya existe una cuenta con este correo electrónico."
            msg.contains("password should be at least", ignoreCase = true) ->
                "La contraseña debe tener al menos 6 caracteres."
            msg.contains("network error", ignoreCase = true) ->
                "Error de conexión. Comprueba tu conexión a Internet."
            msg.contains("invalid credential", ignoreCase = true) ->
                "El correo electrónico o la contraseña son incorrectos."
            msg.contains("too many", ignoreCase = true) ->
                "Demasiados intentos. Espera unos segundos e inténtalo de nuevo."
            msg.contains("requires recent authentication", ignoreCase = true) ->
                "Para cambiar la contraseña necesitas volver a iniciar sesión."
            msg.contains("email", ignoreCase = true) || msg.contains("correo", ignoreCase = true) ->
                "El formato del correo electrónico no es válido."
            else -> "Error al iniciar sesión. Verifica el correo y la contraseña."
        }
    }

    // ─── Task await extension ────────────────────────────────────

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(
                    task.exception ?: Exception("Tarea fallida sin excepción")
                )
            }
            addOnCanceledListener { continuation.cancel() }
        }
}
