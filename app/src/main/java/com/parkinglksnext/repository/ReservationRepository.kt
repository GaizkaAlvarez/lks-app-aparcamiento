package com.parkinglksnext.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.parkinglksnext.Reservation
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Repository for reservation CRUD.
 * Real-time reads use callbackFlow. Writes use plain suspend functions.
 */
class ReservationRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("reservations")

    /**
     * Real-time active reservations — stays as callbackFlow.
     */
    fun getActiveReservations(userId: String): Flow<Resource<List<Reservation>>> = callbackFlow {
        val listener = collection
            .whereEqualTo("userId", userId)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar reservas"))
                    return@addSnapshotListener
                }
                val reservations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(Resource.Success(reservations))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time history — stays as callbackFlow.
     */
    fun getReservationHistory(userId: String): Flow<Resource<List<Reservation>>> = callbackFlow {
        val listener = collection
            .whereEqualTo("userId", userId)
            .whereIn("status", listOf("completed", "cancelled"))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.localizedMessage ?: "Error al cargar historial"))
                    return@addSnapshotListener
                }
                val reservations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Reservation::class.java)?.copy(id = doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(Resource.Success(reservations))
            }
        awaitClose { listener.remove() }
    }

    /**
     * Get all spot IDs that have an active reservation overlapping with the given time range.
     * These spots are "reservada" — they should not appear as available.
     */
    suspend fun getConflictingSpotIds(
        date: String,
        startTime: String,
        endTime: String
    ): Set<String> {
        return try {
            val snapshot = collection
                .whereEqualTo("date", date)
                .whereEqualTo("status", "active")
                .get()
                .await()
            val newStart = startTime.toMinutes()
            val newEnd = endTime.toMinutes()
            snapshot.documents.mapNotNull { doc ->
                val rStart = doc.getString("startTime")?.toMinutes() ?: 0
                val rEnd = doc.getString("endTime")?.toMinutes() ?: 0
                if (newStart < rEnd && newEnd > rStart) {
                    doc.getString("spotId")
                } else null
            }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Check if a specific spot has a conflicting reservation,
     * optionally excluding a reservation ID (for edit flow).
     */
    suspend fun hasConflictingReservation(
        spotId: String, date: String, startTime: String, endTime: String,
        excludeId: String? = null
    ): Boolean {
        return try {
            val snapshot = collection
                .whereEqualTo("spotId", spotId)
                .whereEqualTo("date", date)
                .whereEqualTo("status", "active")
                .get()
                .await()
            val newStart = startTime.toMinutes()
            val newEnd = endTime.toMinutes()
            snapshot.documents.any { doc ->
                if (excludeId != null && doc.id == excludeId) return@any false
                val rStart = doc.getString("startTime")?.toMinutes() ?: 0
                val rEnd = doc.getString("endTime")?.toMinutes() ?: 0
                newStart < rEnd && newEnd > rStart
            }
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Get all active reservations for a specific spot and date (for edit UI).
     */
    suspend fun getReservationsForSpotAndDate(
        spotId: String, date: String
    ): List<Reservation> {
        return try {
            val snapshot = collection
                .whereEqualTo("spotId", spotId)
                .whereEqualTo("date", date)
                .whereEqualTo("status", "active")
                .get()
                .await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Reservation::class.java)?.copy(id = doc.id)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Create reservation — suspend, no flow/emit nesting.
     */
    suspend fun createReservation(reservation: Reservation): Resource<Unit> {
        return try {
            val data = reservation.copy(createdAt = java.time.Instant.now().toString())
            collection.add(data).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al crear reserva")
        }
    }

    /**
     * Update reservation fields — suspend.
     */
    suspend fun updateReservation(id: String, updates: Map<String, Any>): Resource<Unit> {
        return try {
            collection.document(id).update(updates).await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al actualizar reserva")
        }
    }

    /**
     * Cancel reservation — suspend.
     */
    suspend fun cancelReservation(id: String): Resource<Unit> {
        return try {
            collection.document(id).update("status", "cancelled").await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Error al cancelar reserva")
        }
    }

    /**
     * Convert "HH:mm" to total minutes since midnight.
     */
    private fun String.toMinutes(): Int {
        val parts = this.split(":")
        return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
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
