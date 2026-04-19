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
    onNavigateToTemplates: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var toDelete by remember { mutableStateOf<WorkoutSessionWithSets?>(null) }

    // Stats computed from all sessions
    val totalSessions  = state.sessions.size
    val totalMinutes   = state.sessions.mapNotNull { it.session.durationMinutes }.sum()
    val totalSets      = state.sessions.sumOf { it.sets.size }

    Scaffold(
        containerColor = BgPage,
        floatingActionButton = {
            FloatingActionButton(
                onClick     = onNavigateToLog,
                containerColor = Color(0xFF111111),
                contentColor   = Color.White,
                shape          = CircleShape,
                modifier       = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log workout", modifier = Modifier.size(22.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier        = Modifier.fillMaxSize().padding(padding),
            contentPadding  = PaddingValues(bottom = 100.dp)
        ) {

            // ── Page header ───────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgPage)
                        .padding(start = 20.dp, end = 16.dp, top = 56.dp, bottom = 4.dp),
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
                    TextButton(onClick = onNavigateToTemplates) {
                        Text("My Workouts", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                    }
                }
            }

            // ── Stat row: 3 cards matching .ws (no emoji) ────────────────
            item {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WsCard(value = "$totalSessions",                                      label = "Sessions",  modifier = Modifier.weight(1f))
                    WsCard(value = if (totalMinutes > 0) "$totalMinutes" else "--",       label = "Minutes",   modifier = Modifier.weight(1f))
                    WsCard(value = "$totalSets",                                          label = "Total sets",modifier = Modifier.weight(1f))
                }
            }

            // ── "Log a Workout" button — always visible below stats ───────────
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
                item {
                    Text(
                        "Recent sessions",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextSecondary,
                        modifier   = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                item {
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape     = RoundedCornerShape(14.dp),
                        colors    = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        state.sessions.forEachIndexed { idx, item ->
                            SessionRow(
                                item     = item,
                                onDelete = { toDelete = item }
                            )
                            if (idx < state.sessions.lastIndex) {
                                HorizontalDivider(
                                    color     = Color(0xFFF5F5F5),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

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
}

// ── .ws stat card — value 20sp bold, label 10sp #AAA ──────────────────────────
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

// ── Session row — date column (.li pattern) + name + duration + kcal ──────────
@Composable
private fun SessionRow(item: WorkoutSessionWithSets, onDelete: () -> Unit) {
    val zone      = ZoneId.systemDefault()
    val date      = Instant.ofEpochMilli(item.session.date).atZone(zone).toLocalDate()
    val isToday   = date == LocalDate.now()
    val dayName   = if (isToday) "TODAY"
                    else date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase()
    val dayNum    = date.dayOfMonth.toString()

    val setCount  = item.sets.size
    val exCount   = item.sets.map { it.exercise.id }.distinct().size
    val durLabel  = item.session.durationMinutes?.let { "${it} min" } ?: "$setCount sets"

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Date column (design: 9sp day name + 15sp day number)
        Column(
            modifier            = Modifier.width(34.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dayName, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFAAAAAA))
            Text(dayNum,  fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        // Name + detail
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.session.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text(
                "$durLabel · $exCount exercise${if (exCount != 1) "s" else ""}",
                fontSize = 11.sp,
                color    = Color(0xFFAAAAAA)
            )
        }

        // Right side — sets count
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
            onClick          = onLog,
            colors           = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111)),
            shape            = RoundedCornerShape(12.dp),
            contentPadding   = PaddingValues(horizontal = 28.dp, vertical = 13.dp)
        ) {
            Text("Log a workout", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
