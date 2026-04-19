package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutLogScreen(
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var sessionName by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(Step.NAME) }

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
        Step.SETS -> ActiveSessionStep(
            state = state,
            onAddSet = viewModel::addSet,
            onFinish = { viewModel.finishSession(); onFinished() },
            onBack = onBack
        )
    }
}

private enum class Step { NAME, SETS }

// ── Step 1: name the session ──────────────────────────────────────────────────

@Composable
private fun SessionNameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val today = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPage)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                "New Workout",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(Modifier.height(24.dp))

        // Form card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Label
                Text(
                    "SESSION NAME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    placeholder = {
                        Text("e.g. Morning push day", color = TextSecondary, fontSize = 14.sp)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BgPage,
                        focusedContainerColor = CardBg,
                        unfocusedBorderColor = Color(0xFFEBEBEB),
                        focusedBorderColor = TextPrimary
                    )
                )

                // Date hint
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(DesignGreen)
                    )
                    Text(today, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
        ) {
            Text(
                "Start session",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = CardBg
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

// ── Step 2: active session — black header + set logging ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSessionStep(
    state: WorkoutUiState,
    onAddSet: (Long, Int?, Double?, Int?) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var reps by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    // Stopwatch
    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    val timerLabel = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    val setsForSelected = selectedExercise?.let { ex -> state.activeSets.filter { it.exerciseId == ex.id } } ?: emptyList()
    val isCardio = selectedExercise?.category == ExerciseCategory.CARDIO

    Scaffold(containerColor = BgPage) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Black header ─────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                        .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF888888)
                            )
                        }
                        Text(
                            state.activeSession?.name ?: "Workout",
                            fontSize = 13.sp,
                            color = Color(0xFF888888),
                            modifier = Modifier.weight(1f)
                        )
                        // Finish button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DesignGreen)
                                .clickable(onClick = onFinish)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Finish",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Exercise name or placeholder
                    Text(
                        selectedExercise?.name ?: "Select an exercise",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    // Set count for selected exercise
                    Text(
                        if (setsForSelected.isEmpty()) "No sets yet"
                        else "${setsForSelected.size} set${if (setsForSelected.size != 1) "s" else ""} logged",
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Timer
                    Text(
                        timerLabel,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.W200,
                        color = Color.White,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Exercise picker card ──────────────────────────────────────
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .clickable { showPicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(IconBgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            selectedExercise?.name ?: "Choose exercise…",
                            fontSize = 14.sp,
                            fontWeight = if (selectedExercise != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedExercise != null) TextPrimary else TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        selectedExercise?.let {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(TagBlueBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    it.category.name.lowercase()
                                        .replaceFirstChar { c -> c.uppercase() },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TagBlue
                                )
                            }
                        }
                    }
                }
            }

            // ── Set input card ────────────────────────────────────────────
            if (selectedExercise != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "SET ${setsForSelected.size + 1}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = TextSecondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (!isCardio) {
                                    SetInputField(
                                        value = reps,
                                        onValueChange = { reps = it },
                                        label = "REPS",
                                        modifier = Modifier.weight(1f)
                                    )
                                    SetInputField(
                                        value = weight,
                                        onValueChange = { weight = it },
                                        label = "WEIGHT (kg)",
                                        modifier = Modifier.weight(1f),
                                        decimal = true
                                    )
                                } else {
                                    SetInputField(
                                        value = duration,
                                        onValueChange = { duration = it },
                                        label = "DURATION (sec)",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    selectedExercise?.let { ex ->
                                        onAddSet(ex.id, reps.toIntOrNull(), weight.toDoubleOrNull(), duration.toIntOrNull())
                                        reps = ""; weight = ""; duration = ""
                                    }
                                },
                                enabled = if (!isCardio) reps.isNotBlank() || weight.isNotBlank() else duration.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = CardBg)
                                Spacer(Modifier.width(6.dp))
                                Text("Add set", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CardBg)
                            }
                        }
                    }
                }
            }

            // ── Logged sets for selected exercise ─────────────────────────
            if (setsForSelected.isNotEmpty()) {
                item {
                    Text(
                        "LOGGED SETS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp)
                    )
                }
                items(setsForSelected.indices.toList()) { i ->
                    val s = setsForSelected[i]
                    SetLogRow(
                        setNumber = i + 1,
                        reps = s.reps,
                        weightKg = s.weightKg,
                        durationSeconds = s.durationSeconds,
                        isDone = true,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                    )
                }
            }

            // ── All sessions summary ──────────────────────────────────────
            if (state.activeSets.isNotEmpty() && selectedExercise != null) {
                val otherExercises = state.activeSets
                    .filter { it.exerciseId != selectedExercise!!.id }
                    .map { it.exerciseId }
                    .distinct()

                if (otherExercises.isNotEmpty()) {
                    item {
                        Text(
                            "OTHER EXERCISES",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                        )
                    }
                    items(otherExercises) { exId ->
                        val ex = state.exercises.find { it.id == exId }
                        val exSets = state.activeSets.filter { it.exerciseId == exId }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 3.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IconBgGray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(categoryEmoji(ex?.category), fontSize = 14.sp)
                                }
                                Text(
                                    ex?.name ?: "Exercise",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DesignGreenLight)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        "${exSets.size} sets",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DesignGreen
                                    )
                                }
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
            onSelect = { selectedExercise = it; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }
}

