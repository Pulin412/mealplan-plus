package com.mealplanplus.api.domain.workout

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<WorkoutSession>
    fun findByFirebaseUidAndDateBetween(firebaseUid: String, from: LocalDate, to: LocalDate): List<WorkoutSession>
    fun findByFirebaseUidAndDateAndName(firebaseUid: String, date: LocalDate, name: String): WorkoutSession?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<WorkoutSession>
    fun findByServerId(serverId: UUID): WorkoutSession?
}

interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    fun findBySessionId(sessionId: Long): List<WorkoutSet>
    fun findBySessionIdIn(sessionIds: Collection<Long>): List<WorkoutSet>
    fun findBySessionIdAndExerciseId(sessionId: Long, exerciseId: Long): List<WorkoutSet>
    fun deleteBySessionId(sessionId: Long)

    @Query("""
        SELECT ws FROM WorkoutSet ws
        WHERE ws.exerciseId = :exerciseId
          AND ws.sessionId IN (
            SELECT s.id FROM WorkoutSession s
            WHERE s.firebaseUid = :firebaseUid AND s.isCompleted = true
            ORDER BY s.date DESC
          )
        ORDER BY ws.setNumber ASC
    """)
    fun findLastSetsForExercise(firebaseUid: String, exerciseId: Long): List<WorkoutSet>
}

interface WorkoutTemplateRepository : JpaRepository<WorkoutTemplate, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<WorkoutTemplate>
}

interface TemplateExerciseRepository : JpaRepository<TemplateExercise, Long> {
    fun findByTemplateIdOrderByOrderIndex(templateId: Long): List<TemplateExercise>
    fun findByTemplateIdIn(templateIds: Collection<Long>): List<TemplateExercise>
    fun deleteByTemplateId(templateId: Long)
}

interface TemplateExerciseSetRepository : JpaRepository<TemplateExerciseSet, Long> {
    fun findByTemplateExerciseIdOrderBySetNumber(templateExerciseId: Long): List<TemplateExerciseSet>
    fun findByTemplateExerciseIdIn(templateExerciseIds: Collection<Long>): List<TemplateExerciseSet>
    fun deleteByTemplateExerciseIdIn(templateExerciseIds: Collection<Long>)
}
