package com.mealplanplus.data.remote

import retrofit2.http.*

// ── DTOs ─────────────────────────────────────────────────────────────────────

data class FoodDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100: Double = 0.0,
    val proteinPer100: Double = 0.0,
    val carbsPer100: Double = 0.0,
    val fatPer100: Double = 0.0,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null,
    val isSystemFood: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Long? = null
)

data class LoggedFoodDto(
    val id: Long = 0,
    val dailyLogId: Long = 0,
    val foodId: Long = 0,
    val mealSlot: String = "Lunch",
    val quantity: Double = 0.0,
    val unit: String = "GRAM"
)

data class DailyLogDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val date: String = "",   // ISO date "yyyy-MM-dd" — backend deserialises to LocalDate
    val notes: String? = null,
    val loggedFoods: List<LoggedFoodDto> = emptyList(),
    val updatedAt: Long? = null
)

data class MealFoodItemDto(
    val id: Long = 0, val mealId: Long = 0, val foodId: Long = 0,
    val foodServerId: String? = null,
    val quantity: Double = 0.0, val unit: String = "GRAM", val notes: String? = null
)
data class MealDto(
    val id: Long = 0, val serverId: String? = null, val firebaseUid: String = "",
    val name: String = "",
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

data class WorkoutSetSyncDto(
    val exerciseId: Long = 0,
    val setNumber: Int = 0,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val distanceMeters: Double? = null,
    val notes: String? = null
)

data class WorkoutSessionSyncDto(
    val id: Long = 0,
    val serverId: String? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val date: String = "",          // ISO date "yyyy-MM-dd"
    val durationMinutes: Int? = null,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val sets: List<WorkoutSetSyncDto> = emptyList(),
    val updatedAt: Long? = null
)

/** Signals a server-side delete. Android should purge the matching local record. */
data class TombstoneDto(
    val entityType: String = "",
    val serverId: String = "",
    val deletedAt: Long = 0L
)

// ── Sync request / response ───────────────────────────────────────────────────

data class SyncPushRequest(
    val foods: List<FoodDto> = emptyList(),
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList(),
    val dailyLogs: List<DailyLogDto> = emptyList(),
    val workoutSessions: List<WorkoutSessionSyncDto> = emptyList()
)

data class SyncPushResponse(
    val accepted: Int,
    val foods: List<FoodDto> = emptyList(),
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList(),
    val dailyLogs: List<DailyLogDto> = emptyList(),
    val workoutSessions: List<WorkoutSessionSyncDto> = emptyList()
)

data class SyncPullResponse(
    val foods: List<FoodDto> = emptyList(),
    val meals: List<MealDto> = emptyList(),
    val diets: List<DietDto> = emptyList(),
    val healthMetrics: List<HealthMetricDto> = emptyList(),
    val groceryLists: List<GroceryListDto> = emptyList(),
    val dailyLogs: List<DailyLogDto> = emptyList(),
    val workoutSessions: List<WorkoutSessionSyncDto> = emptyList(),
    val tombstones: List<TombstoneDto> = emptyList(),
    /** Server clock at response time — store as lastSyncTimestamp for next pull's `since`. */
    val serverTime: Long? = null
)

// ── Retrofit interface ────────────────────────────────────────────────────────

interface MealPlanApi {
    @POST("api/v1/sync/push")
    suspend fun push(@Body request: SyncPushRequest): SyncPushResponse

    @GET("api/v1/sync/pull")
    suspend fun pull(@Query("since") since: String): SyncPullResponse
}
