package com.mealplanplus.data.repository

import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthMetricDao: HealthMetricDao
) {
    // Health Metrics
    suspend fun logMetric(
        type: MetricType,
        value: Double,
        date: String = LocalDate.now().toString(),
        notes: String? = null
    ): Long {
        return healthMetricDao.insertMetric(
            HealthMetric(
                date = date,
                metricType = type.name,
                value = value,
                notes = notes
            )
        )
    }

    suspend fun logCustomMetric(
        customTypeId: Long,
        value: Double,
        date: String = LocalDate.now().toString(),
        notes: String? = null
    ): Long {
        return healthMetricDao.insertMetric(
            HealthMetric(
                date = date,
                customTypeId = customTypeId,
                metricType = null,
                value = value,
                notes = notes
            )
        )
    }

    suspend fun updateMetric(metric: HealthMetric) = healthMetricDao.updateMetric(metric)

    suspend fun deleteMetric(metric: HealthMetric) = healthMetricDao.deleteMetric(metric)

    fun getMetricsForDate(date: String): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsForDate(date)

    fun getMetricsByType(type: MetricType): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsByType(type.name)

    fun getMetricsByTypeInRange(type: MetricType, startDate: String, endDate: String): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsByTypeInRange(type.name, startDate, endDate)

    fun getRecentMetrics(limit: Int = 50): Flow<List<HealthMetric>> =
        healthMetricDao.getRecentMetrics(limit)

    // Custom Metric Types
    suspend fun addCustomType(name: String, unit: String, minValue: Double? = null, maxValue: Double? = null): Long {
        return healthMetricDao.insertCustomType(
            CustomMetricType(name = name, unit = unit, minValue = minValue, maxValue = maxValue)
        )
    }

    suspend fun updateCustomType(type: CustomMetricType) = healthMetricDao.updateCustomType(type)

    suspend fun deleteCustomType(type: CustomMetricType) = healthMetricDao.deleteCustomType(type)

    fun getActiveCustomTypes(): Flow<List<CustomMetricType>> = healthMetricDao.getActiveCustomTypes()

    fun getAllCustomTypes(): Flow<List<CustomMetricType>> = healthMetricDao.getAllCustomTypes()
}
