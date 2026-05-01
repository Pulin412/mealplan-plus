package com.mealplanplus.api.domain.user

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener::class)
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val firebaseUid: String = "",

    val email: String? = null,
    var displayName: String? = null,

    // Profile fields (Phase 3a #103)
    var age: Int? = null,
    var weightKg: Double? = null,
    var heightCm: Double? = null,
    var gender: String? = null,
    var activityLevel: String? = null,
    var targetCalories: Int? = null,
    var goalType: String? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
