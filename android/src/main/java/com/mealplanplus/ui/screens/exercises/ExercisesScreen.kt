package com.mealplanplus.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.ExerciseDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal

@Composable
fun ExercisesScreen(viewModel: ExercisesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    // Full-screen overlays take over the whole screen when open.
    state.editor?.let { ExerciseEditorScreen(state, viewModel); return }
    state.builder?.let { b ->
        if (b.pickerOpen) ExercisePickerScreen(state, viewModel) else WorkoutBuilderScreen(state, viewModel)
        return
    }
    state.openLog?.let { LogDetailScreen(state, viewModel); return }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 10.dp),
            ) {
                Text("Exercises", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                val count = when (state.tab) {
                    LibTab.EXERCISES -> "${state.exercises.size} saved"
                    LibTab.WORKOUTS -> "${state.workouts.size} saved"
                    LibTab.LOGS -> if (state.logs.isEmpty()) "" else "${state.logs.size} logged"
                }
                if (count.isNotEmpty()) Text(count, fontSize = 13.sp, color = MutedLight)
            }

            LibTabBar(state.tab, viewModel::setTab)

            when (state.tab) {
                LibTab.EXERCISES -> ExercisesTab(state, viewModel)
                LibTab.WORKOUTS -> WorkoutsTab(state, viewModel)
                LibTab.LOGS -> LogsTab(state, viewModel)
            }
        }

        if (state.tab != LibTab.LOGS) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                    .size(56.dp).clip(CircleShape).background(Teal)
                    .clickable { if (state.tab == LibTab.EXERCISES) viewModel.openNewExercise() else viewModel.openNewWorkout() },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = OnAccent, modifier = Modifier.size(28.dp))
            }
        }
    }
}

// ── Tab bar (Exercises · Workouts · Logs) ──────────────────────────────────────
@Composable
private fun LibTabBar(tab: LibTab, onSelect: (LibTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(9.dp)).border(1.dp, BorderCool, RoundedCornerShape(9.dp)),
    ) {
        LibTab.values().forEach { t ->
            val selected = t == tab
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).height(34.dp)
                    .background(if (selected) Ink else Surface)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(t) },
            ) {
                Text(
                    t.name.lowercase().replaceFirstChar { it.uppercase() },
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) Surface else Muted,
                )
            }
        }
    }
}

// ── Exercises tab ──────────────────────────────────────────────────────────────
@Composable
private fun ExercisesTab(state: ExercisesUiState, vm: ExercisesViewModel) {
    when {
        state.loading -> LoadingOrEmpty("Loading…")
        state.exercises.isEmpty() -> EmptyState("🏋️", "No exercises yet", "Tap + to add an exercise with tags.")
        else -> LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            items(state.exercises, key = { it.id ?: it.name.hashCode().toLong() }) { e ->
                ExerciseCard(e, state.tagName) { vm.openEditExercise(e) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ExerciseCard(e: ExerciseDto, tagName: Map<Long, String>, onClick: () -> Unit) {
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(e.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                val names = (e.tagIds ?: emptyList()).mapNotNull { tagName[it] }
                if (names.isNotEmpty()) {
                    Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        names.take(4).forEach { ExerciseTagChip(it) }
                    }
                }
            }
            Text("›", fontSize = 20.sp, color = MutedLight)
        }
    }
}

// ── Workouts tab ───────────────────────────────────────────────────────────────
@Composable
private fun WorkoutsTab(state: ExercisesUiState, vm: ExercisesViewModel) {
    when {
        state.loading -> LoadingOrEmpty("Loading…")
        state.workouts.isEmpty() -> EmptyState("📋", "No workouts yet", "Tap + to build a workout from your exercises.")
        else -> LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
            items(state.workouts, key = { it.id ?: it.name.hashCode().toLong() }) { w ->
                WorkoutCard(w) { vm.openEditWorkout(w) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun WorkoutCard(w: WorkoutTemplateDto, onClick: () -> Unit) {
    val items = w.exercises ?: emptyList()
    val totalSets = items.sumOf { it.sets?.size ?: 0 }
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(w.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${items.size} exercise${if (items.size == 1) "" else "s"} · $totalSets sets",
                    fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp))
                if (items.isNotEmpty()) {
                    Text(items.joinToString(", ") { it.exerciseName ?: "Exercise" },
                        fontSize = 10.sp, color = MutedFaint, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp))
                }
            }
            Text("›", fontSize = 20.sp, color = MutedLight)
        }
    }
}

