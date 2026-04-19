package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.mealplanplus.data.model.WorkoutSet
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("New Workout", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    placeholder = { Text("e.g. Morning push day", color = TextSecondary, fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = BgPage,
                        focusedContainerColor = CardBg,
                        unfocusedBorderColor = Color(0xFFEBEBEB),
                        focusedBorderColor = Color(0xFF111111)
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DesignGreen))
                    Text(today, fontSize = 12.sp, color = TextSecondary)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))
        ) {
            Text("Start session", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder
        ) {
            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        }
    }
}

// ── Step 2: active session — .aw-header + .aw-timer + set rows ────────────────

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
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }
    val timerLabel = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    val setsForSelected = selectedExercise?.let { ex ->
        state.activeSets.filter { it.exerciseId == ex.id }
    } ?: emptyList()
    val currentSetNumber = setsForSelected.size + 1
    val totalSessionSets = state.activeSets.size

    // First other exercise with sets logged
    val nextExercise = selectedExercise?.let { cur ->
        state.activeSets
            .filter { it.exerciseId != cur.id }
            .map { it.exerciseId }
            .firstOrNull()
            ?.let { id -> state.exercises.find { it.id == id } }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgPage)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Black header (.aw-header) ─────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111111))
                        .padding(start = 20.dp, end = 20.dp, top = 52.dp, bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF888888),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "$totalSessionSets sets done",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF888888)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DesignGreen)
                                .clickable(onClick = onFinish)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text("Finish", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text(
                        selectedExercise?.name ?: "Select exercise",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedExercise != null) Color.White else Color(0xFF666666),
                        modifier = Modifier.clickable { showPicker = true }
                    )

                    val lastSet = setsForSelected.lastOrNull()
                    Text(
                        buildString {
                            append("Set $currentSetNumber")
                            if (lastSet != null) {
                                lastSet.reps?.let { append(" · $it reps") }
                                lastSet.weightKg?.let { append(" · ${it.toInt()}kg") }
                            }
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier.padding(top = 3.dp)
                    )

                    if (setsForSelected.isNotEmpty()) {
                        val progress = setsForSelected.size.toFloat() / (setsForSelected.size + 1).toFloat()
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }

            // ── Timer (.aw-timer) — BELOW the header ─────────────────────
            item {
                Text(
                    timerLabel,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.W200,
                    color = TextPrimary,
                    letterSpacing = (-1).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 10.dp)
                )
            }

            if (selectedExercise == null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .clickable { showPicker = true }
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Tap to choose an exercise", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            } else {
                // ── "Sets" section label ──────────────────────────────────
                item {
                    Text(
                        "Sets",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 20.dp, top = 4.dp, bottom = 6.dp)
                    )
                }

                // ── Set log card ──────────────────────────────────────────
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        setsForSelected.forEachIndexed { idx, set ->
                            DoneSetRow(setNumber = idx + 1, set = set)
                            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                        }
                        ActiveSetRow(
                            setNumber = currentSetNumber,
                            weightInput = weightInput,
                            repsInput = repsInput,
                            onWeightChange = { weightInput = it },
                            onRepsChange = { repsInput = it }
                        )
                    }
                }

                // ── .aw-next strip ────────────────────────────────────────
                nextExercise?.let { next ->
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                                .background(Color.White)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "NEXT UP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAAAAAA),
                                letterSpacing = 0.4.sp
                            )
                            Text(
                                next.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Bottom buttons (Skip + Log Set ✓) — always visible ───────────
        if (selectedExercise != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(BgPage)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { weightInput = ""; repsInput = "" },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("Skip", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
                Button(
                    onClick = {
                        selectedExercise?.let { ex ->
                            onAddSet(ex.id, repsInput.toIntOrNull(), weightInput.toDoubleOrNull(), null)
                            weightInput = ""
                            repsInput = ""
                        }
                    },
                    modifier = Modifier.weight(2f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111111))
                ) {
                    Text("Log Set ✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
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

// ── Done set row — green circle, gray fields, ✓ ──────────────────────────────

@Composable
private fun DoneSetRow(setNumber: Int, set: WorkoutSet) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF2E7D52)),
            contentAlignment = Alignment.Center
        ) {
            Text("$setNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        SetDisplayField(
            text = set.weightKg?.let { "${if (it % 1.0 == 0.0) it.toInt() else it} kg" }
                ?: set.durationSeconds?.let { "%d:%02d".format(it / 60, it % 60) }
                ?: "—",
            modifier = Modifier.weight(1f)
        )
        Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCCCCCC))
        SetDisplayField(text = set.reps?.toString() ?: "—", modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D52))
        }
    }
}

@Composable
private fun SetDisplayField(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF7F7F7))
            .padding(horizontal = 9.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, textAlign = TextAlign.Center)
    }
}

// ── Active set row — black circle, white input fields with border ─────────────

@Composable
private fun ActiveSetRow(
    setNumber: Int,
    weightInput: String,
    repsInput: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFAFAFA))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            Text("$setNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        ActiveSetField(
            value = weightInput,
            onValueChange = onWeightChange,
            placeholder = "kg",
            decimal = true,
            modifier = Modifier.weight(1f)
        )
        Text("×", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFCCCCCC))
        ActiveSetField(
            value = repsInput,
            onValueChange = onRepsChange,
            placeholder = "reps",
            decimal = false,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun ActiveSetField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    decimal: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.5.dp, Color(0xFF111111), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        if (value.isEmpty()) {
            Text(
                placeholder,
                fontSize = 14.sp,
                color = Color(0xFFCCCCCC),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1A1A1A)
            ),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
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
            BasicTextField_Placeholder(value = query, onValueChange = { query = it }, placeholder = "Search exercises…")
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
