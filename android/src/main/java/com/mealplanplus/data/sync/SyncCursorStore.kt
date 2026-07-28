package com.mealplanplus.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Persists the "last successful pull" cursor so pulls fetch only what changed since. */
@Singleton
class SyncCursorStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("sync", Context.MODE_PRIVATE)

    /** EPOCH (never synced) means the next pull fetches everything. */
    fun get(): Instant =
        prefs.getLong(KEY, 0L).let { if (it == 0L) Instant.EPOCH else Instant.ofEpochMilli(it) }

    fun set(instant: Instant) = prefs.edit().putLong(KEY, instant.toEpochMilli()).apply()

    private companion object {
        const val KEY = "foods_cursor"
    }
}
