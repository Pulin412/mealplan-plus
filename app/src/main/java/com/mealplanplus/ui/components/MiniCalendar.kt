package com.mealplanplus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

/**
 * Compact inline calendar for HomeScreen.
 * Shows a mini month view with color-coded plan status.
 * Yellow = planned but not completed, Green = completed
 */
@Composable
fun MiniCalendar(
    currentMonth: YearMonth,
    plansForMonth: Map<String, Boolean>,  // date string → isCompleted
    dietNames: Map<String, String> = emptyMap(),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Month header with navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "${currentMonth.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar days grid
            val firstDay = currentMonth.atDay(1)
            val startOffset = (firstDay.dayOfWeek.value % 7)
            val daysInMonth = currentMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            val today = LocalDate.now()

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - startOffset + 1

                        if (dayNumber in 1..daysInMonth) {
                            val date = currentMonth.atDay(dayNumber)
                            val dateStr = date.toString()
                            val isToday = date == today
                            val hasPlan = plansForMonth.containsKey(dateStr)
                            val isCompleted = plansForMonth[dateStr] ?: false
                            val dietName = dietNames[dateStr]

                            MiniCalendarDay(
                                day = dayNumber,
                                isToday = isToday,
                                hasPlan = hasPlan,
                                isCompleted = isCompleted,
                                dietName = dietName,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCalendarDay(
    day: Int,
    isToday: Boolean,
    hasPlan: Boolean,
    isCompleted: Boolean = false,
    dietName: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Color scheme: Green = completed, Yellow = planned but not completed
    val planCompletedColor = Color(0xFF4CAF50)  // Green
    val planPendingColor = Color(0xFFFFC107)    // Yellow/Amber

    val bgColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        hasPlan && isCompleted -> planCompletedColor
        hasPlan && !isCompleted -> planPendingColor
        else -> Color.Transparent
    }
    val textColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        hasPlan && isCompleted -> Color.White
        hasPlan && !isCompleted -> Color.Black
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.dp)
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
                style = MaterialTheme.typography.labelSmall,
                color = textColor
            )
            if (hasPlan && dietName != null) {
                Text(
                    text = dietName,
                    fontSize = 6.sp,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1
                )
            } else if (hasPlan) {
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(textColor.copy(alpha = 0.6f))
                )
            }
        }
    }
}
