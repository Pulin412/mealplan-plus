package com.mealplanplus.ui.tour

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists whether the first-run guided tour has been seen/skipped (per device). Cosmetic only —
 * intentionally local (not server-authoritative like onboarding), so a fresh install re-shows it.
 */
@Singleton
class TourStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("tour", Context.MODE_PRIVATE)

    private val _seen = MutableStateFlow(prefs.getBoolean(KEY, false))
    val seen: StateFlow<Boolean> = _seen

    fun markSeen() {
        _seen.value = true
        prefs.edit().putBoolean(KEY, true).apply()
    }

    /** Clears the flag so the tour runs again (used by Settings → Replay tour). */
    fun reset() {
        _seen.value = false
        prefs.edit().putBoolean(KEY, false).apply()
    }

    private companion object { const val KEY = "seen" }
}
