package com.parkinglksnext.util

import com.parkinglksnext.repository.ParkingSpotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SeedData {

    /**
     * Call once at app startup. Seeds the parkingSpots collection if empty.
     * Safe to call multiple times — only writes if collection is empty.
     */
    fun ensureParkingSpotsSeeded(spotRepo: ParkingSpotRepository) {
        CoroutineScope(Dispatchers.IO).launch {
            spotRepo.seedParkingSpotsIfEmpty().collect { /* seed completes silently */ }
        }
    }
}
