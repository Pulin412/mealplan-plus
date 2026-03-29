package com.mealplanplus

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mealplanplus.data.local.DatabaseSeeder
import com.mealplanplus.util.CrashlyticsReporter
import com.mealplanplus.work.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MealPlanApp : Application() {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var crashlyticsReporter: CrashlyticsReporter

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
        // Seed only if database is empty (first run)
        applicationScope.launch {
            databaseSeeder.seedFromFilesIfNeeded(this@MealPlanApp)
        }
        scheduleSyncWork()
    }

    private fun initCrashlytics() {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        @Suppress("SwallowedException")
        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown"
            crashlyticsReporter.log("app_start", "version=$versionName")
        } catch (e: Exception) {
            crashlyticsReporter.log("app_start")
        }
    }

    private fun scheduleSyncWork() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicRequest()
        )
    }
}
