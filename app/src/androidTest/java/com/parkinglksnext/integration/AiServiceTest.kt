package com.parkinglksnext.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.parkinglksnext.BuildConfig
import com.parkinglksnext.network.*
import com.parkinglksnext.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiServiceTest {

    private lateinit var spotRepo: ParkingSpotRepository
    private lateinit var reservationRepo: ReservationRepository
    private lateinit var userRepo: UserRepository

    @Before fun setUp() {
        spotRepo = ParkingSpotRepository()
        reservationRepo = ReservationRepository()
        userRepo = UserRepository()
    }

    // ── Gemini ───────────────────────────────────────────────────

    @Test fun gemini_basic_chat() = runBlocking {
        assumeTrue("Gemini key not configured", BuildConfig.GEMINI_API_KEY.isNotBlank())
        val r = GeminiService.chat("Hola", "test", emptyList(), spotRepo, reservationRepo, userRepo)
        assertThat(r.reply).isNotEmpty()
    }

    @Test fun gemini_responds_in_spanish() = runBlocking {
        assumeTrue("Gemini key not configured", BuildConfig.GEMINI_API_KEY.isNotBlank())
        val r = GeminiService.chat("Hola necesito aparcar", "test", emptyList(), spotRepo, reservationRepo, userRepo)
        assertThat(r.reply).isNotEmpty()
    }

    @Test fun gemini_tool_calling_finds_spots() = runBlocking {
        assumeTrue("Gemini key not configured", BuildConfig.GEMINI_API_KEY.isNotBlank())
        val r = GeminiService.chat(
            "Busca plazas combustion el 2026-07-20 de 08:00 a 09:00",
            "test", emptyList(), spotRepo, reservationRepo, userRepo
        )
        assertThat(r.reply).isNotEmpty()
    }

    @Test fun gemini_single_recommendation() = runBlocking {
        assumeTrue("Gemini key not configured", BuildConfig.GEMINI_API_KEY.isNotBlank())
        val r = GeminiService.chat(
            "Dame la mejor plaza combustion mañana 08:00 a 09:00",
            "test", emptyList(), spotRepo, reservationRepo, userRepo
        )
        assertThat(r.recommendations.size).isAtMost(1)
    }

    // ── DeepSeek ─────────────────────────────────────────────────

    @Test fun deepseek_basic_chat() = runBlocking {
        assumeTrue("DeepSeek key not configured", BuildConfig.DEEPSEEK_API_KEY.isNotBlank())
        val r = DeepSeekService.chat("Hola", "test", emptyList(), spotRepo, reservationRepo, userRepo)
        assertThat(r.reply).isNotEmpty()
    }

    @Test fun deepseek_tool_calling() = runBlocking {
        assumeTrue("DeepSeek key not configured", BuildConfig.DEEPSEEK_API_KEY.isNotBlank())
        val r = DeepSeekService.chat(
            "Busca plazas combustion el 2026-07-20 de 08:00 a 09:00",
            "test", emptyList(), spotRepo, reservationRepo, userRepo
        )
        assertThat(r.reply).isNotEmpty()
    }

    // ── Conversation history ─────────────────────────────────────

    @Test fun conversation_with_history() = runBlocking {
        assumeTrue("Gemini key not configured", BuildConfig.GEMINI_API_KEY.isNotBlank())
        val history = listOf(
            MessageEntry(role = "user", content = "Tengo un coche electrico"),
            MessageEntry(role = "assistant", content = "Entendido. Que horario?")
        )
        val r = GeminiService.chat(
            "Mañana de 12:00 a 13:00", "test", history, spotRepo, reservationRepo, userRepo
        )
        assertThat(r.reply).isNotEmpty()
    }
}
