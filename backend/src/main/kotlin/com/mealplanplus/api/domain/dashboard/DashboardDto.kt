package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.food.FoodDto
import com.mealplanplus.api.domain.health.HealthMetricDto
import com.mealplanplus.api.domain.log.DailyLogDto
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Dashboard aggregate — all data needed for the home screen in one request")
data class DashboardDto(
    /** Today's log with logged foods, or null if nothing logged today. */
    val todayLog: DailyLogDto?,
    /** Last 5 logs sorted newest-first (includes today if present). */
    val recentLogs: List<DailyLogDto>,
    /** Only the foods referenced by todayLog + recentLogs — not all 650+ system foods. */
    val foods: List<FoodDto>,
    /** Total number of diets for this user. */
    val dietCount: Long,
    /** Most recent WEIGHT metric, or null if none recorded. */
    val latestWeight: HealthMetricDto?,
    /** Consecutive days with at least one logged food entry, ending today (or yesterday). */
    val currentStreak: Int = 0,
    /** Calories per day for the last 7 days — list of {date, calories} pairs, oldest first. */
    val weeklyCalories: List<DayCalories> = emptyList(),
    /** Today's macro totals from logged foods. */
    val todayMacros: MacroTotals = MacroTotals(),
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
