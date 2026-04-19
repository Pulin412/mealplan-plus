package com.mealplanplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mealplanplus.data.model.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE isSystem = 1 OR userId = :userId ORDER BY category, name")
    fun getAllForUser(userId: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises ORDER BY category, name")
    fun getAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE (isSystem = 1 OR userId = :userId) AND category = :category ORDER BY name")
    fun getByCategoryForUser(userId: String, category: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE category = :category ORDER BY name")
    fun getByCategory(category: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(exercises: List<Exercise>)

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM exercises WHERE isSystem = 1")
    suspend fun getSystemExerciseCount(): Int
}
