package com.mealplanplus.data.healthconnect

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
