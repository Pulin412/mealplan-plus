package com.mealplanplus.shared.preferences

import com.mealplanplus.shared.model.sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import platform.Foundation.NSUserDefaults

class IosPreferencesManager : PreferencesManager {

    private val userDefaults = NSUserDefaults.standardUserDefaults

    companion object {
        // Auth keys
        private const val IS_LOGGED_IN = "is_logged_in"
        private const val USER_ID = "user_id"

        // Theme keys
        private const val DARK_MODE = "dark_mode"
        private const val DYNAMIC_COLOR = "dynamic_color"
        private const val FOLLOW_SYSTEM = "follow_system"

        // Sync keys
        private const val LAST_SYNC_TIME = "last_sync_time"
    }

    // State flows to emit changes (NSUserDefaults doesn't have reactive API)
    private val _isLoggedIn = MutableStateFlow(userDefaults.boolForKey(IS_LOGGED_IN))
    private val _userId = MutableStateFlow(getUserIdFromDefaults())
    private val _darkMode = MutableStateFlow(userDefaults.boolForKey(DARK_MODE))
    private val _dynamicColor = MutableStateFlow(getDynamicColorDefault())
    private val _followSystem = MutableStateFlow(getFollowSystemDefault())

    private fun getUserIdFromDefaults(): Long? {
        val value = userDefaults.objectForKey(USER_ID)
        return if (value != null) userDefaults.integerForKey(USER_ID) else null
    }

    private fun getDynamicColorDefault(): Boolean {
        // Default to true if not set
        return if (userDefaults.objectForKey(DYNAMIC_COLOR) != null) {
            userDefaults.boolForKey(DYNAMIC_COLOR)
        } else true
    }

    private fun getFollowSystemDefault(): Boolean {
        // Default to true if not set
        return if (userDefaults.objectForKey(FOLLOW_SYSTEM) != null) {
            userDefaults.boolForKey(FOLLOW_SYSTEM)
        } else true
    }

    // Auth
    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn

    override fun getUserId(): Flow<Long?> = _userId

    override suspend fun setLoggedIn(userId: Long) {
        userDefaults.setBool(true, IS_LOGGED_IN)
        userDefaults.setInteger(userId, USER_ID)
        _isLoggedIn.value = true
        _userId.value = userId
    }

    override suspend fun clearAuth() {
        userDefaults.setBool(false, IS_LOGGED_IN)
        userDefaults.removeObjectForKey(USER_ID)
        _isLoggedIn.value = false
        _userId.value = null
    }

    // Theme
    override fun isDarkMode(): Flow<Boolean> = _darkMode

    override fun isDynamicColor(): Flow<Boolean> = _dynamicColor

    override fun isFollowSystem(): Flow<Boolean> = _followSystem

    override suspend fun setDarkMode(enabled: Boolean) {
        userDefaults.setBool(enabled, DARK_MODE)
        _darkMode.value = enabled
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        userDefaults.setBool(enabled, DYNAMIC_COLOR)
        _dynamicColor.value = enabled
    }

    override suspend fun setFollowSystem(enabled: Boolean) {
        userDefaults.setBool(enabled, FOLLOW_SYSTEM)
        _followSystem.value = enabled
    }

    // Sync
    override suspend fun getLastSyncTime(): Long {
        val value = userDefaults.objectForKey(LAST_SYNC_TIME)
        return if (value != null) userDefaults.integerForKey(LAST_SYNC_TIME) else 0L
    }

    override suspend fun setLastSyncTime(timestamp: Long) {
        userDefaults.setInteger(timestamp, LAST_SYNC_TIME)
    }

    // OAuth provider mapping
    override suspend fun setProviderMapping(provider: String, subject: String, userId: Long) {
        userDefaults.setInteger(userId, providerMappingKey(provider, subject))
    }

    override suspend fun getProviderMapping(provider: String, subject: String): Long? {
        val key = providerMappingKey(provider, subject)
        val value = userDefaults.objectForKey(key) ?: return null
        val id = userDefaults.integerForKey(key)
        return if (id > 0L) id else null
    }

    private fun providerMappingKey(provider: String, subject: String): String {
        return "oauth_${provider.lowercase()}_${sha256(subject)}"
    }
}

actual class PreferencesManagerFactory {
    actual fun create(): PreferencesManager = IosPreferencesManager()
}
