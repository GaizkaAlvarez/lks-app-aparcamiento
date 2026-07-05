package com.parkinglksnext.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.parkinglksnext.UserProfile
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for user profile CRUD.
 * Real-time read uses callbackFlow. Writes use plain suspend functions.
 */
class UserRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("users")

    /**
     * Real-time listener — stays as callbackFlow.
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
     * Save profile — suspend, no flow/emit nesting.
     */
    suspend fun saveUserProfile(uid: String, profile: UserProfile): Resource<Unit> {
        return try {
            collection.document(uid).set(profile).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al guardar perfil")
        }
    }

    /**
     * Partial update — suspend.
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): Resource<Unit> {
        return try {
            collection.document(uid).set(updates, SetOptions.merge()).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al actualizar perfil")
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(
                    task.exception ?: Exception("Tarea de Firestore fallida")
                )
            }
            addOnCanceledListener { continuation.cancel() }
        }
}
