package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercise_categories")
data class ExerciseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSystem: Boolean = false
)

/** Categories that cannot be deleted and mark their exercises as read-only. */
val READONLY_SYSTEM_CATEGORIES = setOf("CARDIO", "FLEXIBILITY", "OTHER")

/** Built-in categories seeded on first install. */
val DEFAULT_CATEGORIES = listOf(
    ExerciseCategoryEntity(name = "STRENGTH",    isSystem = true),
    ExerciseCategoryEntity(name = "CARDIO",      isSystem = true),
    ExerciseCategoryEntity(name = "FLEXIBILITY", isSystem = true),
    ExerciseCategoryEntity(name = "OTHER",       isSystem = true)
)

fun ExerciseCategoryEntity.displayName() = when (name.uppercase()) {
    "STRENGTH"    -> "Strength"
    "CARDIO"      -> "Cardio"
    "FLEXIBILITY" -> "Flexibility"
    "OTHER"       -> "Other"
    else          -> name.replaceFirstChar { it.uppercase() }
}

fun categoryEmojiFor(name: String?) = when (name?.uppercase()) {
    "STRENGTH"    -> "💪"
    "CARDIO"      -> "🏃"
    "FLEXIBILITY" -> "🧘"
    else          -> "🏋️"
}
