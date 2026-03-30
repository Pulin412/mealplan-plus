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
 * Hourly worker that posts meal-slot reminders (Breakfast, Lunch, Dinner) when:
 * 1. The [FeatureFlag.NOTIFICATIONS_ENABLED] kill-switch is on.
 * 2. The user has enabled notifications and meal reminders in [NotificationPreferences].
 * 3. The current hour matches the configured reminder hour for that slot.
 * 4. The slot has not already been logged today.
 */
class MealReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface MealReminderEntryPoint {
        fun dailyLogRepository(): DailyLogRepository
        fun remoteConfigManager(): RemoteConfigManager
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext, MealReminderEntryPoint::class.java
        )

        // Feature flag kill-switch
        if (!ep.remoteConfigManager().isEnabled(FeatureFlag.NOTIFICATIONS_ENABLED)) {
            return Result.success()
        }

        val masterEnabled = NotificationPreferences.getMasterEnabled(applicationContext)
            .firstOrNull() ?: false
        val mealRemindersEnabled = NotificationPreferences.getMealRemindersEnabled(applicationContext)
            .firstOrNull() ?: true

        if (!masterEnabled || !mealRemindersEnabled) return Result.success()

        val userId = AuthPreferences.getUserId(applicationContext).firstOrNull()
            ?: return Result.success()

        val currentHour = LocalTime.now().hour
        val todayLog = ep.dailyLogRepository()
            .getLogWithFoods(LocalDate.now())
            .firstOrNull()

        val slots = listOf(
            Triple(
                "BREAKFAST",
                NotificationPreferences.getBreakfastHour(applicationContext).firstOrNull()
                    ?: NotificationPreferences.DEFAULT_BREAKFAST_HOUR,
                todayLog?.foodsForSlot("BREAKFAST")?.isNotEmpty() ?: false
            ),
            Triple(
                "LUNCH",
                NotificationPreferences.getLunchHour(applicationContext).firstOrNull()
                    ?: NotificationPreferences.DEFAULT_LUNCH_HOUR,
                todayLog?.foodsForSlot("LUNCH")?.isNotEmpty() ?: false
            ),
            Triple(
                "DINNER",
                NotificationPreferences.getDinnerHour(applicationContext).firstOrNull()
                    ?: NotificationPreferences.DEFAULT_DINNER_HOUR,
                todayLog?.foodsForSlot("DINNER")?.isNotEmpty() ?: false
            )
        )

        for ((slotType, targetHour, isLogged) in slots) {
            if (NotificationDecider.shouldSendMealReminder(
                    currentHour = currentHour,
                    notificationsEnabled = masterEnabled,
                    mealRemindersEnabled = mealRemindersEnabled,
                    targetHour = targetHour,
                    isAlreadyLogged = isLogged
                )
            ) {
                NotificationHelper.postMealReminder(applicationContext, slotType)
            }
        }

        return Result.success()
    }

    companion object {
        const val TAG = "MealReminderWorker"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<MealReminderWorker>(1, TimeUnit.HOURS)
                .addTag(TAG)
                .build()
    }
}
