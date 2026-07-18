package com.mealplanplus.api.domain.log

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface DailyLogRepository : JpaRepository<DailyLog, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<DailyLog>
    fun findByServerId(serverId: UUID): DailyLog?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<DailyLog>
    fun findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid: String, date: java.time.LocalDate): DailyLog?
    fun findTop5ByFirebaseUidOrderByDateDesc(firebaseUid: String): List<DailyLog>
    fun findByFirebaseUidAndDateBetweenOrderByDateAsc(
        firebaseUid: String,
        start: java.time.LocalDate,
        end: java.time.LocalDate
    ): List<DailyLog>
}

interface LoggedFoodRepository : JpaRepository<LoggedFood, Long> {
    fun findByDailyLogId(dailyLogId: Long): List<LoggedFood>
    fun findByDailyLogIdIn(dailyLogIds: Collection<Long>): List<LoggedFood>
    fun deleteByDailyLogId(dailyLogId: Long)
}

interface LoggedMealSlotRepository : JpaRepository<LoggedMealSlot, Long> {
    fun findByFirebaseUidAndDate(firebaseUid: String, date: LocalDate): List<LoggedMealSlot>
    fun findByFirebaseUidAndDateAndSlot(firebaseUid: String, date: LocalDate, slot: String): LoggedMealSlot?
    fun findByFirebaseUidAndDateBetween(firebaseUid: String, from: LocalDate, to: LocalDate): List<LoggedMealSlot>
    fun findByFirebaseUidAndDateGreaterThanEqual(firebaseUid: String, date: LocalDate): List<LoggedMealSlot>
    fun existsByFirebaseUidAndDateAndIsLoggedTrue(firebaseUid: String, date: LocalDate): Boolean
}
