package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Plan status dot colours
val PlanCompletedColor = Color(0xFF2E7D52)  // green – planned/completed
val PlanPendingColor   = Color(0xFF2E7D52)  // green – pending plan
val PlanMissedColor    = Color(0xFFE53E3E)  // red   – past, not completed

/**
 * Calendar day cell — rounded square (6 dp corners) matching the design .cd style.
 *
 * - Today   → solid black (#111) background, white bold number, no dot.
 * - Selected (not today) → light-grey (#F0F0F0) background.
 * - Has plan → 5 dp coloured dot below the number (green = planned, red = past missed).
 * - No plan  → muted grey number, no dot — but same total height as a dot row.
 */
@Composable
fun CalendarDayCell(
    day: Int,
    isSelected: Boolean = false,
    isToday: Boolean,
    hasPlan: Boolean,
    isCompleted: Boolean = false,
    isPast: Boolean = false,
    hasWorkout: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isToday    -> Color(0xFF111111)
        isSelected -> Color(0xFFF0F0F0)
        else       -> Color.Transparent
    }
    val numberColor = when {
        isToday                    -> Color.White
        !hasPlan && isPast         -> Color(0xFFCCCCCC)
        !hasPlan                   -> Color(0xFF888888)
        else                       -> Color(0xFF111111)
    }
    // Green for all planned/completed; red only if past and not completed (missed)
    val dotColor = when {
        !hasPlan                   -> Color.Transparent
        isPast && !isCompleted     -> PlanMissedColor
        else                       -> PlanCompletedColor
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = numberColor,
                lineHeight = 14.sp
            )
            // Dot row — always same height; up to two dots (diet + workout)
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(5.dp).clip(CircleShape)
                        .background(if (hasPlan && !isToday) dotColor else Color.Transparent)
                )
                Box(
                    modifier = Modifier.size(5.dp).clip(CircleShape)
                        .background(if (hasWorkout && !isToday) Color(0xFF1E4FBF) else Color.Transparent)
                )
            }
        }
    }
}
