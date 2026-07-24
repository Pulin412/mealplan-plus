package com.mealplanplus.ui.theme

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persists the user's theme choice (system / light / dark) so the toggle survives restarts. */
@Singleton
class ThemeStore @Inject constructor(@ApplicationContext ctx: Context) {
    private val prefs = ctx.getSharedPreferences("theme", Context.MODE_PRIVATE)

    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode

    /** Resolve to an actual dark flag given the current system setting. */
    fun isDark(systemDark: Boolean): Boolean = when (_mode.value) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /** Flip to the opposite of what's currently shown (becomes an explicit choice). */
    fun toggle(currentlyDark: Boolean) {
        val next = if (currentlyDark) ThemeMode.LIGHT else ThemeMode.DARK
        _mode.value = next
        prefs.edit().putString(KEY, next.name).apply()
    }

    private fun read(): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs.getString(KEY, ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM)

    private companion object { const val KEY = "mode" }
}
