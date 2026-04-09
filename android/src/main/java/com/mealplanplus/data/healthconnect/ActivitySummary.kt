package com.mealplanplus.data.healthconnect

import java.time.LocalDate

/**
 * Aggregated activity data for a single day sourced from Android Health Connect.
 * Populated when the user has connected Health Connect and granted permissions.
 */
data class ActivitySummary(
    val stepsToday: Long = 0,
    val caloriesBurnedToday: Int = 0,
    /** True only when HC is available, installed, and all permissions are granted. */
    val isConnected: Boolean = false
)

/** One day of activity history used by the Health screen Activity tab. */
data class ActivityDaySummary(
    val date: LocalDate,
    val steps: Long,
    val caloriesBurned: Int
)
