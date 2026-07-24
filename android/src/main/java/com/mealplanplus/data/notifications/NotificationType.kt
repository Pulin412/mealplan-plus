package com.mealplanplus.data.notifications

/** Minutes since midnight for a wall-clock time. Top-level so enum constants can use it at construction. */
private fun t(hour: Int, minute: Int) = hour * 60 + minute

/**
 * The five reminder types shown in Settings. Each carries a fixed default schedule (daily times, or a
 * weekly day + time), the notification copy, and a stable [requestBase] for AlarmManager PendingIntent
 * identity. Times are minutes-since-midnight. Editing these times in-app is a follow-up — for now they
 * are sensible fixed defaults (decision: fixed configurable times, 2026-07-24).
 */
enum class NotificationType(
    val key: String,
    val title: String,
    val body: String,
    val defaultEnabled: Boolean,
    val requestBase: Int,
    /** Daily fire times (minutes since midnight). Empty when [weeklyDayIso] is set. */
    val dailyTimes: List<Int>,
    /** ISO day-of-week 1=Mon…7=Sun; null = fires daily. */
    val weeklyDayIso: Int? = null,
    /** Fire time (minutes since midnight) when [weeklyDayIso] is set. */
    val weeklyTime: Int = 0,
) {
    MEAL(
        key = "meals", title = "Time to log your meal", body = "Tap to log what you ate and keep your streak.",
        defaultEnabled = true, requestBase = 1000,
        dailyTimes = listOf(t(8, 0), t(13, 0), t(19, 0)),
    ),
    WATER(
        key = "water", title = "Hydration reminder", body = "Time for a glass of water.",
        defaultEnabled = true, requestBase = 1100,
        dailyTimes = listOf(t(8, 0), t(10, 0), t(12, 0), t(14, 0), t(16, 0), t(18, 0), t(20, 0)),
    ),
    WORKOUT(
        key = "workout", title = "Workout reminder", body = "Don't forget today's workout.",
        defaultEnabled = true, requestBase = 1200,
        dailyTimes = listOf(t(18, 0)),
    ),
    WEIGHIN(
        key = "weighin", title = "Weekly weigh-in", body = "Log your weight to track progress.",
        defaultEnabled = false, requestBase = 1300,
        dailyTimes = emptyList(), weeklyDayIso = 7, weeklyTime = t(8, 0),
    ),
    GLUCOSE(
        key = "glucose", title = "Glucose check", body = "Time to check your glucose.",
        defaultEnabled = true, requestBase = 1400,
        dailyTimes = listOf(t(7, 30), t(9, 0), t(12, 30), t(14, 0), t(18, 30), t(20, 0)),
    );

    /** The number of alarm slots this type schedules (one per daily time, or one for the weekly slot). */
    val slotCount: Int get() = if (weeklyDayIso != null) 1 else dailyTimes.size

    companion object {
        fun fromKey(key: String): NotificationType? = entries.firstOrNull { it.key == key }
    }
}
