package com.parkinglksnext.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.parkinglksnext.BuildConfig
import com.parkinglksnext.ParkingSpot
import com.parkinglksnext.repository.ParkingSpotRepository
import com.parkinglksnext.repository.ReservationRepository
import com.parkinglksnext.repository.UserRepository
import com.parkinglksnext.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * DeepSeek API client (OpenAI-compatible format) with function calling.
 * Implements the AiService interface.
 *
 * API key and model are read from BuildConfig (injected from local.properties).
 */
object DeepSeekService : AiService {

    private const val TAG = "DeepSeekService"
    private const val BASE_URL = "https://api.deepseek.com/v1"
    private const val MAX_ITERATIONS = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val SYSTEM_PROMPT = """
Eres un asistente de aparcamiento para la app "LKS Parking". Tu trabajo es ayudar a encontrar plazas usando LAS HERRAMIENTAS disponibles. NO hagas preguntas que puedas responder con las herramientas.

## REGLAS OBLIGATORIAS:

1. **USA LAS HERRAMIENTAS PRIMERO, NO PREGUNTES.** Tienes get_user_vehicles, get_available_spots y create_reservation. Úsalas para obtener datos. NUNCA preguntes por el user_id, ID de usuario, matrícula ni datos que puedas obtener con las herramientas. El user_id se rellena automáticamente.
   - Si el usuario dice "mi coche" o "mi vehículo", usa get_user_vehicles SIN parámetros para ver qué vehículos tiene.
   - Si el usuario no especifica hora de fin, asume 1 hora de duración.

2. **Tipos de vehículo y plaza**: "comun" (coche normal/gasolina), "electric" (eléctrico), "motorcycle" (moto).

3. **Compatibilidad**:
   - común → solo plazas de tipo "comun"
   - eléctrico → plazas "electric" + plazas "comun"
   - moto → solo plazas de tipo "motorcycle"
   Usa la herramienta get_available_spots para ver las plazas reales — NO inventes números.
   **IMPORTANTE**: si el usuario pide "cargador", "enchufe", "carga" o es un coche eléctrico, PRIORIZA las plazas de tipo "electric". La herramienta ya devuelve las plazas ordenadas (eléctricas primero para vehículos eléctricos).

4. **Formato 24h**: 8 = 08:00 AM, 22 = 10:00 PM. El parking abre 06:00-22:55.

5. **Formato fecha**: yyyy-MM-dd. Hoy es ${java.time.LocalDate.now()}. Calcula fechas relativas ("viernes que viene", "mañana") a partir de hoy.

6. **Duración máxima: 8 horas.** Si el usuario pide más de 8h (ej: de 7:00 a 22:55 = 15h 55min), adviértele que el máximo son 8h y sugiérele una duración válida.

7. **SIEMPRE escoge UNA sola plaza** (la de menor número). No des listas. Di: "La plaza 3 es la mejor opción. ¿La reservo?"

8. **NUNCA inventes plazas ni datos.** Solo recomiendas lo que devuelvan las herramientas.

9. **Sé conciso.**

10. **Si no hay plazas**, sugiere alternativas: otro día, otra hora, otro vehículo.
""".trimIndent()

    // ── Function declarations (OpenAI format) ──────────────────

