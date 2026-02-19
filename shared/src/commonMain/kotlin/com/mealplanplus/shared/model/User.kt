package com.mealplanplus.shared.model

/**
 * User account
 */
data class User(
    val id: Long = 0,
    val email: String,
    val passwordHash: String = "",  // Hashed password for local auth
    val displayName: String? = null,
    val photoUrl: String? = null,
    val age: Int? = null,
    val contact: String? = null,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    companion object {
        fun hashPassword(password: String): String {
            return sha256(password)
        }

        fun verifyPassword(password: String, hash: String): Boolean {
            return hashPassword(password) == hash
        }
    }
}

/**
 * Platform-specific SHA-256 hash
 */
expect fun sha256(input: String): String
