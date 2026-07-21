package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A day-plan template: a named, slot-grouped collection of meals and/or foods. Same
 * offline-first shape as [Meal] — client-generated UUID identity, dirty flag, soft-delete
 * tombstone — with its [entries] embedded as JSON so a diet is one syncable record.
 */
@Entity(tableName = "diets")
data class Diet(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val entries: List<DietEntry> = emptyList(),
    val tags: List<DietTag> = emptyList(),
    val targetCalories: Double? = null,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = false,
    val deletedAt: Long? = null,
)

/**
 * A tag assigned to a diet. Tags are server-managed entities (normalized Tag/EntityTag),
 * so [id] is the server tag id used for sync (`DietDto.tagIds`); [name]/[color] are carried
 * for display and filtering.
 */
data class DietTag(
    val id: Long,
    val name: String,
    val color: String? = null,
)

enum class DietEntryKind { MEAL, FOOD }

/**
 * One entry in a diet, tagged to a [slot]. A MEAL entry references a [Meal] by its UUID;
 * a FOOD entry references a [Food] by its UUID with a [quantity] in [unit].
 */
data class DietEntry(
    val kind: DietEntryKind,
    val refServerId: String,   // Meal.id or Food.id (UUID)
    val slot: String,
    val quantity: Double = 1.0,
    val unit: String = "GRAM", // FOOD entries only; meals ignore
)
