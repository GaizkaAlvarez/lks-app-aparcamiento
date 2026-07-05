package com.parkinglksnext.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.parkinglksnext.ParkingWidgetProvider
import com.parkinglksnext.Reservation
import com.parkinglksnext.network.AiServiceFactory
import com.parkinglksnext.network.MessageEntry
import com.parkinglksnext.network.ReservationContextDto
import com.parkinglksnext.network.SpotRecommendationDto
import com.parkinglksnext.repository.AuthRepository
import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.repository.UserRepository
import com.parkinglksnext.util.NotificationHelper
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ChatViewModel"
    }

    private val authRepo = AuthRepository()
    private val userRepo = UserRepository()
    private val spotRepo = ParkingSpotRepository()
    private val reservationRepo = ReservationRepository()

    data class ChatMessage(
        val id: String = UUID.randomUUID().toString(),
        val text: String,
        val isUser: Boolean,
        val recommendations: List<SpotRecommendationDto> = emptyList(),
        val isError: Boolean = false,
        val isSuccess: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ChatUiState(
        val messages: List<ChatMessage> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val reservationContext: ReservationContextDto? = null
    )

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Wait for auth UID to be available (new users may need a moment)
            var uid: String? = null
            for (attempt in 1..5) {
                uid = authRepo.getCurrentUser()?.uid
                if (uid != null) break
                kotlinx.coroutines.delay(800L * attempt)
            }
            if (uid == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        messages = listOf(ChatMessage(
                            text = "👋 ¡Hola! Soy tu asistente de aparcamiento. " +
                                    "Cuéntame qué necesitas y te ayudo.\n\n" +
                                    "Por ejemplo: \"Necesito aparcar mi coche mañana de 9 a 14\"",
                            isUser = false
                        ))
                    )
                }
                return@launch
            }

            // Reactively observe the user profile.
            // When vehicles are added/removed the recommendation refreshes automatically.
            userRepo.getUserProfile(uid).collect { resource ->
                when (resource) {
                    is Resource.Success -> {
                        val profile = resource.data
                        val vehicles = profile.vehicles

                        // Skip if the user already started a conversation (don't interrupt)
                        if (_uiState.value.messages.any { it.isUser }) return@collect

                        if (vehicles.isEmpty()) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    messages = listOf(ChatMessage(
                                        text = "👋 ¡Hola! Soy tu asistente de aparcamiento.\n\n" +
                                                "No tienes vehículos registrados. Añade uno en tu perfil para que pueda " +
                                                "buscarte plaza, o dime qué tipo de vehículo tienes y te ayudo.",
                                        isUser = false
                                    ))
                                )
                            }
                        } else {
                            computeInitialRecommendation(uid, profile)
                        }
                    }
                    is Resource.Error -> {
                        // "Perfil no encontrado" — profile not yet created (e.g. fresh Google sign-in).
                        // Wait silently; the snapshot listener will fire again once the profile is saved.
                    }
                    is Resource.Loading -> {
                        if (_uiState.value.messages.isEmpty()) {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                    }
                }
            }
        }
    }

    // ── Initial deterministic recommendation (no AI) ────────────

    /**
     * Compute the initial parking recommendation for a known profile.
     * Called reactively from init whenever the profile changes (e.g. vehicles added).
     */
    private suspend fun computeInitialRecommendation(uid: String, profile: com.parkinglksnext.UserProfile) {
        val vehicles = profile.vehicles
        if (vehicles.isEmpty()) return

        _uiState.update {
            it.copy(
                isLoading = true,
                messages = listOf(ChatMessage(text = "🔍 Buscando la mejor plaza para ti...", isUser = false))
            )
        }

        try {
            val vehicle = vehicles.first()
            val vehicleType = vehicle.type

            val now = LocalTime.now()
            val today = LocalDate.now()
            val parkingCloseHour = 22

            val (date, startTime, endTime) = if (now.hour < parkingCloseHour) {
                val startHour = maxOf(now.hour + 1, 6)
                val endHour = minOf(startHour + 1, parkingCloseHour)
                Triple(
                    today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    String.format("%02d:00", startHour),
                    String.format("%02d:55", endHour)
                )
            } else {
                val tomorrow = today.plusDays(1)
                Triple(tomorrow.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), "08:00", "09:55")
            }

            // Wait for Firestore spots to load (async snapshot listener)
            var compatibleSpots = spotRepo.getAvailableSpotsForVehicle(vehicleType)
            if (compatibleSpots.isEmpty()) {
                kotlinx.coroutines.delay(1500)
                compatibleSpots = spotRepo.getAvailableSpotsForVehicle(vehicleType)
            }
            val conflictingIds = reservationRepo.getConflictingSpotIds(date, startTime, endTime)
            val freeSpots = compatibleSpots.filter { it.id !in conflictingIds }.sortedBy { it.number }

            val typeName = when (vehicleType) {
                "electric" -> "eléctrico"
                "motorcycle" -> "moto"
                else -> "común"
            }

            if (freeSpots.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reservationContext = ReservationContextDto(vehicleType, date, startTime, endTime),
                        messages = listOf(ChatMessage(
                            text = "👋 ¡Hola! He buscado una plaza para tu vehículo $typeName " +
                                    "(${vehicle.licensePlate}) para hoy a las $startTime, " +
                                    "pero no hay plazas disponibles en ese horario. 😕\n\n" +
                                    "Puedes pedirme que busque en otro horario, otro día, " +
                                    "o con otro vehículo. ¡Dime qué prefieres!",
                            isUser = false
                        ))
                    )
                }
            } else {
                val bestSpot = freeSpots.firstOrNull { it.type == vehicleType } ?: freeSpots.first()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        reservationContext = ReservationContextDto(vehicleType, date, startTime, endTime),
                        messages = listOf(ChatMessage(
                            text = "👋 ¡Hola! He encontrado la mejor plaza para tu vehículo " +
                                    "$typeName (${vehicle.licensePlate}):\n\n" +
                                    "📅 $date · 🕐 $startTime – $endTime",
                            isUser = false,
                            recommendations = listOf(SpotRecommendationDto(bestSpot.number, bestSpot.type, bestSpot.id))
                        ))
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing initial recommendation", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    messages = listOf(ChatMessage(
                        text = "👋 ¡Hola! Soy tu asistente de aparcamiento. " +
                                "Cuéntame qué necesitas y te ayudo.\n\n" +
                                "Por ejemplo: \"Necesito aparcar mi coche eléctrico mañana de 9 a 14\"",
                        isUser = false
                    ))
                )
            }
        }
    }

    // ── AI Chat (Gemini direct) ─────────────────────────────────

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(text = trimmed, isUser = true)
        _uiState.update { state ->
            state.copy(messages = state.messages + userMessage, isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                val uid = authRepo.getCurrentUser()?.uid ?: kotlin.run {
                    // Retry once — auth state may not have propagated yet for new users
                    kotlinx.coroutines.delay(500)
                    authRepo.getCurrentUser()?.uid ?: return@launch
                }

                val currentMessages = _uiState.value.messages
                val history = currentMessages
                    .filter { !it.isError && !it.isSuccess }
                    .takeLast(10)
                    .map { msg ->
                        MessageEntry(
                            role = if (msg.isUser) "user" else "assistant",
                            content = msg.text
                        )
                    }

                Log.d(TAG, "Sending to AI (provider=${com.parkinglksnext.BuildConfig.AI_PROVIDER}): $trimmed")
                val aiService = AiServiceFactory.create()
                val result = aiService.chat(
                    message = trimmed,
                    userId = uid,
                    conversationHistory = history,
                    spotRepo = spotRepo,
                    reservationRepo = reservationRepo,
                    userRepo = userRepo,
                )

                Log.d(TAG, "AI reply: ${result.reply.take(80)}..., recommendations: ${result.recommendations.size}")
                val aiMessage = ChatMessage(
                    text = result.reply,
                    isUser = false,
                    recommendations = result.recommendations
                )

                _uiState.update { state ->
                    state.copy(
                        messages = state.messages + aiMessage,
                        isLoading = false,
                        error = null,
                        reservationContext = result.reservationContext ?: state.reservationContext
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Gemini", e)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        messages = state.messages + ChatMessage(
                            text = "Error: ${e.javaClass.simpleName}: ${e.localizedMessage ?: "desconocido"}\n\n" +
                                    "Verifica tu conexión a internet y que GEMINI_API_KEY esté configurada en local.properties.",
                            isUser = false,
                            isError = true
                        )
                    )
                }
            }
        }
    }

    // ── Reserve spot (Firestore directly) ───────────────────────

    fun reserveSpot(spot: SpotRecommendationDto) {
        val ctx = _uiState.value.reservationContext
        val uid = authRepo.getCurrentUser()?.uid ?: run {
            addErrorMessage("Error: no se pudo identificar al usuario.")
            return
        }

        viewModelScope.launch {
            try {
                val vehicleType = ctx?.vehicleType ?: spot.type
                val vehicleInfo = findCompatibleVehicle(uid, vehicleType)

                if (vehicleInfo == null) {
                    addErrorMessage("No tienes un vehículo compatible con esta plaza. Añade uno en tu perfil.")
                    return@launch
                }

                val date = ctx?.date ?: run {
                    addErrorMessage("Falta la fecha. Pide al asistente que busque plazas primero.")
                    return@launch
                }
                val startTime = ctx?.startTime ?: "09:00"
                val endTime = ctx?.endTime ?: "10:00"

                // Validate max 8 hours
                val startMin = startTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                val endMin = endTime.split(":").let { it[0].toInt() * 60 + it[1].toInt() }
                val duration = endMin - startMin
                if (duration > 480) {
                    addErrorMessage("La duración máxima es de 8 horas. Pide al asistente un horario más corto.")
                    return@launch
                }

                // Check conflicts first
                val hasConflict = reservationRepo.hasConflictingReservation(
                    spotId = spot.id, date = date, startTime = startTime, endTime = endTime
                )
                if (hasConflict) {
                    addErrorMessage("Esta plaza ya está reservada en ese horario. Pide al asistente otra opción.")
                    return@launch
                }

                val (vehicleId, vehiclePlate) = vehicleInfo
                val reservation = Reservation(
                    userId = uid,
                    vehicleId = vehicleId,
                    vehiclePlate = vehiclePlate,
                    spotId = spot.id,
                    spotNumber = spot.number,
                    spotType = spot.type,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    status = "active"
                )

                _uiState.update { it.copy(isLoading = true) }
                val result = reservationRepo.createReservation(reservation)

                when (result) {
                    is Resource.Success -> {
                        val ctx = getApplication<Application>()
                        ParkingWidgetProvider.notifyDataChanged(ctx)
                        NotificationHelper.scheduleStartReminder(
                            ctx, reservation.id, date, startTime, spot.number
                        )
                        NotificationHelper.scheduleExpiryReminder(
                            ctx, reservation.id, date, endTime, spot.number
                        )

                        val spotEmoji = when (spot.type) {
                            "electric" -> "⚡"; "motorcycle" -> "🏍"; else -> "🚗"
                        }
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                messages = state.messages + ChatMessage(
                                    text = "✅ ¡Reserva confirmada!\n\n" +
                                            "Plaza ${spot.number} $spotEmoji\n" +
                                            "Fecha: $date\n" +
                                            "Horario: $startTime – $endTime\n\n" +
                                            "Puedes verla en la sección \"Reservas\".",
                                    isUser = false,
                                    isSuccess = true
                                )
                            )
                        }
                    }
                    is Resource.Error -> {
                        addErrorMessage(result.message)
                    }
                    is Resource.Loading -> {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating reservation", e)
                addErrorMessage("Error al crear la reserva: ${e.localizedMessage ?: "Inténtalo de nuevo."}")
            }
        }
    }

    private fun addErrorMessage(text: String) {
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                messages = state.messages + ChatMessage(text = text, isUser = false, isError = true)
            )
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private suspend fun findCompatibleVehicle(userId: String, vehicleType: String): Pair<String, String>? {
        return try {
            val profile = try {
                var result: com.parkinglksnext.UserProfile? = null
                userRepo.getUserProfile(userId).first { it is Resource.Success || it is Resource.Error }
                    .let { resource -> if (resource is Resource.Success) result = resource.data }
                result
            } catch (_: Exception) { null }
            val vehicles = profile?.vehicles ?: return null
            if (vehicles.isEmpty()) return null
            val match = vehicles.firstOrNull { v -> v.type.equals(vehicleType, ignoreCase = true) }
            val fallback = if (vehicleType in listOf("comun", "combustion")) {
                vehicles.firstOrNull { v ->
                    v.type.equals("electric", ignoreCase = true) || v.type.equals("comun", ignoreCase = true) || v.type.equals("combustion", ignoreCase = true)
                }
            } else vehicles.firstOrNull()
            val vehicle = match ?: fallback ?: return null
            Pair(vehicle.id, vehicle.licensePlate)
        } catch (_: Exception) { null }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun retryLastMessage() {
        val messages = _uiState.value.messages
        val lastUserMsg = messages.findLast { it.isUser } ?: return
        _uiState.update { it.copy(messages = messages.filter { !it.isError }) }
        sendMessage(lastUserMsg.text)
    }
}
