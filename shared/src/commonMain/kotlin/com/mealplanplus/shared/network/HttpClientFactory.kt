package com.mealplanplus.shared.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Platform-specific HTTP client factory
 */
expect fun createHttpClient(): HttpClient

/**
 * Shared JSON configuration
 */
val sharedJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

/**
 * Configure HTTP client with common settings
 */
fun HttpClient.configureJson(): HttpClient {
    return HttpClient(this.engine) {
        install(ContentNegotiation) {
            json(sharedJson)
        }
    }
}
