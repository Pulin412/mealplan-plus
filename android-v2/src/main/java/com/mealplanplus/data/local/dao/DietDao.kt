package com.mealplanplus.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mealplanplus.data.model.Diet
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {

    @Query("SELECT * FROM diets WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun getAllDiets(): Flow<List<Diet>>

    @Upsert
    suspend fun upsert(diet: Diet)

    @Upsert
    suspend fun upsertAll(diets: List<Diet>)

    // ── Sync ─────────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM diets WHERE dirty = 1")
    suspend fun getDirty(): List<Diet>

    @Query("UPDATE diets SET dirty = 0 WHERE id IN (:ids)")
    suspend fun clearDirty(ids: List<String>)

    @Query("DELETE FROM diets WHERE id IN (:ids)")
    suspend fun hardDelete(ids: List<String>)

    @Query("SELECT id FROM diets WHERE dirty = 1")
    suspend fun dirtyIds(): List<String>
}
