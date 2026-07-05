package com.parkinglksnext.network

import com.google.gson.annotations.SerializedName

// ── Chat ─────────────────────────────────────────────────────────────

data class MessageEntry(
    @SerializedName("role") val role: String,       // "user" or "assistant"
    @SerializedName("content") val content: String
)

data class ChatRequest(
    @SerializedName("message") val message: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("conversation_history") val conversationHistory: List<MessageEntry> = emptyList()
)

data class ReservationContextDto(
    @SerializedName("vehicle_type") val vehicleType: String = "",
    @SerializedName("date") val date: String = "",
    @SerializedName("start_time") val startTime: String = "",
    @SerializedName("end_time") val endTime: String = ""
)

data class ChatResponse(
    @SerializedName("reply") val reply: String,
    @SerializedName("recommendations") val recommendations: List<SpotRecommendationDto> = emptyList(),
    @SerializedName("reservation_context") val reservationContext: ReservationContextDto? = null
)

data class SpotRecommendationDto(
    @SerializedName("number") val number: Int,
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String
)

// ── Reservation ──────────────────────────────────────────────────────

data class ReserveRequest(
    @SerializedName("spot_id") val spotId: String,
    @SerializedName("vehicle_id") val vehicleId: String,
    @SerializedName("date") val date: String,
    @SerializedName("start_time") val startTime: String,
    @SerializedName("end_time") val endTime: String,
    @SerializedName("user_id") val userId: String
)

data class ReserveResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("reservation_id") val reservationId: String? = null,
    @SerializedName("error") val error: String? = null
)
