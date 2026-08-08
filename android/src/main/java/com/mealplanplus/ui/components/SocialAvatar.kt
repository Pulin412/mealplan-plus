package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Deterministic, client-rendered generated avatar (no Firebase Storage — banned by the zero-billing
 * guardrail). A stable [seed] maps to a fixed hue + initials, so the same user always looks the same
 * across devices and sessions.
 */
private val AVATAR_PALETTE = listOf(
    Color(0xFF4F46E5), Color(0xFF0EA5E9), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFFEC4899), Color(0xFF8B5CF6), Color(0xFF14B8A6),
)

private fun colorFor(seed: String): Color {
    val h = abs(seed.ifBlank { "?" }.hashCode())
    return AVATAR_PALETTE[h % AVATAR_PALETTE.size]
}

private fun initialsFor(label: String?, fallback: String): String {
    val src = label?.trim().takeUnless { it.isNullOrBlank() } ?: fallback
    val parts = src.split(" ", "_", "-").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }
}

@Composable
fun SocialAvatar(
    seed: String?,
    label: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val effectiveSeed = seed?.takeUnless { it.isBlank() } ?: (label ?: "?")
    Box(
        modifier.size(size).clip(CircleShape).background(colorFor(effectiveSeed)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initialsFor(label, effectiveSeed),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}
