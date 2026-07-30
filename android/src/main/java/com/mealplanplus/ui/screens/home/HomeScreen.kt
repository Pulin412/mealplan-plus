package com.mealplanplus.ui.screens.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.DashboardDto
import com.mealplanplus.data.generated.model.ExerciseDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.generated.model.LoggedFoodResponseDto
import com.mealplanplus.data.generated.model.SlotStatusDto
import com.mealplanplus.data.generated.model.StreakDto
import com.mealplanplus.data.generated.model.TodayMealItemDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.data.healthconnect.HealthConnectSummary
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.data.repository.defaultQtyFor
import com.mealplanplus.data.repository.unitLabel
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.Carbs
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Fat
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.LocalAppColors
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Protein
import com.mealplanplus.ui.theme.StreakFlame
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.roundToInt

private val HOME_SLOTS = listOf(
    "Early Breakfast", "Breakfast", "Noon", "Pre-Lunch", "Post-Lunch", "Evening",
    "Pre-workout", "Post-workout", "Pre-dinner", "Dinner", "Post Dinner",
)

private val CardDark = Color(0xFF14181B)
private val CardDarkText = Color(0xFFEDF1F2)
private val CardDarkMuted = Color(0xFF8A949B)
private val OverColor = Color(0xFFD98A4A)

private sealed interface HomeSheet {
    data object None : HomeSheet
    data object Diet : HomeSheet
    data object AddToday : HomeSheet
    data object AddWorkout : HomeSheet
}

@Composable
fun HomeScreen(onMenu: () -> Unit = {}, onProfile: () -> Unit = {},
               onOpenRunner: (Long, String) -> Unit = { _, _ -> },
               onOpenExerciseRunner: (Long, String) -> Unit = { _, _ -> }) {
    val viewModel: HomeViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val isDark = LocalAppColors.current.isDark
    var sheet by remember { mutableStateOf<HomeSheet>(HomeSheet.None) }
    var expandedSlots by remember { mutableStateOf(setOf<String>()) }

    // Refresh Today whenever we return to Home (e.g. after planning a diet in Plan, or finishing the
    // runner). load() also refreshes the meal slots + calorie ring, which reflect the planned diet.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { viewModel.load(); viewModel.loadWorkouts(); viewModel.loadActivity() }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            HomeAppBar(state.dashboard, isDark, onMenu, onProfile, onToggleTheme = { viewModel.toggleTheme(isDark) },
                onDietClick = { if (state.dashboard?.dietName != null) sheet = HomeSheet.Diet })
            when {
                state.loading && state.dashboard == null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading today…", fontSize = 13.sp, color = MutedLight) }
                state.dashboard == null ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error ?: "Couldn't load today", fontSize = 13.sp, color = MutedLight) }
                else -> {
                    val d = state.dashboard!!
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                        Spacer(Modifier.height(4.dp))
                        CalorieCard(d)
                        Spacer(Modifier.height(16.dp))
                        MealsChecklist(d.slots, state.togglingSlot, expandedSlots,
                            onToggle = viewModel::toggleSlot,
                            onToggleExpand = { s -> expandedSlots = if (s in expandedSlots) expandedSlots - s else expandedSlots + s })
                        if (d.additionalFoods.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            AddedTodaySection(d.additionalFoods, state.foods, viewModel::removeFood)
                        }
                        Spacer(Modifier.height(16.dp))
                        WorkoutSection(state.workouts,
                            onOpen = { w ->
                                when {
                                    w.templateId != null -> onOpenRunner(w.templateId, w.name)
                                    w.exerciseId != null -> onOpenExerciseRunner(w.exerciseId, w.name)
                                }
                            },
                            onAdd = { sheet = HomeSheet.AddWorkout })
                        if (state.hcConnected) {
                            Spacer(Modifier.height(16.dp))
                            ActivityCard(state.hcSummary)
                        }
                        Spacer(Modifier.height(16.dp))
                        StreakCard(d.streak, d)
                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }

        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).size(56.dp).clip(CircleShape).background(Teal)
                .clickable { if (state.dashboard != null) sheet = HomeSheet.AddToday }) {
            Icon(Icons.Default.Add, "Add to today", tint = OnAccent, modifier = Modifier.size(28.dp))
        }
    }

    when (sheet) {
        is HomeSheet.Diet -> state.dashboard?.let { DietDetailSheet(it) { sheet = HomeSheet.None } }
        is HomeSheet.AddToday -> AddToTodaySheet(state.foods, state.dashboard?.slots.orEmpty().map { it.slot },
            onAdd = { foodId, slot, qty, unit -> viewModel.addFood(foodId, slot, qty, unit); sheet = HomeSheet.None }) { sheet = HomeSheet.None }
        is HomeSheet.AddWorkout -> AddWorkoutSheet(state.workoutTemplates, state.exercises,
            onPickWorkout = { viewModel.addWorkout(it); sheet = HomeSheet.None },
            onPickExercise = { ex -> sheet = HomeSheet.None; ex.id?.let { onOpenExerciseRunner(it, ex.name) } },
            onClose = { sheet = HomeSheet.None })
        HomeSheet.None -> {}
    }
}

