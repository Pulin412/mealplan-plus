package com.mealplanplus.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSessionWithSets
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY date DESC")
    fun getSessions(userId: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND date = :date ORDER BY createdAt DESC")
    fun getSessionsForDate(userId: String, date: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getSessionsInRange(userId: String, from: Long, to: Long): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithSets(sessionId: Long): WorkoutSessionWithSets?

    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Update
    suspend fun update(session: WorkoutSession)

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE serverId = :serverId LIMIT 1")
    suspend fun getByServerId(serverId: String): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE userId = :userId AND (syncedAt IS NULL OR syncedAt < updatedAt)")
    suspend fun getUnsyncedSessions(userId: String): List<WorkoutSession>
}
