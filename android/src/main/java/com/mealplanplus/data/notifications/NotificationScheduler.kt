package com.mealplanplus.data.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mealplanplus.receiver.NotificationReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the reminder alarms with [AlarmManager]. Reminders use **inexact** wake-up alarms
 * (`setAndAllowWhileIdle`) — precise-to-the-minute delivery isn't needed and inexact alarms avoid the
 * restricted SCHEDULE_EXACT_ALARM permission. Each alarm is one-shot; [NotificationReceiver] reposts
 * and reschedules the next occurrence when it fires. Enabled state is read from [NotificationStore].
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val store: NotificationStore,
) {
    private val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Cancel every alarm, then (re)schedule the next occurrence of each slot of each enabled type. */
    fun rescheduleAll() {
        val settings = store.state.value
        NotificationType.entries.forEach { type ->
            val enabled = settings.enabled[type] == true
            for (slot in 0 until type.slotCount) {
                if (enabled) scheduleSlot(type, slot) else cancelSlot(type, slot)
            }
        }
    }

    /** (Re)schedule the next fire for one slot — called on toggle-on and after a slot fires. */
    fun scheduleSlot(type: NotificationType, slot: Int) {
        val triggerAt = nextTrigger(type, slot)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(type, slot, create = true)!!)
    }

    private fun cancelSlot(type: NotificationType, slot: Int) {
        pendingIntent(type, slot, create = false)?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    /** Epoch-ms of the next occurrence of this slot (tomorrow/next-week if today's time has passed). */
    private fun nextTrigger(type: NotificationType, slot: Int, now: Calendar = Calendar.getInstance()): Long {
        val c = now.clone() as Calendar
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        val weeklyDay = type.weeklyDayIso
        if (weeklyDay != null) {
            setTime(c, type.weeklyTime)
            // Advance to the target ISO day-of-week (Mon=1…Sun=7), then +7 if already past.
            while (isoDayOfWeek(c) != weeklyDay) c.add(Calendar.DAY_OF_YEAR, 1)
            if (c.timeInMillis <= now.timeInMillis) c.add(Calendar.DAY_OF_YEAR, 7)
        } else {
            setTime(c, type.dailyTimes[slot])
            if (c.timeInMillis <= now.timeInMillis) c.add(Calendar.DAY_OF_YEAR, 1)
        }
        return c.timeInMillis
    }

    private fun pendingIntent(type: NotificationType, slot: Int, create: Boolean): PendingIntent? {
        val intent = Intent(ctx, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.EXTRA_TYPE, type.key)
            putExtra(NotificationReceiver.EXTRA_SLOT, slot)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or
            (if (create) PendingIntent.FLAG_UPDATE_CURRENT else PendingIntent.FLAG_NO_CREATE)
        return PendingIntent.getBroadcast(ctx, type.requestBase + slot, intent, flags)
    }

    private fun setTime(c: Calendar, minuteOfDay: Int) {
        c.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
        c.set(Calendar.MINUTE, minuteOfDay % 60)
    }

    /** Calendar DAY_OF_WEEK is Sun=1…Sat=7; convert to ISO Mon=1…Sun=7. */
    private fun isoDayOfWeek(c: Calendar): Int = ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
}
