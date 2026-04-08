package com.mealplanplus.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Low-level wrapper around the Health Connect SDK.
 *
 * Responsibilities:
 *  - Availability detection (SDK installed / not available)
 *  - Permission set declaration (used both for permission requests and checks)
 *  - Individual suspend reads for each data type we care about
 *
 * All callers must first check [isAvailable] and [hasAllPermissions] before reading
 * data.  The repository layer ([com.mealplanplus.data.repository.HealthConnectRepository])
 * handles these checks and exposes safe, aggregated results.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Permissions MealPlan+ requests from Health Connect. */
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    /**
     * True when the Health Connect app is installed and the SDK is available on this device.
     * On Android 14+ Health Connect is part of the OS; on 9–13 users must install the app.
     */
    val isAvailable: Boolean
        get() = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient? by lazy {
        if (isAvailable) HealthConnectClient.getOrCreate(context) else null
    }

    /** Returns true when all [requiredPermissions] have been granted by the user. */
    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
    }

    /** Sum of all step records for today (midnight → now). */
    suspend fun readStepsToday(): Long {
        val c = client ?: return 0L
        val (start, end) = todayRange()
        return c.readRecords(
            ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        ).records.sumOf { it.count }
    }

    /** Sum of all active + resting calories burned today in kcal. */
    suspend fun readCaloriesBurnedToday(): Double {
        val c = client ?: return 0.0
        val (start, end) = todayRange()
        return c.readRecords(
            ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        ).records.sumOf { it.energy.inKilocalories }
    }

    /**
     * Most recent weight entry within the last 30 days, in kilograms.
     * Returns null if no records exist or permissions are unavailable.
     */
    suspend fun readLatestWeightKg(): Double? {
        val c = client ?: return null
        val zone = ZoneId.systemDefault()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
        val start = LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant()
        return c.readRecords(
            ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        ).records.lastOrNull()?.weight?.inKilograms
    }

    private fun todayRange(): Pair<java.time.Instant, java.time.Instant> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
        return start to end
    }
}