// ── Activity (Health Connect) ────────────────────────────────────────────────────
@Composable
private fun ActivityCard(summary: HealthConnectSummary) {
    Text("Activity", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActivityStat("%,d".format(summary.steps), "steps", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(32.dp).background(SurfaceMuted))
        ActivityStat(summary.caloriesBurned.toString(), "kcal burned", Modifier.weight(1f))
    }
}

@Composable
private fun ActivityStat(value: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier.padding(vertical = 11.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink, fontFamily = DmMono)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.5.sp, color = MutedLight)
    }
}

// ── Today's workout ──────────────────────────────────────────────────────────────
@Composable
private fun WorkoutSection(workouts: List<HomeWorkout>, onOpen: (HomeWorkout) -> Unit, onAdd: () -> Unit) {
    Text("Today's workout", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    if (workouts.isEmpty()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onAdd).padding(vertical = 20.dp), Alignment.Center) {
            Text("＋ Add a workout or exercise", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
        }
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) {
        workouts.forEachIndexed { i, w ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onOpen(w) }.padding(horizontal = 12.dp, vertical = 12.dp)) {
                Text("🏋️", fontSize = 15.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(w.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    WorkoutStatusLabel(w.status)
                }
                Text("›", fontSize = 20.sp, color = MutedLight)
            }
            if (i < workouts.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        Box(Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(vertical = 11.dp), Alignment.Center) {
            Text("＋ Add workout or exercise", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
        }
    }
}

@Composable
private fun WorkoutStatusLabel(status: WorkoutStatus) {
    val (text, color) = when (status) {
        WorkoutStatus.PLANNED -> "Planned · tap to start" to MutedLight
        WorkoutStatus.IN_PROGRESS -> "In progress · tap to continue" to Teal
        WorkoutStatus.DONE -> "✓ Done" to Success
    }
    Text(text, fontSize = 10.5.sp, fontWeight = if (status == WorkoutStatus.DONE) FontWeight.SemiBold else FontWeight.Normal, color = color, modifier = Modifier.padding(top = 1.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddWorkoutSheet(
    templates: List<WorkoutTemplateDto>,
    exercises: List<ExerciseDto>,
    onPickWorkout: (WorkoutTemplateDto) -> Unit,
    onPickExercise: (ExerciseDto) -> Unit,
    onClose: () -> Unit,
) {
    var tab by remember { mutableStateOf(0) } // 0 = Workouts, 1 = Exercises
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("Add to today", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(if (tab == 0) "Pick a workout to plan for today" else "Pick a single exercise to log now",
                fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            // Segmented Workouts | Exercises
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).border(1.dp, BorderCool, RoundedCornerShape(9.dp))) {
                listOf("Workouts", "Exercises").forEachIndexed { i, label ->
                    val on = i == tab
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(34.dp)
                        .background(if (on) Ink else Surface).clickable { tab = i }) {
                        Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (on) Surface else MutedDark)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (tab == 0) {
                if (templates.isEmpty()) {
                    EmptyPicker("No workouts yet — build one in Exercises → Workouts.")
                } else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(templates, key = { it.id ?: it.name.hashCode().toLong() }) { w ->
                        val items = w.exercises ?: emptyList()
                        val totalSets = items.sumOf { it.sets?.size ?: 0 }
                        PickerRow(w.name, "${items.size} exercise${if (items.size == 1) "" else "s"} · $totalSets sets", "+ Add") { onPickWorkout(w) }
                    }
                }
            } else {
                if (exercises.isEmpty()) {
                    EmptyPicker("No exercises yet — add some in Exercises.")
                } else LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    items(exercises, key = { it.id ?: it.name.hashCode().toLong() }) { ex ->
                        PickerRow(ex.name, ex.description?.takeIf { it.isNotBlank() } ?: "Tap to log now", "▶ Log") { onPickExercise(ex) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPicker(text: String) {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), Alignment.Center) {
        Text(text, fontSize = 12.sp, color = MutedLight)
    }
}

@Composable
private fun PickerRow(title: String, sub: String, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 11.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(sub, fontSize = 10.5.sp, color = MutedLight, maxLines = 1)
        }
        Text(action, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(9.dp)).padding(horizontal = 12.dp, vertical = 7.dp))
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
}

@Composable
private fun HomeAppBar(d: DashboardDto?, isDark: Boolean, onMenu: () -> Unit, onProfile: () -> Unit, onToggleTheme: () -> Unit, onDietClick: () -> Unit) {
    val dateText = d?.date?.format(DateTimeFormatter.ofPattern("EEEE, d MMM")) ?: ""
    Column(Modifier.fillMaxWidth().padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Settings, "Settings", tint = Ink) }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onToggleTheme), contentAlignment = Alignment.Center) {
                Text(if (isDark) "☀️" else "🌙", fontSize = 16.sp)
            }
            IconButton(onClick = {}) { Icon(Icons.Default.Notifications, "Notifications", tint = MutedDark, modifier = Modifier.size(20.dp)) }
            Box(Modifier.size(34.dp).clip(CircleShape).background(Teal).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Profile", tint = OnAccent, modifier = Modifier.size(18.dp))
            }
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(start = 10.dp, top = 2.dp)) {
            Text(dateText, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
            d?.dietName?.let {
                Spacer(Modifier.width(8.dp))
                Text("· $it", fontSize = 12.sp, color = Teal, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 3.dp).clip(RoundedCornerShape(4.dp)).clickable(onClick = onDietClick))
            }
        }
    }
}

