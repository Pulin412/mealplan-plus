package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun WorkoutHistoryScreen(
    onNavigateToLog: () -> Unit,
    onNavigateToExercises: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var toDelete by remember { mutableStateOf<WorkoutSessionWithSets?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToLog,
                containerColor = BrandGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log workout", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPage)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TopBarGreen)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Workouts",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onNavigateToExercises) {
                    Text("Exercises", color = Color.White, fontSize = 13.sp)
                }
            }

            if (state.sessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No workouts yet", color = TextSecondary, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onNavigateToLog,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                        ) { Text("Log a workout") }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.sessions, key = { it.session.id }) { item ->
                        WorkoutSessionCard(item, onDelete = { toDelete = item })
                    }
                }
            }
        }
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete workout?") },
            text = { Text("\"${item.session.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(item.session)
                    toDelete = null
                }) { Text("Delete", color = TextDestructive) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WorkoutSessionCard(
    item: WorkoutSessionWithSets,
    onDelete: () -> Unit
) {
    val date = Instant.ofEpochMilli(item.session.date)
        .atZone(ZoneId.systemDefault()).toLocalDate()
    val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")
    val setCount = item.sets.size
    val exerciseCount = item.sets.map { it.exercise.id }.distinct().size

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.session.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    formatter.format(date),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatPill("$exerciseCount exercises")
                    StatPill("$setCount sets")
                    item.session.durationMinutes?.let { StatPill("${it}min") }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
            }
        }
    }
}

@Composable
private fun StatPill(text: String) {
    Text(
        text,
        fontSize = 11.sp,
        color = TextSecondary,
        modifier = Modifier
            .background(Color(0xFFF0F0F0), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}
