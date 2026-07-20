package com.mealplanplus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mealplanplus.data.model.Meal
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {

    @Query("SELECT * FROM meals WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllMeals(): Flow<List<Meal>>

    @Upsert
    suspend fun upsert(meal: Meal)

    @Upsert
    suspend fun upsertAll(meals: List<Meal>)

    // ── Sync ─────────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM meals WHERE dirty = 1")
    suspend fun getDirty(): List<Meal>

    @Query("UPDATE meals SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearDirty(ids: List<String>)

    @Query("DELETE FROM meals WHERE id IN (:ids)")
    suspend fun hardDelete(ids: List<String>)

    @Query("SELECT id FROM meals WHERE dirty = 1")
    suspend fun dirtyIds(): List<String>
}
