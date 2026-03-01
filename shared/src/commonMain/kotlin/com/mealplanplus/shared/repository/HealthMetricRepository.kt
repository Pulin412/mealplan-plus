package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthMetricRepository(private val database: MealPlanDatabase) {

    private val queries = database.healthMetricQueries

    // Custom metric types
    fun getActiveCustomMetricTypes(userId: Long): Flow<List<CustomMetricType>> {
        return queries.selectCustomMetricTypes(userId).asFlowList().map { list ->
            list.map { it.toCustomMetricType() }
        }
    }

    fun getAllCustomMetricTypes(userId: Long): Flow<List<CustomMetricType>> {
        return queries.selectAllCustomMetricTypes(userId).asFlowList().map { list ->
            list.map { it.toCustomMetricType() }
        }
    }

    suspend fun getCustomMetricTypeById(id: Long): CustomMetricType? {
        return queries.selectCustomMetricTypeById(id).executeAsOneOrNull()?.toCustomMetricType()
    }

    suspend fun insertCustomMetricType(type: CustomMetricType): Long {
        queries.insertCustomMetricType(
            userId = type.userId,
            name = type.name,
            unit = type.unit,
            minValue = type.minValue,
            maxValue = type.maxValue,
            isActive = if (type.isActive) 1L else 0L
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateCustomMetricType(type: CustomMetricType) {
        queries.updateCustomMetricType(
            name = type.name,
            unit = type.unit,
            minValue = type.minValue,
            maxValue = type.maxValue,
            isActive = if (type.isActive) 1L else 0L,
            id = type.id
        )
    }

    suspend fun deleteCustomMetricType(id: Long) {
        queries.deleteCustomMetricType(id)
    }

    // Health metrics
    fun getAllHealthMetrics(userId: Long): Flow<List<HealthMetric>> {
        return queries.selectHealthMetrics(userId).asFlowList().map { list ->
            list.map { it.toHealthMetric() }
        }
    }

    fun getHealthMetricsByType(userId: Long, metricType: String): Flow<List<HealthMetric>> {
        return queries.selectHealthMetricsByType(userId, metricType).asFlowList().map { list ->
            list.map { it.toHealthMetric() }
        }
    }

    fun getHealthMetricsByCustomType(userId: Long, customTypeId: Long): Flow<List<HealthMetric>> {
        return queries.selectHealthMetricsByCustomType(userId, customTypeId).asFlowList().map { list ->
            list.map { it.toHealthMetric() }
        }
    }

    fun getHealthMetricsForDateRange(userId: Long, startDate: String, endDate: String): Flow<List<HealthMetric>> {
        return queries.selectHealthMetricsForDateRange(userId, startDate, endDate).asFlowList().map { list ->
            list.map { it.toHealthMetric() }
        }
    }

    suspend fun getHealthMetricById(id: Long): HealthMetric? {
        return queries.selectHealthMetricById(id).executeAsOneOrNull()?.toHealthMetric()
    }

    suspend fun insertHealthMetric(metric: HealthMetric): Long {
        queries.insertHealthMetric(
            userId = metric.userId,
            date = metric.date,
            timestamp = metric.timestamp,
            metricType = metric.metricType,
            customTypeId = metric.customTypeId,
            value_ = metric.value,
            secondaryValue = metric.secondaryValue,
            subType = metric.subType,
            notes = metric.notes,
            updatedAt = metric.updatedAt
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateHealthMetric(metric: HealthMetric) {
        queries.updateHealthMetric(
            date = metric.date,
            timestamp = metric.timestamp,
            metricType = metric.metricType,
            customTypeId = metric.customTypeId,
            value_ = metric.value,
            secondaryValue = metric.secondaryValue,
            subType = metric.subType,
            notes = metric.notes,
            updatedAt = metric.updatedAt,
            id = metric.id
        )
    }

    suspend fun getUnsyncedHealthMetrics(userId: Long): List<HealthMetric> {
        return queries.selectUnsyncedHealthMetrics(userId).executeAsList().map { it.toHealthMetric() }
    }

    suspend fun getHealthMetricByServerId(serverId: String): HealthMetric? {
        return queries.selectHealthMetricByServerId(serverId).executeAsOneOrNull()?.toHealthMetric()
    }

    suspend fun updateHealthMetricSyncState(id: Long, serverId: String, syncedAt: Long) {
        queries.updateHealthMetricSyncState(serverId = serverId, syncedAt = syncedAt, id = id)
    }

    suspend fun updateHealthMetricSyncedAt(id: Long, syncedAt: Long) {
        queries.updateHealthMetricSyncedAt(syncedAt = syncedAt, id = id)
    }

    suspend fun deleteHealthMetric(id: Long) {
        queries.deleteHealthMetric(id)
    }

    suspend fun getLatestMetricByType(userId: Long, metricType: String): HealthMetric? {
        return queries.selectLatestMetricByType(userId, metricType).executeAsOneOrNull()?.toHealthMetric()
    }

    // MARK: - Snapshot functions for iOS
    suspend fun getAllHealthMetricsSnapshot(userId: Long): List<HealthMetric> {
        return queries.selectHealthMetrics(userId).executeAsList().map { it.toHealthMetric() }
    }

    suspend fun getActiveCustomMetricTypesSnapshot(userId: Long): List<CustomMetricType> {
        return queries.selectCustomMetricTypes(userId).executeAsList().map { it.toCustomMetricType() }
    }

    suspend fun getHealthMetricsByTypeSnapshot(userId: Long, metricType: String): List<HealthMetric> {
        return queries.selectHealthMetricsByType(userId, metricType).executeAsList().map { it.toHealthMetric() }
    }

    suspend fun getHealthMetricsForDateRangeSnapshot(userId: Long, startDate: String, endDate: String): List<HealthMetric> {
        return queries.selectHealthMetricsForDateRange(userId, startDate, endDate).executeAsList().map { it.toHealthMetric() }
    }

    private fun com.mealplanplus.shared.db.Custom_metric_types.toCustomMetricType(): CustomMetricType {
        return CustomMetricType(
            id = id,
            userId = userId,
            name = name,
            unit = unit,
            minValue = minValue,
            maxValue = maxValue,
            isActive = isActive == 1L
        )
    }

    private fun com.mealplanplus.shared.db.Health_metrics.toHealthMetric(): HealthMetric {
        return HealthMetric(
            id = id,
            userId = userId,
            date = date,
            timestamp = timestamp,
            metricType = metricType,
            customTypeId = customTypeId,
            value = value_,
            secondaryValue = secondaryValue,
            subType = subType,
            notes = notes,
            serverId = serverId,
            updatedAt = updatedAt,
            syncedAt = syncedAt
        )
    }
}
