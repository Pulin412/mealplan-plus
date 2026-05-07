package com.mealplanplus.work

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mealplanplus.data.repository.SyncPartialFailureException
import com.mealplanplus.data.repository.SyncRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.SyncPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun syncRepository(): SyncRepository
    }

    override suspend fun doWork(): Result {
        val userId = AuthPreferences.getUserId(applicationContext).firstOrNull()
            ?: return Result.success() // not logged in — skip silently

        val syncRepo = EntryPointAccessors.fromApplication(
            applicationContext, SyncWorkerEntryPoint::class.java
        ).syncRepository()

        val since = SyncPreferences.getLastSyncTimestamp(applicationContext).firstOrNull() ?: 0L

        syncRepo.push(userId).onFailure { e ->
            when (e) {
                is SyncPartialFailureException -> {
                    // Step 1 succeeded (foods/meals/etc.) but daily logs failed.
                    // Pull still runs below; daily logs will be retried next cycle.
                    Log.w(TAG, "Push partial failure — daily logs not pushed: ${e.cause?.message}")
                }
                else -> Log.w(TAG, "Push failed (non-fatal): ${e.message}")
            }
            // Don't return failure — pull still runs; unsynced items retried next cycle
        }

        val serverTime = syncRepo.pull(userId, since).getOrElse { e ->
            Log.w(TAG, "Pull failed: ${e.message}")
            return Result.retry()
        }

        SyncPreferences.setLastSyncTimestamp(applicationContext, serverTime)
        Log.d(TAG, "Sync complete, serverTime=$serverTime")
        return Result.success()
    }

    companion object {
        const val TAG = "SyncWorker"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

        fun oneTimeRequest(): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .addTag(TAG)
                .build()
    }
}
