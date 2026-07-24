package com.mealplanplus.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mealplanplus.data.notifications.NotificationHelper
import com.mealplanplus.data.notifications.NotificationScheduler
import com.mealplanplus.data.notifications.NotificationStore
import com.mealplanplus.data.notifications.NotificationType
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import javax.inject.Inject

/**
 * Fires when a reminder alarm goes off: posts the notification (unless the type was turned off or it's
 * quiet hours), then reschedules the same slot for its next occurrence so the reminder repeats.
 */
@AndroidEntryPoint
class NotificationReceiver : BroadcastReceiver() {

    @Inject lateinit var store: NotificationStore
    @Inject lateinit var scheduler: NotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(EXTRA_TYPE)?.let { NotificationType.fromKey(it) } ?: return
        val slot = intent.getIntExtra(EXTRA_SLOT, 0)

        val settings = store.state.value
        if (settings.enabled[type] == true) {
            val nowMin = Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
            if (!settings.inQuietHours(nowMin)) NotificationHelper.post(context, type)
            scheduler.scheduleSlot(type, slot) // repeat for the next day/week
        }
    }

    companion object {
        const val EXTRA_TYPE = "type"
        const val EXTRA_SLOT = "slot"
    }
}
