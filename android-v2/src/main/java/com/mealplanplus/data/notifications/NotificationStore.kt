package com.mealplanplus.data.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Immutable snapshot of the user's notification preferences. */
data class NotificationSettings(
    val enabled: Map<NotificationType, Boolean>,
    val quietHours: Boolean,
    val quietStartMin: Int,
    val quietEndMin: Int,
) {
    val onCount: Int get() = enabled.count { it.value }

    /** True when [minuteOfDay] falls inside the quiet window (which may wrap past midnight). */
    fun inQuietHours(minuteOfDay: Int): Boolean {
        if (!quietHours) return false
        return if (quietStartMin <= quietEndMin) minuteOfDay in quietStartMin until quietEndMin
        else minuteOfDay >= quietStartMin || minuteOfDay < quietEndMin
    }
}

/**
 * Persists the 5 reminder toggles + quiet-hours flag in the "notifications" SharedPreferences and
 * exposes them as a [StateFlow] for the Settings UI — mirrors the [com.mealplanplus.ui.theme.ThemeStore]
 * pattern. Defaults come from each [NotificationType]; quiet hours default on, 10 PM – 7 AM.
 */
@Singleton
class NotificationStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<NotificationSettings> = _state

    fun setEnabled(type: NotificationType, on: Boolean) {
        prefs.edit().putBoolean(key(type), on).apply()
        _state.value = read()
    }

    fun setQuietHours(on: Boolean) {
        prefs.edit().putBoolean(KEY_QUIET, on).apply()
        _state.value = read()
    }

    private fun read(): NotificationSettings = NotificationSettings(
        enabled = NotificationType.entries.associateWith { prefs.getBoolean(key(it), it.defaultEnabled) },
        quietHours = prefs.getBoolean(KEY_QUIET, true),
        quietStartMin = QUIET_START_MIN,
        quietEndMin = QUIET_END_MIN,
    )

    private fun key(type: NotificationType) = "enabled_${type.key}"

    private companion object {
        const val KEY_QUIET = "quiet_hours"
        const val QUIET_START_MIN = 22 * 60 // 10 PM
        const val QUIET_END_MIN = 7 * 60    // 7 AM
    }
}
