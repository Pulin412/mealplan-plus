package com.mealplanplus.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mealplanplus.data.model.CachedResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedResponseDao {

    /** Read the cached payload for a key, or null if nothing is cached yet. */
    @Query("SELECT * FROM cached_response WHERE `key` = :key")
    suspend fun get(key: String): CachedResponse?

    /** Observe a key — reserved for when a screen graduates to observing the DB directly (path B). */
    @Query("SELECT * FROM cached_response WHERE `key` = :key")
    fun observe(key: String): Flow<CachedResponse?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(row: CachedResponse)

    /** Drop the whole cache — called on logout so the next account never reads stale data. */
    @Query("DELETE FROM cached_response")
    suspend fun clearAll()
}
