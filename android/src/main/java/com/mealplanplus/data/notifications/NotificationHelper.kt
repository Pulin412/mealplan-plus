package com.mealplanplus.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mealplanplus.MainActivity
import com.mealplanplus.R

/** Creates the reminders channel, checks the runtime permission, and posts a notification. */
object NotificationHelper {

    const val CHANNEL_ID = "reminders"
    private const val CHANNEL_NAME = "Reminders"
    private const val CHANNEL_DESC = "Meal, water, workout, weigh-in and glucose reminders"

    /** Idempotent — safe to call on every app start. */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = CHANNEL_DESC }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** True when the app may post notifications (always true below API 33). */
    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Posts the reminder for [type]. No-op without permission. Stable id per type so it updates in place. */
    fun post(context: Context, type: NotificationType) {
        if (!canPost(context)) return
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context, type.requestBase, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(type.title)
            .setContentText(type.body)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(type.requestBase, notification)
    }
}
