package com.mealplanplus.api.domain.feedback

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "feedback")
class Feedback(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    @Column(columnDefinition = "text")
    val message: String = "",
    val appVersion: String? = null,
    val platform: String? = null,
    val createdAt: Instant = Instant.now()
)
