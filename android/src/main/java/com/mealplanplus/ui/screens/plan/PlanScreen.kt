package com.mealplanplus.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle as ComposeTextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.AppText
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Carbs
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PlanScreen() {
    val viewModel: PlanViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp)) {
            Text("Plan", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            MonthCalendar(state, onPrev = viewModel::prevMonth, onNext = viewModel::nextMonth, onDay = viewModel::selectDay)
            Spacer(Modifier.height(18.dp))
            NextSeven(state, onDay = viewModel::selectDay)
            Spacer(Modifier.height(24.dp))
        }
    }

    state.selectedDate?.let { date ->
        val openWorkout = state.openWorkout
        when {
            openWorkout != null -> WorkoutDetail(openWorkout, onBack = viewModel::closeWorkoutDetail)
            state.workoutPickerOpen -> WorkoutPicker(date, state, viewModel)
            state.pickerOpen -> DietPicker(date, state, viewModel)
            else -> DayPlanSheet(date, state,
                onPick = viewModel::openPicker,
                onAddWorkout = viewModel::openWorkoutPicker,
                onOpenWorkout = viewModel::openWorkoutDetail,
                onRemoveWorkout = { id -> viewModel.removePlannedWorkout(date, id) },
                onClear = { viewModel.clearDay(date) },
                onClose = { viewModel.selectDay(null) })
        }
    }
}

// ── Month calendar ──────────────────────────────────────────────────────────────
@Composable
private fun MonthCalendar(state: PlanUiState, onPrev: () -> Unit, onNext: () -> Unit, onDay: (LocalDate) -> Unit) {
    val month = state.month
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onPrev), Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = MutedDark, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(30.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onNext), Alignment.Center) {
                Icon(Icons.Default.KeyboardArrowRight, "Next", tint = MutedDark, modifier = Modifier.size(18.dp))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Box(Modifier.weight(1f), Alignment.Center) { Text(it, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint) }
            }
        }
        Spacer(Modifier.height(4.dp))
        val firstDow = month.atDay(1).dayOfWeek.value // Mon=1..Sun=7
        val cells = List(firstDow - 1) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f), Alignment.Center) {
                        if (date != null) DayCell(date, state, onDay)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, state: PlanUiState, onDay: (LocalDate) -> Unit) {
    val plan = state.plansByDate[date]
    val isToday = date == state.today
    Column(horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.size(34.dp).clip(CircleShape)
            .background(if (isToday) Teal else androidx.compose.ui.graphics.Color.Transparent)
            .clickable { onDay(date) }, verticalArrangement = Arrangement.Center) {
        Text("${date.dayOfMonth}", fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = if (isToday) OnAccent else Ink)
        // Meal dot: green = marked complete · blue = planned (today/upcoming) · red = planned but the
        // day passed without completing. Workout dot: always blue.
        val completed = date in state.completedDays
        val past = date.isBefore(state.today)
        val planned = plan?.dietId != null
        val mealDot = when {
            completed -> Success
            planned && past -> Danger
            planned -> Carbs
            else -> null
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 1.dp)) {
            mealDot?.let { Dot(if (isToday) OnAccent else it) }
            if (!plan?.plannedWorkouts.isNullOrEmpty()) Dot(if (isToday) OnAccent else Carbs)
        }
    }
}

@Composable private fun Dot(color: androidx.compose.ui.graphics.Color) = Box(Modifier.size(4.dp).clip(CircleShape).background(color))

// ── Next 7 days ──────────────────────────────────────────────────────────────
@Composable
private fun NextSeven(state: PlanUiState, onDay: (LocalDate) -> Unit) {
    Text("Next 7 days", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) {
        (0..6).forEach { offset ->
            val date = state.today.plusDays(offset.toLong())
            val plan = state.plansByDate[date]
            val diet = plan?.dietId?.let { id -> state.diets.firstOrNull { it.id == id } }
            val workouts = plan?.plannedWorkouts?.mapNotNull { it.activityName }?.takeIf { it.isNotEmpty() }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onDay(date) }.padding(horizontal = 12.dp, vertical = 11.dp)) {
                Column(Modifier.width(52.dp)) {
                    Text(if (offset == 0) "Today" else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()), fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM")), fontSize = 9.5.sp, color = MutedFaint)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    if (diet != null) Text(diet.name, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                    else Text("No diet", fontSize = 11.sp, color = MutedLight)
                    Text(workouts?.joinToString(", ") ?: "No workout", fontSize = 10.sp, color = if (workouts != null) Success else MutedFaint)
                }
                if (diet != null) Text("${diet.kcal}", fontFamily = DmMono, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        }
    }
}

