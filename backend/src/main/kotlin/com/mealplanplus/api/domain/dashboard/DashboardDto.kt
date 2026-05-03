package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.food.FoodDto
import com.mealplanplus.api.domain.health.HealthMetricDto
import com.mealplanplus.api.domain.log.DailyLogDto

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
)
