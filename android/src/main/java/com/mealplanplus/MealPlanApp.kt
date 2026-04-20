package com.mealplanplus

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.mealplanplus.data.local.MealSlotReseeder
import com.google.firebase.analytics.FirebaseAnalytics
import com.mealplanplus.notification.NotificationAlarmBootstrapper
import com.mealplanplus.util.AnalyticsManager
import com.mealplanplus.util.CrashlyticsReporter
import com.mealplanplus.util.FeatureFlag
import com.mealplanplus.util.RemoteConfigManager
import com.mealplanplus.util.NotificationHelper
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
    lateinit var mealSlotReseeder: MealSlotReseeder

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
        // Re-seed system foods whenever the bundled version changes
        // Only meal-slot config is seeded automatically — all data (foods, exercises,
        // workout templates, backup) is imported manually from Profile → Import Data.
        applicationScope.launch {
            mealSlotReseeder.reseedIfNeeded(this@MealPlanApp)
        }
        scheduleSyncWork()
        cancelLegacyNotificationWork()
        scheduleNotificationAlarms()
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

    /** Cancel legacy WorkManager notification workers (replaced by AlarmManager). */
    private fun cancelLegacyNotificationWork() {
        val wm = WorkManager.getInstance(this)
        wm.cancelUniqueWork("meal_reminder_work")
        wm.cancelUniqueWork("streak_protection_work")
        wm.cancelUniqueWork("weekly_plan_work")
    }

    /** Schedule exact AlarmManager alarms for all notification types. */
    private fun scheduleNotificationAlarms() {
        applicationScope.launch {
            NotificationAlarmBootstrapper.scheduleAll(this@MealPlanApp)
        }
    }
}
