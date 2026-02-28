package com.mealplanplus.api.domain.log

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface DailyLogRepository : JpaRepository<DailyLog, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<DailyLog>
    fun findByServerId(serverId: UUID): DailyLog?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<DailyLog>
}

interface LoggedFoodRepository : JpaRepository<LoggedFood, Long> {
    fun findByDailyLogId(dailyLogId: Long): List<LoggedFood>
    fun deleteByDailyLogId(dailyLogId: Long)
}