// ── Logs tab (month calendar + read-only recent-session history) ────────────────
@Composable
private fun LogsTab(state: ExercisesUiState, vm: ExercisesViewModel) {
    if (state.loading) { LoadingOrEmpty("Loading…"); return }
    // Recent sessions capped to the last 7 days.
    val recent = state.logs.filter { s -> s.date?.let { !it.isBefore(state.today.minusDays(7)) } ?: true }
    LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
        item {
            LogsCalendar(state, onPrev = vm::prevLogsMonth, onNext = vm::nextLogsMonth,
                onDay = { day -> state.logsByDate[day]?.firstOrNull()?.let(vm::openLog) })
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(start = 2.dp, bottom = 10.dp)) {
                Text("Recent sessions", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.width(6.dp))
                Text("· last 7 days", fontSize = 10.5.sp, color = MutedFaint)
            }
        }
        if (recent.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), Alignment.Center) {
                    Text("No sessions in the last 7 days.", fontSize = 12.sp, color = MutedLight)
                }
            }
        } else {
            items(recent, key = { it.id ?: it.name.hashCode().toLong() }) { s ->
                LogCard(s) { vm.openLog(s) }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Logs month calendar (logged days filled green, tap → detail) ────────────────
@Composable
private fun LogsCalendar(state: ExercisesUiState, onPrev: () -> Unit, onNext: () -> Unit, onDay: (java.time.LocalDate) -> Unit) {
    val month = state.logsMonth
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(16.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceMuted).clickable(onClick = onPrev), Alignment.Center) {
                Text("‹", fontSize = 16.sp, color = MutedDark)
            }
            Spacer(Modifier.weight(1f))
            Text("${month.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault())} ${month.year}",
                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceMuted).clickable(onClick = onNext), Alignment.Center) {
                Text("›", fontSize = 16.sp, color = MutedDark)
            }
        }
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint) }
            }
        }
        val firstDow = month.atDay(1).dayOfWeek.value // Mon=1..Sun=7
        val cells = List(firstDow - 1) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                (0 until 7).forEach { i ->
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(36.dp).padding(1.dp), Alignment.Center) {
                        if (date != null) LogsDayCell(date, state, onDay)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(com.mealplanplus.ui.theme.Success))
            Spacer(Modifier.width(6.dp))
            Text("Workout logged · tap a day for details", fontSize = 10.sp, color = MutedLight)
        }
    }
}

@Composable
private fun LogsDayCell(date: java.time.LocalDate, state: ExercisesUiState, onDay: (java.time.LocalDate) -> Unit) {
    val logged = state.logsByDate.containsKey(date)
    val isToday = date == state.today
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(9.dp))
            .background(if (logged) com.mealplanplus.ui.theme.Success else androidx.compose.ui.graphics.Color.Transparent)
            .then(if (isToday && !logged) Modifier.border(1.5.dp, BorderCool, RoundedCornerShape(9.dp)) else Modifier)
            .then(if (logged) Modifier.clickable { onDay(date) } else Modifier),
    ) {
        Text("${date.dayOfMonth}", fontSize = 11.5.sp,
            fontWeight = if (logged || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = if (logged) OnAccent else Ink)
    }
}

@Composable
private fun LogCard(s: com.mealplanplus.data.generated.model.WorkoutSessionDto, onClick: () -> Unit) {
    val sets = s.sets ?: emptyList()
    val exerciseCount = sets.map { it.exerciseId }.distinct().size
    AppCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (s.isCompleted == true) {
                        Spacer(Modifier.width(6.dp))
                        Text("✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = com.mealplanplus.ui.theme.Success)
                    }
                }
                Text(logMeta(s, exerciseCount, sets.size), fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", fontSize = 20.sp, color = MutedLight)
        }
    }
}

