package com.mealplanplus.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AlarmScheduler
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.FeatureFlag
import com.mealplanplus.util.NotificationAlarmType
import com.mealplanplus.util.NotificationDecider
import com.mealplanplus.util.NotificationHelper
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.RemoteConfigManager
import android.util.Log
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.mealplanplus.util.toLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Receives exact alarm broadcasts and posts the appropriate notification.
 * After posting, immediately re-schedules the next occurrence so that the alarm repeats daily
 * (or weekly for [NotificationAlarmType.WEEKLY_PLAN]) at the same configured time.
 *
 * Uses [goAsync] to keep the receiver process alive while suspend functions run on [Dispatchers.IO].
 */
class NotificationAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun dailyLogRepository(): DailyLogRepository
        fun planRepository(): PlanRepository
        fun remoteConfigManager(): RemoteConfigManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action} type=${intent.getStringExtra(EXTRA_ALARM_TYPE)}")
        if (intent.action != ACTION_NOTIFICATION_ALARM) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                withTimeout(9_000L) {
                    handleAlarm(appContext, intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "handleAlarm failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        val typeName = intent.getStringExtra(EXTRA_ALARM_TYPE) ?: return
        val type = runCatching { NotificationAlarmType.valueOf(typeName) }.getOrNull() ?: return

        val ep = EntryPointAccessors.fromApplication(context, ReceiverEntryPoint::class.java)

        // Remote Config kill-switch
        if (!ep.remoteConfigManager().isEnabled(FeatureFlag.NOTIFICATIONS_ENABLED)) {
            rescheduleNext(context, type)
            return
        }

        val masterEnabled = NotificationPreferences.getMasterEnabled(context).firstOrNull() ?: false
        Log.d(TAG, "handleAlarm type=$type masterEnabled=$masterEnabled")
        if (!masterEnabled) return  // master off — don't re-schedule either

        when (type) {
            NotificationAlarmType.BREAKFAST,
            NotificationAlarmType.LUNCH,
            NotificationAlarmType.DINNER -> handleMealAlarm(context, type, ep, masterEnabled)

            NotificationAlarmType.STREAK -> handleStreakAlarm(context, ep, masterEnabled)

            NotificationAlarmType.WEEKLY_PLAN -> handleWeeklyPlanAlarm(context, ep, masterEnabled)
        }

        // Re-schedule next occurrence (alarms are one-shot)
        rescheduleNext(context, type)
    }

    private suspend fun handleMealAlarm(
        context: Context,
        type: NotificationAlarmType,
        ep: ReceiverEntryPoint,
        masterEnabled: Boolean
    ) {
        val mealRemindersEnabled = NotificationPreferences.getMealRemindersEnabled(context)
            .firstOrNull() ?: true
        if (!mealRemindersEnabled) return

        val todayLog = ep.dailyLogRepository()
            .getLogWithFoods(LocalDate.now())
            .firstOrNull()

        val slotType = type.slotType ?: return
        val isLogged = todayLog?.foodsForSlot(slotType)?.isNotEmpty() ?: false
        Log.d(TAG, "handleMealAlarm slot=$slotType mealReminders=$mealRemindersEnabled isLogged=$isLogged")

        if (shouldPostMealAlarm(masterEnabled, mealRemindersEnabled, isLogged)) {
            Log.d(TAG, "Posting meal reminder for $slotType")
            NotificationHelper.postMealReminder(context, slotType)
        } else {
            Log.d(TAG, "Skipping meal reminder for $slotType (already logged or disabled)")
        }
    }

    private suspend fun handleStreakAlarm(
        context: Context,
        ep: ReceiverEntryPoint,
        masterEnabled: Boolean
    ) {
        val streakEnabled = NotificationPreferences.getStreakProtectionEnabled(context)
            .firstOrNull() ?: true
        if (!streakEnabled) return

        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val completedDays = ep.dailyLogRepository()
            .getCompletedDaysCalories(thirtyDaysAgo, today)
            .firstOrNull() ?: emptyList()

        val streakDays = NotificationDecider.computeStreak(
            completedDays.map { it.date.toLocalDate() }, today
        )
        val todayCalories = completedDays
            .firstOrNull { it.date.toLocalDate() == today }?.calories ?: 0.0

        if (NotificationDecider.shouldSendStreakAlert(
                currentHour = 0, // hour check irrelevant — alarm fires at exact time
                notificationsEnabled = masterEnabled,
                streakProtectionEnabled = streakEnabled,
                alertHour = 0,   // same
                streakDays = streakDays,
                todayCalories = todayCalories
            )
        ) {
            NotificationHelper.postStreakAlert(context, streakDays)
        }
    }

    private suspend fun handleWeeklyPlanAlarm(
        context: Context,
        ep: ReceiverEntryPoint,
        masterEnabled: Boolean
    ) {
        val weeklyEnabled = NotificationPreferences.getWeeklyPlanEnabled(context)
            .firstOrNull() ?: true
        if (!weeklyEnabled) return

        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val plans = ep.planRepository()
            .getPlansWithDietNames(weekStart, weekEnd)
            .firstOrNull() ?: emptyList()

        // Alarm fires on Monday — if somehow it fires on another day, skip
        if (today.dayOfWeek != DayOfWeek.MONDAY) return

        if (NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = DayOfWeek.MONDAY.value,
                currentHour = 8, // irrelevant — alarm fires at exact time
                notificationsEnabled = masterEnabled,
                weeklyPlanEnabled = weeklyEnabled,
                plansThisWeekCount = plans.size
            )
        ) {
            NotificationHelper.postWeeklyPlanReminder(context)
        }
    }

    private suspend fun rescheduleNext(context: Context, type: NotificationAlarmType) {
        when (type) {
            NotificationAlarmType.BREAKFAST,
            NotificationAlarmType.LUNCH,
            NotificationAlarmType.DINNER -> {
                val hour = when (type) {
                    NotificationAlarmType.BREAKFAST ->
                        NotificationPreferences.getBreakfastHour(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_BREAKFAST_HOUR
                    NotificationAlarmType.LUNCH ->
                        NotificationPreferences.getLunchHour(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_LUNCH_HOUR
                    else ->
                        NotificationPreferences.getDinnerHour(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_DINNER_HOUR
                }
                val minute = when (type) {
                    NotificationAlarmType.BREAKFAST ->
                        NotificationPreferences.getBreakfastMinute(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_BREAKFAST_MINUTE
                    NotificationAlarmType.LUNCH ->
                        NotificationPreferences.getLunchMinute(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_LUNCH_MINUTE
                    else ->
                        NotificationPreferences.getDinnerMinute(context).firstOrNull()
                            ?: NotificationPreferences.DEFAULT_DINNER_MINUTE
                }
                AlarmScheduler.scheduleMealAlarm(context, type, hour, minute)
            }
            NotificationAlarmType.STREAK -> {
                val hour = NotificationPreferences.getStreakAlertHour(context).firstOrNull()
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

    companion object {
        private const val TAG = "NotificationAlarmRcvr"
        const val ACTION_NOTIFICATION_ALARM = "com.mealplanplus.ACTION_NOTIFICATION_ALARM"
        const val EXTRA_ALARM_TYPE = "alarm_type"

        /**
         * Pure helper exposed for unit testing — no Android dependencies.
         * Returns true if a meal notification should be posted given the current state.
         */
        fun shouldPostMealAlarm(
            notificationsEnabled: Boolean,
            mealRemindersEnabled: Boolean,
            isAlreadyLogged: Boolean
        ): Boolean {
            if (!notificationsEnabled || !mealRemindersEnabled) return false
            return !isAlreadyLogged
        }
    }
}
