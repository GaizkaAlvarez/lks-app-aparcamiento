package com.parkinglksnext.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.parkinglksnext.ParkingSpot
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Repository for parking spots (read-only, seeded data).
 * Collection: parkingSpots/{spotId} — 100 documents (1-85 normal, 86-95 electric, 96-100 motorcycle)
 */
class ParkingSpotRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("parkingSpots")

    private val _spots = MutableStateFlow<List<ParkingSpot>>(emptyList())
    val spots: StateFlow<List<ParkingSpot>> = _spots.asStateFlow()

    /**
     * Real-time listener for all parking spots.
     * Caches results in _spots for fast filtering.
     */
    fun getParkingSpots(): Flow<Resource<List<ParkingSpot>>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(Resource.Error(error.localizedMessage ?: "Error al cargar plazas"))
                return@addSnapshotListener
            }
            val spotsList = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(ParkingSpot::class.java)?.copy(id = doc.id)
            } ?: emptyList()
            _spots.value = spotsList
            trySend(Resource.Success(spotsList))
        }
        awaitClose { listener.remove() }
    }

    /**
     * Returns available parking spots compatible with the given vehicle type.
     *
     * Compatibility rules (from Figma reference):
     * - "normal"     → normal spots only
     * - "electric"   → electric OR normal spots
     * - "motorcycle" → motorcycle spots only
     */
    fun getAvailableSpotsForVehicle(vehicleType: String): List<ParkingSpot> {
        val allAvailable = _spots.value.filter { it.available }
        return when (vehicleType.lowercase()) {
            "motorcycle", "moto" -> allAvailable.filter { it.type == "motorcycle" }
            "electric", "eléctrico" -> allAvailable.filter { it.type in listOf("electric", "normal") }
            else -> allAvailable.filter { it.type == "normal" }
        }
    }

    /**
     * Initialize parking spots seed data on first launch.
     * Creates 100 spots: 1-85 normal, 86-95 electric, 96-100 motorcycle.
     */
    fun seedParkingSpotsIfEmpty(): Flow<Resource<Unit>> = callbackFlow {
        collection.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot != null && snapshot.isEmpty) {
                    // Collection is empty — seed it
                    val batch = db.batch()
                    for (i in 1..100) {
                        val type = when {
                            i <= 85 -> "normal"
                            i <= 95 -> "electric"
                            else -> "motorcycle"
                        }
                        val spot = hashMapOf<String, Any>(
                            "number" to i,
                            "type" to type,
                            "available" to true
                        )
                        batch.set(collection.document("spot-$i"), spot)
                    }
                    batch.commit().addOnCompleteListener { commitTask ->
                        if (commitTask.isSuccessful) {
                            trySend(Resource.Success(Unit))
                        } else {
                            trySend(Resource.Error("Error al inicializar plazas"))
                        }
                    }
                } else {
                    trySend(Resource.Success(Unit)) // Already seeded
                }
            } else {
                trySend(Resource.Error("Error al verificar plazas"))
            }
        }
        awaitClose { }
    }
}
