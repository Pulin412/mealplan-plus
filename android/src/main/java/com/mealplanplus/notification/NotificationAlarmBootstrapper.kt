package com.mealplanplus.notification

import android.content.Context
import com.mealplanplus.util.AlarmScheduler
import com.mealplanplus.util.NotificationAlarmType
import com.mealplanplus.util.NotificationPreferences
import kotlinx.coroutines.flow.firstOrNull

/**
 * Coordinator that reads all notification preferences and schedules / cancels AlarmManager
 * alarms accordingly.
 *
 * Called from:
 * - [com.mealplanplus.MealPlanApp.onCreate] on every app launch
 * - [BootReceiver] after device reboot
 * - [com.mealplanplus.ui.screens.settings.SettingsViewModel] when the user changes a preference
 */
object NotificationAlarmBootstrapper {

    /**
     * Reads all notification preferences and (re)schedules or cancels alarms to match.
     * If the master toggle is off, all alarms are cancelled.
     */
    suspend fun scheduleAll(context: Context) {
        val masterEnabled = NotificationPreferences.getMasterEnabled(context).firstOrNull() ?: false
        if (!masterEnabled) {
            AlarmScheduler.cancelAllNotificationAlarms(context)
            return
        }

        val mealEnabled   = NotificationPreferences.getMealRemindersEnabled(context).firstOrNull() ?: true
        val streakEnabled = NotificationPreferences.getStreakProtectionEnabled(context).firstOrNull() ?: true
        val weeklyEnabled = NotificationPreferences.getWeeklyPlanEnabled(context).firstOrNull() ?: true

        scheduleMealAlarms(context, mealEnabled)
        scheduleStreakAlarm(context, streakEnabled)
        scheduleWeeklyPlanAlarm(context, weeklyEnabled)
    }

    /**
     * Cancels the existing alarm for [type] and immediately re-schedules it using the latest
     * preferences. Used when the user changes a time setting in the UI.
     */
    suspend fun rescheduleForType(context: Context, type: NotificationAlarmType) {
        AlarmScheduler.cancelAlarm(context, type)
        when (type) {
            NotificationAlarmType.BREAKFAST,
            NotificationAlarmType.LUNCH,
            NotificationAlarmType.DINNER -> {
                val (hour, minute) = readHourMinuteForMealType(context, type)
                AlarmScheduler.scheduleMealAlarm(context, type, hour, minute)
            }
            NotificationAlarmType.STREAK -> {
                val hour   = NotificationPreferences.getStreakAlertHour(context).firstOrNull()
                    ?: NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR
                val minute = NotificationPreferences.getStreakAlertMinute(context).firstOrNull()
                    ?: NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE
                AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.STREAK, hour, minute)
            }
            NotificationAlarmType.WEEKLY_PLAN -> {
                AlarmScheduler.scheduleWeeklyPlanAlarm(context)
            }
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private suspend fun scheduleMealAlarms(context: Context, enabled: Boolean) {
        if (!enabled) {
            AlarmScheduler.cancelAlarm(context, NotificationAlarmType.BREAKFAST)
            AlarmScheduler.cancelAlarm(context, NotificationAlarmType.LUNCH)
            AlarmScheduler.cancelAlarm(context, NotificationAlarmType.DINNER)
            return
        }
        listOf(
            NotificationAlarmType.BREAKFAST,
            NotificationAlarmType.LUNCH,
            NotificationAlarmType.DINNER
        ).forEach { type ->
            val (hour, minute) = readHourMinuteForMealType(context, type)
            AlarmScheduler.scheduleMealAlarm(context, type, hour, minute)
        }
    }

    private suspend fun scheduleStreakAlarm(context: Context, enabled: Boolean) {
        if (!enabled) {
            AlarmScheduler.cancelAlarm(context, NotificationAlarmType.STREAK)
            return
        }
        val hour   = NotificationPreferences.getStreakAlertHour(context).firstOrNull()
            ?: NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR
        val minute = NotificationPreferences.getStreakAlertMinute(context).firstOrNull()
            ?: NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE
        AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.STREAK, hour, minute)
    }

    private suspend fun scheduleWeeklyPlanAlarm(context: Context, enabled: Boolean) {
        if (!enabled) {
            AlarmScheduler.cancelAlarm(context, NotificationAlarmType.WEEKLY_PLAN)
            return
        }
        AlarmScheduler.scheduleWeeklyPlanAlarm(context)
    }

    private suspend fun readHourMinuteForMealType(
        context: Context,
        type: NotificationAlarmType
    ): Pair<Int, Int> = when (type) {
        NotificationAlarmType.BREAKFAST -> Pair(
            NotificationPreferences.getBreakfastHour(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_BREAKFAST_HOUR,
            NotificationPreferences.getBreakfastMinute(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_BREAKFAST_MINUTE
        )
        NotificationAlarmType.LUNCH -> Pair(
            NotificationPreferences.getLunchHour(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_LUNCH_HOUR,
            NotificationPreferences.getLunchMinute(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_LUNCH_MINUTE
        )
        NotificationAlarmType.DINNER -> Pair(
            NotificationPreferences.getDinnerHour(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_DINNER_HOUR,
            NotificationPreferences.getDinnerMinute(context).firstOrNull()
                ?: NotificationPreferences.DEFAULT_DINNER_MINUTE
        )
        else -> Pair(0, 0)
    }
}
