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
 * Gemini API client with function calling (tool use).
 * Implements the AiService interface.
 */
object GeminiService : AiService {

    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
    private const val MAX_ITERATIONS = 3

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ── System prompt (same as the Python backend) ──────────────

    private val SYSTEM_PROMPT = """
Eres un asistente de aparcamiento para la app "LKS Parking". Tu trabajo es ayudar a encontrar plazas usando LAS HERRAMIENTAS disponibles. NO hagas preguntas que puedas responder con las herramientas.

## REGLAS OBLIGATORIAS:

1. **USA LAS HERRAMIENTAS PRIMERO, NO PREGUNTES.** Tienes get_user_vehicles y get_available_spots. Úsalas para obtener datos. NUNCA preguntes por el user_id ni datos que puedas obtener con las herramientas. El user_id se rellena automáticamente.
   - Si el usuario dice "mi coche" o "mi vehículo", usa get_user_vehicles SIN parámetros.

2. **Tipos de vehículo y plaza**: "comun" (coche normal/gasolina), "electric" (eléctrico/EV), "motorcycle" (moto).

3. **Compatibilidad**:
   - común → solo plazas de tipo "comun"
   - eléctrico → plazas "electric" + plazas "comun"
   - moto → solo plazas de tipo "motorcycle"
   Usa la herramienta get_available_spots para ver las plazas reales — NO inventes números.
   **IMPORTANTE**: si el usuario pide "cargador", "enchufe", "carga" o es un coche eléctrico, PRIORIZA las plazas de tipo "electric". La herramienta ya devuelve las plazas ordenadas (eléctricas primero para vehículos eléctricos).

4. **Formato 24h**: 8 = 08:00 AM, 22 = 10:00 PM. El parking abre 06:00-22:55.

5. **Formato fecha**: yyyy-MM-dd. Calcula fechas relativas ("viernes que viene", "mañana") a partir de la fecha actual.

6. **Duración máxima: 8 horas.** Si el usuario pide más de 8h (ej: de 7:00 a 22:55 = 15h 55min), adviértele que el máximo son 8h y sugiérele una duración válida.

7. **SIEMPRE escoge UNA sola plaza** (la de menor número). No des listas. Di: "La plaza 3 es la mejor opción. ¿La reservo?"

8. **NUNCA inventes plazas ni datos.** Solo recomiendas lo que devuelvan las herramientas.

9. **Sé conciso.**

10. **Si no hay plazas**, sugiere alternativas: otro día, otra hora, otro vehículo.
""".trimIndent()

    // ── Function declarations (same tools as Python backend) ────