private fun logMeta(s: com.mealplanplus.data.generated.model.WorkoutSessionDto, exerciseCount: Int, setCount: Int): String {
    val parts = mutableListOf<String>()
    s.date?.let { parts.add(it.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM"))) }
    parts.add("$exerciseCount exercise${if (exerciseCount == 1) "" else "s"} · $setCount sets")
    s.durationMinutes?.let { parts.add("${it} min") }
    return parts.joinToString(" · ")
}

// ── Log detail (full-screen, read-only) ────────────────────────────────────────
@Composable
private fun LogDetailScreen(state: ExercisesUiState, vm: ExercisesViewModel) {
    val s = state.openLog ?: return
    val exName = state.exerciseName
    // Group sets by exercise, preserving first-seen order.
    val grouped = (s.sets ?: emptyList()).groupBy { it.exerciseId }
    Column(Modifier.fillMaxSize().background(AppBg)) {
        EditorHeader(s.name, onBack = vm::closeLog)
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            item {
                Text(logMeta(s, grouped.size, (s.sets ?: emptyList()).size), fontSize = 11.sp, color = MutedLight,
                    modifier = Modifier.padding(bottom = 6.dp))
                s.notes?.takeIf { it.isNotBlank() }?.let {
                    Text(it, fontSize = 12.sp, color = MutedDark, modifier = Modifier.padding(bottom = 6.dp))
                }
                Spacer(Modifier.height(4.dp))
            }
            items(grouped.entries.toList(), key = { it.key }) { (exId, sets) ->
                AppCard {
                    Text(exName[exId] ?: "Exercise", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
                        Text("Set", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(48.dp))
                        Text("Reps", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(64.dp))
                        Text("Weight", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                    }
                    sets.sortedBy { it.setNumber }.forEachIndexed { i, set ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${i + 1}", fontSize = 11.sp, fontFamily = DmMono, color = MutedDark, modifier = Modifier.width(48.dp))
                            Text(set.reps?.toString() ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink, modifier = Modifier.width(64.dp))
                            Text(set.weightKg?.let { fmtKg(it) } ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun fmtKg(v: Double): String = (if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()) + " kg"

// ── Exercise editor (full-screen) ──────────────────────────────────────────────
@Composable
private fun ExerciseEditorScreen(state: ExercisesUiState, vm: ExercisesViewModel) {
    val ed = state.editor ?: return
    Column(Modifier.fillMaxSize().background(AppBg)) {
        EditorHeader(if (ed.id == null) "New exercise" else "Edit exercise", onBack = vm::closeEditor)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Label("Name")
            NameField(ed.name, vm::setEditorName, "e.g. Bench Press")

            Spacer(Modifier.height(14.dp))
            Label("Description")
            NameField(ed.description, vm::setEditorDescription, "Optional notes / how-to", singleLine = false)

            Spacer(Modifier.height(18.dp))
            Label("Tags")
            if (state.tags.isEmpty()) {
                Text("No tags available.", fontSize = 12.sp, color = MutedFaint, modifier = Modifier.padding(top = 4.dp))
            } else {
                FlowTags(Modifier.padding(top = 6.dp)) {
                    state.tags.forEach { tag ->
                        TagToggle(tag.name, tag.id in ed.tagIds) { vm.toggleEditorTag(tag.id) }
                    }
                }
            }

            state.error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp)) }

            Spacer(Modifier.weight(1f))
            PrimaryButton("Save exercise", enabled = ed.name.isNotBlank(), onClick = vm::saveExercise)
            if (ed.id != null) {
                Spacer(Modifier.height(10.dp))
                Text("✕ Delete exercise", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                    modifier = Modifier.fillMaxWidth().clickable { vm.deleteExercise(ed.id) }.padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Selectable tag chip: on = filled with tag colour, off = outlined muted. */
@Composable
private fun TagToggle(name: String, on: Boolean, onClick: () -> Unit) {
    val c = exerciseTagColor(name)
    Text(
        name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        color = if (on) Surface else c,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .then(if (on) Modifier.background(c) else Modifier.border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(20.dp)))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

// ── Workout builder (full-screen) ──────────────────────────────────────────────
@Composable
private fun WorkoutBuilderScreen(state: ExercisesUiState, vm: ExercisesViewModel) {
    val b = state.builder ?: return
    Column(Modifier.fillMaxSize().background(AppBg)) {
        EditorHeader(if (b.id == null) "New workout" else "Edit workout", onBack = vm::closeBuilder)
        // Scrollable middle so all exercises + their sets are reachable.
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Label("Name")
            NameField(b.name, vm::setBuilderName, "e.g. Push Day")

            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Exercises", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                Spacer(Modifier.weight(1f))
                Text("＋ Add exercise", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                    modifier = Modifier.clickable(onClick = vm::openPicker))
            }

            if (b.items.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp))
                    .background(SurfaceMuted).clickable(onClick = vm::openPicker).padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center) {
                    Text("Add exercises from your library", fontSize = 12.5.sp, color = MutedDark)
                }
            } else {
                Spacer(Modifier.height(8.dp))
                b.items.forEach { item ->
                    BuilderRow(
                        item,
                        onDuplicateSet = { vm.duplicateSet(item.exerciseId, it) },
                        onRemoveSet = { vm.removeSet(item.exerciseId, it) },
                        onReps = { i, r -> vm.setReps(item.exerciseId, i, r) },
                        onWeight = { i, w -> vm.setWeight(item.exerciseId, i, w) },
                        onRemove = { vm.removeFromBuilder(item.exerciseId) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            state.error?.let { Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            Spacer(Modifier.height(12.dp))
        }
        // Pinned footer.
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            PrimaryButton("Save workout", enabled = b.canSave, onClick = vm::saveWorkout)
            if (b.id != null) {
                Spacer(Modifier.height(8.dp))
                Text("✕ Delete workout", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                    modifier = Modifier.fillMaxWidth().clickable { vm.deleteWorkout(b.id) }.padding(vertical = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BuilderRow(
    item: BuilderItem,
    onDuplicateSet: (Int) -> Unit,
    onRemoveSet: (Int) -> Unit,
    onReps: (Int, Int?) -> Unit,
    onWeight: (Int, Double?) -> Unit,
    onRemove: () -> Unit,
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(item.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${item.sets.size} set${if (item.sets.size == 1) "" else "s"}", fontSize = 10.sp, color = MutedFaint)
            Text("✕", fontSize = 13.sp, color = MutedLight, modifier = Modifier.clickable(onClick = onRemove).padding(start = 10.dp))
        }
        // Column headers
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
            Spacer(Modifier.width(40.dp))
            Text("Reps", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(96.dp))
            Spacer(Modifier.width(10.dp))
            Text("Weight", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
        }
        item.sets.forEachIndexed { i, s ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text("Set ${i + 1}", fontSize = 10.5.sp, fontFamily = DmMono, color = MutedDark, modifier = Modifier.width(40.dp))
                RepsStepper(s.reps ?: 0) { onReps(i, it) }
                Spacer(Modifier.width(10.dp))
                WeightField(s.weightKg) { onWeight(i, it) }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy set", tint = MutedLight,
                    modifier = Modifier.size(15.dp).clickable { onDuplicateSet(i) })
                if (item.sets.size > 1) {
                    Text("✕", fontSize = 12.sp, color = MutedLight,
                        modifier = Modifier.clickable { onRemoveSet(i) }.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
private fun RepsStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−") { onChange((value - 1).coerceAtLeast(1)) }
        Text("$value", fontFamily = DmMono, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink,
            modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        StepBtn("+") { onChange(value + 1) }
    }
}

/** Optional target weight (kg). Blank = no target. Stores canonical kg. */
@Composable
private fun WeightField(weightKg: Double?, onChange: (Double?) -> Unit) {
    val text = weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(74.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, BorderCool, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)) {
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text("–", fontSize = 12.sp, color = MutedLight)
            BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val cleaned = raw.filter { it.isDigit() || it == '.' }
                    onChange(if (cleaned.isBlank()) null else cleaned.toDoubleOrNull() ?: weightKg)
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 12.sp, color = Ink, fontFamily = DmMono),
                cursorBrush = SolidColor(Teal),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Text("kg", fontSize = 9.5.sp, color = MutedFaint)
    }
}

@Composable
private fun StepBtn(sign: String, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).border(1.dp, BorderCool, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)) {
        Text(sign, fontSize = 16.sp, color = Ink)
    }
}

// ── Exercise picker (full-screen, inside the builder) ──────────────────────────
@Composable
private fun ExercisePickerScreen(state: ExercisesUiState, vm: ExercisesViewModel) {
    val b = state.builder ?: return
    val candidates = vm.pickerCandidates()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        EditorHeader("Add exercise", onBack = vm::closePicker)
        SearchBar(b.pickerSearch, vm::setPickerSearch, "Search exercises or tags…")
        if (candidates.isEmpty()) {
            EmptyState("🔍", "No exercises", "Create exercises in the Exercises tab first.")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                items(candidates, key = { it.id ?: it.name.hashCode().toLong() }) { e ->
                    AppCard(modifier = Modifier.clickable { vm.addToBuilder(e) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                                val names = (e.tagIds ?: emptyList()).mapNotNull { state.tagName[it] }
                                if (names.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    names.take(4).forEach { ExerciseTagChip(it) }
                                }
                            }
                            Text("+", fontSize = 20.sp, color = Teal, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

// ── Shared building blocks ─────────────────────────────────────────────────────
@Composable
private fun EditorHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack), Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
}

@Composable
private fun Label(text: String) =
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))

@Composable
private fun NameField(value: String, onChange: (String) -> Unit, hint: String, singleLine: Boolean = true) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceMuted)
            .padding(horizontal = 14.dp, vertical = 13.dp)) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 14.sp, color = MutedLight)
            BasicTextField(value, onChange, singleLine = singleLine,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink), cursorBrush = SolidColor(Teal),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Icon(Icons.Default.Search, null, tint = MutedLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, fontSize = 14.sp, color = MutedLight)
            BasicTextField(query, onChange, singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = Ink), cursorBrush = SolidColor(Teal),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Teal else SurfaceMuted).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) OnAccent else MutedLight)
    }
}

@Composable
private fun EmptyState(glyph: String, title: String, sub: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(glyph, fontSize = 40.sp)
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MutedDark, modifier = Modifier.padding(top = 10.dp))
        Text(sub, fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(top = 4.dp, start = 40.dp, end = 40.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun LoadingOrEmpty(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = MutedLight)
    }
}

/** Minimal wrapping row layout for tag chips (avoids extra deps). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowTags(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}
