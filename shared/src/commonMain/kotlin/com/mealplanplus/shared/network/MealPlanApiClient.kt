package com.mealplanplus.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class MealDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val slot: String = "LUNCH",
    val updatedAt: Long? = null
)

@Serializable
data class DietDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val description: String? = null,
    val updatedAt: Long? = null
)

@Serializable
data class HealthMetricDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val date: String = "",
    val metricType: String? = null,
    val value: Double = 0.0,
    val secondaryValue: Double? = null,
    val subType: String? = null,
    val notes: String? = null,
    val updatedAt: Long? = null
)

@Serializable
data class GroceryListDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val startDate: String? = null,
    val endDate: String? = null,
    val updatedAt: Long? = null
)

@Serializable
data class SyncPushRequest(
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList()
)

@Serializable
data class SyncPushResponse(
    val accepted: Int = 0,
    val conflicts: List<String> = emptyList()
)

@Serializable
data class SyncPullResponse(
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList(),
    val serverTime: String? = null
)

// ── Client ────────────────────────────────────────────────────────────────────

class MealPlanApiClient(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val tokenProvider: () -> String?   // returns current Firebase ID token
) {
    private val httpClient = createHttpClient()

    suspend fun push(request: SyncPushRequest): Result<SyncPushResponse> = runCatching {
        val token = tokenProvider() ?: error("Not authenticated")
        httpClient.post("$baseUrl/api/v1/sync/push") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun pull(since: String): Result<SyncPullResponse> = runCatching {
        val token = tokenProvider() ?: error("Not authenticated")
        httpClient.get("$baseUrl/api/v1/sync/pull") {
            bearerAuth(token)
            parameter("since", since)
        }.body()
    }

    fun close() = httpClient.close()

    companion object {
        // Override at app startup via SyncRepository.configure()
        const val DEFAULT_BASE_URL = "https://api.mealplanplus.com"
    }
}
