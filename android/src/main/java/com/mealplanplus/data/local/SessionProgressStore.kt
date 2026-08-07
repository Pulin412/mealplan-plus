package com.mealplanplus.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists which exercises are checked off "done" while logging a workout, keyed by session id, so
 * the ticks survive leaving and re-opening the runner. Purely local, ephemeral progress — never
 * synced to the backend.
 */
@Singleton
class SessionProgressStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("session_progress", Context.MODE_PRIVATE)

    fun getDone(sessionId: Long): Set<Long> =
        prefs.getStringSet(key(sessionId), emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()

    fun setDone(sessionId: Long, exerciseIds: Set<Long>) =
        prefs.edit().putStringSet(key(sessionId), exerciseIds.map { it.toString() }.toSet()).apply()

    private fun key(sessionId: Long) = "done_$sessionId"
}