    private fun buildTools(): JsonArray {
        return JsonArray().apply {
            add(JsonObject().apply {
                addProperty("type", "function")
                add("function", JsonObject().apply {
                    addProperty("name", "get_available_spots")
                    addProperty("description", "Busca plazas de aparcamiento disponibles compatibles con un tipo de vehículo, fecha y franja horaria.")
                    add("parameters", JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {
                            add("vehicle_type", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Tipo de vehículo: comun, electric, o motorcycle")
                                add("enum", JsonArray().apply {
                                    add("comun"); add("electric"); add("motorcycle")
                                })
                            })
                            add("date", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Fecha en formato yyyy-MM-dd")
                            })
                            add("start_time", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Hora de inicio en formato HH:mm (24h)")
                            })
                            add("end_time", JsonObject().apply {
                                addProperty("type", "string")
                                addProperty("description", "Hora de fin en formato HH:mm (24h)")
                            })
                        })
                        add("required", JsonArray().apply {
                            add("vehicle_type"); add("date"); add("start_time"); add("end_time")
                        })
                    })
                })
            })
            add(JsonObject().apply {
                addProperty("type", "function")
                add("function", JsonObject().apply {
                    addProperty("name", "get_user_vehicles")
                    addProperty("description", "Obtiene la lista de vehículos del usuario actual. No necesita parámetros — el user_id se rellena automáticamente.")
                    add("parameters", JsonObject().apply {
                        addProperty("type", "object")
                        add("properties", JsonObject().apply {})
                    })
                })
            })
        }
    }

    // ── Public API ──────────────────────────────────────────────

    override suspend fun chat(
        message: String,
        userId: String,
        conversationHistory: List<MessageEntry>,
        spotRepo: ParkingSpotRepository,
        reservationRepo: ReservationRepository,
        userRepo: UserRepository,
    ): AiService.ChatResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.DEEPSEEK_API_KEY
        val model = BuildConfig.DEEPSEEK_MODEL.ifBlank { "deepseek-chat" }

        if (apiKey.isBlank()) {
            return@withContext AiService.ChatResult(
                reply = "Error: no se ha configurado DEEPSEEK_API_KEY.\n\nAñádela en local.properties:\nDEEPSEEK_API_KEY=sk-...",
                recommendations = emptyList(),
                reservationContext = null,
            )
        }

        val url = "$BASE_URL/chat/completions"
        val tools = buildTools()

        // Build messages with system prompt
        val messages = JsonArray().apply {
            add(buildSystemMessage())
            for (entry in conversationHistory) {
                add(buildContentEntry(entry.role, entry.content))
            }
            add(buildContentEntry("user", message))
        }

        var lastReservationContext: ReservationContextDto? = null
        val allSpotResults = mutableListOf<SpotRecommendationDto>()

        var currentMessages = messages

        for (iteration in 0..MAX_ITERATIONS) {
            val requestBody = JsonObject().apply {
                addProperty("model", model)
                add("messages", currentMessages)
                add("tools", tools)
                addProperty("temperature", 0.2)
                addProperty("max_tokens", 1024)
            }

            Log.d(TAG, "DeepSeek request iteration $iteration")
            val responseJson = callDeepSeekApi(url, apiKey, requestBody)
                ?: return@withContext AiService.ChatResult(
                    reply = "Error al contactar con DeepSeek. Verifica tu conexión a internet y que la API key sea válida.",
                    recommendations = emptyList(),
                    reservationContext = null
                )

            val error = responseJson.getAsJsonObject("error")
            if (error != null) {
                val msg = error.get("message")?.asString ?: "Error desconocido"
                Log.e(TAG, "DeepSeek API error: $msg")
                return@withContext AiService.ChatResult(
                    reply = "Error de DeepSeek: $msg",
                    recommendations = emptyList(),
                    reservationContext = null
                )
            }

            val choices = responseJson.getAsJsonArray("choices") ?: JsonArray()
            if (choices.isEmpty) {
                return@withContext AiService.ChatResult(
                    reply = "DeepSeek no generó respuesta. Inténtalo de nuevo.",
                    recommendations = emptyList(),
                    reservationContext = null
                )
            }

            val choice = choices[0].asJsonObject
            val msg = choice.getAsJsonObject("message") ?: continue
            val content = msg.get("content")?.asString ?: ""
            val toolCalls = msg.getAsJsonArray("tool_calls")

            if (toolCalls == null || toolCalls.isEmpty) {
                // Final response (no more tool calls)
                val finalReply = content.ifBlank {
                    // If we have recommendations from previous tool calls, show them
                    if (allSpotResults.isNotEmpty()) {
                        val spotsText = allSpotResults.joinToString(", ") { "Plaza ${it.number} (${it.type})" }
                        "He encontrado estas plazas disponibles: $spotsText. ¿Quieres reservar alguna?"
                    } else {
                        "No encontré plazas disponibles para tu búsqueda. ¿Quieres probar con otro horario o tipo de vehículo?"
                    }
                }
                return@withContext AiService.ChatResult(
                    reply = finalReply,
                    recommendations = allSpotResults,
                    reservationContext = lastReservationContext,
                )
            }

            // Execute tool calls
            val toolResults = JsonArray()

            // Add assistant message with tool calls
            currentMessages = JsonArray().apply {
                for (m in currentMessages) add(m)
                val assistantMsg = JsonObject().apply {
                    addProperty("role", "assistant")
                    addProperty("content", content)
                    add("tool_calls", toolCalls)
                }
                add(assistantMsg)
            }

            // Execute each tool and collect results
            for (tc in toolCalls) {
                val tcObj = tc.asJsonObject
                val callId = tcObj.get("id")?.asString ?: ""
                val fn = tcObj.getAsJsonObject("function") ?: continue
                val fnName = fn.get("name")?.asString ?: continue
                val fnArgsStr = fn.get("arguments")?.asString ?: "{}"
                val fnArgs = try {
                    JsonParser.parseString(fnArgsStr).asJsonObject
                } catch (_: Exception) { JsonObject() }

                Log.d(TAG, "Executing function: $fnName with args: $fnArgsStr")

                val result = when (fnName) {
                    "get_available_spots" -> {
                        val spots = executeGetAvailableSpots(
                            vehicleType = fnArgs.get("vehicle_type")?.asString ?: "comun",
                            date = fnArgs.get("date")?.asString ?: "",
                            startTime = fnArgs.get("start_time")?.asString ?: "08:00",
                            endTime = fnArgs.get("end_time")?.asString ?: "09:55",
                            spotRepo = spotRepo,
                            reservationRepo = reservationRepo
                        )
                        allSpotResults.clear()
                        if (spots.isNotEmpty()) allSpotResults.add(spots.first())
                        lastReservationContext = ReservationContextDto(
                            vehicleType = fnArgs.get("vehicle_type")?.asString ?: "",
                            date = fnArgs.get("date")?.asString ?: "",
                            startTime = fnArgs.get("start_time")?.asString ?: "",
                            endTime = fnArgs.get("end_time")?.asString ?: ""
                        )
                        gson.toJson(spots)
                    }
                    "get_user_vehicles" -> {
                        val vehicles = executeGetUserVehicles(
                            userId = fnArgs.get("user_id")?.asString ?: userId,
                            userRepo = userRepo
                        )
                        gson.toJson(vehicles)
                    }
                    else -> "\"Unknown function: $fnName\""
                }

                toolResults.add(JsonObject().apply {
                    addProperty("role", "tool")
                    addProperty("tool_call_id", callId)
                    addProperty("content", result)
                })
            }

            // Add tool results to messages
            currentMessages = JsonArray().apply {
                for (m in currentMessages) add(m)
                for (tr in toolResults) add(tr)
            }
        }

        AiService.ChatResult(
            reply = "Lo siento, no he podido completar tu solicitud. ¿Puedes intentarlo de nuevo con más detalles?",
            recommendations = allSpotResults,
            reservationContext = lastReservationContext,
        )
    }

    // ── HTTP call ───────────────────────────────────────────────

    private fun callDeepSeekApi(url: String, apiKey: String, body: JsonObject): JsonObject? {
        return try {
            val jsonBody = gson.toJson(body)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null

            Log.d(TAG, "DeepSeek response code: ${response.code}")
            if (response.code != 200) {
                Log.e(TAG, "DeepSeek error body: $responseBody")
            }

            JsonParser.parseString(responseBody).asJsonObject
        } catch (e: Exception) {
            Log.e(TAG, "DeepSeek API call failed", e)
            null
        }
    }

    // ── Tool implementations ────────────────────────────────────

    private suspend fun executeGetAvailableSpots(
        vehicleType: String,
        date: String,
        startTime: String,
        endTime: String,
        spotRepo: ParkingSpotRepository,
        reservationRepo: ReservationRepository,
    ): List<SpotRecommendationDto> {
        val vehicle = vehicleType.lowercase().trim()
        val matchingType = when {
            vehicle in listOf("motorcycle", "moto") -> "motorcycle"
            vehicle in listOf("electric", "eléctrico", "electrico") -> "electric"
            else -> "comun"
        }
        val compatibleTypes = when (matchingType) {
            "motorcycle" -> setOf("motorcycle")
            "electric" -> setOf("electric", "comun", "combustion")
            else -> setOf("comun", "combustion")
        }
        val allSpots = spotRepo.spots.value
        val compatibleSpots = allSpots
            .filter { it.type in compatibleTypes && it.available }
        val conflictingIds = try {
            reservationRepo.getConflictingSpotIds(date, startTime, endTime)
        } catch (_: Exception) { emptySet() }
        return compatibleSpots
            .filter { it.id !in conflictingIds }
            .sortedWith(compareBy<ParkingSpot> { if (it.type == matchingType) 0 else 1 }
                .thenBy { it.number })
            .map { SpotRecommendationDto(number = it.number, type = it.type, id = it.id) }
    }

    private suspend fun executeGetUserVehicles(
        userId: String,
        userRepo: UserRepository,
    ): List<Map<String, String>> {
        return try {
            val profile: com.parkinglksnext.UserProfile? = try {
                var result: com.parkinglksnext.UserProfile? = null
                userRepo.getUserProfile(userId).first { it is Resource.Success || it is Resource.Error }
                    .let { resource -> if (resource is Resource.Success) result = resource.data }
                result
            } catch (_: Exception) { null }
            profile?.vehicles?.map {
                mapOf("id" to it.id, "licensePlate" to it.licensePlate, "type" to it.type)
            } ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun buildSystemMessage(): JsonObject {
        return JsonObject().apply {
            addProperty("role", "system")
            addProperty("content", SYSTEM_PROMPT)
        }
    }

    private fun buildContentEntry(role: String, text: String): JsonObject {
        val openAiRole = when (role) {
            "assistant" -> "assistant"
            else -> "user"
        }
        return JsonObject().apply {
            addProperty("role", openAiRole)
            addProperty("content", text)
        }
    }
}
