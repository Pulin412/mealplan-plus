package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A named collection of foods. Same offline-first shape as [Food]: client-generated UUID
 * identity, dirty flag, and soft-delete tombstone. Its food entries are embedded as JSON
 * ([items]) so a meal is one syncable record.
 */
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val slots: List<String> = emptyList(),
    val items: List<MealItem> = emptyList(),
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = false,
    val deletedAt: Long? = null,
)

/** One food entry in a meal — references a [Food] by its UUID (`Food.id`). */
data class MealItem(
    val foodServerId: String,
    val quantity: Double,
    val unit: String = "GRAM",
    val notes: String? = null,
)
