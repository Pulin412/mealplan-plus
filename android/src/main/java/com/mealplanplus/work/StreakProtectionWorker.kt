package com.mealplanplus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.FeatureFlag
import com.mealplanplus.util.NotificationDecider
import com.mealplanplus.util.NotificationHelper
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.RemoteConfigManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Hourly worker that posts a streak-protection alert when:
 * 1. The [FeatureFlag.NOTIFICATIONS_ENABLED] kill-switch is on.
 * 2. The user has enabled notifications and streak protection in [NotificationPreferences].
 * 3. The current hour is at or past the configured alert hour (default 21:00).
 * 4. The user has an active streak (≥ 1 day) but nothing has been logged today.
 */
class StreakProtectionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StreakProtectionEntryPoint {
        fun dailyLogRepository(): DailyLogRepository
        fun remoteConfigManager(): RemoteConfigManager
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext, StreakProtectionEntryPoint::class.java
        )

        if (!ep.remoteConfigManager().isEnabled(FeatureFlag.NOTIFICATIONS_ENABLED)) {
            return Result.success()
        }

        val masterEnabled = NotificationPreferences.getMasterEnabled(applicationContext)
            .firstOrNull() ?: false
        val streakProtectionEnabled = NotificationPreferences.getStreakProtectionEnabled(applicationContext)
            .firstOrNull() ?: true

        if (!masterEnabled || !streakProtectionEnabled) return Result.success()

        val userId = AuthPreferences.getUserId(applicationContext).firstOrNull()
            ?: return Result.success()

        val alertHour = NotificationPreferences.getStreakAlertHour(applicationContext)
            .firstOrNull() ?: NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR
        val currentHour = LocalTime.now().hour

        // Collect last 30 days of completed days to compute streak
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        val completedDays = ep.dailyLogRepository()
            .getCompletedDaysCalories(thirtyDaysAgo, today)
            .firstOrNull() ?: emptyList()

        val completedDateStrings = completedDays.map { it.date }
        val streakDays = NotificationDecider.computeStreak(completedDateStrings, today)

        // Today's calories (0 if not yet logged)
        val todayCalories = completedDays
            .firstOrNull { it.date == today.toString() }
            ?.calories ?: 0.0

        if (NotificationDecider.shouldSendStreakAlert(
                currentHour = currentHour,
                notificationsEnabled = masterEnabled,
                streakProtectionEnabled = streakProtectionEnabled,
                alertHour = alertHour,
                streakDays = streakDays,
                todayCalories = todayCalories
            )
        ) {
            NotificationHelper.postStreakAlert(applicationContext, streakDays)
        }

        return Result.success()
    }

    companion object {
        const val TAG = "StreakProtectionWorker"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<StreakProtectionWorker>(1, TimeUnit.HOURS)
                .addTag(TAG)
                .build()
    }
}
