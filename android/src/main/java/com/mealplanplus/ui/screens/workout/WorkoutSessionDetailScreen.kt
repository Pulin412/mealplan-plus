package com.mealplanplus.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
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
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.WorkoutSet
import com.mealplanplus.data.model.WorkoutSetWithExercise
import com.mealplanplus.data.model.WorkoutTemplateExerciseWithDetails
import com.mealplanplus.ui.theme.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val detail by viewModel.detailSession.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId) { viewModel.loadDetailSession(sessionId) }
    DisposableEffect(Unit) { onDispose { viewModel.clearDetailSession() } }

    val session = detail?.session
    val sets    = detail?.sets ?: emptyList()

    // Load full template exercise details (planned sets, targets) from notes-stored templateId
    val scope = rememberCoroutineScope()
    var templateDetails by remember { mutableStateOf<List<WorkoutTemplateExerciseWithDetails>>(emptyList()) }
    LaunchedEffect(session?.notes) {
        val templateId = session?.notes?.toLongOrNull()
        if (templateId != null) {
            templateDetails = viewModel.getTemplateWithExercises(templateId)?.exercises ?: emptyList()
        }
    }
    // Quick lookup: exerciseId → template entry
    val templateDetailMap = templateDetails.associateBy { it.exercise.id }

    // Merge: template exercise order first, then any extra logged exercises not in template
    val loggedExercises = sets.map { it.exercise }.distinctBy { it.id }
    val exercises: List<Exercise> = if (templateDetails.isEmpty()) {
        loggedExercises
    } else {
        val templateIds = templateDetails.map { it.exercise.id }.toSet()
        templateDetails.map { it.exercise } + loggedExercises.filter { it.id !in templateIds }
    }

    // Draft inputs per exercise (edit mode: exerciseId → field values)
    val draftReps   = remember { mutableStateMapOf<Long, String>() }
    val draftWeight = remember { mutableStateMapOf<Long, String>() }

    val dateLabel = session?.let {
        Instant.ofEpochMilli(it.date)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    } ?: ""

    if (showDatePicker) {
        val initialMs = session?.date ?: System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { ms ->
                        val selected = Instant.ofEpochMilli(ms).atZone(ZoneId.of("UTC")).toLocalDate()
                        viewModel.updateSessionDate(sessionId, selected)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(containerColor = BgPage) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Dark header ───────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF111111))
                        .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 18.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF888888))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(session?.name ?: "Workout", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            if (dateLabel.isNotBlank()) {
                                Text(
                                    dateLabel + if (isEditing) " ✎" else "",
                                    fontSize = 12.sp,
                                    color = if (isEditing) DesignGreen else Color(0xFF888888),
                                    modifier = if (isEditing) Modifier.clickable { showDatePicker = true } else Modifier
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(DesignGreen)
                                .clickable { onEdit(sessionId) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "Edit",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        HeaderStat("${sets.size}", "Sets logged")
                        HeaderStat("${exercises.size}", if (templateDetails.isNotEmpty()) "Planned" else "Exercises")
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (session != null && exercises.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("No sets were logged.", fontSize = 14.sp, color = TextMuted)
                    }
                }
            }

            // ── Exercise sections ─────────────────────────────────────────────
            items(exercises, key = { it.id }) { exercise ->
                val setsForEx = sets
                    .filter { it.exercise.id == exercise.id }
                    .sortedBy { it.workoutSet.setNumber }
                val tmpl = templateDetailMap[exercise.id]
                val isCardio = exercise.category.uppercase() == "CARDIO"

                // Build planned-set list for ghost rows: prefer per-set plannedSets,
                // fall back to targetSets × (targetReps, targetWeightKg)
                val plannedSets: List<Pair<Int?, Double?>> = when {
                    tmpl == null -> emptyList()
                    tmpl.plannedSets.isNotEmpty() ->
                        tmpl.plannedSets.sortedBy { it.setIndex }.map { it.reps to it.weightKg }
                    tmpl.templateExercise.targetSets != null ->
                        List(tmpl.templateExercise.targetSets) {
                            tmpl.templateExercise.targetReps to tmpl.templateExercise.targetWeightKg
                        }
                    else -> emptyList()
                }

                // Pre-fill draft for the next unlogged set
                val nextPlanned = plannedSets.getOrNull(setsForEx.size)
                LaunchedEffect(isEditing, setsForEx.size, tmpl) {
                    if (isEditing && nextPlanned != null) {
                        if (draftReps[exercise.id].isNullOrEmpty())
                            draftReps[exercise.id] = nextPlanned.first?.toString() ?: ""
                        if (draftWeight[exercise.id].isNullOrEmpty())
                            draftWeight[exercise.id] = nextPlanned.second?.let {
                                if (it % 1 == 0.0) it.toInt().toString() else "%.1f".format(it)
                            } ?: ""
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Exercise header row: name + optional target badge
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        exercise.name.uppercase(),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                        letterSpacing = 0.8.sp, modifier = Modifier.weight(1f)
                    )
                    // Target badge
                    val targetLabel = when {
                        tmpl == null -> null
                        isCardio && tmpl.templateExercise.targetDurationSeconds != null ->
                            "%d:%02d".format(
                                tmpl.templateExercise.targetDurationSeconds / 60,
                                tmpl.templateExercise.targetDurationSeconds % 60
                            )
                        plannedSets.isNotEmpty() -> buildString {
                            append("${plannedSets.size} × ")
                            val reps = plannedSets.firstOrNull()?.first
                            val wt   = plannedSets.firstOrNull()?.second
                            if (reps != null) append("${reps}r")
                            if (wt != null) append(" @ ${if (wt % 1 == 0.0) wt.toInt() else wt}kg")
                        }
                        else -> null
                    }
                    if (targetLabel != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DesignGreenLight)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(targetLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = DesignGreen)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {

                        // Show planned sets; logged ones are solid, unlogged ones are ghost
                        val totalRows = maxOf(plannedSets.size, setsForEx.size)
                        if (totalRows == 0 && !isEditing) {
                            Text(
                                "No sets logged",
                                fontSize = 13.sp, color = TextMuted,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                        repeat(totalRows) { idx ->
                            if (idx > 0) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                            val loggedSet = setsForEx.getOrNull(idx)
                            val planned   = plannedSets.getOrNull(idx)
                            if (loggedSet != null) {
                                // Logged set — solid row
                                SetDetailRow(
                                    index = idx,
                                    setWithEx = loggedSet,
                                    isEditing = isEditing,
                                    onDelete = { viewModel.deleteSet(loggedSet.workoutSet) }
                                )
                            } else if (planned != null) {
                                // Planned but not yet logged — ghost row
                                PlannedSetRow(
                                    index = idx,
                                    reps = planned.first,
                                    weightKg = planned.second,
                                    isCardio = isCardio,
                                    targetDurationSec = tmpl?.templateExercise?.targetDurationSeconds
                                )
                            }
                        }

                        // Add set row (edit mode only)
                        if (isEditing) {
                            val nextNum = setsForEx.size + 1
                            if (totalRows > 0) HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "SET $nextNum",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                color = TextSecondary, letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
                            )
                            if (isCardio) {
                                SetInputField(
                                    label = "DURATION (min:sec)",
                                    value = draftReps[exercise.id] ?: "",
                                    onValueChange = { draftReps[exercise.id] = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SetInputField(
                                        label = "REPS",
                                        value = draftReps[exercise.id] ?: "",
                                        onValueChange = { draftReps[exercise.id] = it },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SetInputField(
                                        label = "WEIGHT (kg)",
                                        value = draftWeight[exercise.id] ?: "",
                                        onValueChange = { draftWeight[exercise.id] = it },
                                        modifier = Modifier.weight(1.2f),
                                        decimal = true
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val repsVal   = draftReps[exercise.id]?.toIntOrNull()
                                    val weightVal = draftWeight[exercise.id]?.toDoubleOrNull()
                                    val durVal    = if (isCardio) draftReps[exercise.id]?.replace(":", "")?.toIntOrNull() else null
                                    viewModel.addSetToSession(
                                        sessionId   = sessionId,
                                        exerciseId  = exercise.id,
                                        reps        = if (isCardio) null else repsVal,
                                        weightKg    = weightVal,
                                        durationSec = durVal,
                                        notes       = null
                                    )
                                    draftReps.remove(exercise.id)
                                    draftWeight.remove(exercise.id)
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Set", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = CardBg)
                            }
                        }
                    }
                }
            }

            // ── Add exercise button (edit mode) ───────────────────────────────
            if (isEditing) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp)).background(CardBg)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(TagGrayBg), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                        Text("Add exercise coming soon…", fontSize = 14.sp, color = TextMuted)
                    }
                }
            }
        }
    }
}

