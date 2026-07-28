package com.mealplanplus.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/**
 * Fixed exercise-tag colour palette (design_v2 §3): each tag maps to `oklch(0.52 0.13 hue)`,
 * pre-converted to sRGB. Chips render as the tag colour text on a 12%-alpha fill. Tags outside
 * this map (user-created) fall back to a neutral colour.
 */
private val TAG_COLORS: Map<String, Color> = mapOf(
    "Chest"     to Color(0xFFA74541),
    "Back"      to Color(0xFF2E69B2),
    "Legs"      to Color(0xFF1D7D3E),
    "Shoulders" to Color(0xFF9A5500),
    "Arms"      to Color(0xFF8050A0),
    "Core"      to Color(0xFF007D86),
    "Cardio"    to Color(0xFFA74449),
    "Push"      to Color(0xFF007A97),
    "Pull"      to Color(0xFF635BB0),
    "Mobility"  to Color(0xFF00805D),
)

/** Neutral fallback for user-created tags outside the fixed palette (theme `mutedDark`). */
private val TAG_FALLBACK = Color(0xFF5B666E)

fun exerciseTagColor(name: String): Color = TAG_COLORS[name] ?: TAG_FALLBACK

/** A single exercise-tag chip (coloured text on 12%-alpha fill). */
@Composable
fun ExerciseTagChip(name: String, modifier: Modifier = Modifier) {
    val c = exerciseTagColor(name)
    Text(
        name, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = c,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.copy(alpha = 0.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}
