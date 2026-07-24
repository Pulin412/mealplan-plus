package com.mealplanplus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.FavoriteEmpty
import com.mealplanplus.ui.theme.FavoriteGold
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.Success

/** "P12 · C21 · F21" macro line (DM Mono) with breathing room between each macro. */
@Composable
fun MacroText(
    protein: Double,
    carbs: Double,
    fat: Double,
    fontSize: TextUnit = 10.5.sp,
    color: Color = MutedDark,
    gap: androidx.compose.ui.unit.Dp = 12.dp,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        listOf("P" to protein, "C" to carbs, "F" to fat).forEachIndexed { i, (letter, value) ->
            if (i > 0) {
                Text("·", fontFamily = DmMono, fontSize = fontSize, color = color,
                    modifier = Modifier.padding(horizontal = gap))
            }
            Text("$letter${value.macro()}", fontFamily = DmMono, fontSize = fontSize, color = color)
        }
    }
}

/** Bold DM Mono calorie value with a legible "kcal" unit. */
@Composable
fun CalorieValue(
    kcal: Int,
    fontSize: TextUnit = 13.sp,
    showUnit: Boolean = true,
) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
        Text(
            "$kcal",
            fontFamily = DmMono,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = Ink,
        )
        if (showUnit) {
            Text(
                " kcal",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MutedLight,
                modifier = Modifier.padding(bottom = 1.dp),
            )
        }
    }
}

/** Favourite star — gold when active, faint grey when not (design tokens). */
@Composable
fun FavoriteStar(active: Boolean, size: androidx.compose.ui.unit.Dp = 16.dp) {
    Icon(
        imageVector = if (active) Icons.Default.Star else Icons.Default.StarBorder,
        contentDescription = if (active) "Favourite" else "Add to favourites",
        tint = if (active) FavoriteGold else FavoriteEmpty,
        modifier = Modifier.size(size),
    )
}

/** "✓ Verified" (from a trusted DB) or "Custom" (user-created). */
@Composable
fun VerifiedBadge(verified: Boolean) {
    Text(
        if (verified) "✓ Verified" else "Custom",
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (verified) Success else MutedLight,
    )
}

/** Macro/number formatting: drop the decimal when whole, else one place. */
private fun Double.macro(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
