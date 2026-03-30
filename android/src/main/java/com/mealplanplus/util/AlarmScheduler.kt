package com.mealplanplus.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mealplanplus.notification.NotificationAlarmReceiver
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters

/**
 * Each notification alarm type with a stable [requestCode] used for PendingIntent identity
 * (i.e. to cancel or replace an alarm). [slotType] is passed as an extra for meal alarms.
 */
enum class NotificationAlarmType(val requestCode: Int, val slotType: String?) {
    BREAKFAST(1, "BREAKFAST"),
    LUNCH(2, "LUNCH"),
    DINNER(3, "DINNER"),
    STREAK(4, null),
    WEEKLY_PLAN(5, null)
}

/**
 * Scheduling utilities for exact notification alarms.
 *
 * The time-calculation functions ([nextTriggerMillis], [nextMondayTriggerMillis]) are pure and
 * injectable with a [Clock] — making them fully unit-testable without Android dependencies.
 *
 * The Android-calling methods ([scheduleAlarm], [cancelAlarm], etc.) wrap [AlarmManager] with a
 * graceful fallback for devices that deny the SCHEDULE_EXACT_ALARM permission.
 */
object AlarmScheduler {

    // ── Pure time calculations ────────────────────────────────────────────────

    /**
     * Returns the epoch-millisecond timestamp for the next occurrence of [hour]:[minute].
     * If that time has already passed today (or is exactly now), returns tomorrow's occurrence.
     */
    fun nextTriggerMillis(
        hour: Int,
        minute: Int,
        clock: Clock = Clock.systemDefaultZone()
    ): Long {
        val now = LocalDateTime.now(clock)
        var target = now.toLocalDate().atTime(hour, minute)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return target.atZone(clock.zone).toInstant().toEpochMilli()
    }

    /**
     * Returns the epoch-millisecond timestamp for the next Monday at [hour]:[minute].
     * If today IS Monday, returns the following Monday (prevents double-firing on the same day).
     */
    fun nextMondayTriggerMillis(
        hour: Int,
        minute: Int,
        clock: Clock = Clock.systemDefaultZone()
    ): Long {
        val today = LocalDateTime.now(clock).toLocalDate()
        // Always move to NEXT Monday (strictly in the future)
        val nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        return nextMonday.atTime(hour, minute)
            .atZone(clock.zone)
            .toInstant()
            .toEpochMilli()
    }

    // ── Android AlarmManager calls ────────────────────────────────────────────

    /**
     * Schedules an exact alarm for [type] at [triggerAtMillis].
     * Falls back to [AlarmManager.setAndAllowWhileIdle] on API 31+ if exact alarm permission
     * has not been granted by the user.
     */
    fun scheduleAlarm(context: Context, type: NotificationAlarmType, triggerAtMillis: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmPendingIntent(context, type)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Near-exact fallback — may be delayed by a few minutes in Doze
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    /** Cancels any pending alarm for [type]. Safe to call even if no alarm is scheduled. */
    fun cancelAlarm(context: Context, type: NotificationAlarmType) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(alarmPendingIntent(context, type))
    }

    /** Cancels all five notification alarm types. Called when the master toggle is turned off. */
    fun cancelAllNotificationAlarms(context: Context) {
        NotificationAlarmType.entries.forEach { cancelAlarm(context, it) }
    }

    /** Convenience: compute next trigger time and call [scheduleAlarm]. */
    fun scheduleMealAlarm(
        context: Context,
        type: NotificationAlarmType,
        hour: Int,
        minute: Int
    ) {
        scheduleAlarm(context, type, nextTriggerMillis(hour, minute))
    }

    /** Convenience: schedule the weekly plan alarm for the next Monday at 08:00. */
    fun scheduleWeeklyPlanAlarm(context: Context) {
        scheduleAlarm(context, NotificationAlarmType.WEEKLY_PLAN, nextMondayTriggerMillis(8, 0))
    }

    // ── PendingIntent construction ────────────────────────────────────────────

    internal fun alarmPendingIntent(
        context: Context,
        type: NotificationAlarmType
    ): PendingIntent {
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            action = NotificationAlarmReceiver.ACTION_NOTIFICATION_ALARM
            putExtra(NotificationAlarmReceiver.EXTRA_ALARM_TYPE, type.name)
        }
        return PendingIntent.getBroadcast(
            context,
            type.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
