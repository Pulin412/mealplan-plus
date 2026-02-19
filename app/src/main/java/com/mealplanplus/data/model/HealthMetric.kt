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
    FASTING_SUGAR("Fasting Sugar", "mg/dL"),
    HBA1C("HbA1c", "%")
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
        get() = metric.metricType?.let { MetricType.valueOf(it).displayName }
            ?: customType?.name ?: "Unknown"

    val unit: String
        get() = metric.metricType?.let { MetricType.valueOf(it).unit }
            ?: customType?.unit ?: ""
}
