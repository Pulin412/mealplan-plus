package com.mealplanplus.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Design-system tokens — source of truth: design_v2 "Build Spec" §3.
// oklch values from the spec are noted; Compose uses their sRGB approximations.
// ─────────────────────────────────────────────────────────────────────────────

// Core
val Teal        = Color(0xFF5BA4A4)   // primary — oklch(0.62 0.09 210)
val Ink         = Color(0xFF14181B)
val AppBg       = Color(0xFFF7F9FA)
val Surface     = Color(0xFFFFFFFF)
val Success     = Color(0xFF4DA876)   // workout — oklch(0.66 0.13 150)
val StreakFlame = Color(0xFFD4813A)   // oklch(0.7 0.18 45)
val Danger      = Color(0xFFB23B3B)

// Muted text ramp: #5b666e → #8a949b → #9aa4aa → #a2abb1
val MutedDark   = Color(0xFF5B666E)
val Muted       = Color(0xFF8A949B)
val MutedLight  = Color(0xFF9AA4AA)
val MutedFaint  = Color(0xFFA2ABB1)

// Border / surface ramp: #eaeef0, #eef1f3, #dfe6e8, #e4e8eb, #f2f4f5
val CardBorder   = Color(0xFFEAEEF0)
val BorderSoft   = Color(0xFFEEF1F3)
val BorderCool   = Color(0xFFDFE6E8)
val BorderMuted  = Color(0xFFE4E8EB)
val SurfaceMuted = Color(0xFFF2F4F5)   // chips, search field, expand button bg

// Macro colours (used on Home strip / chips) — spec §3
val Protein = Color(0xFF3E8C97)   // oklch(0.60 0.10 200)
val Carbs   = Color(0xFF5E7BB8)   // oklch(0.60 0.11 255)
val Fat     = Color(0xFF4CA47B)   // oklch(0.62 0.11 150)

// Favourite star — gold when active (spec: oklch(0.72 0.13 75)), grey when empty
val FavoriteGold  = Color(0xFFE0A93B)
val FavoriteEmpty = Color(0xFFC4CCD1)

// Aliases for readability at call sites
val DeleteColor = FavoriteEmpty
val SearchBg    = SurfaceMuted
