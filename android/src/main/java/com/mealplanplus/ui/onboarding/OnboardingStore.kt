package com.mealplanplus.ui.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Persists whether first-run onboarding has been completed/skipped (per device). Default false. */
@Singleton
class OnboardingStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("onboarding", Context.MODE_PRIVATE)

    private val _done = MutableStateFlow(prefs.getBoolean(KEY, false))
    val done: StateFlow<Boolean> = _done

    fun markDone() {
        _done.value = true
        prefs.edit().putBoolean(KEY, true).apply()
    }

    fun reset() {
        _done.value = false
        prefs.edit().putBoolean(KEY, false).apply()
    }

    private companion object { const val KEY = "done" }
}
