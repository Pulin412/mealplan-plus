package com.mealplanplus.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the user's notification preferences in the shared DataStore ("settings").
 *
 * Keys are prefixed with "notifications_" to avoid collisions with other preference
 * namespaces ([ThemePreferences], [AuthPreferences], [WidgetPreferences]).
 *
 * Default values:
 * - Master toggle: **off** (user must explicitly enable; required for Android 13 permission)
 * - Individual toggles: **on** (enabled once the master is turned on)
 * - Breakfast: 8 AM  |  Lunch: 1 PM  |  Dinner: 7 PM  |  Streak alert: 9 PM
 */
object NotificationPreferences {

    // ── Keys ──────────────────────────────────────────────────────────────────

    private val MASTER_ENABLED = booleanPreferencesKey("notifications_master_enabled")
    private val MEAL_REMINDERS_ENABLED = booleanPreferencesKey("notifications_meal_reminders_enabled")
    private val STREAK_PROTECTION_ENABLED = booleanPreferencesKey("notifications_streak_protection_enabled")
    private val WEEKLY_PLAN_ENABLED = booleanPreferencesKey("notifications_weekly_plan_enabled")
    private val BREAKFAST_HOUR = intPreferencesKey("notifications_breakfast_hour")
    private val LUNCH_HOUR = intPreferencesKey("notifications_lunch_hour")
    private val DINNER_HOUR = intPreferencesKey("notifications_dinner_hour")
    private val STREAK_ALERT_HOUR = intPreferencesKey("notifications_streak_alert_hour")

    // ── Defaults ──────────────────────────────────────────────────────────────

    const val DEFAULT_BREAKFAST_HOUR = 8
    const val DEFAULT_LUNCH_HOUR = 13
    const val DEFAULT_DINNER_HOUR = 19
    const val DEFAULT_STREAK_ALERT_HOUR = 21

    // ── Reads ─────────────────────────────────────────────────────────────────

    fun getMasterEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[MASTER_ENABLED] ?: false }

    fun getMealRemindersEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[MEAL_REMINDERS_ENABLED] ?: true }

    fun getStreakProtectionEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[STREAK_PROTECTION_ENABLED] ?: true }

    fun getWeeklyPlanEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[WEEKLY_PLAN_ENABLED] ?: true }

    fun getBreakfastHour(context: Context): Flow<Int> =
        context.dataStore.data.map { it[BREAKFAST_HOUR] ?: DEFAULT_BREAKFAST_HOUR }

    fun getLunchHour(context: Context): Flow<Int> =
        context.dataStore.data.map { it[LUNCH_HOUR] ?: DEFAULT_LUNCH_HOUR }

    fun getDinnerHour(context: Context): Flow<Int> =
        context.dataStore.data.map { it[DINNER_HOUR] ?: DEFAULT_DINNER_HOUR }

    fun getStreakAlertHour(context: Context): Flow<Int> =
        context.dataStore.data.map { it[STREAK_ALERT_HOUR] ?: DEFAULT_STREAK_ALERT_HOUR }

    // ── Writes ────────────────────────────────────────────────────────────────

    suspend fun setMasterEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[MASTER_ENABLED] = enabled }
    }

    suspend fun setMealRemindersEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[MEAL_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setStreakProtectionEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[STREAK_PROTECTION_ENABLED] = enabled }
    }

    suspend fun setWeeklyPlanEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[WEEKLY_PLAN_ENABLED] = enabled }
    }

    suspend fun setBreakfastHour(context: Context, hour: Int) {
        context.dataStore.edit { it[BREAKFAST_HOUR] = hour }
    }

    suspend fun setLunchHour(context: Context, hour: Int) {
        context.dataStore.edit { it[LUNCH_HOUR] = hour }
    }

    suspend fun setDinnerHour(context: Context, hour: Int) {
        context.dataStore.edit { it[DINNER_HOUR] = hour }
    }

    suspend fun setStreakAlertHour(context: Context, hour: Int) {
        context.dataStore.edit { it[STREAK_ALERT_HOUR] = hour }
    }
}
