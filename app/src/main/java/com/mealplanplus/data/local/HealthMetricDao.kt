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

    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND date = :date ORDER BY timestamp DESC")
    fun getMetricsForDate(userId: Long, date: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND metricType = :type ORDER BY date DESC, timestamp DESC")
    fun getMetricsByType(userId: Long, type: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND customTypeId = :customTypeId ORDER BY date DESC, timestamp DESC")
    fun getMetricsByCustomType(userId: Long, customTypeId: Long): Flow<List<HealthMetric>>

    @Query("""
        SELECT * FROM health_metrics
        WHERE userId = :userId AND metricType = :type AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC, timestamp ASC
    """)
    fun getMetricsByTypeInRange(userId: Long, type: String, startDate: String, endDate: String): Flow<List<HealthMetric>>

    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY date DESC, timestamp DESC LIMIT :limit")
    fun getRecentMetrics(userId: Long, limit: Int = 50): Flow<List<HealthMetric>>

    // Custom Metric Types
    @Insert
    suspend fun insertCustomType(type: CustomMetricType): Long

    @Update
    suspend fun updateCustomType(type: CustomMetricType)

    @Delete
    suspend fun deleteCustomType(type: CustomMetricType)

    @Query("SELECT * FROM custom_metric_types WHERE userId = :userId AND isActive = 1 ORDER BY name")
    fun getActiveCustomTypes(userId: Long): Flow<List<CustomMetricType>>

    @Query("SELECT * FROM custom_metric_types WHERE userId = :userId ORDER BY name")
    fun getAllCustomTypes(userId: Long): Flow<List<CustomMetricType>>

    @Query("SELECT * FROM custom_metric_types WHERE id = :id")
    suspend fun getCustomTypeById(id: Long): CustomMetricType?
}
