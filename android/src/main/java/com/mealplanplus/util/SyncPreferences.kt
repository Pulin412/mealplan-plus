package com.mealplanplus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SyncPreferences {
    private val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")

    fun getLastSyncTimestamp(context: Context): Flow<Long> =
        context.dataStore.data.map { it[LAST_SYNC_TIMESTAMP] ?: 0L }

    suspend fun setLastSyncTimestamp(context: Context, timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNC_TIMESTAMP] = timestamp }
    }
}
