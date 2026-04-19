package com.mealplanplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mealplanplus.data.model.PlannedWorkout
import com.mealplanplus.data.model.PlannedWorkoutWithTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedWorkoutDao {

    @Transaction
    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND date = :date")
    fun getPlannedForDate(userId: String, date: Long): Flow<List<PlannedWorkoutWithTemplate>>

    @Transaction
    @Query("SELECT * FROM planned_workouts WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date ASC")
    fun getPlannedInRange(userId: String, from: Long, to: Long): Flow<List<PlannedWorkoutWithTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun plan(plannedWorkout: PlannedWorkout): Long

    @Delete
    suspend fun unplan(plannedWorkout: PlannedWorkout)

    @Query("DELETE FROM planned_workouts WHERE userId = :userId AND date = :date AND templateId = :templateId")
    suspend fun unplanByKey(userId: String, date: Long, templateId: Long)

    @Query("DELETE FROM planned_workouts WHERE userId = :userId AND date = :date")
    suspend fun clearDay(userId: String, date: Long)
}
