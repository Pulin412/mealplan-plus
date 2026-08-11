package com.mealplanplus

import android.app.Application
import com.mealplanplus.data.notifications.NotificationHelper
import com.mealplanplus.data.notifications.NotificationPollWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MealPlanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        NotificationPollWorker.schedule(this)
    }
}
