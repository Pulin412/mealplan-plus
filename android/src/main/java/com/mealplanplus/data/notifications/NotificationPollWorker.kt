package com.mealplanplus.data.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mealplanplus.data.generated.model.NotificationDto
import com.mealplanplus.data.repository.SocialRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

/**
 * Periodically polls the social notification feed and posts a phone (system) notification for items
 * that arrived since the last poll. This is the Android delivery path (no FCM — stays inside the
 * zero-billing guardrail); web push is handled separately for the PWA.
 *
 * Dependencies are pulled from Hilt via [EntryPointAccessors] so this stays a plain [CoroutineWorker]
 * (no Hilt-Work wiring needed). Delivery is near-real-time (~15 min, WorkManager's minimum period).
 */
class NotificationPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun socialRepository(): SocialRepository
    }

    override suspend fun doWork(): Result {
        val repo = EntryPointAccessors
            .fromApplication(applicationContext, WorkerEntryPoint::class.java)
            .socialRepository()

        // Null = offline or signed-out; try again next cycle without disturbing baseline.
        val items = repo.notifications(50)?.items ?: return Result.success()
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(KEY_LAST_SEEN, -1L)
        val maxId = items.maxOfOrNull { it.id } ?: return Result.success()

        // First-ever poll: set the baseline so we never replay a user's whole backlog as a push.
        if (lastSeen < 0) {
            prefs.edit().putLong(KEY_LAST_SEEN, maxId).apply()
            return Result.success()
        }

        val fresh = items.filter { it.id > lastSeen }
        if (fresh.isNotEmpty()) {
            NotificationHelper.postSocial(applicationContext, title(fresh), body(fresh))
            prefs.edit().putLong(KEY_LAST_SEEN, maxId).apply()
        }
        return Result.success()
    }

    private fun title(fresh: List<NotificationDto>): String =
        if (fresh.size == 1) "New notification" else "${fresh.size} new notifications"

    private fun body(fresh: List<NotificationDto>): String {
        val n = fresh.first()
        val who = n.actorDisplayName ?: n.actorHandle?.let { "@$it" } ?: "Someone"
        val line = when (n.type) {
            NotificationDto.Type.FOLLOW -> "$who started following you"
            NotificationDto.Type.SHARE -> {
                val kind = when (n.subjectKind) {
                    NotificationDto.SubjectKind.DIET -> "a diet"
                    NotificationDto.SubjectKind.MEAL -> "a meal"
                    NotificationDto.SubjectKind.WORKOUT -> "a workout"
                    null -> "an item"
                }
                "$who shared $kind" + (n.subjectName?.let { ": $it" } ?: "")
            }
        }
        return if (fresh.size == 1) line else "$line and more"
    }

    companion object {
        private const val PREFS = "social_notifications"
        private const val KEY_LAST_SEEN = "last_seen_id"
        private const val UNIQUE_WORK = "social-notification-poll"

        /** Enqueue the periodic poll (idempotent — KEEP preserves an existing schedule). */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationPollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
