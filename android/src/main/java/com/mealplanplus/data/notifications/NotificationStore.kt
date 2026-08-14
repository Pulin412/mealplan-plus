package com.mealplanplus.data.notifications

import android.content.Context
import com.mealplanplus.data.model.MEAL_SLOTS
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** One meal-slot reminder: the slot name, whether it fires, and its wall-clock time (minutes/midnight). */
data class MealSlotReminder(val slot: String, val enabled: Boolean, val minute: Int)

/** Immutable snapshot of the user's notification preferences. */
data class NotificationSettings(
    /** One entry per canonical meal slot, in [MEAL_SLOTS] order. */
    val mealSlots: List<MealSlotReminder>,
    val waterEnabled: Boolean,
    val workoutEnabled: Boolean,
    val workoutMinute: Int,
    val weighinEnabled: Boolean,
    val weighinDayIso: Int,   // 1=Mon … 7=Sun
    val weighinMinute: Int,
    val quietHours: Boolean,
    val quietStartMin: Int,
    val quietEndMin: Int,
    /** Master switch — when off, no reminders fire regardless of the per-type flags. */
    val masterEnabled: Boolean = true,
) {
    val anyMealOn: Boolean get() = mealSlots.any { it.enabled }

    /** Categories currently on — for the "N of M on" header. */
    val onCount: Int get() = listOf(anyMealOn, waterEnabled, workoutEnabled, weighinEnabled).count { it }

    /** True when [minuteOfDay] falls inside the quiet window (which may wrap past midnight). */
    fun inQuietHours(minuteOfDay: Int): Boolean {
        if (!quietHours) return false
        return if (quietStartMin <= quietEndMin) minuteOfDay in quietStartMin until quietEndMin
        else minuteOfDay >= quietStartMin || minuteOfDay < quietEndMin
    }

    /** Is this alarm slot enabled? Master off disables everything; else per meal slot / per type flag. */
    fun isEnabled(type: NotificationType, slot: Int): Boolean {
        if (!masterEnabled) return false
        return when (type) {
            NotificationType.MEAL -> mealSlots.getOrNull(slot)?.enabled == true
            NotificationType.WATER -> waterEnabled
            NotificationType.WORKOUT -> workoutEnabled
            NotificationType.WEIGHIN -> weighinEnabled
        }
    }

    /** How many alarm slots this type schedules. */
    fun slotCount(type: NotificationType): Int = when (type) {
        NotificationType.MEAL -> mealSlots.size
        NotificationType.WATER -> type.dailyTimes.size
        NotificationType.WORKOUT, NotificationType.WEIGHIN -> 1
    }

    /** (minuteOfDay, weeklyDayIso) — weeklyDayIso is null for a daily reminder. */
    fun timeFor(type: NotificationType, slot: Int): Pair<Int, Int?> = when (type) {
        NotificationType.MEAL -> (mealSlots.getOrNull(slot)?.minute ?: 0) to null
        NotificationType.WATER -> type.dailyTimes.getOrElse(slot) { 0 } to null
        NotificationType.WORKOUT -> workoutMinute to null
        NotificationType.WEIGHIN -> weighinMinute to weighinDayIso
    }
}

/**
 * Persists notification preferences in the "notifications" SharedPreferences and exposes them as a
 * [StateFlow] for the Settings UI. Meal reminders are per-slot (each opt-in with its own time); workout
 * and weigh-in carry an editable time (weigh-in also a weekday). Water keeps its fixed schedule.
 */
@Singleton
class NotificationStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("notifications", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<NotificationSettings> = _state

    // ── Meals (per slot) ─────────────────────────────────────────────────────────
    fun setMealSlotEnabled(slot: String, on: Boolean) {
        prefs.edit().putBoolean(mealEnKey(slot), on).apply(); _state.value = read()
    }

    fun setMealSlotTime(slot: String, minute: Int) {
        prefs.edit().putInt(mealMinKey(slot), minute).apply(); _state.value = read()
    }

    // ── Water / Workout / Weigh-in master toggles ────────────────────────────────
    fun setEnabled(type: NotificationType, on: Boolean) {
        prefs.edit().putBoolean(enabledKey(type), on).apply(); _state.value = read()
    }

    fun setWorkoutTime(minute: Int) { prefs.edit().putInt(KEY_WORKOUT_MIN, minute).apply(); _state.value = read() }
    fun setWeighinTime(minute: Int) { prefs.edit().putInt(KEY_WEIGHIN_MIN, minute).apply(); _state.value = read() }
    fun setWeighinDay(dayIso: Int) { prefs.edit().putInt(KEY_WEIGHIN_DAY, dayIso).apply(); _state.value = read() }

    fun setQuietHours(on: Boolean) { prefs.edit().putBoolean(KEY_QUIET, on).apply(); _state.value = read() }

    fun setMaster(on: Boolean) { prefs.edit().putBoolean(KEY_MASTER, on).apply(); _state.value = read() }

    private fun read(): NotificationSettings = NotificationSettings(
        mealSlots = MEAL_SLOTS.map { slot ->
            MealSlotReminder(
                slot = slot,
                enabled = prefs.getBoolean(mealEnKey(slot), false),
                minute = prefs.getInt(mealMinKey(slot), MEAL_SLOT_DEFAULTS[slot] ?: t(12, 0)),
            )
        },
        waterEnabled = prefs.getBoolean(enabledKey(NotificationType.WATER), NotificationType.WATER.defaultEnabled),
        workoutEnabled = prefs.getBoolean(enabledKey(NotificationType.WORKOUT), NotificationType.WORKOUT.defaultEnabled),
        workoutMinute = prefs.getInt(KEY_WORKOUT_MIN, t(18, 0)),
        weighinEnabled = prefs.getBoolean(enabledKey(NotificationType.WEIGHIN), NotificationType.WEIGHIN.defaultEnabled),
        weighinDayIso = prefs.getInt(KEY_WEIGHIN_DAY, 7),
        weighinMinute = prefs.getInt(KEY_WEIGHIN_MIN, t(8, 0)),
        quietHours = prefs.getBoolean(KEY_QUIET, true),
        quietStartMin = QUIET_START_MIN,
        quietEndMin = QUIET_END_MIN,
        masterEnabled = prefs.getBoolean(KEY_MASTER, true),
    )

    private fun enabledKey(type: NotificationType) = "enabled_${type.key}"
    private fun mealEnKey(slot: String) = "meal_en_$slot"
    private fun mealMinKey(slot: String) = "meal_min_$slot"

    private companion object {
        const val KEY_QUIET = "quiet_hours"
        const val KEY_MASTER = "master_enabled"
        const val KEY_WORKOUT_MIN = "workout_min"
        const val KEY_WEIGHIN_MIN = "weighin_min"
        const val KEY_WEIGHIN_DAY = "weighin_day"
        const val QUIET_START_MIN = 22 * 60 // 10 PM
        const val QUIET_END_MIN = 7 * 60    // 7 AM

        /** Sensible default time each meal slot uses when first enabled. */
        val MEAL_SLOT_DEFAULTS: Map<String, Int> = mapOf(
            "Early Morning" to t(6, 30), "Breakfast" to t(8, 0), "Noon" to t(12, 0),
            "Lunch" to t(13, 0), "Evening" to t(17, 0), "Pre-Workout" to t(17, 30),
            "Post-Workout" to t(19, 0), "Dinner" to t(20, 0), "Post-Dinner" to t(21, 30),
        )
    }
}
