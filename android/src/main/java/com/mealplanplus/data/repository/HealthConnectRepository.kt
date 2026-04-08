package com.mealplanplus.data.repository

import com.mealplanplus.data.healthconnect.ActivitySummary
import com.mealplanplus.data.healthconnect.HealthConnectManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level repository for Android Health Connect data.
 *
 * This repository abstracts availability and permission checks from callers.
 * All read operations gracefully return empty/default values when Health Connect
 * is unavailable or permissions have not been granted.
 *
 * Data flow:
 *   Fitness watch (Garmin, Fitbit, etc.)
 *     → vendor companion app (Garmin Connect, etc.)
 *       → Android Health Connect store
 *         → HealthConnectManager (SDK reads)
 *           → HealthConnectRepository
 *             → ViewModels (HomeViewModel, SettingsViewModel)
 *
 * Only Steps, Active Calories Burned, and Weight are requested.
 * No network calls are made — all reads are from the local HC data store.
 */
@Singleton
class HealthConnectRepository @Inject constructor(
    private val manager: HealthConnectManager
) {
    /** True when the HC SDK is installed and available on this device. */
    val isAvailable: Boolean get() = manager.isAvailable

    /** The set of permission strings the user must grant. Passed to the permission launcher. */
    val requiredPermissions: Set<String> get() = manager.requiredPermissions

    /** Checks whether all required permissions have been granted. */
    suspend fun hasPermissions(): Boolean = manager.hasAllPermissions()

    /**
     * Returns today's aggregated activity data.
     * Returns a disconnected [ActivitySummary] (all zeros, isConnected = false) when:
     *  - Health Connect is not installed
     *  - Required permissions are not granted
     */
    suspend fun getTodayActivity(): ActivitySummary {
        if (!manager.isAvailable || !manager.hasAllPermissions()) {
            return ActivitySummary(isConnected = false)
        }
        return ActivitySummary(
            stepsToday = manager.readStepsToday(),
            caloriesBurnedToday = manager.readCaloriesBurnedToday().toInt(),
            isConnected = true
        )
    }

    /**
     * Returns the most recent weight recorded in Health Connect (last 30 days), in kg.
     * Returns null if unavailable, no permissions, or no recent records.
     *
     * Used by [com.mealplanplus.ui.screens.settings.SettingsViewModel] to optionally
     * pre-fill the Health screen weight entry.
     */
    suspend fun getLatestWeightKg(): Double? {
        if (!manager.isAvailable || !manager.hasAllPermissions()) return null
        return manager.readLatestWeightKg()
    }
}
