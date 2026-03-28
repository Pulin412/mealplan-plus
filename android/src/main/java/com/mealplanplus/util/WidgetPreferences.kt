package com.mealplanplus.util

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// ─── Widget appearance preference constants ───────────────────────────────────

/** Built-in background style IDs (stored as the bgStyle string). */
object WidgetBgStyle {
    const val LIGHT       = "light"        // pure white
    const val LIGHT_GREY  = "light_grey"   // #F5F5F5
    const val GREEN_TINT  = "green_tint"   // #E8F5E9
    const val TEAL_TINT   = "teal_tint"    // #E0F2F1
    const val DARK        = "dark"         // #1C1C1E
    const val DYNAMIC     = "dynamic"      // follows GlanceTheme / Material You

    /** Resolved ARGB integer (without alpha) for each style except DYNAMIC. */
    fun toArgb(style: String): Int = when (style) {
        LIGHT      -> 0xFFFFFFFF.toInt()
        LIGHT_GREY -> 0xFFF5F5F5.toInt()
        GREEN_TINT -> 0xFFE8F5E9.toInt()
        TEAL_TINT  -> 0xFFE0F2F1.toInt()
        DARK       -> 0xFF1C1C1E.toInt()
        else       -> 0xFFFFFFFF.toInt() // fallback
    }
}

/** Accent/header color presets (stored as ARGB hex, e.g. "#2E7D52"). */
object WidgetAccentPresets {
    val all = listOf(
        "#2E7D52",  // Brand Green (default)
        "#00695C",  // Teal
        "#1565C0",  // Blue
        "#6A1B9A",  // Purple
        "#B71C1C",  // Red
        "#E65100",  // Orange
    )
}

/**
 * Text color presets. [AUTO] means use the theme/contrast-aware default
 * (dark text on light bg, light text on dark bg). Other entries are explicit colors.
 */
object WidgetTextColorPresets {
    const val AUTO = "auto"
    val all = listOf(
        AUTO,        // auto-contrast
        "#1A1C1E",   // Near-black  (default dark)
        "#FFFFFF",   // White
        "#2E7D52",   // Brand Green
        "#1565C0",   // Blue
        "#6A1B9A",   // Purple
        "#B71C1C",   // Red
    )
}

/** Text weight / style presets. */
enum class WidgetTextWeight { NORMAL, BOLD }

/** Text size scale presets (multiplier applied to base font sizes). */
enum class WidgetTextSize(val label: String, val scale: Float) {
    SMALL("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.2f)
}

// ─── DataStore wrapper ────────────────────────────────────────────────────────

/**
 * Reads and writes widget appearance settings to the app's shared DataStore
 * (the same "settings" store used by [ThemePreferences]).
 */
object WidgetPreferences {

    private val BG_STYLE_KEY    = stringPreferencesKey("widget_bg_style")
    private val BG_ALPHA_KEY    = floatPreferencesKey("widget_bg_alpha")
    private val ACCENT_KEY      = stringPreferencesKey("widget_accent_color")
    private val DYNAMIC_KEY     = booleanPreferencesKey("widget_use_dynamic")
    private val TEXT_COLOR_KEY  = stringPreferencesKey("widget_text_color")
    private val TEXT_WEIGHT_KEY = stringPreferencesKey("widget_text_weight")
    private val TEXT_SIZE_KEY   = stringPreferencesKey("widget_text_size")

    // ── Getters (Flow) ────────────────────────────────────────────────────────

    fun bgStyleFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[BG_STYLE_KEY] ?: WidgetBgStyle.LIGHT }

    fun bgAlphaFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { it[BG_ALPHA_KEY] ?: 1.0f }

    fun accentColorFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[ACCENT_KEY] ?: WidgetAccentPresets.all.first() }

    fun useDynamicFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[DYNAMIC_KEY] ?: false }

    fun textColorFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[TEXT_COLOR_KEY] ?: WidgetTextColorPresets.AUTO }

    fun textWeightFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[TEXT_WEIGHT_KEY] ?: WidgetTextWeight.NORMAL.name }

    fun textSizeFlow(context: Context): Flow<String> =
        context.dataStore.data.map { it[TEXT_SIZE_KEY] ?: WidgetTextSize.MEDIUM.name }

    /** One-shot snapshot (for use in Glance's provideGlance coroutine). */
    suspend fun getAppearance(context: Context): WidgetAppearanceState {
        val prefs = context.dataStore.data.first()
        return WidgetAppearanceState(
            bgStyle     = prefs[BG_STYLE_KEY]    ?: WidgetBgStyle.LIGHT,
            bgAlpha     = prefs[BG_ALPHA_KEY]    ?: 1.0f,
            accentColor = prefs[ACCENT_KEY]      ?: WidgetAccentPresets.all.first(),
            useDynamic  = prefs[DYNAMIC_KEY]     ?: false,
            textColor   = prefs[TEXT_COLOR_KEY]  ?: WidgetTextColorPresets.AUTO,
            textWeight  = prefs[TEXT_WEIGHT_KEY]?.let {
                runCatching { WidgetTextWeight.valueOf(it) }.getOrDefault(WidgetTextWeight.NORMAL)
            } ?: WidgetTextWeight.NORMAL,
            textSize    = prefs[TEXT_SIZE_KEY]?.let {
                runCatching { WidgetTextSize.valueOf(it) }.getOrDefault(WidgetTextSize.MEDIUM)
            } ?: WidgetTextSize.MEDIUM
        )
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setBgStyle(context: Context, style: String) {
        context.dataStore.edit { it[BG_STYLE_KEY] = style }
    }

    suspend fun setBgAlpha(context: Context, alpha: Float) {
        context.dataStore.edit { it[BG_ALPHA_KEY] = alpha.coerceIn(0.3f, 1.0f) }
    }

    suspend fun setAccentColor(context: Context, hex: String) {
        context.dataStore.edit { it[ACCENT_KEY] = hex }
    }

    suspend fun setUseDynamic(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_KEY] = enabled }
    }

    suspend fun setTextColor(context: Context, hex: String) {
        context.dataStore.edit { it[TEXT_COLOR_KEY] = hex }
    }

    suspend fun setTextWeight(context: Context, weight: WidgetTextWeight) {
        context.dataStore.edit { it[TEXT_WEIGHT_KEY] = weight.name }
    }

    suspend fun setTextSize(context: Context, size: WidgetTextSize) {
        context.dataStore.edit { it[TEXT_SIZE_KEY] = size.name }
    }
}

/** Snapshot of all widget appearance settings, used by both the ViewModel and the widgets. */
data class WidgetAppearanceState(
    val bgStyle: String           = WidgetBgStyle.LIGHT,
    val bgAlpha: Float            = 1.0f,
    val accentColor: String       = "#2E7D52",
    val useDynamic: Boolean       = false,
    val textColor: String         = WidgetTextColorPresets.AUTO,
    val textWeight: WidgetTextWeight = WidgetTextWeight.NORMAL,
    val textSize: WidgetTextSize  = WidgetTextSize.MEDIUM
)
