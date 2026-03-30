package com.mealplanplus.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-schedules all notification alarms after a device reboot.
 *
 * AlarmManager alarms do not survive a reboot; this receiver listens for
 * [Intent.ACTION_BOOT_COMPLETED] and delegates to [NotificationAlarmBootstrapper.scheduleAll]
 * to restore them from the persisted DataStore preferences.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                NotificationAlarmBootstrapper.scheduleAll(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
