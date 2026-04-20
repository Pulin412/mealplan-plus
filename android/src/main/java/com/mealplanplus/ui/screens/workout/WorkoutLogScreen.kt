package com.mealplanplus.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import com.mealplanplus.data.model.WorkoutTemplateExerciseWithDetails
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.ui.theme.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One occurrence of an exercise in the active session. Same exercise can appear multiple times. */
private data class ExerciseSlot(
    val exercise: Exercise,
    val slotKey: String,               // unique per slot; exerciseId_index
    val templateEntry: WorkoutTemplateExerciseWithDetails? = null
)

private data class DraftSet(
    val slotKey: String,
    val reps: String = "",
    val weightKg: String = "",
    val durationSec: String = "",
    val notes: String = "",
    val isDone: Boolean = false
)

private enum class Step { PICK_TEMPLATE, ACTIVE_SESSION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogScreen(
    preselectedTemplateId: Long? = null,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var step by remember { mutableStateOf(if (preselectedTemplateId != null) Step.ACTIVE_SESSION else Step.PICK_TEMPLATE) }
    var chosenTemplate by remember { mutableStateOf<WorkoutTemplateWithExercises?>(null) }
    var sessionName by remember { mutableStateOf("") }

    LaunchedEffect(preselectedTemplateId) {
        if (preselectedTemplateId != null) {
            val t = viewModel.getTemplateWithExercises(preselectedTemplateId)
            chosenTemplate = t
            sessionName = t?.template?.name ?: "Workout"
        }
    }

    when (step) {
        Step.PICK_TEMPLATE -> TemplatePickStep(
            templates = state.templates,
            onPickTemplate = { template ->
                chosenTemplate = template
                sessionName = template.template.name
                viewModel.startSession(template.template.name, LocalDate.now(), template.template.id)
                step = Step.ACTIVE_SESSION
            },
            onStartBlank = {
                viewModel.startSession("Workout", LocalDate.now())
                step = Step.ACTIVE_SESSION
            },
            onBack = onBack
        )
        Step.ACTIVE_SESSION -> ActiveSessionStep(
            state = state,
            template = chosenTemplate,
            onAddSet = viewModel::addSet,
            onFinish = { viewModel.finishSession(); onFinished() },
            onBack = onBack,
            allExercises = state.exercises
        )
    }
}

// ── Step 1: Pick template ─────────────────────────────────────────────────────

@Composable
private fun TemplatePickStep(
    templates: List<WorkoutTemplateWithExercises>,
    onPickTemplate: (WorkoutTemplateWithExercises) -> Unit,
    onStartBlank: () -> Unit,
    onBack: () -> Unit
) {
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(BgPage)
                .padding(start = 4.dp, end = 16.dp, top = 52.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Column {
                Text("New Workout", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(today, fontSize = 12.sp, color = TextSecondary)
            }
        }

        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)) {
            if (templates.isNotEmpty()) {
                item {
                    Text(
                        "START FROM TEMPLATE",
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                        letterSpacing = 0.8.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                }
                items(templates, key = { it.template.id }) { template ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { onPickTemplate(template) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(IconBgGray),
                                contentAlignment = Alignment.Center
                            ) { Text(template.template.category.emoji(), fontSize = 20.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.template.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    "${template.exercises.size} exercises · ${template.template.category.displayName()}",
                                    fontSize = 12.sp, color = TextSecondary
                                )
                            }
                            Text("›", fontSize = 22.sp, color = TextMuted)
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
            item {
                OutlinedButton(
                    onClick = onStartBlank,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start blank session", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }
            }
        }
    }
}

// ── Step 2: Active session ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActiveSessionStep(
    state: WorkoutUiState,
    template: WorkoutTemplateWithExercises?,
    allExercises: List<Exercise>,
    onAddSet: (Long, Int?, Double?, Int?, String?) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val draftSets = remember { mutableStateListOf<DraftSet>() }
    var expandedSlotKey by remember { mutableStateOf<String?>(null) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var slotCounter by remember { mutableStateOf(0) }

    // Build initial slots from template
    val sessionSlots = remember {
        mutableStateListOf<ExerciseSlot>().also { list ->
            template?.exercises?.forEachIndexed { idx, entry ->
                list.add(ExerciseSlot(entry.exercise, "${entry.exercise.id}_$idx", entry))
            }
            expandedSlotKey = list.firstOrNull()?.slotKey
            slotCounter = list.size
        }
    }

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsedSeconds++ } }
    val timerLabel = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60)

