package com.mealplanplus.api.domain.user

import java.time.Instant

data class UserResponse(
    val id: Long,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val createdAt: Instant
)
