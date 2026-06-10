package com.mealplanplus.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

object PrepCheckPreferences {
    private fun keyFor(date: LocalDate) = stringSetPreferencesKey("prep_checks_$date")

    fun getChecks(context: Context, date: LocalDate): Flow<Set<String>> =
        context.dataStore.data.map { it[keyFor(date)] ?: emptySet() }

    suspend fun toggle(context: Context, date: LocalDate, itemKey: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[keyFor(date)] ?: emptySet()
            prefs[keyFor(date)] = if (itemKey in current) current - itemKey else current + itemKey
        }
    }
}