// ── Day plan sheet ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DayPlanSheet(date: LocalDate, state: PlanUiState, onPick: () -> Unit, onAddWorkout: () -> Unit, onOpenWorkout: (Long?) -> Unit, onRemoveWorkout: (Long) -> Unit, onClear: () -> Unit, onClose: () -> Unit) {
    val plan: DayPlanDto? = state.plansByDate[date]
    val workouts = plan?.plannedWorkouts.orEmpty()
    val selectedDiet = plan?.dietId?.let { id -> state.diets.firstOrNull { it.id == id } }
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMM")), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                if (plan != null) Text("Clear day", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Danger, modifier = Modifier.clickable(onClick = onClear))
            }

            // Past-day recap: whether the day was marked complete and which meal slots were logged.
            if (date.isBefore(state.today)) {
                val completed = date in state.completedDays
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("This day", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                    Spacer(Modifier.weight(1f))
                    Text(if (completed) "Completed" else "Not completed", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                        color = if (completed) Success else MutedFaint)
                }
                if (state.selectedDaySlots.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        state.selectedDaySlots.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(if (s.isLogged) "✓" else "✗", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (s.isLogged) Success else Danger, modifier = Modifier.width(20.dp))
                                Text(s.slot, fontSize = 12.sp, color = if (s.isLogged) Ink else MutedLight)
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
                Text("Diet plan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                Spacer(Modifier.weight(1f))
                if (selectedDiet != null) Text("Change diet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal, modifier = Modifier.clickable(onClick = onPick))
            }
            if (selectedDiet != null) {
                Text(selectedDiet.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 2.dp))
                DietSlots(selectedDiet)
            } else {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onPick).padding(vertical = 13.dp)) {
                    Text("＋ Pick a diet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
                }
            }

            SectionLabel("Exercises")
            if (workouts.isEmpty()) {
                Text("No workouts planned.", fontSize = 11.5.sp, color = MutedLight, modifier = Modifier.padding(bottom = 8.dp))
            } else {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    workouts.forEach { w ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(SurfaceMuted).padding(start = 11.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
                            Text(w.activityName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                                modifier = Modifier.clickable { onOpenWorkout(w.workoutTemplateId) })
                            val wid = w.id
                            Text("  ×", fontSize = 13.sp, color = MutedFaint,
                                modifier = if (wid != null) Modifier.clickable { onRemoveWorkout(wid) } else Modifier)
                        }
                    }
                }
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.5.dp, CardBorder, RoundedCornerShape(11.dp)).clickable(onClick = onAddWorkout).padding(vertical = 11.dp)) {
                Text("＋ Add from library", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal)
            }
        }
    }
}

