package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Offline-first Food. The **UUID `id` is the single identity** — client-generated on
 * create, used as the Room primary key AND the server `serverId`. Sync is an idempotent
 * upsert-by-UUID, so pulling the same record never duplicates it.
 *
 * Sync bookkeeping:
 *  - [dirty]     — has local changes not yet pushed to the server.
 *  - [deletedAt] — soft-delete tombstone; the row is hidden from the UI and physically
 *                  removed only after the delete is confirmed synced.
 */
@Entity(tableName = "foods")
data class Food(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val brand: String? = null,
    val servingLabel: String? = null,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    /** The food's natural measurement unit (GRAM/ML/PIECE/CUP/TBSP/TSP). Stored as the enum name. */
    val unit: String = "GRAM",
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null,
    val isFavorite: Boolean = false,
    val isSystemFood: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val dirty: Boolean = false,
    val deletedAt: Long? = null,
)
