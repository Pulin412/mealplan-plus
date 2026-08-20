package com.mealplanplus.api.domain.plan

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface DayPlanRepository : JpaRepository<DayPlan, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<DayPlan>
    fun findByFirebaseUidAndDateBetween(firebaseUid: String, from: LocalDate, to: LocalDate): List<DayPlan>
    fun findByFirebaseUidAndDate(firebaseUid: String, date: LocalDate): DayPlan?
    fun findByFirebaseUidAndDietId(firebaseUid: String, dietId: Long): List<DayPlan>
    fun findByServerId(serverId: UUID): DayPlan?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<DayPlan>
    fun deleteByFirebaseUidAndDate(firebaseUid: String, date: LocalDate)
}

interface PlannedWorkoutRepository : JpaRepository<PlannedWorkout, Long> {
    fun findByDayPlanId(dayPlanId: Long): List<PlannedWorkout>
    fun findByDayPlanIdIn(dayPlanIds: Collection<Long>): List<PlannedWorkout>
    fun deleteByDayPlanId(dayPlanId: Long)
}

interface PlannedMealRepository : JpaRepository<PlannedMeal, Long> {
    fun findByDayPlanId(dayPlanId: Long): List<PlannedMeal>
    fun findByDayPlanIdIn(dayPlanIds: Collection<Long>): List<PlannedMeal>
    fun deleteByDayPlanId(dayPlanId: Long)
}
