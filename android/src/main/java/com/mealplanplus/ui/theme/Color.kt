package com.mealplanplus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Design-system colour tokens. Source of truth: design_v2 "Build Spec" §3 (light).
// Every screen/component reads the SEMANTIC tokens below (Ink, AppBg, …), which
// resolve from [LocalAppColors] so the whole app is theme-aware. Add a new field
// to [AppColors] + both palettes rather than hardcoding a Color at a call site.
// ─────────────────────────────────────────────────────────────────────────────

/** The full semantic palette for one theme. */
data class AppColors(
    val accent: Color,          // primary (teal)
    val onAccent: Color,        // text/icon on an accent fill
    val ink: Color,             // primary text / strong fills
    val appBg: Color,           // window background
    val surface: Color,         // cards, sheets, raised segments
    val surfaceMuted: Color,    // chips, search field, expand button bg
    // muted text ramp (darkest → faintest)
    val mutedDark: Color,
    val muted: Color,
    val mutedLight: Color,
    val mutedFaint: Color,
    // border / hairline ramp
    val cardBorder: Color,
    val borderSoft: Color,
    val borderCool: Color,
    val borderMuted: Color,
    // status
    val danger: Color,
    val success: Color,
    val streakFlame: Color,
    // macros + their pill backgrounds
    val protein: Color,
    val carbs: Color,
    val fat: Color,
    val proteinBg: Color,
    val carbsBg: Color,
    val fatBg: Color,
    // favourite star
    val favoriteGold: Color,
    val favoriteEmpty: Color,
    // misc one-offs kept semantic so they adapt
    val onlineAdd: Color,
    val mealItemName: Color,
    val dashedStroke: Color,
    val disabledFill: Color,
    val disabledText: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightAppColors = AppColors(
    accent       = Color(0xFF5BA4A4),
    onAccent     = Color(0xFFFFFFFF),
    ink          = Color(0xFF14181B),
    appBg        = Color(0xFFF7F9FA),
    surface      = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF2F4F5),
    mutedDark    = Color(0xFF5B666E),
    muted        = Color(0xFF8A949B),
    mutedLight   = Color(0xFF9AA4AA),
    mutedFaint   = Color(0xFFA2ABB1),
    cardBorder   = Color(0xFFEAEEF0),
    borderSoft   = Color(0xFFEEF1F3),
    borderCool   = Color(0xFFDFE6E8),
    borderMuted  = Color(0xFFE4E8EB),
    danger       = Color(0xFFB23B3B),
    success      = Color(0xFF4DA876),
    streakFlame  = Color(0xFFD4813A),
    protein      = Color(0xFF3E8C97),
    carbs        = Color(0xFF5E7BB8),
    fat          = Color(0xFF4CA47B),
    proteinBg    = Color(0xFFD6EEEE),
    carbsBg      = Color(0xFFD6E4F7),
    fatBg        = Color(0xFFD6F0DE),
    favoriteGold = Color(0xFFE0A93B),
    favoriteEmpty= Color(0xFFC4CCD1),
    onlineAdd    = Color(0xFF5B8FA4),
    mealItemName = Color(0xFF3F4A51),
    dashedStroke = Color(0xFFCDD7DA),
    disabledFill = Color(0xFFCDD8DB),
    disabledText = Color(0xFF9AADB2),
    scrim        = Color(0x8C0F1416),
    isDark       = false,
)

val DarkAppColors = AppColors(
    accent       = Color(0xFF63B4B4),
    onAccent     = Color(0xFF06231F),
    ink          = Color(0xFFE7ECEE),
    appBg        = Color(0xFF0F1416),
    surface      = Color(0xFF171D20),
    surfaceMuted = Color(0xFF1E2529),
    mutedDark    = Color(0xFF9CA8AF),
    muted        = Color(0xFF808B92),
    mutedLight   = Color(0xFF6C767C),
    mutedFaint   = Color(0xFF5C666C),
    cardBorder   = Color(0xFF262E32),
    borderSoft   = Color(0xFF222A2E),
    borderCool   = Color(0xFF2E373B),
    borderMuted  = Color(0xFF293135),
    danger       = Color(0xFFD46B6B),
    success      = Color(0xFF5FB98A),
    streakFlame  = Color(0xFFDE9457),
    protein      = Color(0xFF5FB4BF),
    carbs        = Color(0xFF7E9AD6),
    fat          = Color(0xFF66BE95),
    proteinBg    = Color(0xFF1E3438),
    carbsBg      = Color(0xFF24304A),
    fatBg        = Color(0xFF1F3A2E),
    favoriteGold = Color(0xFFE8B653),
    favoriteEmpty= Color(0xFF3A444A),
    onlineAdd    = Color(0xFF7BAFC4),
    mealItemName = Color(0xFFC2CCD1),
    dashedStroke = Color(0xFF333C40),
    disabledFill = Color(0xFF2A3338),
    disabledText = Color(0xFF63707A),
    scrim        = Color(0x99000000),
    isDark       = true,
)

/** Resolved palette for the current theme; provided by [MealPlanTheme]. */
val LocalAppColors = staticCompositionLocalOf { LightAppColors }

// ── Semantic aliases — keep the historical names, now theme-aware ─────────────
// These read [LocalAppColors] so existing call sites (Ink, AppBg, …) adapt for free.
val Teal:         Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accent
val OnAccent:     Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onAccent
val Ink:          Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.ink
val AppBg:        Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.appBg
val Surface:      Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface
val SurfaceMuted: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceMuted
val MutedDark:    Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mutedDark
val Muted:        Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.muted
val MutedLight:   Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mutedLight
val MutedFaint:   Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mutedFaint
val CardBorder:   Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.cardBorder
val BorderSoft:   Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderSoft
val BorderCool:   Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderCool
val BorderMuted:  Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderMuted
val Danger:       Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.danger
val Success:      Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.success
val StreakFlame:  Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.streakFlame
val Protein:      Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.protein
val Carbs:        Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.carbs
val Fat:          Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.fat
val FavoriteGold: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.favoriteGold
val FavoriteEmpty:Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.favoriteEmpty
val DeleteColor:  Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.favoriteEmpty
val SearchBg:     Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surfaceMuted
val OnlineAdd:    Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onlineAdd
val MealItemName: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.mealItemName
val DashedStroke: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.dashedStroke
val DisabledFill: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledFill
val DisabledText: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.disabledText
val ProteinBg:    Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.proteinBg
val CarbsBg:      Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.carbsBg
val FatBg:        Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.fatBg
val Scrim:        Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.scrim
