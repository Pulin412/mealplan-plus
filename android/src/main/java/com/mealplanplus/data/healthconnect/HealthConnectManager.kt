package com.mealplanplus.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Period
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HealthConnectManager"

/**
 * Low-level wrapper around the Health Connect SDK.
 *
 * All suspend functions are safe — they catch [Exception] and return empty/default values
 * rather than propagating crashes. This is especially important on Android 14+ where
 * [HealthConnectClient.permissionController.getGrantedPermissions] throws
 * [IllegalStateException] when the manifest is missing the
 * `VIEW_PERMISSION_USAGE / HEALTH_PERMISSIONS` intent filter.
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
     * True when the Health Connect SDK is available on this device.
     * On Android 14+ HC is part of the OS; on 9–13 users must install the companion app.
     */
    val isAvailable: Boolean
        get() = try {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        } catch (e: Exception) {
            Log.w(TAG, "getSdkStatus failed", e)
            false
        }

    private val client: HealthConnectClient? by lazy {
        if (isAvailable) {
            try { HealthConnectClient.getOrCreate(context) } catch (e: Exception) {
                Log.w(TAG, "getOrCreate failed", e); null
            }
        } else null
    }

    /**
     * Returns true when all [requiredPermissions] have been granted.
     * Returns false (never throws) on any SDK or manifest misconfiguration.
     */
    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return try {
            c.permissionController.getGrantedPermissions().containsAll(requiredPermissions)
        } catch (e: Exception) {
            Log.w(TAG, "hasAllPermissions failed", e)
            false
        }
    }

    /**
     * Today's total step count using the aggregate API — no record-count limit, single call.
     * Returns 0 on any error.
     */
    suspend fun readStepsToday(): Long {
        val c = client ?: return 0L
        return try {
            val start = LocalDate.now().atStartOfDay()
            val end = LocalDate.now().plusDays(1).atStartOfDay()
            val result = c.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            result[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "readStepsToday failed", e); 0L
        }
    }

    /**
     * Today's total calories burned (kcal) using the aggregate API.
     * Returns 0 on any error.
     */
    suspend fun readCaloriesBurnedToday(): Double {
        val c = client ?: return 0.0
        return try {
            val start = LocalDate.now().atStartOfDay()
            val end = LocalDate.now().plusDays(1).atStartOfDay()
            val result = c.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            Log.w(TAG, "readCaloriesBurnedToday failed", e); 0.0
        }
    }

    /**
     * Returns total step count per calendar day for the given date range (inclusive).
     *
     * Uses [aggregateGroupByPeriod] — the correct HC API for daily totals. This avoids the
     * 1000-record page-size limit that raw [ReadRecordsRequest] hits when a fitness watch writes
     * hundreds of small StepsRecords per day.
     */
    suspend fun readStepsByDay(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Long> {
        val c = client ?: return emptyMap()
        return try {
            val start = startDate.atStartOfDay()
            val end = endDate.plusDays(1).atStartOfDay()
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            ).associate { bucket ->
                bucket.startTime.toLocalDate() to (bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "readStepsByDay failed", e); emptyMap()
        }
    }

    /**
     * Returns total calories burned (kcal) per calendar day for the given date range (inclusive).
     *
     * Uses [aggregateGroupByPeriod] for the same reasons as [readStepsByDay].
     */
    suspend fun readCaloriesBurnedByDay(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, Int> {
        val c = client ?: return emptyMap()
        return try {
            val start = startDate.atStartOfDay()
            val end = endDate.plusDays(1).atStartOfDay()
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            ).associate { bucket ->
                bucket.startTime.toLocalDate() to
                    (bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.toInt() ?: 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "readCaloriesBurnedByDay failed", e); emptyMap()
        }
    }

    /** Most recent weight in kg within the last 30 days. Returns null on any error. */
    suspend fun readLatestWeightKg(): Double? {
        val c = client ?: return null
        return try {
            val zone = ZoneId.systemDefault()
            val end = LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
            val start = LocalDate.now().minusDays(30).atStartOfDay(zone).toInstant()
            c.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.between(start, end))
            ).records.lastOrNull()?.weight?.inKilograms
        } catch (e: Exception) {
            Log.w(TAG, "readLatestWeightKg failed", e); null
        }
    }

}
