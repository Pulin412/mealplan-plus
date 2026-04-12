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
val PlanCompletedColor = Color(0xFF2E7D52)  // green – logged/completed
val PlanPendingColor   = Color(0xFF2E7D52)  // green – planned (same dot colour)
val PlanMissedColor    = Color(0xFFE53E3E)  // red   – past, was planned, not completed

/**
 * Calendar day cell — rounded square (6 dp corners).
 * Today   → black background, white bold number, no dot.
 * Selected (not today) → light-grey background.
 * Has plan → small 4 dp coloured dot below the number.
 * No plan  → plain muted number, no dot.
 */
@Composable
fun CalendarDayCell(
    day: Int,
    isSelected: Boolean = false,
    isToday: Boolean,
    hasPlan: Boolean,
    isCompleted: Boolean = false,
    isPast: Boolean = false,
    compact: Boolean = false,
    dietName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isToday    -> Color(0xFF111111)
        isSelected -> Color(0xFFF0F0F0)
        else       -> Color.Transparent
    }
    val numberColor = when {
        isToday    -> Color.White
        !hasPlan && isPast -> Color(0xFFCCCCCC)
        !hasPlan   -> Color(0xFF888888)
        else       -> Color(0xFF111111)
    }
    val dotColor = when {
        !hasPlan              -> Color.Transparent
        isPast && !isCompleted -> PlanMissedColor   // missed log
        else                  -> PlanCompletedColor  // planned or completed
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
                fontSize = if (isToday) 13.sp else 12.sp,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                color = numberColor
            )
            if (hasPlan && !isToday) {
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            } else {
                // Keep height consistent whether dot shows or not
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
