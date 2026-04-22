package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.ExerciseCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseCategoryDao {

    @Query("SELECT * FROM exercise_categories ORDER BY isSystem DESC, name ASC")
    fun getAllCategories(): Flow<List<ExerciseCategoryEntity>>

    @Query("SELECT COUNT(*) FROM exercise_categories WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: ExerciseCategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<ExerciseCategoryEntity>)

    @Delete
    suspend fun delete(category: ExerciseCategoryEntity)
}
