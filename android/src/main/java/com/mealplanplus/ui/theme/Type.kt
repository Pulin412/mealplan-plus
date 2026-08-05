package com.mealplanplus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mealplanplus.R

// DM Mono for numerals & data (spec §3). DM Mono ships Regular (400) + Medium (500);
// heavier weights render as synthetic bold.
val DmMono = FontFamily(
    Font(R.font.dm_mono_regular, FontWeight.Normal),
    Font(R.font.dm_mono_medium, FontWeight.Medium),
)

val Typography = Typography(
    headlineLarge = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

/**
 * Semantic font-size tokens — the single place to tune text sizes for list/detail
 * content across screens. Screens reference these instead of hardcoding `sp`, so a
 * readability change is one edit here. Any individual `Text` may still pass its own
 * `fontSize = …` to override a token locally where a screen needs something different.
 *
 * Bumped up from the previous inline literals (slot labels were 8.5, metas 9.5–10,
 * item names 11.5–12) for legibility on the Plan and Diets screens.
 */
object AppText {
    val slotLabel = 10.sp   // slot header chips (BREAKFAST, LUNCH, …)
    val meta      = 11.sp   // kcal / macro / secondary numeric rows
    val subItem   = 11.5.sp // nested foods listed under a meal
    val itemName  = 13.sp   // meal & ingredient names
    val section   = 13.sp   // section titles ("Plan", "Today's meals")
    val body      = 13.sp   // default body copy
}
