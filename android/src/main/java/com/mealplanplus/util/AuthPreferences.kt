package com.mealplanplus.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

object AuthPreferences {
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val USER_ID = longPreferencesKey("user_id")

    fun isLoggedIn(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }
    }

    fun getUserId(context: Context): Flow<Long?> {
        return context.dataStore.data.map { preferences ->
            preferences[USER_ID]
        }
    }

    suspend fun setLoggedIn(context: Context, userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = true
            preferences[USER_ID] = userId
        }
    }

    suspend fun clearAuth(context: Context) {
        context.dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = false
            preferences.remove(USER_ID)
        }
    }

    suspend fun setProviderSubjectMapping(
        context: Context,
        provider: String,
        subject: String,
        userId: Long
    ) {
        val key = longPreferencesKey(providerMappingKey(provider, subject))
        context.dataStore.edit { preferences ->
            preferences[key] = userId
        }
    }

    suspend fun getUserIdForProviderSubject(
        context: Context,
        provider: String,
        subject: String
    ): Long? {
        val key = longPreferencesKey(providerMappingKey(provider, subject))
        return context.dataStore.data.map { prefs -> prefs[key] }.first()
    }

    private fun providerMappingKey(provider: String, subject: String): String {
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(subject.toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "oauth_${provider.lowercase()}_$hash"
    }
}
