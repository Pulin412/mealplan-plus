package com.mealplanplus

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mealplanplus.data.local.DatabaseSeeder
import com.google.firebase.analytics.FirebaseAnalytics
import com.mealplanplus.util.AnalyticsManager
import com.mealplanplus.util.CrashlyticsReporter
import com.mealplanplus.util.FeatureFlag
import com.mealplanplus.util.RemoteConfigManager
import com.mealplanplus.util.NotificationHelper
import com.mealplanplus.work.MealReminderWorker
import com.mealplanplus.work.StreakProtectionWorker
import com.mealplanplus.work.SyncWorker
import com.mealplanplus.work.WeeklyPlanWorker
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

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        initCrashlytics()
        initRemoteConfig()
        NotificationHelper.createChannel(this)
        // Seed only if database is empty (first run)
        applicationScope.launch {
            databaseSeeder.seedFromFilesIfNeeded(this@MealPlanApp)
        }
        scheduleSyncWork()
        scheduleNotificationWork()
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

    private fun initRemoteConfig() {
        remoteConfigManager.applyDefaults()
        applicationScope.launch {
            remoteConfigManager.fetchAndActivate()
            // Apply analytics collection state after config is fresh
            val analyticsEnabled = remoteConfigManager.isEnabled(FeatureFlag.ANALYTICS_ENABLED)
            FirebaseAnalytics.getInstance(this@MealPlanApp)
                .setAnalyticsCollectionEnabled(analyticsEnabled)
        }
    }

    private fun scheduleSyncWork() {
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            SyncWorker.periodicRequest()
        )
    }

    private fun scheduleNotificationWork() {
        val wm = WorkManager.getInstance(this)
        wm.enqueueUniquePeriodicWork(
            MealReminderWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            MealReminderWorker.periodicRequest()
        )
        wm.enqueueUniquePeriodicWork(
            StreakProtectionWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            StreakProtectionWorker.periodicRequest()
        )
        wm.enqueueUniquePeriodicWork(
            WeeklyPlanWorker.TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            WeeklyPlanWorker.periodicRequest()
        )
    }
}