// ── Workout picker (choose a template from the library to plan for the day) ──────
@Composable
private fun WorkoutPicker(date: LocalDate, state: PlanUiState, viewModel: PlanViewModel) {
    val plannedIds = state.plansByDate[date]?.plannedWorkouts.orEmpty().mapNotNull { it.workoutTemplateId }.toSet()
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = viewModel::closeWorkoutPicker), Alignment.Center) {
                Text("‹", fontSize = 24.sp, color = Ink)
            }
            Text("Add workout", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text("${state.workouts.size} saved", fontSize = 12.sp, color = MutedLight)
        }
        if (state.workouts.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("📋", fontSize = 40.sp)
                Text("No workouts yet", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(top = 8.dp))
                Text("Build one in Exercises → Workouts first.", fontSize = 11.5.sp, color = MutedLight, modifier = Modifier.padding(top = 4.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                items(state.workouts, key = { it.id ?: it.name.hashCode().toLong() }) { w ->
                    WorkoutPickerCard(w, added = w.id != null && w.id in plannedIds) { viewModel.addPlannedWorkout(date, w) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkoutPickerCard(w: WorkoutTemplateDto, added: Boolean, onAdd: () -> Unit) {
    val items = w.exercises.orEmpty()
    val totalSets = items.sumOf { it.sets?.size ?: 0 }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
        .border(1.dp, if (added) Success else CardBorder, RoundedCornerShape(14.dp))
        .clickable(enabled = !added, onClick = onAdd).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(w.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("${items.size} exercise${if (items.size == 1) "" else "s"} · $totalSets sets",
                    fontSize = 10.5.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp))
                if (items.isNotEmpty()) {
                    Text(items.joinToString(", ") { it.exerciseName ?: "Exercise" },
                        fontSize = 10.sp, color = MutedFaint, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(if (added) "✓ Added" else "+ Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (added) Success else Teal)
        }
    }
}

// ── Planned workout detail (read-only: exercises + per-set targets) ──────────────
@Composable
private fun WorkoutDetail(w: WorkoutTemplateDto, onBack: () -> Unit) {
    val items = w.exercises.orEmpty().sortedBy { it.orderIndex }
    val totalSets = items.sumOf { it.sets?.size ?: 0 }
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack), Alignment.Center) {
                Text("‹", fontSize = 24.sp, color = Ink)
            }
            Text(w.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
            item {
                Text("${items.size} exercise${if (items.size == 1) "" else "s"} · $totalSets sets",
                    fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(items, key = { it.exerciseId }) { te ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(te.exerciseName ?: "Exercise", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp)) {
                        Text("Set", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(48.dp))
                        Text("Reps", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.width(64.dp))
                        Text("Weight", fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint)
                    }
                    te.sets.orEmpty().sortedBy { it.setNumber }.forEachIndexed { i, s ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Text("${i + 1}", fontSize = 11.sp, fontFamily = DmMono, color = MutedDark, modifier = Modifier.width(48.dp))
                            Text(s.reps?.toString() ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink, modifier = Modifier.width(64.dp))
                            Text(s.weightKg?.let { fmtKg(it) } ?: "–", fontSize = 12.sp, fontFamily = DmMono, color = Ink)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private fun fmtKg(v: Double): String = (if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()) + " kg"

// ── Selected diet detail (slots + foods, like the Home diet view) ────────────────
@Composable
private fun DietSlots(diet: DietSummary) {
    if (diet.slots.isEmpty()) {
        Text("This diet has no meals yet.", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 8.dp))
        return
    }
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        diet.slots.forEach { slot ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                SlotBadge(slot.slot)
                Spacer(Modifier.weight(1f))
                Text("${slot.kcal} kcal", fontFamily = DmMono, fontSize = AppText.meta, color = MutedFaint)
            }
            // Group each meal header with the food lines that follow it; render meals expandable.
            mealBlocks(slot.lines).forEach { block ->
                if (block.first != null) ExpandableMeal(block.first!!, block.second)
                else block.second.forEach { FoodLineRow(it) }
            }
        }
    }
}

private fun mealBlocks(lines: List<DietLine>): List<Pair<DietLine?, List<DietLine>>> {
    val out = mutableListOf<Pair<DietLine?, MutableList<DietLine>>>()
    lines.forEach { line ->
        if (line.header) out.add(line to mutableListOf())
        else if (out.isNotEmpty() && out.last().first != null) out.last().second.add(line)
        else out.add(null to mutableListOf(line))   // a direct food with no meal
    }
    return out
}

@Composable
private fun ExpandableMeal(header: DietLine, foods: List<DietLine>) {
    var open by remember(header.name) { mutableStateOf(false) }
    val expandable = foods.isNotEmpty()
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().then(if (expandable) Modifier.clickable { open = !open } else Modifier).padding(top = 4.dp)) {
        Text(header.name, fontSize = AppText.itemName, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.weight(1f))
        if (expandable) Text(if (open) " ▲" else " ▼", fontSize = 9.sp, color = MutedFaint)
        Text(header.meta, fontSize = AppText.meta, color = MutedFaint, fontFamily = DmMono)
    }
    if (open) foods.forEach { FoodLineRow(it) }
}

@Composable
private fun FoodLineRow(line: DietLine) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 8.dp)) {
        Text("• ${line.name}", fontSize = AppText.subItem, color = MutedDark, modifier = Modifier.weight(1f))
        Text(line.meta, fontSize = AppText.meta, color = MutedFaint, fontFamily = DmMono)
    }
}

@Composable
private fun SlotBadge(slot: String) =
    Text(slot.uppercase(), fontSize = AppText.slotLabel, fontWeight = FontWeight.SemiBold, color = Teal,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))

@Composable
private fun SectionLabel(text: String) =
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))

