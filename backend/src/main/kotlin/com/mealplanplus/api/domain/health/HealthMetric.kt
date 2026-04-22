package com.mealplanplus.api.domain.health

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "health_metrics")
class HealthMetric(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val type: String = "",          // WEIGHT, STEPS, BLOOD_PRESSURE, etc.
    val subType: String? = null,    // e.g. SYSTOLIC / DIASTOLIC for blood pressure
    @Column(name = "metric_value")
    val value: Double = 0.0,
    val secondaryValue: Double? = null,
    val unit: String = "",
    val recordedAt: Instant = Instant.now()
) : SyncableEntity()

@Entity
@Table(name = "custom_metric_types")
class CustomMetricType(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val unit: String = "",
    val icon: String? = null
) : SyncableEntity()
