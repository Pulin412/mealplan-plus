package com.mealplanplus.api.domain.plan

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface DayPlanRepository : JpaRepository<DayPlan, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<DayPlan>
    fun findByFirebaseUidAndDate(firebaseUid: String, date: LocalDate): DayPlan?
    fun findByServerId(serverId: UUID): DayPlan?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<DayPlan>
    fun deleteByFirebaseUidAndDate(firebaseUid: String, date: LocalDate)
}
