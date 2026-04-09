package com.mealplanplus.data.healthconnect

import android.content.Context
import android.util.Log
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

    /** Today's total step count. Returns 0 on any error. */
    suspend fun readStepsToday(): Long {
        val c = client ?: return 0L
        return try {
            val (start, end) = todayRange()
            c.readRecords(
                ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end))
            ).records.sumOf { it.count }
        } catch (e: Exception) {
            Log.w(TAG, "readStepsToday failed", e); 0L
        }
    }

    /** Today's total calories burned in kcal. Returns 0 on any error. */
    suspend fun readCaloriesBurnedToday(): Double {
        val c = client ?: return 0.0
        return try {
            val (start, end) = todayRange()
            c.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, TimeRangeFilter.between(start, end))
            ).records.sumOf { it.energy.inKilocalories }
        } catch (e: Exception) {
            Log.w(TAG, "readCaloriesBurnedToday failed", e); 0.0
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

    private fun todayRange(): Pair<java.time.Instant, java.time.Instant> {
        val zone = ZoneId.systemDefault()
        return LocalDate.now().atStartOfDay(zone).toInstant() to
               LocalDate.now().plusDays(1).atStartOfDay(zone).toInstant()
    }
}
