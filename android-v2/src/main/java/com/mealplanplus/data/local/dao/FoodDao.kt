package com.mealplanplus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mealplanplus.data.model.Food
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    // ── Reads (UI) — never show soft-deleted rows ────────────────────────────────
    @Query("SELECT * FROM foods WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllFoods(): Flow<List<Food>>

    @Query(
        "SELECT * FROM foods WHERE deletedAt IS NULL AND " +
        "(name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%') " +
        "ORDER BY updatedAt DESC"
    )
    fun searchFoods(query: String): Flow<List<Food>>

    // ── Local writes ─────────────────────────────────────────────────────────────
    @Upsert
    suspend fun upsert(food: Food)

    @Upsert
    suspend fun upsertAll(foods: List<Food>)

    // ── Sync: push side ──────────────────────────────────────────────────────────
    /** Rows with un-pushed local changes (includes soft-deleted tombstones). */
    @Query("SELECT * FROM foods WHERE dirty = 1")
    suspend fun getDirty(): List<Food>

    /** Clear the dirty flag after a successful push (for non-deleted rows). */
    @Query("UPDATE foods SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearDirty(ids: List<String>)

    /** Physically remove rows (a synced tombstone, or a server-side delete on pull). */
    @Query("DELETE FROM foods WHERE id IN (:ids)")
    suspend fun hardDelete(ids: List<String>)

    // ── Sync: pull side ──────────────────────────────────────────────────────────
    /** ids of locally-dirty rows — a pull must NOT clobber these (un-pushed edits win). */
    @Query("SELECT id FROM foods WHERE dirty = 1")
    suspend fun dirtyIds(): List<String>
}