// ── Calorie strip (dark hero card) ─────────────────────────────────────────────
@Composable
private fun CalorieCard(d: DashboardDto) {
    val ring = d.calorieRing
    val over = ring.isOver
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(CardDark).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CalorieRing(ring.consumed, ring.target.toDouble(), over) {
                val big = if (over) (-ring.remaining) else ring.remaining
                Text("${big.roundToInt()}", fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = CardDarkText)
                Text(if (over) "kcal over" else "kcal left", fontSize = 10.sp, color = CardDarkMuted)
            }
            Spacer(Modifier.width(20.dp))
            Column(Modifier.weight(1f)) {
                Text("${ring.consumed.roundToInt()} of ${ring.target} kcal", fontSize = 12.sp, color = CardDarkMuted)
                Spacer(Modifier.height(10.dp))
                MacroBar("Protein", d.macros.consumedProtein, d.macros.targetProtein, Protein)
                Spacer(Modifier.height(7.dp))
                MacroBar("Carbs", d.macros.consumedCarbs, d.macros.targetCarbs, Carbs)
                Spacer(Modifier.height(7.dp))
                MacroBar("Fat", d.macros.consumedFat, d.macros.targetFat, Fat)
            }
        }
    }
}

@Composable
private fun CalorieRing(consumed: Double, target: Double, over: Boolean, center: @Composable () -> Unit) {
    val frac = if (target <= 0) 0f else (consumed / target).coerceIn(0.0, 1.0).toFloat()
    val progressColor = if (over) OverColor else Teal
    Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(96.dp)) {
            val sw = 11.dp.toPx()
            val dm = size.minDimension - sw
            val tl = Offset(sw / 2, sw / 2)
            val sz = Size(dm, dm)
            drawArc(Color(0xFF2A3136), -90f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = tl, size = sz)
            drawArc(progressColor, -90f, 360f * frac, false, style = Stroke(sw, cap = StrokeCap.Round), topLeft = tl, size = sz)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) { center() }
    }
}

@Composable
private fun MacroBar(label: String, consumed: Double, target: Int?, color: Color) {
    val frac = if (target == null || target <= 0) 0f else (consumed / target).coerceIn(0.0, 1.0).toFloat()
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 10.sp, color = CardDarkMuted, modifier = Modifier.width(52.dp))
            Text("${consumed.roundToInt()}${target?.let { " / ${it}g" } ?: "g"}", fontFamily = DmMono, fontSize = 10.sp, color = CardDarkText)
        }
        Spacer(Modifier.height(3.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF2A3136))) {
            Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
        }
    }
}

