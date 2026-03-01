package com.mealplanplus.shared.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mealplan_prefs")

class AndroidPreferencesManager(private val context: Context) : PreferencesManager {

    companion object {
        // Auth keys
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        private val USER_ID = longPreferencesKey("user_id")

        // Theme keys
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        private val FOLLOW_SYSTEM = booleanPreferencesKey("follow_system")

        // Sync keys
        private val LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    // Auth
    override fun isLoggedIn(): Flow<Boolean> {
        return context.dataStore.data.map { it[IS_LOGGED_IN] ?: false }
    }

    override fun getUserId(): Flow<Long?> {
        return context.dataStore.data.map { it[USER_ID] }
    }

    override suspend fun setLoggedIn(userId: Long) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = true
            prefs[USER_ID] = userId
        }
    }

    override suspend fun clearAuth() {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN] = false
            prefs.remove(USER_ID)
        }
    }

    // Theme
    override fun isDarkMode(): Flow<Boolean> {
        return context.dataStore.data.map { it[DARK_MODE] ?: false }
    }

    override fun isDynamicColor(): Flow<Boolean> {
        return context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }
    }

    override fun isFollowSystem(): Flow<Boolean> {
        return context.dataStore.data.map { it[FOLLOW_SYSTEM] ?: true }
    }

    override suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = enabled }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setFollowSystem(enabled: Boolean) {
        context.dataStore.edit { it[FOLLOW_SYSTEM] = enabled }
    }

    // Sync
    override suspend fun getLastSyncTime(): Long {
        return context.dataStore.data.map { it[LAST_SYNC_TIME] ?: 0L }.firstOrNull() ?: 0L
    }

    override suspend fun setLastSyncTime(timestamp: Long) {
        context.dataStore.edit { it[LAST_SYNC_TIME] = timestamp }
    }
}

actual class PreferencesManagerFactory(private val context: Context) {
    actual fun create(): PreferencesManager = AndroidPreferencesManager(context)
}
