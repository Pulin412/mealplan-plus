package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    private val healthMetricDao: HealthMetricDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    // Health Metrics
    suspend fun logMetric(
        type: MetricType,
        value: Double,
        date: Long = LocalDate.now().toEpochMs(),
        subType: String? = null,
        secondaryValue: Double? = null,
        notes: String? = null
    ): Long {
        return healthMetricDao.insertMetric(
            HealthMetric(
                userId = getCurrentUserId(),
                date = date,
                metricType = type.name,
                value = value,
                subType = subType,
                secondaryValue = secondaryValue,
                notes = notes
            )
        )
    }

    suspend fun logCustomMetric(
        customTypeId: Long,
        value: Double,
        date: Long = LocalDate.now().toEpochMs(),
        notes: String? = null
    ): Long {
        return healthMetricDao.insertMetric(
            HealthMetric(
                userId = getCurrentUserId(),
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

    fun getMetricsForDate(date: Long): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsForDate(getCurrentUserId(), date)

    fun getMetricsByType(type: MetricType): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsByType(getCurrentUserId(), type.name)

    fun getMetricsByTypeInRange(type: MetricType, startDate: Long, endDate: Long): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsByTypeInRange(getCurrentUserId(), type.name, startDate, endDate)

    fun getRecentMetrics(limit: Int = 50): Flow<List<HealthMetric>> =
        healthMetricDao.getRecentMetrics(getCurrentUserId(), limit)

    fun getMetricsByCustomType(customTypeId: Long): Flow<List<HealthMetric>> =
        healthMetricDao.getMetricsByCustomType(getCurrentUserId(), customTypeId)

    // Custom Metric Types
    suspend fun addCustomType(name: String, unit: String, minValue: Double? = null, maxValue: Double? = null): Long {
        return healthMetricDao.insertCustomType(
            CustomMetricType(userId = getCurrentUserId(), name = name, unit = unit, minValue = minValue, maxValue = maxValue)
        )
    }

    suspend fun updateCustomType(type: CustomMetricType) = healthMetricDao.updateCustomType(type)

    suspend fun deleteCustomType(type: CustomMetricType) = healthMetricDao.deleteCustomType(type)

    fun getActiveCustomTypes(): Flow<List<CustomMetricType>> = healthMetricDao.getActiveCustomTypes(getCurrentUserId())

    fun getAllCustomTypes(): Flow<List<CustomMetricType>> = healthMetricDao.getAllCustomTypes(getCurrentUserId())
}
