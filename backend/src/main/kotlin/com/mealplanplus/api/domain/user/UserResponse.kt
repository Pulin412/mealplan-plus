package com.mealplanplus.api.domain.user

import java.time.Instant

data class UserResponse(
    val id: Long,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val age: Int?,
    val weightKg: Double?,
    val heightCm: Double?,
    val gender: String?,
    val activityLevel: String?,
    val targetCalories: Int?,
    val goalType: String?,
    val createdAt: Instant
)

data class UpdateUserRequest(
    val displayName: String? = null,
    val age: Int? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val gender: String? = null,
    val activityLevel: String? = null,
    val targetCalories: Int? = null,
    val goalType: String? = null
)
