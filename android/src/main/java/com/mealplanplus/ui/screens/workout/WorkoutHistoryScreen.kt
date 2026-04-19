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
import androidx.compose.material.icons.filled.Delete
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
import com.mealplanplus.data.model.WorkoutSessionWithSets
import com.mealplanplus.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutHistoryScreen(
    onNavigateToLog: () -> Unit,
    onNavigateToExercises: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var toDelete by remember { mutableStateOf<WorkoutSessionWithSets?>(null) }

    val totalSessions = state.sessions.size
    val totalSets = state.sessions.sumOf { it.sets.size }
    val totalMins = state.sessions.mapNotNull { it.session.durationMinutes }.sum()
    val uniqueExercises = state.sessions.flatMap { it.sets.map { s -> s.exercise.id } }.distinct().size

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLog,
                containerColor = TextPrimary,
                contentColor = CardBg,
                shape = CircleShape,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log workout", modifier = Modifier.size(22.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Page header ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgPage)
                        .padding(start = 20.dp, end = 16.dp, top = 56.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Workouts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "Training history",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    TextButton(onClick = onNavigateToExercises) {
                        Text(
                            "Exercises",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DesignGreen
                        )
                    }
                }
            }

            // ── Stat row ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WorkoutStatCard("$totalSessions", "Sessions",  "🏋️", Modifier.weight(1f))
                    WorkoutStatCard("$totalSets",     "Total sets", "🔁",  Modifier.weight(1f))
                    WorkoutStatCard(if (totalMins > 0) "${totalMins}m" else "--", "Minutes", "⏱️", Modifier.weight(1f))
                    WorkoutStatCard("$uniqueExercises", "Exercises", "💪",  Modifier.weight(1f))
                }
            }

            if (state.sessions.isEmpty()) {
                item { EmptyWorkoutState(onNavigateToLog) }
            } else {
                // ── Section label ─────────────────────────────────────────
                item {
                    Text(
                        "RECENT SESSIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                    )
                }

                items(state.sessions, key = { it.session.id }) { item ->
                    SessionListItem(
                        item = item,
                        onDelete = { toDelete = item },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete workout?", fontWeight = FontWeight.Bold) },
            text = { Text("\"${item.session.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSession(item.session); toDelete = null }) {
                    Text("Delete", color = TextDestructive, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Stat card (matches design-future .ws pattern) ────────────────────────────
@Composable
private fun WorkoutStatCard(value: String, label: String, emoji: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                letterSpacing = (-0.3).sp
            )
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
    }
}

// ── Session list item (matches design-future .ex-item pattern) ────────────────
@Composable
private fun SessionListItem(
    item: WorkoutSessionWithSets,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val date = Instant.ofEpochMilli(item.session.date)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val isToday = date == LocalDate.now()
    val dateLabel = when {
        isToday -> "Today"
        date == LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }

    val exerciseCount = item.sets.map { it.exercise.id }.distinct().size
    val setCount = item.sets.size
    val detailText = buildString {
        append("$exerciseCount exercise${if (exerciseCount != 1) "s" else ""}")
        append(" · $setCount set${if (setCount != 1) "s" else ""}")
        item.session.durationMinutes?.let { append(" · ${it}min") }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon box — 38×38, 11dp radius (design-future .ex-item icon)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(IconBgGray),
                contentAlignment = Alignment.Center
            ) {
                Text("🏋️", fontSize = 18.sp)
            }

            // Name + detail
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    item.session.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    "$dateLabel · $detailText",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            // Sets badge (design-future .sets-badge)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(TagGrayBg)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    "$setCount sets",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyWorkoutState(onNavigateToLog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("🏋️", fontSize = 44.sp)
        Text(
            "No workouts yet",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            "Tap + to log your first session",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onNavigateToLog,
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Log a workout", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CardBg)
        }
    }
}
