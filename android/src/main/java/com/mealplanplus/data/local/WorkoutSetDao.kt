package com.mealplanplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mealplanplus.data.model.WorkoutSet
import com.mealplanplus.data.model.WorkoutSetWithExercise
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Transaction
    @Query("SELECT * FROM workout_sets WHERE sessionId = :sessionId ORDER BY setNumber")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSetWithExercise>>

    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Update
    suspend fun update(set: WorkoutSet)

    @Delete
    suspend fun delete(set: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: Long)
}
