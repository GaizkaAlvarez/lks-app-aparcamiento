package com.parkinglksnext.network

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatApiService {

    @POST("api/chat")
    suspend fun sendMessage(
        @Header("Authorization") authToken: String,
        @Body request: ChatRequest
    ): ChatResponse

    @POST("api/reservations")
    suspend fun createReservation(
        @Header("Authorization") authToken: String,
        @Body request: ReserveRequest
    ): ReserveResponse
}
