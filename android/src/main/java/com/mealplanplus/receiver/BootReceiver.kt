package com.mealplanplus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mealplanplus.data.notifications.NotificationScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Alarms don't survive a reboot — re-arm every enabled reminder on BOOT_COMPLETED. */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) scheduler.rescheduleAll()
    }
}
