package com.mealplanplus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Page & Surface ─────────────────────────────────────────────────────────────
val BgPage: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF111111) else Color(0xFFF7F7F7)
val CardBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1E1E) else Color.White
val DividerColor: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2E2E2E) else Color(0xFFEEEEEE)

// ── Text hierarchy ─────────────────────────────────────────────────────────────
// Light: dark/near-black text. Dark: light/near-white text.
val TextPrimary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFEEEEEE) else Color(0xFF111111)
val TextSecondary: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFAAAAAA) else Color(0xFF444444)
val TextMuted: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF888888) else Color(0xFF666666)
val TextPlaceholder: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF666666) else Color(0xFF999999)
val TextDestructive: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFFFF6B6B) else Color(0xFFE53E3E)

// ── Brand green ────────────────────────────────────────────────────────────────
val DesignGreen: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF4CAF7D) else Color(0xFF2E7D52)
val DesignGreenLight: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2D23) else Color(0xFFE8F5EE)

// Convenience aliases used in various screens
val PrimaryGreen: Color
    @Composable get() = DesignGreen
val TopBarGreen: Color
    @Composable get() = DesignGreen
val DarkGreen: Color
    @Composable get() = DesignGreen

// ── Chart / data viz — fixed vivid hues, readable in both modes ──────────────
val ChartProtein = Color(0xFF7C3AED)
val ChartCarbs   = Color(0xFF2E7D52)
val ChartFat     = Color(0xFFE53935)

// ── Macro nutrition colours ───────────────────────────────────────────────────
// Vivid and saturated enough to be legible on both light and dark surfaces.
val MacroProtein  = Color(0xFF2E7D52)
val MacroCarbs    = Color(0xFFC05200)
val MacroFat      = Color(0xFF1E4FBF)
val MacroCal      = Color(0xFFF59E0B)
// Aliases used in DailyLogScreen
val ProteinColor  = MacroProtein
val CarbsColor    = MacroCarbs
val FatColor      = MacroFat
val CaloriesColor = MacroCal
val OverColor     = MacroCal

// ── Semantic category tags ────────────────────────────────────────────────────
val TagGreen   = Color(0xFF2E7D52)
val TagGreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2D23) else Color(0xFFE8F5EE)
val TagBlue    = Color(0xFF1E4FBF)
val TagBlueBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2035) else Color(0xFFE8EEFF)
val TagOrange  = Color(0xFFC05200)
val TagOrangeBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D1A00) else Color(0xFFFFF0E6)
val TagPurple  = Color(0xFF7C3AED)
val TagPurpleBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1035) else Color(0xFFF3EEFF)
val TagGray    = Color(0xFF888888)
val TagGrayBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2A) else Color(0xFFF0F0F0)

// Alias used in HomeScreen AI insight strip
val AiPurple = TagPurple

// ── "Extra" / orange extras (logged extras section in DailyLogScreen) ────────
val ExtraBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D1A00) else Color(0xFFFFF3E0)
val ExtraText = Color(0xFFC05200)

// ── Meal slot dots (vivid — readable in both modes) ──────────────────────────
val SlotBreakfast = Color(0xFFF59E0B)
val SlotLunch     = Color(0xFF2E7D52)
val SlotDinner    = Color(0xFF7C3AED)
val SlotSnack     = Color(0xFF2196F3)
val SlotDefault   = Color(0xFF888888)

// ── Icon background tints ──────────────────────────────────────────────────────
val IconBgYellow: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D2100) else Color(0xFFFFF8E6)
val IconBgBlue: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF001830) else Color(0xFFF0F8FF)
val IconBgGray: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
val IconBgRed: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D0000) else Color(0xFFFFF0F0)
val IconBgGreen: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF002D00) else Color(0xFFF5FFF5)
val IconBgPurple: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1E1035) else Color(0xFFF3EEFF)
val IconBgOrange: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D1A00) else Color(0xFFFFF0E6)
