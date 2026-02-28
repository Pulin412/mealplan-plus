package com.mealplanplus.data.remote

import retrofit2.http.*

// ── DTOs ─────────────────────────────────────────────────────────────────────

data class MealFoodItemDto(
    val id: Long = 0, val mealId: Long = 0, val foodId: Long = 0,
    val quantity: Double = 0.0, val unit: String = "GRAM", val notes: String? = null
)
data class MealDto(
    val id: Long = 0, val serverId: String? = null, val firebaseUid: String = "",
    val name: String = "", val slot: String = "Lunch",
    val items: List<MealFoodItemDto> = emptyList(), val updatedAt: Long? = null
)

data class DietMealDto(
    val id: Long = 0, val dietId: Long = 0, val mealId: Long = 0,
    val dayOfWeek: Int = 0, val slot: String = "Lunch", val instructions: String? = null
)
data class DietDto(
    val id: Long = 0, val serverId: String? = null, val firebaseUid: String = "",
    val name: String = "", val description: String? = null,
    val meals: List<DietMealDto> = emptyList(), val tagIds: List<Long> = emptyList(),
    val updatedAt: Long? = null
)

data class HealthMetricDto(
    val id: Long = 0, val serverId: String? = null, val firebaseUid: String = "",
    val type: String = "", val subType: String? = null, val value: Double = 0.0,
    val secondaryValue: Double? = null, val unit: String = "",
    val recordedAt: Long? = null, val updatedAt: Long? = null
)

data class GroceryItemDto(
    val id: Long = 0, val groceryListId: Long = 0, val foodId: Long? = null,
    val name: String = "", val quantity: Double = 1.0, val unit: String = "GRAM",
    val category: String? = null, val done: Boolean = false
)
data class GroceryListDto(
    val id: Long = 0, val serverId: String? = null, val firebaseUid: String = "",
    val name: String = "", val dietId: Long? = null,
    val items: List<GroceryItemDto> = emptyList(), val updatedAt: Long? = null
)

// ── Sync request / response ───────────────────────────────────────────────────

data class SyncPushRequest(
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList()
)

data class SyncPushResponse(val accepted: Int)

data class SyncPullResponse(
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList(),
    val serverTime: String? = null
)

// ── Retrofit interface ────────────────────────────────────────────────────────

interface MealPlanApi {
    @POST("api/v1/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun pull(@Query("since") since: String): SyncPullResponse
}
