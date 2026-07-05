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
 * Collection: parkingSpots/{spotId} — 35 documents (1-21 comun, 22-28 electric, 29-35 motorcycle)
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
     * Compatibility rules:
     * - "comun" → comun spots only (1-21)
     * - "electric"   → electric OR comun spots (1-28)
     * - "motorcycle" → motorcycle spots only (29-35)
     */
    fun getAvailableSpotsForVehicle(vehicleType: String): List<ParkingSpot> {
        val allAvailable = _spots.value.filter { it.available }
        val vType = vehicleType.lowercase()
        val filtered = when {
            vType in listOf("motorcycle", "moto") -> allAvailable.filter { it.type == "motorcycle" }
            vType in listOf("electric", "eléctrico", "electrico") -> allAvailable.filter {
                it.type in listOf("electric", "comun", "combustion")
            }
            // "comun", "combustion", "normal", or fallback
            else -> allAvailable.filter { it.type in listOf("comun", "combustion") }
        }
        // Sort: matching type first, then by number
        val matchingType = when {
            vType in listOf("motorcycle", "moto") -> "motorcycle"
            vType in listOf("electric", "eléctrico", "electrico") -> "electric"
            else -> "comun"
        }
        return filtered.sortedBy { if (it.type == matchingType) 0 else 1 }.sortedBy { it.number }
    }

    /**
     * Initialize parking spots seed data on first launch.
     * Creates 35 spots: 1-21 comun, 22-28 electric, 29-35 motorcycle.
     */
    fun seedParkingSpotsIfEmpty(): Flow<Resource<Unit>> = callbackFlow {
        collection.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val snapshot = task.result
                if (snapshot != null && snapshot.isEmpty) {
                    // Collection is empty — seed it
                    val batch = db.batch()
                    for (i in 1..35) {
                        val type = when {
                            i <= 21 -> "comun"
                            i <= 28 -> "electric"
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
