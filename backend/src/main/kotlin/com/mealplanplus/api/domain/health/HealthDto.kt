package com.mealplanplus.api.domain.health

import java.time.Instant
import java.util.UUID

data class HealthMetricDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val type: String = "",
    val subType: String? = null,
    val value: Double = 0.0,
    val secondaryValue: Double? = null,
    val unit: String = "",
    val recordedAt: Instant? = null,
    val updatedAt: Instant? = null
)

data class CustomMetricTypeDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val unit: String = "",
    val icon: String? = null,
    val updatedAt: Instant? = null
)

fun HealthMetric.toDto() =
    HealthMetricDto(id, serverId, firebaseUid, type, subType, value, secondaryValue, unit, recordedAt, updatedAt)

fun CustomMetricType.toDto() =
    CustomMetricTypeDto(id, serverId, firebaseUid, name, unit, icon, updatedAt)