// ── Diet picker (the Diets screen as a chooser: search + tag filter + expand) ────
@Composable
private fun DietPicker(date: LocalDate, state: PlanUiState, viewModel: PlanViewModel) {
    val diets = state.filteredDiets
    var expandedId by remember { mutableStateOf<Long?>(null) }
    val plan = state.plansByDate[date]
    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 6.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = viewModel::closePicker), Alignment.Center) {
                Text("‹", fontSize = 24.sp, color = Ink)
            }
            Text("Choose a diet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            Text("${state.diets.size} saved", fontSize = 12.sp, color = MutedLight)
        }
        PickerSearchBar(state.pickerSearch, viewModel::setPickerSearch)
        if (state.allTags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item { PickerTagChip("All", state.pickerTag == null) { viewModel.setPickerTag(null) } }
                items(state.allTags) { t -> PickerTagChip(t, state.pickerTag == t) { viewModel.setPickerTag(if (state.pickerTag == t) null else t) } }
            }
        }
        if (diets.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🥗", fontSize = 40.sp)
                Text("No diets match", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(top = 8.dp))
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                items(diets, key = { it.id }) { d ->
                    PickerDietCard(
                        diet = d,
                        selected = plan?.dietId == d.id,
                        expanded = expandedId == d.id,
                        onToggleExpand = { expandedId = if (expandedId == d.id) null else d.id },
                        onChoose = { viewModel.chooseDiet(date, d.id) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun PickerSearchBar(query: String, onChange: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text("🔍", fontSize = 13.sp)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Search your diets…", fontSize = 14.sp, color = MutedLight)
            BasicTextField(query, onChange,
                textStyle = ComposeTextStyle(fontSize = 14.sp, color = Ink), singleLine = true,
                cursorBrush = SolidColor(Teal), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PickerTagChip(text: String, on: Boolean, onClick: () -> Unit) =
    Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) Surface else MutedDark,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) Ink else SurfaceMuted)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp))

@Composable
private fun PickerDietCard(diet: DietSummary, selected: Boolean, expanded: Boolean, onToggleExpand: () -> Unit, onChoose: () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface)
        .border(1.dp, if (selected) Teal else CardBorder, RoundedCornerShape(14.dp))
        .clickable(onClick = onToggleExpand).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(diet.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                if (diet.tags.isNotEmpty()) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        diet.tags.take(3).forEach { tag ->
                            Text(tag, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (diet.tags.size > 3) Text("+${diet.tags.size - 3}", fontSize = 8.5.sp, color = MutedFaint)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Text("${diet.kcal}", fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink)
            Text(" kcal", fontSize = 9.sp, color = MutedFaint)
        }
        if (expanded) {
            DietSlots(diet)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                .clip(RoundedCornerShape(11.dp)).background(if (selected) SurfaceMuted else Teal)
                .clickable(enabled = !selected, onClick = onChoose).padding(vertical = 11.dp)) {
                Text(if (selected) "✓ Selected" else "Choose this diet", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = if (selected) MutedDark else OnAccent)
            }
        } else {
            Text(if (selected) "✓ Selected · tap to view" else "Tap to view meals", fontSize = 10.sp, color = if (selected) Teal else MutedFaint, modifier = Modifier.padding(top = 6.dp))
        }
    }
}
