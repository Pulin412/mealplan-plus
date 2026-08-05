package com.mealplanplus.api.domain.feedback

import org.springframework.data.jpa.repository.JpaRepository

interface FeedbackRepository : JpaRepository<Feedback, Long> {
    fun findByFirebaseUidOrderByCreatedAtDesc(firebaseUid: String): List<Feedback>
}
