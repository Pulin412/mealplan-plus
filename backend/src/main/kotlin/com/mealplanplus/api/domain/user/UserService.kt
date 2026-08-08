package com.mealplanplus.api.domain.user

import com.mealplanplus.api.generated.model.UserResponse
import com.mealplanplus.api.generated.model.UserUpdateRequest
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class UserService(private val userRepository: UserRepository) {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional
    fun getOrCreate(firebaseUid: String, email: String? = null, displayName: String? = null): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: userRepository.save(User(firebaseUid = firebaseUid, email = email, displayName = displayName))
        // Backfill the email once we learn it from the token (rows created before we captured it).
        if (user.email.isNullOrBlank() && !email.isNullOrBlank()) {
            user.email = email
            userRepository.save(user)
        }
        return user.toResponse()
    }

    @Transactional
    fun update(firebaseUid: String, req: UserUpdateRequest): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: userRepository.save(User(firebaseUid = firebaseUid))
        req.displayName?.let      { user.displayName    = it }
        req.age?.let              { user.age            = it }
        req.weightKg?.let         { user.weightKg       = it }
        req.heightCm?.let         { user.heightCm       = it }
        req.gender?.let           { user.gender         = mapGender(it) }
        req.activityLevel?.let    { user.activityLevel  = mapActivityLevel(it) }
        req.targetCalories?.let   { user.targetCalories = it }
        req.goalType?.let         { user.goalType       = mapGoalType(it) }
        req.targetProtein?.let    { user.targetProtein  = it }
        req.targetCarbs?.let      { user.targetCarbs    = it }
        req.targetFat?.let        { user.targetFat      = it }
        req.units?.let            { user.preferredUnits = mapUnits(it) }
        // Consent: recording a policy version stamps the consent time.
        req.privacyPolicyVersion?.let {
            user.privacyPolicyVersion = it
            user.consentedAt = Instant.now()
        }
        // Onboarding: first completion/skip stamps the time (idempotent — keep the first).
        if (req.onboardingCompleted == true && user.onboardingCompletedAt == null) {
            user.onboardingCompletedAt = Instant.now()
        }
        return userRepository.save(user).toResponse()
    }

    /**
     * Right-to-erasure: delete the user and ALL their data. Top-level tables are keyed by
     * firebase_uid; their child rows are removed by FK ON DELETE CASCADE. System foods/exercises
     * (owned by no user) are untouched. The caller deletes its own Firebase auth account.
     */
    @Transactional
    fun deleteMe(firebaseUid: String) {
        val userOwnedTables = listOf(
            "food_user_prefs", "logged_meal_slots", "daily_logs", "grocery_lists",
            "day_plans", "workout_sessions", "workout_templates", "exercises",
            "meals", "diets", "tags", "health_metrics", "custom_metric_types",
            "foods", "entity_embeddings", "tombstones",
        )
        userOwnedTables.forEach { table ->
            em.createNativeQuery("DELETE FROM $table WHERE firebase_uid = :uid")
                .setParameter("uid", firebaseUid)
                .executeUpdate()
        }
        // Social graph is keyed by role-specific columns, not firebase_uid — purge separately.
        em.createNativeQuery("DELETE FROM follows WHERE follower_uid = :uid OR followee_uid = :uid")
            .setParameter("uid", firebaseUid).executeUpdate()
        em.createNativeQuery("DELETE FROM blocks WHERE blocker_uid = :uid OR blocked_uid = :uid")
            .setParameter("uid", firebaseUid).executeUpdate()
        em.createNativeQuery("DELETE FROM content_reports WHERE reporter_uid = :uid OR reported_uid = :uid")
            .setParameter("uid", firebaseUid).executeUpdate()
        em.createNativeQuery("DELETE FROM users WHERE firebase_uid = :uid")
            .setParameter("uid", firebaseUid)
            .executeUpdate()
    }

    private fun mapGender(g: UserUpdateRequest.Gender): GenderEnum? = when (g) {
        UserUpdateRequest.Gender.MALE   -> GenderEnum.MALE
        UserUpdateRequest.Gender.FEMALE -> GenderEnum.FEMALE
        UserUpdateRequest.Gender.OTHER  -> GenderEnum.OTHER
    }

    private fun mapActivityLevel(a: UserUpdateRequest.ActivityLevel): ActivityLevelEnum? = when (a) {
        UserUpdateRequest.ActivityLevel.SEDENTARY    -> ActivityLevelEnum.SEDENTARY
        UserUpdateRequest.ActivityLevel.LIGHT        -> ActivityLevelEnum.LIGHTLY_ACTIVE
        UserUpdateRequest.ActivityLevel.MODERATE     -> ActivityLevelEnum.MODERATELY_ACTIVE
        UserUpdateRequest.ActivityLevel.VERY_ACTIVE  -> ActivityLevelEnum.VERY_ACTIVE
        UserUpdateRequest.ActivityLevel.EXTRA_ACTIVE -> ActivityLevelEnum.EXTRA_ACTIVE
    }

    private fun mapGoalType(g: UserUpdateRequest.GoalType): GoalTypeEnum? = when (g) {
        UserUpdateRequest.GoalType.LOSE     -> GoalTypeEnum.LOSE_WEIGHT
        UserUpdateRequest.GoalType.MAINTAIN -> GoalTypeEnum.MAINTAIN
        UserUpdateRequest.GoalType.GAIN     -> GoalTypeEnum.GAIN_WEIGHT
    }

    private fun mapUnits(u: UserUpdateRequest.Units): UnitsEnum = when (u) {
        UserUpdateRequest.Units.METRIC   -> UnitsEnum.METRIC
        UserUpdateRequest.Units.IMPERIAL -> UnitsEnum.IMPERIAL
    }
}

