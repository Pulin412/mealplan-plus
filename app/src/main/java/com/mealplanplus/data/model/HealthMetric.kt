package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Built-in health metric types
 */
enum class MetricType(val displayName: String, val unit: String) {
    WEIGHT("Weight", "kg"),
    BLOOD_GLUCOSE("Blood Glucose", "mg/dL"),
    BLOOD_PRESSURE("Blood Pressure", "mmHg")
}

enum class GlucoseSubType(val displayName: String) {
    FASTING("Fasting"),
    POST_MEAL("Post-meal"),
    RANDOM("Random")
}

/**
 * User-defined custom metric type
 */
@Entity(
    tableName = "custom_metric_types",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class CustomMetricType(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val unit: String,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val isActive: Boolean = true
)

/**
 * A health metric reading
 */
@Entity(
    tableName = "health_metrics",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("date"), Index("metricType"), Index("customTypeId")]
)
data class HealthMetric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: String,  // ISO format: yyyy-MM-dd
    val timestamp: Long = System.currentTimeMillis(),
    val metricType: String?,  // MetricType.name or null for custom
    val customTypeId: Long? = null,  // For custom metrics
    val value: Double,
    val secondaryValue: Double? = null,  // Diastolic for blood pressure
    val subType: String? = null,  // GlucoseSubType.name for blood glucose readings
    val notes: String? = null
)

/**
 * Health metric with custom type details (if applicable)
 */
data class HealthMetricWithType(
    val metric: HealthMetric,
    val customType: CustomMetricType? = null
) {
    val displayName: String
        get() = metric.metricType?.let { runCatching { MetricType.valueOf(it).displayName }.getOrNull() }
            ?: customType?.name ?: "Unknown"

    val unit: String
        get() = metric.metricType?.let { runCatching { MetricType.valueOf(it).unit }.getOrNull() }
            ?: customType?.unit ?: ""
}
