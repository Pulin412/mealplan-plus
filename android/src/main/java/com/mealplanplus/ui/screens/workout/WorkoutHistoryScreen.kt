package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSessionWithSets
import com.mealplanplus.ui.theme.*
import com.mealplanplus.util.toEpochMs
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class HistoryView { LIST, CALENDAR }

@Composable
fun WorkoutHistoryScreen(
    onNavigateToLog: () -> Unit,
    onNavigateToExercises: () -> Unit,
    onNavigateToTemplates: () -> Unit = {},
    onNavigateToSession: (Long) -> Unit = {},
    onCreateTemplate: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var toDelete by remember { mutableStateOf<WorkoutSessionWithSets?>(null) }
    var viewMode by remember { mutableStateOf(HistoryView.LIST) }
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var bulkDeleteTarget by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var bulkDeleteLabel by remember { mutableStateOf("") }

    val zone = ZoneId.systemDefault()

    val sessionDates: Set<LocalDate> = remember(state.sessions) {
        state.sessions.map { Instant.ofEpochMilli(it.session.date).atZone(zone).toLocalDate() }.toSet()
    }

    val visibleSessions: List<WorkoutSessionWithSets> = remember(state.sessions, viewMode, selectedDay) {
        if (viewMode == HistoryView.CALENDAR && selectedDay != null) {
            state.sessions.filter {
                Instant.ofEpochMilli(it.session.date).atZone(zone).toLocalDate() == selectedDay
            }
        } else {
            state.sessions
        }
    }

    val totalSessions = state.sessions.size
    val totalMinutes  = state.sessions.mapNotNull { it.session.durationMinutes }.sum()
    val totalSets     = state.sessions.sumOf { it.sets.size }

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onCreateTemplate,
                containerColor = Color(0xFF111111),
                contentColor   = Color.White,
                shape          = CircleShape,
                modifier       = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create workout", modifier = Modifier.size(22.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {

            // ── Page header ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgPage)
                        .padding(start = 20.dp, end = 4.dp, top = 56.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Workouts",
                            fontSize      = 20.sp,
                            fontWeight    = FontWeight.Bold,
                            color         = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "Training history",
                            fontSize = 12.sp,
                            color    = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    TextButton(onClick = onNavigateToExercises) {
                        Text("My Exercises", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                    }
                    TextButton(onClick = onNavigateToTemplates) {
                        Text("My Workouts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                    }
                    IconButton(onClick = {
                        viewMode = if (viewMode == HistoryView.LIST) HistoryView.CALENDAR else HistoryView.LIST
                        selectedDay = null
                    }) {
                        Icon(
                            if (viewMode == HistoryView.LIST) Icons.Default.CalendarMonth else Icons.AutoMirrored.Filled.ViewList,
                            contentDescription = "Toggle view",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Stat row ──────────────────────────────────────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WsCard(value = "$totalSessions",                                 label = "Sessions",   modifier = Modifier.weight(1f))
                    WsCard(value = if (totalMinutes > 0) "$totalMinutes" else "--",  label = "Minutes",    modifier = Modifier.weight(1f))
                    WsCard(value = "$totalSets",                                     label = "Total sets", modifier = Modifier.weight(1f))
                }
            }

            // ── "Log a Workout" button ────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF111111))
                        .clickable(onClick = onNavigateToLog)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("LOG A WORKOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF888888), letterSpacing = 0.5.sp)
                            Text("Start a new session", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 3.dp))
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("▶ Start", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111111))
                        }
                    }
                }
            }

            if (state.sessions.isEmpty()) {
                item { EmptyWorkoutsState(onNavigateToLog) }
            } else {

                // ── Calendar view ─────────────────────────────────────────
                if (viewMode == HistoryView.CALENDAR) {
                    item {
                        WorkoutCalendarView(
                            month          = calendarMonth,
                            sessionDates   = sessionDates,
                            selectedDay    = selectedDay,
                            onDaySelected  = { day -> selectedDay = if (selectedDay == day) null else day },
                            onPrevMonth    = { calendarMonth = calendarMonth.minusMonths(1); selectedDay = null },
                            onNextMonth    = { calendarMonth = calendarMonth.plusMonths(1); selectedDay = null },
                            onDeleteWeek   = { from, to, label ->
                                bulkDeleteTarget = from to to
                                bulkDeleteLabel = label
                            },
                            onDeleteMonth  = {
                                val from = calendarMonth.atDay(1).toEpochMs()
                                val to   = calendarMonth.atEndOfMonth().toEpochMs()
                                bulkDeleteTarget = from to to
                                bulkDeleteLabel = calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                            }
                        )
                    }
                }

                // ── Section header ────────────────────────────────────────
                item {
                    val label = when {
                        viewMode == HistoryView.CALENDAR && selectedDay != null ->
                            selectedDay!!.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                        viewMode == HistoryView.CALENDAR ->
                            calendarMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                        else -> "Recent sessions"
                    }
                    Text(
                        label,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextSecondary,
                        modifier   = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                if (visibleSessions.isEmpty()) {
                    item {
                        Text(
                            "No sessions on this day",
                            fontSize = 13.sp,
                            color    = TextSecondary,
                            modifier = Modifier.padding(start = 20.dp, top = 8.dp)
                        )
                    }
                } else {
                    item {
                        Card(
                            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            shape     = RoundedCornerShape(14.dp),
                            colors    = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            visibleSessions.forEachIndexed { idx, item ->
                                SessionRow(
                                    item     = item,
                                    onTap    = { onNavigateToSession(item.session.id) },
                                    onDelete = { toDelete = item }
                                )
                                if (idx < visibleSessions.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Single-session delete dialog ──────────────────────────────────────────
    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title   = { Text("Delete workout?", fontWeight = FontWeight.Bold) },
            text    = { Text("\"${item.session.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSession(item.session); toDelete = null }) {
                    Text("Delete", color = TextDestructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ── Bulk delete dialog ────────────────────────────────────────────────────
    bulkDeleteTarget?.let { (from, to) ->
        AlertDialog(
            onDismissRequest = { bulkDeleteTarget = null },
            title   = { Text("Delete sessions?", fontWeight = FontWeight.Bold) },
            text    = { Text("All workout sessions in \"$bulkDeleteLabel\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSessionsInRange(from, to)
                    bulkDeleteTarget = null
                    selectedDay = null
                }) {
                    Text("Delete all", color = TextDestructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { bulkDeleteTarget = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Calendar composable ────────────────────────────────────────────────────────

@Composable
private fun WorkoutCalendarView(
    month: YearMonth,
    sessionDates: Set<LocalDate>,
    selectedDay: LocalDate?,
    onDaySelected: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteWeek: (Long, Long, String) -> Unit,
    onDeleteMonth: () -> Unit
) {
    val today = LocalDate.now()

    // Build week rows: each row is a list of 7 LocalDate? (null = outside month)
    val firstOfMonth = month.atDay(1)
    val startDow = firstOfMonth.dayOfWeek.value % 7  // Sunday=0 … Saturday=6
    val daysInMonth = month.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    repeat(startDow) { cells.add(null) }
    for (d in 1..daysInMonth) cells.add(month.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    val weeks = cells.chunked(7)

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Month navigation header
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = TextSecondary)
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                IconButton(onClick = onNextMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = TextSecondary)
                }
            }

            // Day-of-week header (S M T W T F S)
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        label,
                        modifier  = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize  = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color     = Color(0xFFAAAAAA)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Week rows
            weeks.forEach { week ->
                val weekDays = week.filterNotNull()
                val weekHasSessions = weekDays.any { it in sessionDates }

                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    week.forEach { day ->
                        Box(
                            modifier            = Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment    = Alignment.Center
                        ) {
                            if (day != null) {
                                val isSelected  = day == selectedDay
                                val isToday     = day == today
                                val hasSession  = day in sessionDates

                                val bgColor = when {
                                    isSelected -> DesignGreen
                                    isToday    -> Color(0xFFEEEEEE)
                                    else       -> Color.Transparent
                                }

                                Box(
                                    modifier         = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .clickable { onDaySelected(day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            day.dayOfMonth.toString(),
                                            fontSize   = 12.sp,
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color      = when {
                                                isSelected -> Color.White
                                                isToday    -> TextPrimary
                                                else       -> TextPrimary
                                            }
                                        )
                                        if (hasSession) {
                                            Spacer(Modifier.height(1.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else DesignGreen)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Delete-week icon (only show if row has sessions)
                    if (weekHasSessions) {
                        val from = weekDays.first().toEpochMs()
                        val to   = weekDays.last().toEpochMs()
                        val fmt  = DateTimeFormatter.ofPattern("MMM d")
                        val label = "${weekDays.first().format(fmt)} – ${weekDays.last().format(fmt)}"
                        IconButton(
                            onClick  = { onDeleteWeek(from, to, label) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete week",
                                tint     = Color(0xFFCCCCCC),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else {
                        Spacer(Modifier.size(28.dp))
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = Color(0xFFF0F0F0), thickness = 1.dp)
            Spacer(Modifier.height(8.dp))

            // Delete month button
            val monthHasSessions = sessionDates.any { it.year == month.year && it.monthValue == month.monthValue }
            if (monthHasSessions) {
                TextButton(
                    onClick  = onDeleteMonth,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = TextDestructive, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Delete ${month.format(DateTimeFormatter.ofPattern("MMMM"))}",
                        fontSize = 12.sp,
                        color    = TextDestructive
                    )
                }
            }
        }
    }
}

// ── Stat card ──────────────────────────────────────────────────────────────────
@Composable
private fun WsCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                value,
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Bold,
                color         = TextPrimary,
                letterSpacing = (-0.4).sp
            )
            Text(label, fontSize = 10.sp, color = Color(0xFFAAAAAA))
        }
    }
}

// ── Session row ────────────────────────────────────────────────────────────────
@Composable
private fun SessionRow(item: WorkoutSessionWithSets, onTap: () -> Unit = {}, onDelete: () -> Unit) {
    val zone     = ZoneId.systemDefault()
    val date     = Instant.ofEpochMilli(item.session.date).atZone(zone).toLocalDate()
    val isToday  = date == LocalDate.now()
    val dayName  = if (isToday) "TODAY"
                   else date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase()
    val dayNum   = date.dayOfMonth.toString()

    val setCount = item.sets.size
    val exCount  = item.sets.map { it.exercise.id }.distinct().size
    val durLabel = item.session.durationMinutes?.let { "${it} min" } ?: "$setCount sets"

    Row(
        modifier              = Modifier.fillMaxWidth().clickable(onClick = onTap).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier            = Modifier.width(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAAAAAA))
            Text(dayNum,  fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.session.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                "$durLabel · $exCount exercise${if (exCount != 1) "s" else ""}",
                fontSize = 11.sp,
                color    = Color(0xFFAAAAAA)
            )
        }

        Text(
            "$setCount sets",
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            color      = TextSecondary
        )

        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFCCCCCC), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptyWorkoutsState(onLog: () -> Unit) {
    Column(
        modifier              = Modifier.fillMaxWidth().padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(10.dp)
    ) {
        Text("🏋️", fontSize = 44.sp)
        Text("No workouts yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Tap + to log your first session", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick        = onLog,
            colors         = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
            shape          = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 13.dp)
        ) {
            Text("Log a workout", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
