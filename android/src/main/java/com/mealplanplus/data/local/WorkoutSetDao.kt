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

    @Query("DELETE FROM workout_sets WHERE sessionId IN (:sessionIds)")
    suspend fun deleteAllForSessions(sessionIds: List<Long>)

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions sess ON sess.id = ws.sessionId
        WHERE ws.exerciseId = :exerciseId
          AND (sess.userId = :userId OR sess.userId = '')
          AND sess.isCompleted = 1
          AND sess.id != :excludeSessionId
        ORDER BY sess.date DESC, ws.setNumber ASC
        LIMIT 50
    """)
    suspend fun getLastSetsForExercise(userId: String, exerciseId: Long, excludeSessionId: Long): List<WorkoutSet>

    // ── Backup ────────────────────────────────────────────────────────────────
    @Query("SELECT * FROM workout_sets ORDER BY sessionId, setNumber")
    suspend fun getAllSetsOnce(): List<WorkoutSet>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun upsert(set: WorkoutSet): Long
}
