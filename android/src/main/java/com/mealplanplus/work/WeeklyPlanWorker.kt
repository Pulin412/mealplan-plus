package com.mealplanplus.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.mealplanplus.data.repository.PlanRepository
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Hourly worker that posts a weekly-plan reminder when:
 * 1. The [FeatureFlag.NOTIFICATIONS_ENABLED] kill-switch is on.
 * 2. The user has enabled notifications and the weekly plan reminder in [NotificationPreferences].
 * 3. Today is Monday and the current hour is ≥ 8.
 * 4. No meal plans exist for any day in the current week (Mon–Sun).
 */
class WeeklyPlanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WeeklyPlanEntryPoint {
        fun planRepository(): PlanRepository
        fun remoteConfigManager(): RemoteConfigManager
    }

    override suspend fun doWork(): Result {
        val ep = EntryPointAccessors.fromApplication(
            applicationContext, WeeklyPlanEntryPoint::class.java
        )

        if (!ep.remoteConfigManager().isEnabled(FeatureFlag.NOTIFICATIONS_ENABLED)) {
            return Result.success()
        }

        val masterEnabled = NotificationPreferences.getMasterEnabled(applicationContext)
            .firstOrNull() ?: false
        val weeklyPlanEnabled = NotificationPreferences.getWeeklyPlanEnabled(applicationContext)
            .firstOrNull() ?: true

        if (!masterEnabled || !weeklyPlanEnabled) return Result.success()

        AuthPreferences.getUserId(applicationContext).firstOrNull()
            ?: return Result.success()

        val today = LocalDate.now()
        val currentHour = LocalTime.now().hour
        val dayOfWeek = today.dayOfWeek.value  // 1=Monday … 7=Sunday

        // Compute current week bounds (Monday–Sunday)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)

        val plansThisWeek = ep.planRepository()
            .getPlansWithDietNames(weekStart.toString(), weekEnd.toString())
            .firstOrNull() ?: emptyList()

        if (NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = dayOfWeek,
                currentHour = currentHour,
                notificationsEnabled = masterEnabled,
                weeklyPlanEnabled = weeklyPlanEnabled,
                plansThisWeekCount = plansThisWeek.size
            )
        ) {
            NotificationHelper.postWeeklyPlanReminder(applicationContext)
        }

        return Result.success()
    }

    companion object {
        const val TAG = "WeeklyPlanWorker"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<WeeklyPlanWorker>(1, TimeUnit.HOURS)
                .addTag(TAG)
                .build()
    }
}
