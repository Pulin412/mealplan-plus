package com.mealplanplus.work

import android.content.Context
import android.util.Log
import androidx.work.*
import com.mealplanplus.data.repository.SyncRepository
import com.mealplanplus.util.AuthPreferences
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

        val since = inputData.getLong(KEY_SINCE, 0L)

        syncRepo.push(userId).onFailure { e ->
            Log.w(TAG, "Push failed: ${e.message}")
            return Result.retry()
        }

        syncRepo.pull(userId, since).onFailure { e ->
            Log.w(TAG, "Pull failed: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }

    companion object {
        const val TAG = "SyncWorker"
        const val KEY_SINCE = "since"

        fun periodicRequest(): PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()
    }
}
