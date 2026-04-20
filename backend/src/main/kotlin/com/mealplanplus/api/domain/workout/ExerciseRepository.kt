package com.mealplanplus.api.domain.workout

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByFirebaseUidOrIsSystemTrue(firebaseUid: String): List<Exercise>
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Exercise>
    fun findByIsSystemTrueAndUpdatedAtAfter(since: Instant): List<Exercise>
    fun findByServerId(serverId: UUID): Exercise?
}