    Scaffold(containerColor = BgPage) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp)
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
                        Text(state.activeSession?.name ?: "Workout", fontSize = 13.sp, color = Color(0xFF888888), modifier = Modifier.weight(1f))
                        Text(timerLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White, modifier = Modifier.padding(end = 12.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(DesignGreen)
                                .clickable(onClick = onFinish).padding(horizontal = 16.dp, vertical = 8.dp)
                        ) { Text("Finish", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White) }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        HeaderStat("${draftSets.count { it.isDone }}", "Sets logged")
                        HeaderStat("${sessionSlots.size}", "Exercises")
                    }
                }
            }

            // ── Exercise sections ─────────────────────────────────────────────
            if (sessionSlots.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🏋️", fontSize = 36.sp)
                        Text("No exercises yet", fontSize = 15.sp, color = TextSecondary)
                        Text("Add an exercise below to start logging.", fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            items(sessionSlots, key = { it.slotKey }) { slot ->
                val isExpanded = expandedSlotKey == slot.slotKey
                val setsForSlot = draftSets.filter { it.slotKey == slot.slotKey }
                val doneSets = setsForSlot.count { it.isDone }

                Spacer(Modifier.height(12.dp))

                // Show "(2)" suffix if same exercise appears more than once
                val duplicateCount = sessionSlots.count { it.exercise.id == slot.exercise.id }
                val slotIndex = sessionSlots.filter { it.exercise.id == slot.exercise.id }.indexOfFirst { it.slotKey == slot.slotKey }
                val nameLabel = if (duplicateCount > 1) "${slot.exercise.name} (${slotIndex + 1})" else slot.exercise.name

                Text(
                    nameLabel.uppercase(),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(start = 20.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clickable { expandedSlotKey = if (isExpanded) null else slot.slotKey },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(IconBgGray),
                                contentAlignment = Alignment.Center
                            ) { Text(categoryEmoji(slot.exercise.category), fontSize = 16.sp) }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(slot.exercise.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(
                                    slot.exercise.muscleGroup ?: categoryDisplayName(slot.exercise.category),
                                    fontSize = 11.sp, color = TextSecondary
                                )
                            }
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(7.dp))
                                    .background(if (doneSets > 0) DesignGreenLight else TagGrayBg)
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text("$doneSets sets", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (doneSets > 0) DesignGreen else TextSecondary)
                            }
                        }

                        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 12.dp)) {
                                HorizontalDivider(color = DividerColor)
                                Spacer(Modifier.height(10.dp))

                                // ── Pyramid reference (from template) ─────────
                                val plannedSets = slot.templateEntry?.plannedSets ?: emptyList()
                                if (plannedSets.isNotEmpty()) {
                                    Text("TARGET", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                        color = TextMuted, letterSpacing = 0.6.sp)
                                    Spacer(Modifier.height(4.dp))

                                    data class SetGroup(val reps: Int?, val weightKg: Double?, val count: Int)
                                    val groups = mutableListOf<SetGroup>()
                                    for (s in plannedSets) {
                                        val last = groups.lastOrNull()
                                        if (last != null && last.reps == s.reps && last.weightKg == s.weightKg) {
                                            groups[groups.lastIndex] = last.copy(count = last.count + 1)
                                        } else {
                                            groups.add(SetGroup(s.reps, s.weightKg, 1))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        groups.forEach { g ->
                                            val label = buildString {
                                                g.weightKg?.let { w ->
                                                    append(if (w % 1 == 0.0) "${w.toInt()}kg" else "${"%.1f".format(w)}kg")
                                                    append(" · ")
                                                }
                                                g.reps?.let { append("${it} reps") }
                                                if (g.count > 1) append(" ×${g.count}")
                                            }
                                            if (label.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                                        .background(TagBlueBg)
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = TagBlue)
                                                }
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(10.dp))
                                } else {
                                    // Flat target fallback (no pyramid data)
                                    slot.templateEntry?.let { entry ->
                                        val flat = buildString {
                                            entry.templateExercise.targetSets?.let { append("${it} × ") }
                                            entry.templateExercise.targetReps?.let { append("$it reps") }
                                            entry.templateExercise.targetWeightKg?.let { append(" @ ${it}kg") }
                                        }
                                        if (flat.isNotBlank()) {
                                            Text("TARGET  $flat", fontSize = 10.sp, color = TextMuted, letterSpacing = 0.4.sp)
                                            Spacer(Modifier.height(10.dp))
                                        }
                                    }
                                }

                                // ── Logged sets ───────────────────────────────
                                setsForSlot.forEachIndexed { idx, draftSet ->
                                    if (draftSet.isDone) {
                                        DoneSetRow(setNumber = idx + 1, draft = draftSet)
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }

                                // ── New set input ─────────────────────────────
                                val pendingDraft = setsForSlot.firstOrNull { !it.isDone }
                                    ?: DraftSet(slotKey = slot.slotKey).also { draftSets.add(it) }
                                val pendingIdx = draftSets.indexOfFirst { it === pendingDraft }.takeIf { it >= 0 } ?: draftSets.lastIndex
                                val isCardio = slot.exercise.category.uppercase() == "CARDIO"

                                Spacer(Modifier.height(4.dp))
                                Text("SET ${doneSets + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    color = TextSecondary, letterSpacing = 0.6.sp)
                                Spacer(Modifier.height(6.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isCardio) {
                                        SetInputField(
                                            label = "DURATION (min:sec)",
                                            value = pendingDraft.durationSec,
                                            onValueChange = { if (pendingIdx >= 0) draftSets[pendingIdx] = draftSets[pendingIdx].copy(durationSec = it) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        SetInputField(
                                            label = "REPS",
                                            value = pendingDraft.reps,
                                            onValueChange = { if (pendingIdx >= 0) draftSets[pendingIdx] = draftSets[pendingIdx].copy(reps = it) },
                                            modifier = Modifier.weight(1f)
                                        )
                                        SetInputField(
                                            label = "WEIGHT (kg)",
                                            value = pendingDraft.weightKg,
                                            onValueChange = { if (pendingIdx >= 0) draftSets[pendingIdx] = draftSets[pendingIdx].copy(weightKg = it) },
                                            modifier = Modifier.weight(1.2f),
                                            decimal = true
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                SetInputField(
                                    label = "REMARKS (OPTIONAL)",
                                    value = pendingDraft.notes,
                                    onValueChange = { if (pendingIdx >= 0) draftSets[pendingIdx] = draftSets[pendingIdx].copy(notes = it) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val d = if (pendingIdx >= 0) draftSets[pendingIdx] else pendingDraft
                                        val reps = d.reps.toIntOrNull()
                                        val weight = d.weightKg.toDoubleOrNull()
                                        val dur = d.durationSec.replace(":", "").toIntOrNull()
                                        onAddSet(slot.exercise.id, reps, weight, dur, d.notes.ifBlank { null })
                                        if (pendingIdx >= 0) draftSets[pendingIdx] = draftSets[pendingIdx].copy(isDone = true)
                                        draftSets.add(DraftSet(slotKey = slot.slotKey))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TextPrimary)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Log Set", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CardBg)
                                }
                            }
                        }
                    }
                }
            }

            // ── Add exercise ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp)).background(CardBg)
                        .clickable { showExercisePicker = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(TagGrayBg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    Text("Add exercise…", fontSize = 14.sp, color = TextSecondary)
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerSheet(
            exercises = allExercises,
            excludeIds = emptySet(), // allow same exercise multiple times
            onSelect = { ex ->
                val key = "${ex.id}_$slotCounter"
                slotCounter++
                sessionSlots.add(ExerciseSlot(ex, key))
                expandedSlotKey = key
                showExercisePicker = false
            },
            onDismiss = { showExercisePicker = false }
        )
    }
}

// ── Done set row ──────────────────────────────────────────────────────────────

@Composable
private fun DoneSetRow(setNumber: Int, draft: DraftSet) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(BgPage)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(DesignGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Text("Set $setNumber", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        if (draft.reps.isNotBlank() || draft.weightKg.isNotBlank()) {
            Text("${draft.reps.ifBlank { "--" }} reps · ${draft.weightKg.ifBlank { "--" }} kg",
                fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        } else if (draft.durationSec.isNotBlank()) {
            Text(draft.durationSec, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Set input field ───────────────────────────────────────────────────────────

@Composable
private fun SetInputField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = false) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, color = TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = BgPage, focusedContainerColor = CardBg,
                unfocusedBorderColor = Color(0xFFEBEBEB), focusedBorderColor = TextPrimary
            )
        )
    }
}

@Composable
private fun HeaderStat(value: String, label: String) {
    Column {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = (-0.3).sp)
        Text(label, fontSize = 11.sp, color = Color(0xFF888888))
    }
}
