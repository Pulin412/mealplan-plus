package com.mealplanplus.ui.screens.runner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.components.Stepper
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal

@Composable
fun SessionRunnerScreen(onBack: () -> Unit, viewModel: SessionRunnerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Header(state.workoutName, phaseLabel(state.phase), onBack)
        when (state.phase) {
            RunPhase.LOADING -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading…", fontSize = 13.sp, color = MutedLight) }
            RunPhase.READY -> ReadyPhase(state, viewModel)
            RunPhase.ACTIVE -> ActivePhase(state, viewModel)
            RunPhase.DONE -> DonePhase(state, viewModel, onBack)
        }
    }
}

private fun phaseLabel(p: RunPhase) = when (p) {
    RunPhase.READY -> "Ready"
    RunPhase.ACTIVE -> "In progress"
    RunPhase.DONE -> "Completed"
    RunPhase.LOADING -> ""
}

// ── Ready ────────────────────────────────────────────────────────────────────────
@Composable
private fun ReadyPhase(state: RunnerUiState, vm: SessionRunnerViewModel) {
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            items(state.exercises, key = { it.exerciseId }) { ex ->
                AppCard {
                    Text(ex.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    ExerciseDesc(ex.description)
                    ColumnHeaders(showActions = false)
                    ex.templateSets.forEachIndexed { i, s ->
                        ReadOnlyRow(i + 1, s.reps, s.weightKg)
                    }
                    if (ex.lastTime.isNotEmpty()) {
                        Text("Last time: " + ex.lastTime.joinToString("  ") { setSummary(it.reps, it.weightKg) },
                            fontSize = 10.sp, color = MutedFaint, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Footer {
            state.error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            PrimaryButton(if (state.busy) "Starting…" else "▶  Start workout", enabled = !state.busy && state.exercises.isNotEmpty(), onClick = vm::start)
        }
    }
}

// ── Active ───────────────────────────────────────────────────────────────────────
@Composable
private fun ActivePhase(state: RunnerUiState, vm: SessionRunnerViewModel) {
    // Which exercise cards are expanded (by exerciseId). Collapsed shows the name + description; tap
    // to reveal its sets. Ephemeral UI state for the current logging view.
    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
    var pickerOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
            items(state.exercises, key = { it.exerciseId }) { ex ->
                ExerciseCard(
                    ex = ex,
                    expanded = expanded[ex.exerciseId] == true,
                    onToggle = { expanded[ex.exerciseId] = !(expanded[ex.exerciseId] == true) },
                    vm = vm,
                )
                Spacer(Modifier.height(8.dp))
            }
            item {
                // Add an exercise on the fly — logged to THIS session only, never the template.
                Row(
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .border(1.dp, BorderCool, RoundedCornerShape(10.dp))
                        .clickable { pickerOpen = true }.padding(vertical = 12.dp),
                ) {
                    Text("＋  Add exercise", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                }
            }
        }
        Footer {
            state.error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)) }
            PrimaryButton(if (state.busy) "Finishing…" else "✓  Finish workout", enabled = !state.busy, onClick = vm::finish)
        }
    }
    if (pickerOpen) {
        val present = state.exercises.map { it.exerciseId }.toSet()
        ExercisePickerSheet(
            options = state.library.filter { it.id !in present },
            onPick = { vm.addExercise(it); pickerOpen = false },
            onDismiss = { pickerOpen = false },
        )
    }
}

/**
 * One exercise while logging: a tappable header (name + description) that expands to its sets — the
 * familiar rows with Copy last, per-set reps/weight steppers, ✕ remove, and Add set.
 */
