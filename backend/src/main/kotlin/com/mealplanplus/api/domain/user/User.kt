package com.mealplanplus.api.domain.user

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

enum class GenderEnum { MALE, FEMALE, OTHER }
enum class ActivityLevelEnum { SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE }
enum class GoalTypeEnum { LOSE_WEIGHT, MAINTAIN, GAIN_MUSCLE, GAIN_WEIGHT }
enum class UnitsEnum { METRIC, IMPERIAL }

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

    var age: Int? = null,
    var weightKg: Double? = null,
    var heightCm: Double? = null,

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "gender_enum")
    var gender: GenderEnum? = null,

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "activity_level_enum")
    var activityLevel: ActivityLevelEnum? = null,

    var targetCalories: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "goal_type_enum")
    var goalType: GoalTypeEnum? = null,

    var targetWeightKg: Double? = null,
    var targetProtein: Int? = null,
    var targetCarbs: Int? = null,
    var targetFat: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "units_enum", nullable = false)
    var preferredUnits: UnitsEnum = UnitsEnum.METRIC,

    // Consent + onboarding (see V4 migration).
    var consentedAt: Instant? = null,
    var privacyPolicyVersion: String? = null,
    var onboardingCompletedAt: Instant? = null,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
