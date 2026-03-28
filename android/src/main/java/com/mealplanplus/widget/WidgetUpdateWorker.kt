package com.mealplanplus.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background worker that periodically refreshes all home screen widgets.
 * Runs every 15 minutes (the minimum allowed by WorkManager).
 *
 * Enqueue via [WidgetUpdateWorker.enqueue].
 */
class WidgetUpdateWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            CalendarWidget().updateAll(appContext)
            TodayPlanWidget().updateAll(appContext)
            DietSummaryWidget().updateAll(appContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "widget_refresh"

        /**
         * Enqueues a periodic refresh every 15 minutes (minimum WorkManager interval).
         * Safe to call multiple times — uses [ExistingPeriodicWorkPolicy.KEEP] so an
         * already-enqueued job is not replaced.
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
