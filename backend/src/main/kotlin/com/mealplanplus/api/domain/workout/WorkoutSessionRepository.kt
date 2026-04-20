package com.mealplanplus.api.domain.workout

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<WorkoutSession>
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<WorkoutSession>
    fun findByServerId(serverId: UUID): WorkoutSession?
}

interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    fun findBySessionId(sessionId: Long): List<WorkoutSet>
    fun deleteBySessionId(sessionId: Long)
}
