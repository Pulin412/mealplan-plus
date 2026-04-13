package com.mealplanplus.ui.theme

import androidx.compose.ui.graphics.Color

// ── Page & Surface ────────────────────────────────────────────────────────────
val BgPage        = Color(0xFFF7F7F7)   // scaffold / page background
val CardBg        = Color.White         // card / surface
val DividerColor  = Color(0xFFF5F5F5)   // subtle row divider

// ── Text hierarchy ─────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFF111111)
val TextSecondary = Color(0xFF888888)
val TextMuted     = Color(0xFFAAAAAA)
val TextPlaceholder = Color(0xFFBBBBBB)
val TextDestructive = Color(0xFFE53E3E)

// Chart / data viz (nutrition breakdown) — fixed hues so charts stay readable off-theme
val ChartProtein = Color(0xFF7C3AED)
val ChartCarbs   = Color(0xFF2E7D52)
val ChartFat     = Color(0xFFE53935)

// ── Brand green ───────────────────────────────────────────────────────────────
// BrandGreen is already defined in Color.kt; re-exported here for convenience
val DesignGreen      = Color(0xFF2E7D52)
val DesignGreenLight = Color(0xFFE8F5EE)

// ── Semantic category tags ────────────────────────────────────────────────────
// Remission / health
val TagGreen      = Color(0xFF2E7D52)
val TagGreenBg    = Color(0xFFE8F5EE)
// Maintenance / info
val TagBlue       = Color(0xFF1E4FBF)
val TagBlueBg     = Color(0xFFE8EEFF)
// SOS / warning
val TagOrange     = Color(0xFFC05200)
val TagOrangeBg   = Color(0xFFFFF0E6)
// Secondary / purple
val TagPurple     = Color(0xFF7C3AED)
val TagPurpleBg   = Color(0xFFF3EEFF)
// Neutral / gray
val TagGray       = Color(0xFF666666)
val TagGrayBg     = Color(0xFFF0F0F0)

// ── Meal slot dots ─────────────────────────────────────────────────────────────
val SlotBreakfast   = Color(0xFFF59E0B)
val SlotLunch       = Color(0xFF2E7D52)
val SlotDinner      = Color(0xFF7C3AED)
val SlotSnack       = Color(0xFF2196F3)
val SlotDefault     = Color(0xFF888888)

// ── Icon background tints ─────────────────────────────────────────────────────
val IconBgYellow  = Color(0xFFFFF8E6)
val IconBgBlue    = Color(0xFFF0F8FF)
val IconBgGray    = Color(0xFFF5F5F5)
val IconBgRed     = Color(0xFFFFF0F0)
val IconBgGreen   = Color(0xFFF5FFF5)
val IconBgPurple  = Color(0xFFF3EEFF)
val IconBgOrange  = Color(0xFFFFF0E6)
