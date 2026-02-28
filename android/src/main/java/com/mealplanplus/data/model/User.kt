package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.security.MessageDigest

enum class Gender(val displayName: String) {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other")
}

enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    SEDENTARY("Sedentary (desk job)", 1.2),
    LIGHT("Lightly active (1-3d/wk)", 1.375),
    MODERATE("Moderately active (3-5d/wk)", 1.55),
    VERY_ACTIVE("Very active (6-7d/wk)", 1.725),
    EXTRA_ACTIVE("Athlete / physical job", 1.9)
}

enum class GoalType(val displayName: String) {
    LOSE("Lose weight"),
    MAINTAIN("Maintain weight"),
    GAIN("Gain muscle")
}

@Entity(
    tableName = "users",
    indices = [Index(value = ["email"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val passwordHash: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val age: Int? = null,
    val contact: String? = null,
    val weightKg: Double? = null,
    val heightCm: Double? = null,
    val gender: String? = null,
    val activityLevel: String? = null,
    val targetCalories: Int? = null,
    val goalType: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun hashPassword(password: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }

        fun verifyPassword(password: String, hash: String): Boolean {
            return hashPassword(password) == hash
        }
    }
}
