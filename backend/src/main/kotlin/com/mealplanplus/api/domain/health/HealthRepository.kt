package com.mealplanplus.api.domain.health

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface HealthMetricRepository : JpaRepository<HealthMetric, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<HealthMetric>
    fun findByServerId(serverId: UUID): HealthMetric?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<HealthMetric>
    fun findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid: String, type: String): HealthMetric?
    fun findTop1ByFirebaseUidAndTypeAndRecordedAtAfterOrderByRecordedAtDesc(
        firebaseUid: String, type: String, since: Instant
    ): HealthMetric?
}

interface CustomMetricTypeRepository : JpaRepository<CustomMetricType, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<CustomMetricType>
    fun findByServerId(serverId: UUID): CustomMetricType?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<CustomMetricType>
}
