package com.mealplanplus.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mealplanplus.MainActivity
import com.mealplanplus.R

/**
 * Thin helper for creating the notification channel and posting the three
 * notification types defined in the Notifications PRD.
 *
 * All posting methods are no-ops when the user has not granted
 * [android.Manifest.permission.POST_NOTIFICATIONS] (Android 13+).
 */
object NotificationHelper {

    const val CHANNEL_ID = "meal_reminders"
    private const val CHANNEL_NAME = "Meal Reminders"
    private const val CHANNEL_DESC =
        "Reminders to log meals, protect your streak, and plan your week"

    // Stable notification IDs — one per type so each can be updated in-place.
    private const val ID_BREAKFAST = 1001
    private const val ID_LUNCH = 1002
    private const val ID_DINNER = 1003
    private const val ID_STREAK = 1004
    private const val ID_WEEKLY_PLAN = 1005

    // ── Channel ───────────────────────────────────────────────────────────────

    /**
     * Creates the notification channel. Safe to call multiple times (idempotent).
     * Must be called before any notification is posted (done in [com.mealplanplus.MealPlanApp]).
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = CHANNEL_DESC }
        notificationManager(context).createNotificationChannel(channel)
    }

    // ── Permission guard ──────────────────────────────────────────────────────

    /**
     * Returns true if the app may post notifications.
     * Always true below Android 13 (TIRAMISU); requires the runtime permission on 13+.
     */
    fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    // ── Post helpers ──────────────────────────────────────────────────────────

    /**
     * Posts a meal-slot reminder for [slotType] ("BREAKFAST", "LUNCH", or "DINNER").
     * No-op for unrecognised slot types.
     */
    fun postMealReminder(context: Context, slotType: String) {
        if (!canPostNotifications(context)) return
        val (notifId, title, text) = when (slotType.uppercase()) {
            "BREAKFAST" -> Triple(ID_BREAKFAST, "Breakfast reminder 🍳", "Don't forget to log your breakfast!")
            "LUNCH" -> Triple(ID_LUNCH, "Lunch reminder ☀️", "Time to log your lunch!")
            "DINNER" -> Triple(ID_DINNER, "Dinner reminder 🌙", "Time to log your dinner!")
            else -> return
        }
        post(context, notifId, title, text)
    }

    /**
     * Posts the streak-protection alert showing the current [streakDays].
     */
    fun postStreakAlert(context: Context, streakDays: Int) {
        if (!canPostNotifications(context)) return
        post(
            context,
            ID_STREAK,
            title = "Don't break your streak! 🔥",
            text = "You have a $streakDays-day streak. Log something today to keep it going."
        )
    }

    /**
     * Posts the weekly plan reminder on Monday mornings when no plan exists.
     */
    fun postWeeklyPlanReminder(context: Context) {
        if (!canPostNotifications(context)) return
        post(
            context,
            ID_WEEKLY_PLAN,
            title = "Plan your week 📅",
            text = "New week, new plan! You haven't set up your meal plan for this week yet."
        )
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private fun post(context: Context, notificationId: Int, title: String, text: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(mainActivityIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager(context).notify(notificationId, notification)
    }

    private fun mainActivityIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
