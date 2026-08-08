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

    /** Completed sessions of a given workout (matched by name), most-recent first; take the first for "last time". */
    @Query("""
        SELECT s.id FROM WorkoutSession s
        WHERE s.firebaseUid = :firebaseUid AND s.isCompleted = true AND s.name = :name
        ORDER BY s.date DESC, s.id DESC
    """)
    fun findCompletedSessionIdsByName(firebaseUid: String, name: String): List<Long>

    /** Completed sessions that contain the exercise, most-recent first (fallback when no workout given). */
    @Query("""
        SELECT s.id FROM WorkoutSession s
        WHERE s.firebaseUid = :firebaseUid AND s.isCompleted = true
          AND EXISTS (SELECT 1 FROM WorkoutSet ws WHERE ws.sessionId = s.id AND ws.exerciseId = :exerciseId)
        ORDER BY s.date DESC, s.id DESC
    """)
    fun findCompletedSessionIdsForExercise(firebaseUid: String, exerciseId: Long): List<Long>
}

interface WorkoutSetRepository : JpaRepository<WorkoutSet, Long> {
    fun findBySessionId(sessionId: Long): List<WorkoutSet>
    fun findBySessionIdIn(sessionIds: Collection<Long>): List<WorkoutSet>
    fun findBySessionIdAndExerciseId(sessionId: Long, exerciseId: Long): List<WorkoutSet>
    fun deleteBySessionId(sessionId: Long)
}

interface WorkoutTemplateRepository : JpaRepository<WorkoutTemplate, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<WorkoutTemplate>
    fun findByServerId(serverId: UUID): WorkoutTemplate?
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
