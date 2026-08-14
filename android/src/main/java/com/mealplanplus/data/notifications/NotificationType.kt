package com.mealplanplus.data.notifications

/** Minutes since midnight for a wall-clock time. Top-level so enum constants can use it at construction. */
internal fun t(hour: Int, minute: Int) = hour * 60 + minute

/**
 * The reminder categories shown in Settings, carrying only their identity + copy. Editable schedules
 * (meal per-slot times, workout time, weigh-in day+time) live in [NotificationStore] now — the enum no
 * longer hard-codes them. WATER keeps a fixed daily schedule (not user-editable). [requestBase] gives
 * each type a stable AlarmManager PendingIntent id block (one code per slot).
 */
enum class NotificationType(
    val key: String,
    val title: String,
    val body: String,
    val defaultEnabled: Boolean,
    val requestBase: Int,
    /** Fixed daily fire times (minutes since midnight). Used only by WATER; others schedule from the store. */
    val dailyTimes: List<Int> = emptyList(),
) {
    MEAL(
        key = "meals", title = "Time to log your meal", body = "Tap to log what you ate and keep your streak.",
        defaultEnabled = false, requestBase = 1000,
    ),
    WATER(
        key = "water", title = "Hydration reminder", body = "Time for a glass of water.",
        defaultEnabled = true, requestBase = 1100,
        dailyTimes = listOf(t(8, 0), t(10, 0), t(12, 0), t(14, 0), t(16, 0), t(18, 0), t(20, 0)),
    ),
    WORKOUT(
        key = "workout", title = "Workout reminder", body = "Don't forget today's workout.",
        defaultEnabled = true, requestBase = 1200,
    ),
    WEIGHIN(
        key = "weighin", title = "Weigh-in", body = "Log your weight to track progress.",
        defaultEnabled = false, requestBase = 1300,
    );

    companion object {
        fun fromKey(key: String): NotificationType? = entries.firstOrNull { it.key == key }
    }
}