// Ghost row for a planned-but-not-yet-logged set
@Composable
private fun PlannedSetRow(
    index: Int,
    reps: Int?,
    weightKg: Double?,
    isCardio: Boolean,
    targetDurationSec: Int?
) {
    val summary = buildString {
        if (isCardio && targetDurationSec != null) {
            append("%d:%02d".format(targetDurationSec / 60, targetDurationSec % 60))
        } else {
            reps?.let { append("$it reps") }
            weightKg?.let { w ->
                if (isNotEmpty()) append(" · ")
                append(if (w % 1 == 0.0) "${w.toInt()} kg" else "${"%.1f".format(w)} kg")
            }
        }
    }.ifBlank { "—" }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dimmed circle — not logged yet
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(DividerColor),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
        Text(
            "Set ${index + 1}",
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            color = TextMuted
        )
        Spacer(Modifier.weight(1f))
        Text(
            summary,
            fontSize = 13.sp, fontWeight = FontWeight.Normal,
            color = TextMuted
        )
        // "planned" pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF0F0F0))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text("planned", fontSize = 9.sp, fontWeight = FontWeight.Medium, color = TextMuted)
        }
    }
}

@Composable
private fun SetDetailRow(
    index: Int,
    setWithEx: WorkoutSetWithExercise,
    isEditing: Boolean,
    onDelete: () -> Unit
) {
    val set = setWithEx.workoutSet
    val summary = buildString {
        set.reps?.let { append("$it reps") }
        set.weightKg?.let { w ->
            if (isNotEmpty()) append(" · ")
            append(if (w % 1 == 0.0) "${w.toInt()} kg" else "${"%.1f".format(w)} kg")
        }
        set.durationSeconds?.let { d ->
            if (isNotEmpty()) append(" · ")
            append("%02d:%02d".format(d / 60, d % 60))
        }
        set.notes?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append(" · ")
            append(it)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(DesignGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DesignGreen)
        }
        Text("Set ${index + 1}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        Text(summary.ifBlank { "—" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (isEditing) {
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete set", tint = TextDestructive, modifier = Modifier.size(14.dp))
            }
        }
    }
}