// ── Set log row (matches design-future .set-log-row) ─────────────────────────

@Composable
private fun SetLogRow(
    setNumber: Int,
    reps: Int?,
    weightKg: Double?,
    durationSeconds: Int?,
    isDone: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Set number circle
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isDone) DesignGreen else TextPrimary),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            } else {
                Text("$setNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // Values
        if (reps != null || weightKg != null) {
            SetValuePill(value = reps?.toString() ?: "--", unit = "reps", modifier = Modifier.weight(1f))
            SetValuePill(value = weightKg?.let { if (it % 1 == 0.0) it.toInt().toString() else "%.1f".format(it) } ?: "--", unit = "kg", modifier = Modifier.weight(1f))
        } else if (durationSeconds != null) {
            SetValuePill(value = "%d:%02d".format(durationSeconds / 60, durationSeconds % 60), unit = "min:sec", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun SetValuePill(value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clip(RoundedCornerShape(8.dp)).background(BgPage).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text(unit, fontSize = 10.sp, color = TextSecondary)
    }
}

// ── Set input field ───────────────────────────────────────────────────────────

@Composable
private fun SetInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = TextSecondary)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BgPage)
        ) {
            BasicInputField(
                value = value,
                onValueChange = onValueChange,
                decimal = decimal
            )
        }
    }
}

@Composable
private fun BasicInputField(value: String, onValueChange: (String) -> Unit, decimal: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = BgPage,
            focusedContainerColor = CardBg,
            unfocusedBorderColor = Color.Transparent,
            focusedBorderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

// ── Exercise picker bottom sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    onSelect: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = exercises.filter { it.name.contains(query, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgPage)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            BasicTextField_Placeholder(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search exercises…"
            )
        }

        Spacer(Modifier.height(4.dp))

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp)) {
            items(filtered, key = { it.id }) { ex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(ex) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(IconBgGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(categoryEmoji(ex.category), fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ex.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            listOfNotNull(
                                ex.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                ex.muscleGroup
                            ).joinToString(" · "),
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
            }
        }
    }
}

@Composable
private fun BasicTextField_Placeholder(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box {
        if (value.isEmpty()) Text(placeholder, fontSize = 14.sp, color = TextSecondary)
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = TextPrimary),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun categoryEmoji(category: ExerciseCategory?) = when (category) {
    ExerciseCategory.STRENGTH    -> "💪"
    ExerciseCategory.CARDIO      -> "🏃"
    ExerciseCategory.FLEXIBILITY -> "🧘"
    ExerciseCategory.OTHER, null -> "🏋️"
}