    private fun buildFunctionDeclarations(): JsonArray {
        return JsonArray().apply {
            // get_available_spots
            add(JsonObject().apply {
                add("functionDeclarations", JsonArray().apply {
                    add(JsonObject().apply {
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
                    // get_user_vehicles
                    add(JsonObject().apply {
                        addProperty("name", "get_user_vehicles")
                        addProperty("description", "Obtiene la lista de vehículos del usuario actual. No necesita parámetros — el user_id se rellena automáticamente.")
                        add("parameters", JsonObject().apply {
                            addProperty("type", "object")
                            add("properties", JsonObject().apply {})
                        })
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
        val apiKey = BuildConfig.GEMINI_API_KEY
        val model = BuildConfig.GEMINI_MODEL.ifBlank { "gemini-2.0-flash" }

        if (apiKey.isBlank()) {
            return@withContext AiService.ChatResult(
                reply = "Error: no se ha configurado GEMINI_API_KEY.\n\nAñádela en local.properties:\nGEMINI_API_KEY=tu_clave",
                recommendations = emptyList(), reservationContext = null
            )
        }

        val url = "$BASE_URL/models/$model:generateContent?key=$apiKey"
        val functions = buildFunctionDeclarations()

        // Build contents array from conversation history + new message
        val contents = JsonArray()

        // Add system instruction as first user message (Gemini API doesn't have system_instruction in v1beta for generateContent)
        // Actually, let me prepend the system prompt to the first user message
        var systemPrepended = false

        for (entry in conversationHistory) {
            if (!systemPrepended) {
                // Prepend system prompt to the first message
                val enhancedContent = "$SYSTEM_PROMPT\n\n---\n\n${entry.content}"
                contents.add(buildContentEntry(entry.role, enhancedContent))
                systemPrepended = true
            } else {
                contents.add(buildContentEntry(entry.role, entry.content))
            }
        }

        if (!systemPrepended) {
            // No history — system prompt goes with the new message
            contents.add(buildContentEntry("user", "$SYSTEM_PROMPT\n\n---\n\n$message"))
        } else {
            contents.add(buildContentEntry("user", message))
        }

        var lastReservationContext: ReservationContextDto? = null
        val allSpotResults = mutableListOf<SpotRecommendationDto>()

        // Agent loop
        var currentContents = contents
        for (iteration in 0..MAX_ITERATIONS) {
            val requestBody = JsonObject().apply {
                add("contents", currentContents)
                add("tools", functions)
            }

            Log.d(TAG, "Gemini request iteration $iteration")
            val responseJson = callGeminiApi(url, requestBody)
                ?: return@withContext AiService.ChatResult(
                    reply = "Error al contactar con Gemini. Verifica tu conexión a internet y que la API key sea válida.",
                    recommendations = emptyList(), reservationContext = null
                )

            // Check for errors
            val error = responseJson.getAsJsonObject("error")
            if (error != null) {
                val msg = error.get("message")?.asString ?: "Error desconocido"
                Log.e(TAG, "Gemini API error: $msg")
                return@withContext AiService.ChatResult(
                    reply = "Error de Gemini: $msg",
                    recommendations = emptyList(), reservationContext = null
                )
            }

            val candidates = responseJson.getAsJsonArray("candidates") ?: JsonArray()
            if (candidates.isEmpty) {
                return@withContext AiService.ChatResult(
                    reply = "Gemini no generó respuesta. Inténtalo de nuevo.",
                    recommendations = emptyList(), reservationContext = null
                )
            }

            val candidate = candidates[0].asJsonObject
            val content = candidate.getAsJsonObject("content") ?: continue
            val parts = content.getAsJsonArray("parts") ?: JsonArray()

            // Check for function calls
            val functionCalls = mutableListOf<Pair<String, JsonObject>>()
            val textParts = mutableListOf<String>()

            for (part in parts) {
                val fnCall = part.asJsonObject.getAsJsonObject("functionCall")
                if (fnCall != null) {
                    val name = fnCall.get("name")?.asString ?: continue
                    val args = fnCall.getAsJsonObject("args") ?: JsonObject()
                    functionCalls.add(name to args)
                }
                val text = part.asJsonObject.get("text")?.asString
                if (text != null && text.isNotBlank()) {
                    textParts.add(text)
                }
            }

            if (functionCalls.isEmpty()) {
                // Final response — no more function calls
                val reply = textParts.joinToString("\n\n").ifBlank { "Lo siento, no pude procesar tu solicitud." }

                return@withContext AiService.ChatResult(
                    reply = reply,
                    recommendations = allSpotResults, reservationContext = lastReservationContext
                )
            }

            // Execute function calls
            val functionResponses = JsonArray()
            for ((name, args) in functionCalls) {
                Log.d(TAG, "Executing function: $name with args: $args")

                when (name) {
                    "get_available_spots" -> {
                        val result = executeGetAvailableSpots(
                            vehicleType = args.get("vehicle_type")?.asString ?: "comun",
                            date = args.get("date")?.asString ?: "",
                            startTime = args.get("start_time")?.asString ?: "08:00",
                            endTime = args.get("end_time")?.asString ?: "09:55",
                            spotRepo = spotRepo,
                            reservationRepo = reservationRepo
                        )
                        allSpotResults.clear()
                        if (result.isNotEmpty()) allSpotResults.add(result.first())

                        lastReservationContext = ReservationContextDto(
                            vehicleType = args.get("vehicle_type")?.asString ?: "",
                            date = args.get("date")?.asString ?: "",
                            startTime = args.get("start_time")?.asString ?: "",
                            endTime = args.get("end_time")?.asString ?: ""
                        )

                        functionResponses.add(buildFunctionResponse(name, result))
                    }
                    "get_user_vehicles" -> {
                        val result = executeGetUserVehicles(
                            userId = args.get("user_id")?.asString ?: userId,
                            userRepo = userRepo
                        )
                        functionResponses.add(buildFunctionResponse(name, result))
                    }
                    else -> {
                        functionResponses.add(buildFunctionResponse(name, "Unknown function: $name"))
                    }
                }
            }

            // Add model's function call + function responses to contents
            val modelTurn = JsonObject().apply {
                addProperty("role", "model")
                add("parts", JsonArray().apply {
                    for ((name, args) in functionCalls) {
                        add(JsonObject().apply {
                            add("functionCall", JsonObject().apply {
                                addProperty("name", name)
                                add("args", args)
                            })
                        })
                    }
                })
            }
            val functionTurn = JsonObject().apply {
                addProperty("role", "function")
                add("parts", functionResponses)
            }

            currentContents = JsonArray().apply {
                for (c in currentContents) add(c)  // copy previous
                add(modelTurn)
                add(functionTurn)
            }
        }

        // Exhausted iterations
        AiService.ChatResult(
            reply = "Lo siento, no he podido completar tu solicitud. ¿Puedes intentarlo de nuevo con más detalles?",
            recommendations = allSpotResults, reservationContext = lastReservationContext
        )
    }

    // ── HTTP call ───────────────────────────────────────────────

    private fun callGeminiApi(url: String, body: JsonObject): JsonObject? {
        return try {
            val jsonBody = gson.toJson(body)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return null

            Log.d(TAG, "Gemini response code: ${response.code}")
            if (response.code != 200) {
                Log.e(TAG, "Gemini error body: $responseBody")
            }

            JsonParser.parseString(responseBody).asJsonObject
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed", e)
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

        // Compatibility rules (accept both "comun" and legacy "combustion")
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

        // Check conflicts directly (already on IO dispatcher)
        val conflictingIds = try {
            reservationRepo.getConflictingSpotIds(date, startTime, endTime)
        } catch (_: Exception) {
            emptySet()
        }

        // Return sorted: matching type first, then by number
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
                mapOf(
                    "id" to it.id,
                    "licensePlate" to it.licensePlate,
                    "type" to it.type
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Helpers ──────────────────────────────────────────────────

    private fun buildContentEntry(role: String, text: String): JsonObject {
        val geminiRole = when (role) {
            "assistant" -> "model"
            else -> "user"
        }
        return JsonObject().apply {
            addProperty("role", geminiRole)
            add("parts", JsonArray().apply {
                add(JsonObject().apply { addProperty("text", text) })
            })
        }
    }

    private fun buildFunctionResponse(name: String, result: Any): JsonObject {
        return JsonObject().apply {
            add("functionResponse", JsonObject().apply {
                addProperty("name", name)
                add("response", JsonObject().apply {
                    addProperty("result", gson.toJson(result))
                })
            })
        }
    }
}