// ── Meals checklist ────────────────────────────────────────────────────────────
@Composable
private fun MealsChecklist(slots: List<SlotStatusDto>, toggling: String?, expanded: Set<String>, onToggle: (String) -> Unit, onToggleExpand: (String) -> Unit) {
    Text("Today's meals", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    if (slots.isEmpty()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(vertical = 22.dp), Alignment.Center) {
            Text("No diet planned for today.", fontSize = 12.sp, color = MutedLight)
        }
        return
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) {
        slots.forEachIndexed { i, slot ->
            SlotRow(slot, toggling == slot.slot, slot.slot in expanded, onToggle, onToggleExpand)
            if (i < slots.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        }
    }
}

@Composable
private fun SlotRow(slot: SlotStatusDto, busy: Boolean, expanded: Boolean, onToggle: (String) -> Unit, onToggleExpand: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggleExpand(slot.slot) }.padding(horizontal = 12.dp, vertical = 11.dp)) {
            // Check circle is its own tap target — logs/unlogs; the rest of the row expands/collapses.
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp).clip(CircleShape).clickable(enabled = !busy) { onToggle(slot.slot) }) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(22.dp).clip(CircleShape)
                    .background(if (slot.isLogged) Teal else Color.Transparent)
                    .border(1.5.dp, if (slot.isLogged) Teal else BorderCool, CircleShape)) {
                    if (slot.isLogged) Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(slot.slot, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(slot.mealName ?: "—", fontSize = 10.5.sp, color = MutedLight)
            }
            Text("${slot.kcal.roundToInt()}", fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Ink)
            Text(" kcal", fontSize = 9.sp, color = MutedFaint, modifier = Modifier.padding(top = 2.dp))
        }
        if (expanded) SlotDetail(slot)
    }
}

/** Inline meal detail: a cooking checklist (ticking does NOT log the meal) + macro totals. */
@Composable
private fun SlotDetail(slot: SlotStatusDto) {
    var checked by remember(slot.slot) { mutableStateOf(setOf<Long>()) }
    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
        slot.items.forEach { item ->
            val on = item.foodId in checked
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()
                .clickable { checked = if (on) checked - item.foodId else checked + item.foodId }.padding(vertical = 6.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp).clip(RoundedCornerShape(5.dp))
                    .background(if (on) Teal else Color.Transparent).border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(5.dp))) {
                    if (on) Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.width(9.dp))
                Text(item.foodName, fontSize = 11.5.sp, color = MutedDark, modifier = Modifier.weight(1f))
                Text("${item.quantity.trimNum()} ${unitLabel(item.unit.value)}", fontSize = 10.sp, color = MutedFaint, fontFamily = DmMono)
            }
        }
        Text("P${slot.protein.roundToInt()} · C${slot.carbs.roundToInt()} · F${slot.fat.roundToInt()}", fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint, modifier = Modifier.padding(top = 6.dp))
    }
}

// ── Added today (unplanned foods logged via FAB) ────────────────────────────────
@Composable
private fun AddedTodaySection(added: List<LoggedFoodResponseDto>, foods: List<FoodDto>, onRemove: (Long) -> Unit) {
    val byId = foods.associateBy { it.id }
    Text("Added today", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(start = 2.dp, bottom = 8.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) {
        added.forEachIndexed { i, lf ->
            val food = byId[lf.foodId]
            val unit = lf.unit ?: FoodUnit.GRAM
            val kcal = ((food?.caloriesPer100 ?: 0.0) * gramsForDto(food, lf.quantity, unit.value) / 100.0).roundToInt()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(food?.name ?: "Food", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                        SlotBadge(lf.mealSlot)
                        Spacer(Modifier.width(6.dp))
                        Text("${lf.quantity.trimNum()} ${unitLabel(unit.value)}", fontSize = 10.sp, color = MutedFaint, fontFamily = DmMono)
                    }
                }
                Text("$kcal", fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Ink)
                Text(" kcal", fontSize = 9.sp, color = MutedFaint, modifier = Modifier.padding(top = 2.dp))
                Spacer(Modifier.width(10.dp))
                Text("✕", fontSize = 13.sp, color = MutedLight, modifier = Modifier.clickable { onRemove(lf.id) })
            }
            if (i < added.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
        }
    }
}

