package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Plan status colors
val PlanCompletedColor = Color(0xFF4CAF50)  // Green
val PlanPendingColor = Color(0xFFFFC107)    // Yellow/Amber

/**
 * Shared calendar day cell component for both full calendar and mini calendar
 */
@Composable
fun CalendarDayCell(
    day: Int,
    isSelected: Boolean = false,
    isToday: Boolean,
    hasPlan: Boolean,
    isCompleted: Boolean = false,
    compact: Boolean = false,
    dietName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> if (compact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
        hasPlan && isCompleted -> PlanCompletedColor
        hasPlan && !isCompleted -> PlanPendingColor
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> if (compact) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
        hasPlan && isCompleted -> Color.White
        hasPlan && !isCompleted -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    val padding: Dp = if (compact) 1.dp else 2.dp
    val textStyle: TextStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodyMedium
    val dietNameSize = if (compact) 6.sp else 8.sp
    val dotSize: Dp = if (compact) 3.dp else 4.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(padding)
            .clip(CircleShape)
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
                style = textStyle,
                color = textColor
            )
            if (hasPlan && dietName != null) {
                Text(
                    text = dietName,
                    fontSize = dietNameSize,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            } else if (hasPlan) {
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.6f))
                )
            }
        }
    }
}
