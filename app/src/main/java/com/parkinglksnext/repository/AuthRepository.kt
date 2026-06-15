package com.parkinglksnext.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Wraps Firebase Authentication operations.
 * Exposes reactive auth state via callbackFlow and suspend-based login/register.
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
        // Emit initial state immediately
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /**
     * Sign in with email and password.
     */
    fun login(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let {
                emit(Resource.Success(it))
            } ?: emit(Resource.Error("Usuario no encontrado"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al iniciar sesión"))
        }
    }

    /**
     * Create a new user account with email and password.
     */
    fun register(email: String, password: String): Flow<Resource<FirebaseUser>> = flow {
        emit(Resource.Loading())
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            result.user?.let {
                emit(Resource.Success(it))
            } ?: emit(Resource.Error("No se pudo crear la cuenta"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al registrar usuario"))
        }
    }

    /**
     * Send password reset email.
     */
    fun sendPasswordReset(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            auth.sendPasswordResetEmail(email).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al enviar el correo"))
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Get the currently signed-in user (null if not authenticated).
     */
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    /**
     * Extension: suspend until a Google Task completes.
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Tarea fallida sin excepción")
                    )
                }
            }
            addOnCanceledListener {
                continuation.cancel()
            }
        }
}
