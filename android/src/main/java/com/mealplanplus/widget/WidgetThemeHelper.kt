package com.mealplanplus.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.text.FontWeight
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetBgStyle
import com.mealplanplus.util.WidgetTextColorPresets
import com.mealplanplus.util.WidgetTextWeight
import com.mealplanplus.util.WidgetTextSize

/**
 * Converts a [WidgetAppearanceState] into the concrete [Color] values used by
 * all three Glance widgets.
 *
 * Call these from inside a @Composable context.  The "dynamic" case is handled
 * by the caller (it falls through to the white fallback here so the widget
 * composable can swap in GlanceTheme.colors.widgetBackground instead).
 */

/**
 * Returns the widget background [Color] (with alpha applied).
 *
 * When [WidgetAppearanceState.useDynamic] is true the caller should use
 * `GlanceTheme.colors.widgetBackground` instead of this value.
 */
fun widgetBgColor(prefs: WidgetAppearanceState): Color {
    val argb = WidgetBgStyle.toArgb(prefs.bgStyle)
    val base = Color(argb)
    return base.copy(alpha = prefs.bgAlpha)
}

/**
 * Returns the header / accent [Color] parsed from the stored hex string.
 * Falls back to Brand Green on any parse error.
 */
fun widgetAccentColor(prefs: WidgetAppearanceState): Color {
    return try {
        Color(android.graphics.Color.parseColor(prefs.accentColor))
    } catch (_: IllegalArgumentException) {
        Color(0xFF2E7D52)
    }
}

/**
 * A lighter version of the accent used for secondary surfaces (badge, footer,
 * day-cell highlights).  Blends 15 % of the accent into white.
 */
fun widgetAccentContainer(prefs: WidgetAppearanceState): Color {
    val accent = widgetAccentColor(prefs)
    return Color(
        red   = 1f - (1f - accent.red)   * 0.18f,
        green = 1f - (1f - accent.green) * 0.18f,
        blue  = 1f - (1f - accent.blue)  * 0.18f,
        alpha = prefs.bgAlpha
    )
}

/**
 * Returns a suitable "on-accent" text/icon color (white or near-black),
 * chosen by rough luminance of the accent.
 */
fun widgetOnAccentColor(prefs: WidgetAppearanceState): Color {
    val a = widgetAccentColor(prefs)
    val luminance = 0.2126f * a.red + 0.7152f * a.green + 0.0722f * a.blue
    return if (luminance < 0.45f) Color.White else Color(0xFF1A1C1E)
}

// ─── Text helpers ─────────────────────────────────────────────────────────────

/**
 * Returns the primary text [Color] for widget body text (slot names, day numbers, etc.).
 *
 * Returns `null` when [WidgetTextColorPresets.AUTO] is selected — the caller should
 * fall back to `GlanceTheme.colors.onSurface` in that case.
 */
fun widgetTextColor(prefs: WidgetAppearanceState): Color? {
    if (prefs.textColor == WidgetTextColorPresets.AUTO) return null
    return try {
        Color(android.graphics.Color.parseColor(prefs.textColor))
    } catch (_: IllegalArgumentException) { null }
}

/**
 * Returns the secondary/muted text [Color] (labels, sub-text).
 *
 * Returns `null` for AUTO — caller falls back to `GlanceTheme.colors.onSurfaceVariant`.
 */
fun widgetSubTextColor(prefs: WidgetAppearanceState): Color? {
    val base = widgetTextColor(prefs) ?: return null
    return base.copy(alpha = 0.65f)
}

/**
 * Returns the Glance [FontWeight] for widget body labels.
 */
fun widgetFontWeight(prefs: WidgetAppearanceState): FontWeight =
    if (prefs.textWeight == WidgetTextWeight.BOLD) FontWeight.Bold else FontWeight.Normal

/**
 * Scales a base font size (in sp) by the user's text size preference.
 */
fun Float.scaled(prefs: WidgetAppearanceState): Float = this * prefs.textSize.scale
