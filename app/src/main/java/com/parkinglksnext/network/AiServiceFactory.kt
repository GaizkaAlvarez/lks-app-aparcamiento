package com.parkinglksnext.network

import com.parkinglksnext.BuildConfig

/**
 * Factory that returns the correct AiService implementation
 * based on the AI_PROVIDER setting in local.properties.
 *
 * Supported values:
 *   - "gemini"   → GeminiService (default)
 *   - "deepseek" → DeepSeekService
 */
object AiServiceFactory {

    fun create(): AiService {
        return when (BuildConfig.AI_PROVIDER.lowercase().trim()) {
            "deepseek" -> DeepSeekService
            else -> GeminiService  // default + "gemini"
        }
    }
}
