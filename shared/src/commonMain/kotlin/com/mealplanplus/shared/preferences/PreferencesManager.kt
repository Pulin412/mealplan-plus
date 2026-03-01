package com.mealplanplus.shared.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Cross-platform preferences manager interface
 */
interface PreferencesManager {
    // Auth preferences
    fun isLoggedIn(): Flow<Boolean>
    fun getUserId(): Flow<Long?>
    suspend fun setLoggedIn(userId: Long)
    suspend fun clearAuth()

    // Theme preferences
    fun isDarkMode(): Flow<Boolean>
    fun isDynamicColor(): Flow<Boolean>
    fun isFollowSystem(): Flow<Boolean>
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setFollowSystem(enabled: Boolean)

    // Sync preferences
    suspend fun getLastSyncTime(): Long
    suspend fun setLastSyncTime(timestamp: Long)
}

/**
 * Factory to create platform-specific PreferencesManager
 */
expect class PreferencesManagerFactory {
    fun create(): PreferencesManager
}
