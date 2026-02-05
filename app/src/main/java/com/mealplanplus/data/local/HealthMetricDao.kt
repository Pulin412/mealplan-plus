package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.HealthMetric
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {

    // Health Metrics
    @Insert
    suspend fun insertMetric(metric: HealthMetric): Long

    @Update
    suspend fun updateMetric(metric: HealthMetric)

    @Delete
    suspend fun deleteMetric(metric: HealthMetric)

    @Query("SELECT * FROM health_metrics WHERE id = :id")
    suspend fun getMetricById(id: Long): HealthMetric?

    @Query("SELECT * FROM health_metrics WHERE date = :date ORDER BY timestamp DESC")
    fun getMetricsForDate(date: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics WHERE metricType = :type ORDER BY date DESC, timestamp DESC")
    fun getMetricsByType(type: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics WHERE customTypeId = :customTypeId ORDER BY date DESC, timestamp DESC")
    fun getMetricsByCustomType(customTypeId: Long): Flow<List<HealthMetric>>

    @Query("""
        SELECT * FROM health_metrics
        WHERE metricType = :type AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, timestamp ASC
    """)
    fun getMetricsByTypeInRange(type: String, startDate: String, endDate: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics ORDER BY date DESC, timestamp DESC LIMIT :limit")
    fun getRecentMetrics(limit: Int = 50): Flow<List<HealthMetric>>

    // Custom Metric Types
    @Insert
    suspend fun insertCustomType(type: CustomMetricType): Long

    @Update
    suspend fun updateCustomType(type: CustomMetricType)

    @Delete
    suspend fun deleteCustomType(type: CustomMetricType)

    @Query("SELECT * FROM custom_metric_types WHERE isActive = 1 ORDER BY name")
    fun getActiveCustomTypes(): Flow<List<CustomMetricType>>

    @Query("SELECT * FROM custom_metric_types ORDER BY name")
    fun getAllCustomTypes(): Flow<List<CustomMetricType>>

    @Query("SELECT * FROM custom_metric_types WHERE id = :id")
    suspend fun getCustomTypeById(id: Long): CustomMetricType?
}
