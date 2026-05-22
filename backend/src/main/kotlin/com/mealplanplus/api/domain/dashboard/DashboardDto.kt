package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.food.FoodDto
import com.mealplanplus.api.domain.health.HealthMetricDto
import com.mealplanplus.api.domain.log.DailyLogDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dashboard aggregate — all data needed for the home screen in one request")
data class DashboardDto(
    val todayLog: DailyLogDto?,
    val recentLogs: List<DailyLogDto>,
    val foods: List<FoodDto>,
    val dietCount: Long,
    val latestWeight: HealthMetricDto?,
    val currentStreak: Int = 0,
    val weeklyCalories: List<DayCalories> = emptyList(),
    val todayMacros: MacroTotals = MacroTotals(),
    /** Today's planned diet with meals and ingredients, or null if no day plan set. */
    val todayPlan: TodayPlanDto? = null,
    /** Steps recorded today via Health Connect, or null if not available. */
    val todaySteps: Double? = null,
    /** Calories burned today via Health Connect, or null if not available. */
    val todayCaloriesBurned: Double? = null,
)

@Schema(description = "Calories logged on a specific date")
data class DayCalories(val date: String, val calories: Double)

@Schema(description = "Macro totals (protein / carbs / fat) in grams")
data class MacroTotals(
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val calories: Double = 0.0
)

data class TodayMealItemDto(
    val foodId: Long,
    val foodName: String,
    val quantity: Double,
    val unit: String,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val glycemicIndex: Int?,
    val notes: String?
)

data class TodaySlotDto(
    val slot: String,
    val mealId: Long,
    val mealName: String,
    val items: List<TodayMealItemDto>
)

data class TodayPlanDto(
    val dietId: Long,
    val dietName: String,
    val description: String?,
    val targetCalories: Double?,
    val targetProtein: Double?,
    val targetCarbs: Double?,
    val targetFat: Double?,
    val avgGlycemicIndex: Int?,
    val slots: List<TodaySlotDto>
)