@Composable
private fun ExerciseCard(ex: RunExercise, expanded: Boolean, onToggle: () -> Unit, vm: SessionRunnerViewModel) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
            Column(Modifier.weight(1f)) {
                Text(ex.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                ExerciseDesc(ex.description)
                if (!expanded)
                    Text("${ex.sets.size} set${if (ex.sets.size == 1) "" else "s"}", fontSize = 10.5.sp, color = MutedFaint,
                        modifier = Modifier.padding(top = 2.dp))
            }
            Text(if (expanded) "▾" else "▸", fontSize = 13.sp, color = MutedLight, modifier = Modifier.padding(start = 8.dp))
        }
        if (expanded) {
            if (ex.lastTime.isNotEmpty())
                Text("Copy last", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                    modifier = Modifier.fillMaxWidth().clickable { vm.copyLast(ex.exerciseId) }.padding(top = 6.dp), textAlign = TextAlign.End)
            ColumnHeaders(showActions = true)
            ex.sets.forEachIndexed { i, s ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text("Set ${i + 1}", fontSize = 10.5.sp, fontFamily = DmMono, color = MutedDark, modifier = Modifier.width(44.dp))
                    // Uniform +/- and directly-editable controls (weight steps by 0.5).
                    Stepper(value = s.reps ?: 0, onChange = { vm.setReps(ex.exerciseId, i, it) },
                        min = 0, max = 100, modifier = Modifier.width(100.dp))
                    Spacer(Modifier.width(8.dp))
                    Stepper(value = s.weightKg ?: 0.0, onChange = { vm.setWeight(ex.exerciseId, i, it.takeIf { w -> w > 0.0 }) },
                        min = 0.0, max = 1000.0, step = 0.5, decimals = 1, modifier = Modifier.width(120.dp))
                    Spacer(Modifier.weight(1f))
                    if (ex.sets.size > 1)
                        Text("✕", fontSize = 12.sp, color = MutedLight, modifier = Modifier.clickable { vm.removeSet(ex.exerciseId, i) }.padding(start = 8.dp))
                }
            }
            Text("＋ Add set", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.padding(top = 6.dp).clickable { vm.addSet(ex.exerciseId) })
        }
    }
}

/** Pick a library exercise to add to the current session on the fly (not saved to the template). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(options: List<LibExercise>, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Add exercise", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Logged to this workout only — not saved to the template.", fontSize = 11.sp, color = MutedFaint,
                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            if (options.isEmpty())
                Text("No more exercises to add.", fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(vertical = 12.dp))
            else
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(options, key = { it.id }) { opt ->
                        Column(Modifier.fillMaxWidth().clickable { onPick(opt.id) }.padding(vertical = 10.dp)) {
                            Text(opt.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            if (!opt.description.isNullOrBlank())
                                Text(opt.description, fontSize = 10.5.sp, color = MutedLight, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Divider(color = SurfaceMuted)
                    }
                }
        }
    }
}

// ── Done ─────────────────────────────────────────────────────────────────────────
@Composable
private fun DonePhase(state: RunnerUiState, vm: SessionRunnerViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Success.copy(alpha = 0.12f)).fillMaxWidth().padding(12.dp)) {
                Text("✓", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Success)
                Text("  Workout complete — logged to your history.", fontSize = 12.sp, color = MutedDark)
            }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 2.dp)) {
            items(state.exercises, key = { it.exerciseId }) { ex ->
                AppCard {
                    Text(ex.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    ExerciseDesc(ex.description)
                    ColumnHeaders(showActions = false)
                    ex.sets.forEachIndexed { i, s -> ReadOnlyRow(i + 1, s.reps, s.weightKg) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Footer {
            PrimaryButton("Done", enabled = true, onClick = onBack)
            Spacer(Modifier.height(8.dp))
            Text("Edit workout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.fillMaxWidth().clickable(onClick = vm::edit).padding(vertical = 6.dp), textAlign = TextAlign.Center)
        }
    }
}

// ── Shared building blocks ─────────────────────────────────────────────────────────
@Composable
private fun Header(title: String, sub: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack), Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
        }
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (sub.isNotEmpty()) Text(sub, fontSize = 10.5.sp, color = MutedFaint)
        }
    }
}

@Composable
private fun ExerciseDesc(desc: String?) {
    if (!desc.isNullOrBlank())
        Text(desc, fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 3.dp))
}

@Composable
private fun ColumnHeaders(showActions: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
        Spacer(Modifier.width(44.dp))
        Text("Reps", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(if (showActions) 100.dp else 64.dp))
        Spacer(Modifier.width(if (showActions) 10.dp else 0.dp))
        Text("Weight", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
    }
}

@Composable
private fun ReadOnlyRow(setNo: Int, reps: Int?, weightKg: Double?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text("Set $setNo", fontSize = 10.5.sp, fontFamily = DmMono, color = MutedDark, modifier = Modifier.width(44.dp))
        Text(reps?.toString() ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink, modifier = Modifier.width(64.dp))
        Text(weightKg?.let { fmtKg(it) } ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink)
    }
}

private fun setSummary(reps: Int?, weightKg: Double?): String {
    val r = reps?.toString() ?: "–"
    return if (weightKg != null) "$r×${fmtKg(weightKg)}" else r
}

private fun fmtKg(v: Double): String = (if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()) + "kg"

@Composable
private fun Footer(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) { content() }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Teal else SurfaceMuted).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) OnAccent else MutedLight)
    }
}