fun User.toResponse() = UserResponse(
    id             = id,
    firebaseUid    = firebaseUid,
    email          = email,
    displayName    = displayName,
    photoUrl       = null,
    age            = age,
    weightKg       = weightKg,
    heightCm       = heightCm,
    gender         = gender?.let { mapGenderToSpec(it) },
    activityLevel  = activityLevel?.let { mapActivityLevelToSpec(it) },
    targetCalories = targetCalories,
    goalType       = goalType?.let { mapGoalTypeToSpec(it) },
    targetProtein  = targetProtein,
    targetCarbs    = targetCarbs,
    targetFat      = targetFat,
    units          = mapUnitsToSpec(preferredUnits),
    createdAt      = createdAt,
    consentedAt           = consentedAt,
    privacyPolicyVersion  = privacyPolicyVersion,
    onboardingCompletedAt = onboardingCompletedAt,
    handle       = handle,
    bio          = bio,
    avatarSeed   = avatarSeed,
    isSearchable = isSearchable
)

private fun mapGenderToSpec(g: GenderEnum): UserResponse.Gender = when (g) {
    GenderEnum.MALE   -> UserResponse.Gender.MALE
    GenderEnum.FEMALE -> UserResponse.Gender.FEMALE
    GenderEnum.OTHER  -> UserResponse.Gender.OTHER
}

private fun mapActivityLevelToSpec(a: ActivityLevelEnum): UserResponse.ActivityLevel = when (a) {
    ActivityLevelEnum.SEDENTARY           -> UserResponse.ActivityLevel.SEDENTARY
    ActivityLevelEnum.LIGHTLY_ACTIVE      -> UserResponse.ActivityLevel.LIGHT
    ActivityLevelEnum.MODERATELY_ACTIVE   -> UserResponse.ActivityLevel.MODERATE
    ActivityLevelEnum.VERY_ACTIVE         -> UserResponse.ActivityLevel.VERY_ACTIVE
    ActivityLevelEnum.EXTRA_ACTIVE        -> UserResponse.ActivityLevel.EXTRA_ACTIVE
}

private fun mapGoalTypeToSpec(g: GoalTypeEnum): UserResponse.GoalType = when (g) {
    GoalTypeEnum.LOSE_WEIGHT   -> UserResponse.GoalType.LOSE
    GoalTypeEnum.MAINTAIN      -> UserResponse.GoalType.MAINTAIN
    GoalTypeEnum.GAIN_MUSCLE   -> UserResponse.GoalType.GAIN
    GoalTypeEnum.GAIN_WEIGHT   -> UserResponse.GoalType.GAIN
}

private fun mapUnitsToSpec(u: UnitsEnum): UserResponse.Units = when (u) {
    UnitsEnum.METRIC   -> UserResponse.Units.METRIC
    UnitsEnum.IMPERIAL -> UserResponse.Units.IMPERIAL
}
