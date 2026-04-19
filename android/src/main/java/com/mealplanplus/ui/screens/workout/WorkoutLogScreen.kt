package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.ui.theme.*
import java.time.LocalDate

@Composable
fun WorkoutLogScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var sessionName by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(Step.NAME) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    when (step) {
        Step.NAME -> SessionNameStep(
            name = sessionName,
            onNameChange = { sessionName = it },
            onBack = onBack,
            onNext = {
                viewModel.startSession(sessionName.ifBlank { "Workout" }, LocalDate.now())
                step = Step.SETS
            }
        )
        Step.SETS -> SetLoggingStep(
            state = state,
            selectedExercise = selectedExercise,
            onSelectExercise = { selectedExercise = it },
            onAddSet = { ex, reps, weight, dur ->
                viewModel.addSet(ex.id, reps, weight, dur)
            },
            onFinish = {
                viewModel.finishSession()
                onFinished()
            }
        )
    }
}

private enum class Step { NAME, SETS }

@Composable
private fun SessionNameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TopBarGreen)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Log Workout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Session name", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("e.g. Morning push") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
            ) { Text("Continue") }
        }
    }
}

@Composable
private fun SetLoggingStep(
    state: WorkoutUiState,
    selectedExercise: Exercise?,
    onSelectExercise: (Exercise) -> Unit,
    onAddSet: (Exercise, Int?, Double?, Int?) -> Unit,
    onFinish: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TopBarGreen)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.activeSession?.name ?: "Workout",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onFinish) {
                Icon(Icons.Default.Check, contentDescription = "Finish", tint = Color.White)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exercise picker
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = BrandGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            selectedExercise?.name ?: "Select exercise…",
                            color = if (selectedExercise != null) TextPrimary else TextMuted,
                            fontWeight = if (selectedExercise != null) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            // Set entry
            if (selectedExercise != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Add set", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (selectedExercise.category != ExerciseCategory.CARDIO) {
                                    OutlinedTextField(
                                        value = reps,
                                        onValueChange = { reps = it },
                                        label = { Text("Reps") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = weight,
                                        onValueChange = { weight = it },
                                        label = { Text("kg") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = duration,
                                        onValueChange = { duration = it },
                                        label = { Text("Duration (sec)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onAddSet(
                                        selectedExercise,
                                        reps.toIntOrNull(),
                                        weight.toDoubleOrNull(),
                                        duration.toIntOrNull()
                                    )
                                    reps = ""; weight = ""; duration = ""
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                            ) { Text("Add set") }
                        }
                    }
                }
            }

            // Logged sets summary
            val grouped = state.activeSets.groupBy { it.exerciseId }
            if (grouped.isNotEmpty()) {
                item {
                    Text("Logged sets", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextSecondary)
                }
                items(grouped.entries.toList()) { (_, sets) ->
                    val ex = state.exercises.find { it.id == sets.first().exerciseId }
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(ex?.name ?: "Exercise", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                            sets.forEachIndexed { i, s ->
                                val summary = buildString {
                                    append("Set ${i + 1}: ")
                                    s.reps?.let { append("${it} reps") }
                                    s.weightKg?.let { append(" × ${it}kg") }
                                    s.durationSeconds?.let { append("${it}s") }
                                }
                                Text(summary, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            exercises = state.exercises,
            onSelect = { ex ->
                onSelectExercise(ex)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { it.name.contains(query, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text("Select exercise", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            items(filtered, key = { it.id }) { ex ->
                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { Text(ex.category.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                    modifier = Modifier.clickable { onSelect(ex) }
                )
                Divider()
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
