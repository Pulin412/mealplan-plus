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
    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND isCompleted = 1 ORDER BY date DESC")
    fun getSessions(userId: String): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND isCompleted = 1 ORDER BY date DESC")
    fun getSessionsWithSets(userId: String): Flow<List<WorkoutSessionWithSets>>

    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND date = :date ORDER BY createdAt DESC")
    fun getSessionsForDate(userId: String, date: Long): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND date BETWEEN :from AND :to ORDER BY date DESC")
    fun getSessionsInRange(userId: String, from: Long, to: Long): Flow<List<WorkoutSession>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithSets(sessionId: Long): WorkoutSessionWithSets?

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSessionWithSets(sessionId: Long): Flow<WorkoutSessionWithSets?>

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

    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND date BETWEEN :from AND :to ORDER BY date DESC")
    suspend fun getSessionsInRangeOnce(userId: String, from: Long, to: Long): List<WorkoutSession>

    @Query("DELETE FROM workout_sessions WHERE (userId = :userId OR userId = '') AND date BETWEEN :from AND :to")
    suspend fun deleteSessionsInRange(userId: String, from: Long, to: Long)

    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    fun observeInProgressSession(userId: String): Flow<WorkoutSession?>

    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getInProgressSession(userId: String): WorkoutSession?

    // ── Backup ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM workout_sessions WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAllSessionsOnce(userId: String): List<WorkoutSession>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WorkoutSession): Long

    @Query("UPDATE workout_sessions SET isCompleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markCompleted(id: Long, updatedAt: Long)

    @Query("UPDATE workout_sessions SET isCompleted = 1, updatedAt = :updatedAt WHERE (userId = :userId OR userId = '') AND isCompleted = 0")
    suspend fun markAllInProgressCompleted(userId: String, updatedAt: Long)

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE (userId = :userId OR userId = '') AND date = :date AND isCompleted = 1 ORDER BY createdAt DESC")
    fun getCompletedSessionsWithSetsForDate(userId: String, date: Long): Flow<List<WorkoutSessionWithSets>>
}
