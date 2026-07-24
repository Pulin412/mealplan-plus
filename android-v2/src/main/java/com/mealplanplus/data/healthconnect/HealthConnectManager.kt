package com.mealplanplus.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Today's Health Connect snapshot shown in the Settings card. */
data class HealthConnectSummary(
    val steps: Long = 0,
    val caloriesBurned: Int = 0,
    val latestWeightKg: Double? = null,
)

/**
 * Safe, read-only wrapper around the Health Connect SDK. Reads steps, calories burned, and latest
 * weight from the on-device HC store — **no network, no cloud cost**. Every call catches failures and
 * returns empty/default values, so a missing SDK, a revoked permission, or a manifest gap can never
 * crash a caller (notably `getGrantedPermissions()` throwing on Android 14+ without the
 * VIEW_PERMISSION_USAGE intent filter). HC is built into the OS on Android 14+; 9–13 need the
 * companion app from Play.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** The read permissions MealPlan+ requests — also passed to the permission-request launcher. */
    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
    )

    val isAvailable: Boolean
        get() = runCatching { HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE }
            .getOrDefault(false)

    private val client: HealthConnectClient? by lazy {
        if (isAvailable) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null
    }

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return runCatching { c.permissionController.getGrantedPermissions().containsAll(requiredPermissions) }
            .getOrDefault(false)
    }

    /** Revoke all granted HC permissions (drives the Settings toggle off). No-op on failure. */
    suspend fun revokeAll() {
        runCatching { client?.permissionController?.revokeAllPermissions() }
    }

    /** Today's steps + calories + latest weight (last 30 days). All defaults when ungranted/unavailable. */
    suspend fun readTodaySummary(): HealthConnectSummary {
        val c = client ?: return HealthConnectSummary()
        if (!hasAllPermissions()) return HealthConnectSummary()
        return HealthConnectSummary(
            steps = readStepsToday(c),
            caloriesBurned = readCaloriesToday(c).toInt(),
            latestWeightKg = readLatestWeightKg(c),
        )
    }

    private suspend fun readStepsToday(c: HealthConnectClient): Long = runCatching {
        c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), todayRange()))[StepsRecord.COUNT_TOTAL] ?: 0L
    }.getOrDefault(0L)

    private suspend fun readCaloriesToday(c: HealthConnectClient): Double = runCatching {
        c.aggregate(AggregateRequest(setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL), todayRange()))[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
    }.getOrDefault(0.0)

    private suspend fun readLatestWeightKg(c: HealthConnectClient): Double? = runCatching {
        val zone = ZoneId.systemDefault()
        val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
        val start = LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant()
        c.readRecords(ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(start, end)))
            .records.lastOrNull()?.weight?.inKilograms
    }.getOrNull()

    private fun todayRange(): TimeRangeFilter =
        TimeRangeFilter.between(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay())
}
