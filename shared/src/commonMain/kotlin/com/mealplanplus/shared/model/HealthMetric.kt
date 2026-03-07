package com.mealplanplus.shared.model

/**
 * Built-in health metric types
 */
enum class MetricType(val displayName: String, val unit: String) {
    WEIGHT("Weight", "kg"),
    BLOOD_GLUCOSE("Blood Glucose", "mg/dL"),
    BLOOD_PRESSURE("Blood Pressure", "mmHg"),
    // Legacy values kept for backward-compat with existing iOS data
    FASTING_SUGAR("Fasting Sugar", "mg/dL"),
    HBA1C("HbA1c", "%")
}

/**
 * User-defined custom metric type
 */
data class CustomMetricType(
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
data class HealthMetric(
    val id: Long = 0,
    val userId: Long,
    val date: String,  // ISO format: yyyy-MM-dd
    val timestamp: Long = currentTimeMillis(),
    val metricType: String?,  // MetricType.name or null for custom
    val customTypeId: Long? = null,  // For custom metrics
    val value: Double,
    val secondaryValue: Double? = null,  // Diastolic for blood pressure
    val subType: String? = null,         // GlucoseSubType.name for blood glucose
    val notes: String? = null,
    val serverId: String? = null,
    val updatedAt: Long = currentTimeMillis(),
    val syncedAt: Long? = null
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
