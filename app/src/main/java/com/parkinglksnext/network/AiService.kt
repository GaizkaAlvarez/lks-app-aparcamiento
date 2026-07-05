package com.parkinglksnext.network

import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.repository.UserRepository

/**
 * Common interface for AI chat providers (Gemini, DeepSeek, etc.).
 * Each implementation handles provider-specific API format and function calling.
 */
interface AiService {

    data class ReservationConfirmation(
        val spotNumber: Int,
        val spotType: String,
        val date: String,
        val startTime: String,
        val endTime: String
    )

    data class ChatResult(
        val reply: String,
        val recommendations: List<SpotRecommendationDto>,
        val reservationContext: ReservationContextDto?,
        val reservationConfirmed: ReservationConfirmation? = null
    )

    suspend fun chat(
        message: String,
        userId: String,
        conversationHistory: List<MessageEntry>,
        spotRepo: ParkingSpotRepository,
        reservationRepo: ReservationRepository,
        userRepo: UserRepository,
    ): ChatResult
}
