package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.HealthMetricsApi
import com.mealplanplus.data.generated.model.HealthMetricDto
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed health-metric log (glucose / weight / blood pressure). Health metrics have a clean
 * REST API and are read/written directly (needs a backend), mirroring the Plan + Exercises domains.
 * Built-in types: WEIGHT (kg), GLUCOSE (mg/dL), BLOOD_PRESSURE (value=systolic, secondaryValue=diastolic).
 */
@Singleton
class HealthRepository @Inject constructor(
    private val api: HealthMetricsApi,
) {
    /** All readings of one metric type, oldest-first. Empty on failure/offline. */
    suspend fun list(type: String): List<HealthMetricDto> =
        runCatching { api.listHealthMetrics(type = type).body().orEmpty() }
            .getOrDefault(emptyList())
            .sortedBy { it.recordedAt }

    /** Log a reading. [secondaryValue] carries diastolic for blood pressure. */
    suspend fun create(
        type: String,
        value: Double,
        unit: String,
        recordedAt: Instant = Instant.now(),
        secondaryValue: Double? = null,
    ): Result<HealthMetricDto> = runCatching {
        api.createHealthMetric(
            HealthMetricDto(
                type = type,
                value = value,
                unit = unit,
                recordedAt = recordedAt,
                secondaryValue = secondaryValue,
            ),
        ).body()!!
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching { api.deleteHealthMetric(id); Unit }
}
