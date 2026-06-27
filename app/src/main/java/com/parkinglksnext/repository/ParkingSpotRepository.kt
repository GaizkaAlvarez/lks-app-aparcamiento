package com.parkinglksnext.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.parkinglksnext.ParkingSpot
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Repository for parking spots (read-only, seeded data).
 * Collection: parkingSpots/{spotId} — 25 documents (1-21 combustion, 22-24 electric, 25 motorcycle)
 *
 * Starts a real-time Firestore snapshot listener on construction so _spots stays in sync.
 */
class ParkingSpotRepository {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val collection = db.collection("parkingSpots")

    // Dedicated scope for the snapshot listener — lives as long as the repository instance.
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _spots = MutableStateFlow<List<ParkingSpot>>(emptyList())
    val spots: StateFlow<List<ParkingSpot>> = _spots.asStateFlow()

    init {
        // Start the real-time snapshot listener immediately so any consumer
        // that reads getAvailableSpotsForVehicle() sees live data.
        repoScope.launch {
            getParkingSpots().collect { /* _spots is updated inside getParkingSpots */ }
        }
    }

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
     * - "combustion" → combustion spots only
     * - "electric"   → electric OR combustion spots
     * - "motorcycle" → motorcycle spots only
     */
    fun getAvailableSpotsForVehicle(vehicleType: String): List<ParkingSpot> {
        val allAvailable = _spots.value.filter { it.available }
        val filtered = when (vehicleType.lowercase()) {
            "motorcycle", "moto" -> allAvailable.filter { it.type == "motorcycle" }
            "electric", "eléctrico" -> allAvailable.filter {
                it.type in listOf("electric", "combustion")
            }
            else -> allAvailable.filter { it.type == "combustion" }
        }
        return filtered.sortedBy { it.number }
    }

    /**
     * Initialize parking spots seed data on first launch.
     * Creates 25 spots: 1-21 combustion, 22-24 electric, 25 motorcycle.
     */
    fun seedParkingSpotsIfEmpty(): Flow<Resource<Unit>> = callbackFlow {
        collection.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot != null && snapshot.isEmpty) {
                    // Collection is empty — seed it
                    val batch = db.batch()
                    for (i in 1..25) {
                        val type = when {
                            i <= 21 -> "combustion"
                            i <= 24 -> "electric"
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