private fun gramsForDto(food: FoodDto?, quantity: Double, unit: String): Double = when (unit) {
    "PIECE" -> quantity * (food?.gramsPerPiece ?: 1.0)
    "CUP"   -> quantity * (food?.gramsPerCup ?: 1.0)
    "TBSP"  -> quantity * (food?.gramsPerTbsp ?: 1.0)
    "TSP"   -> quantity * (food?.gramsPerTsp ?: 1.0)
    else    -> quantity
}

// ── Streak card (week) ──────────────────────────────────────────────────────────
@Composable
private fun StreakCard(streak: StreakDto, d: DashboardDto) {
    val today = d.date
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text("${streak.current}", fontFamily = DmMono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink)
            Spacer(Modifier.width(8.dp))
            Text("Day streak · best ${streak.best}", fontSize = 10.5.sp, color = MutedLight)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            streak.dots.forEachIndexed { i, on ->
                val date = today?.minusDays((6 - i).toLong())
                val label = date?.dayOfWeek?.getDisplayName(JTextStyle.NARROW, Locale.getDefault()) ?: ""
                val isToday = i == streak.dots.lastIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(26.dp).clip(CircleShape)
                        .background(if (on) Teal else SurfaceMuted)
                        .border(if (isToday) 1.5.dp else 0.dp, if (isToday) Teal else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center) {
                        if (on) Icon(Icons.Default.Check, null, tint = OnAccent, modifier = Modifier.size(13.dp))
                    }
                    Text(label, fontSize = 9.sp, color = if (isToday) Teal else MutedFaint, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

// ── Diet detail sheet ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietDetailSheet(d: DashboardDto, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(d.dietName ?: "Diet", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("${d.calorieRing.target} kcal target · ${d.slots.size} slots", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            d.slots.forEach { slot ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    SlotBadge(slot.slot)
                    Spacer(Modifier.weight(1f))
                    Text("${slot.kcal.roundToInt()} kcal", fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
                }
                Text(slot.mealName ?: "—", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.padding(top = 2.dp))
                slot.items.forEach { ItemLine(it) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted).padding(top = 6.dp))
            }
        }
    }
}

// ── Add to today (unplanned food → slot) ────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddToTodaySheet(foods: List<FoodDto>, plannedSlots: List<String>, onAdd: (Long, String, Double, FoodUnit) -> Unit, onClose: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var slot by remember { mutableStateOf(plannedSlots.firstOrNull() ?: HOME_SLOTS[1]) }
    var query by remember { mutableStateOf("") }
    val list = foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp).height(520.dp)) {
            Text("Add to today", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Log an unplanned food into a slot", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            Text("Slot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(bottom = 5.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                HOME_SLOTS.forEach { s ->
                    val on = s == slot
                    Text(s, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) OnAccent else MutedDark,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) Teal else Color.Transparent)
                            .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp)).clickable { slot = s }.padding(horizontal = 11.dp, vertical = 6.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(SurfaceMuted).padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text("⌕", fontSize = 14.sp, color = MutedLight)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search your foods…", fontSize = 13.sp, color = MutedLight)
                    BasicTextField(query, { query = it }, singleLine = true, cursorBrush = SolidColor(Teal),
                        textStyle = TextStyle(fontSize = 13.sp, color = Ink), modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id ?: it.name.hashCode().toLong() }) { food ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(food.name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text("${food.caloriesPer100.toInt()} kcal / 100g", fontSize = 10.5.sp, color = MutedLight)
                        }
                        val unit = food.unit ?: FoodUnit.GRAM
                        Text("+ Add", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(9.dp))
                                .clickable { food.id?.let { onAdd(it, slot, defaultQtyFor(unit.value), unit) } }.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
                }
            }
        }
    }
}

// ── small helpers ──
@Composable
private fun SlotBadge(slot: String) {
    Text(slot.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun ItemLine(item: TodayMealItemDto) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(item.foodName, fontSize = 11.sp, color = MutedDark, modifier = Modifier.weight(1f))
        Text("${item.quantity.trimNum()} ${unitLabel(item.unit.value)}", fontSize = 9.5.sp, color = MutedFaint, fontFamily = DmMono)
    }
}

private fun Double.trimNum(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
