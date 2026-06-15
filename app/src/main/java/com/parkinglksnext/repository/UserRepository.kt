package com.parkinglksnext.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.parkinglksnext.UserProfile
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for user profile CRUD operations in Firestore.
 * Collection: users/{uid}
 */
class UserRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("users")

    /**
     * Real-time listener for the current user's profile.
     * Emits Loading, Success(UserProfile), or Error on every change.
     */
    fun getUserProfile(uid: String): Flow<Resource<UserProfile>> = callbackFlow {
        val listener = collection.document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar perfil"))
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)?.copy(id = uid)
                    if (profile != null) {
                        trySend(Resource.Success(profile))
                    } else {
                        trySend(Resource.Error("Perfil corrupto en base de datos"))
                    }
                } else {
                    trySend(Resource.Error("Perfil no encontrado"))
                }
            }
        awaitClose { listener.remove() }
    }

    /**
     * Create a new user profile document (called after registration).
     */
    fun saveUserProfile(uid: String, profile: UserProfile): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            collection.document(uid).set(profile).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al guardar perfil"))
        }
    }

    /**
     * Partial update to user profile fields.
     */
    fun updateUserProfile(uid: String, updates: Map<String, Any?>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            collection.document(uid).set(updates, SetOptions.merge()).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error al actualizar perfil"))
        }
    }

    /**
     * Extension: suspend until a Firestore Task completes.
     */
    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Tarea de Firestore fallida")
                    )
                }
            }
            addOnCanceledListener { continuation.cancel() }
        }
}
