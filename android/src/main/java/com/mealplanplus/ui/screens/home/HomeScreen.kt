package com.mealplanplus.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.healthconnect.ActivitySummary
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.util.ThemePreferences
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.mealplanplus.util.toEpochMs
import com.mealplanplus.util.toLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// ── Design tokens ──────────────────────────────────────────────────────────────
private val PrimaryGreen  = Color(0xFF2E7D52)
private val BgPage        = Color(0xFFF7F7F7)
private val CardBg        = Color.White
private val TextPrimary   = Color(0xFF111111)
private val TextSecondary = Color(0xFF888888)
private val TextMuted     = Color(0xFFBBBBBB)
private val Divider       = Color(0xFFEEEEEE)
private val MacroProtein  = Color(0xFF2E7D52)
private val MacroCarbs    = Color(0xFFC05200)
private val MacroFat      = Color(0xFF1E4FBF)
private val MacroCal      = Color(0xFFF59E0B)
private val SlotBreakfast = Color(0xFFF59E0B)
private val SlotLunch     = Color(0xFF2E7D52)
private val SlotDinner    = Color(0xFF7C3AED)
private val SlotDefault   = Color(0xFF888888)
private val AiPurple      = Color(0xFF7C3AED)

@Composable
fun HomeScreen(
    onNavigateToLog: () -> Unit = {},
    onNavigateToLogWithDate: (String) -> Unit = {},
    onNavigateToHealth: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToGroceryLists: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToDietPickerForToday: () -> Unit = {},
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
    onNavigateToFoods: () -> Unit = {},
    onNavigateToMeals: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDiets: () -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState().value
    val scope   = rememberCoroutineScope()
    val context    = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.finishCompleted) {
        if (uiState.finishCompleted) {
            snackbarHostState.showSnackbar("Day completed! Great work 🎉", duration = SnackbarDuration.Short)
            viewModel.clearFinishCompleted()
        }
    }

    val followSystem by ThemePreferences.isFollowSystem(context).collectAsState(initial = true)
    val darkModePref by ThemePreferences.isDarkMode(context).collectAsState(initial = false)
    val isDark = if (followSystem) isSystemInDarkTheme() else darkModePref

    // Observe diet selection result from DietPickerScreen
    val selectedDietId by (savedStateHandle
        ?.getStateFlow("selected_diet_id", -1L)
        ?.collectAsState() ?: remember { mutableStateOf(-1L) })
    LaunchedEffect(selectedDietId) {
        if (selectedDietId != -1L) {
            viewModel.planDietForToday(selectedDietId)
            savedStateHandle?.set("selected_diet_id", -1L)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPage
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top: greeting + inline stat strip ─────────────────────────
            HomeGreetingHeader(
                userName      = uiState.userName,
                userInitial   = uiState.userInitial,
                dayStreak     = uiState.dayStreak,
                caloriesConsumed = uiState.todaySummary.calories,
                activitySessions = if (uiState.activitySummary.isConnected) (uiState.activitySummary.stepsToday / 1000).toInt() else 0,
                isDark        = isDark,
                onThemeToggle = {
                    scope.launch {
                        ThemePreferences.setFollowSystem(context, false)
                        ThemePreferences.setDarkMode(context, !isDark)
                    }
                },
                onNotificationClick = onNavigateToSettings,
                onProfileClick = onNavigateToProfile
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Macros card ────────────────────────────────────────────────
            MacroProgressCard(
                calories      = uiState.todaySummary.calories,
                calorieGoal   = uiState.calorieGoal,
                protein       = uiState.todaySummary.protein,
                carbs         = uiState.todaySummary.carbs,
                fat           = uiState.todaySummary.fat,
                modifier      = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── AI insight strip (Phase 4 placeholder) ─────────────────────
            AiInsightStrip(
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // ── Today's meals ──────────────────────────────────────────────
            TodayMealsSection(
                slots            = uiState.todayPlanSlots,
                hasDietToday     = uiState.hasDietToday,
                isTodayCompleted = uiState.isTodayCompleted,
                onPlanOrChangeDiet = onNavigateToDietPickerForToday,
                onSlotToggle     = { slot -> viewModel.toggleSlotLogged(slot) },
                onSlotTap        = { slot ->
                    val dId = slot.dietId
                    if (dId != null && slot.plannedMealId != null)
                        onNavigateToMealDetail(dId, slot.slotType)
                },
                onFinishDay      = { viewModel.finishTodayPlan() },
                onReopenDay      = { viewModel.reopenTodayPlan() },
                modifier         = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Greeting header + stat strip ──────────────────────────────────────────────

@Composable
fun HomeGreetingHeader(
    userName: String,
    userInitial: String,
    dayStreak: Int,
    caloriesConsumed: Int,
    activitySessions: Int = 0,
    isDark: Boolean = false,
    onThemeToggle: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }
    val today = LocalDate.now()
    val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
    val dateStr = "$dayOfWeek, ${today.dayOfMonth} ${today.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPage)
            .padding(start = 20.dp, end = 16.dp, top = 56.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$greeting, ${userName.ifBlank { "there" }}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onThemeToggle, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onNotificationClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Settings",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5EE))
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial,
                        color = PrimaryGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Inline stat strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(emoji = "🔥", value = "$dayStreak", label = "days")
            StatDivider()
            StatChip(emoji = "⚡", value = "%,d".format(caloriesConsumed), label = "kcal")
            StatDivider()
            StatChip(emoji = "💪", value = "$activitySessions", label = "sessions")
        }
    }
}

@Composable
private fun StatChip(emoji: String, value: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(end = 14.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), letterSpacing = (-0.3).sp)
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun StatDivider() {
    Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color(0xFFDEDEDE)).padding(end = 14.dp))
}

// ── Macro progress card ────────────────────────────────────────────────────────

@Composable
fun MacroProgressCard(
    calories: Int,
    calorieGoal: Int,
    protein: Int,
    carbs: Int,
    fat: Int,
    modifier: Modifier = Modifier
) {
    val proteinGoal = ((calorieGoal * 0.30f) / 4).toInt()
    val carbsGoal   = ((calorieGoal * 0.40f) / 4).toInt()
    val fatGoal     = ((calorieGoal * 0.30f) / 9).toInt()

    val isOnTrack = calories <= calorieGoal && protein <= proteinGoal * 1.1f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Today vs plan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(
                        "Target: $calorieGoal kcal",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnTrack) Color(0xFFE8F5EE) else Color(0xFFFFF3E0)
                ) {
                    Text(
                        text = if (isOnTrack) "On track" else "Over",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnTrack) PrimaryGreen else Color(0xFFC05200),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2×2 grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MacroProgressItem("Protein", protein, proteinGoal, "g", MacroProtein, Modifier.weight(1f))
                MacroProgressItem("Carbs", carbs, carbsGoal, "g", MacroCarbs, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                MacroProgressItem("Fat", fat, fatGoal, "g", MacroFat, Modifier.weight(1f))
                MacroProgressItem("Calories", calories, calorieGoal, "", MacroCal, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MacroProgressItem(
    label: String,
    value: Int,
    goal: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (value.toFloat() / goal).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF444444))
            Text(
                text = "$value / $goal$unit",
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = Color(0xFFF0F0F0),
            strokeCap = StrokeCap.Round
        )
    }
}

// ── AI insight strip ───────────────────────────────────────────────────────────

@Composable
fun AiInsightStrip(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3EEFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", fontSize = 12.sp, color = AiPurple)
            }
            Text(
                text = "AI Coach — coming in Phase 4. Tap to learn more.",
                fontSize = 12.sp,
                color = Color(0xFF444444),
                modifier = Modifier.weight(1f),
                lineHeight = 17.sp
            )
            Text("›", fontSize = 16.sp, color = TextMuted)
        }
    }
}

// ── Today's meals section ──────────────────────────────────────────────────────

@Composable
fun TodayMealsSection(
    slots: List<TodayPlanSlot>,
    hasDietToday: Boolean,
    isTodayCompleted: Boolean,
    onPlanOrChangeDiet: () -> Unit,
    onSlotToggle: (TodayPlanSlot) -> Unit,
    onSlotTap: (TodayPlanSlot) -> Unit,
    onFinishDay: () -> Unit,
    onReopenDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val loggedCount = slots.count { it.isLogged }
    val totalCount  = slots.size
    val allLogged   = totalCount > 0 && loggedCount == totalCount

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAY",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isTodayCompleted && totalCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0F0F0)
                    ) {
                        Text(
                            text = "$loggedCount / $totalCount logged",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    }
                }
                // Day-complete tick
                if (hasDietToday && totalCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isTodayCompleted) PrimaryGreen else Color.Transparent)
                            .then(
                                if (isTodayCompleted) Modifier else Modifier.background(Color.Transparent)
                            )
                            .clickable(onClick = if (isTodayCompleted) onReopenDay else if (allLogged) onFinishDay else { {} }),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTodayCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Day complete – tap to reopen",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color.Transparent)
                                ) {
                                    Canvas22dp(
                                        color = if (allLogged) PrimaryGreen else Color(0xFFD4D4D4)
                                    )
                                }
                            }
                        }
                    }
                }
                // Plan / change diet button
                TextButton(
                    onClick = onPlanOrChangeDiet,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = if (hasDietToday) "Change ›" else "Plan diet ›",
                        color = PrimaryGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Slot cards
        if (slots.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Text(
                    text = if (hasDietToday) "Loading plan…"
                           else "No diet planned for today.\nTap \"Plan diet\" to get started.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(16.dp),
                    lineHeight = 19.sp
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                slots.forEachIndexed { index, slot ->
                    val canNavigate = slot.dietId != null && slot.plannedMealId != null
                    NewTodaySlotRow(
                        slot     = slot,
                        onToggle = if (!isTodayCompleted) { -> onSlotToggle(slot) } else { -> },
                        onTap    = if (canNavigate) { -> onSlotTap(slot) } else null
                    )
                    if (index < slots.lastIndex) {
                        HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
                    }
                }
            }
        }
    }
}

// Draws the outline-circle tick (no Canvas dep — uses nested Box with border)
@Composable
private fun Canvas22dp(color: Color) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Transparent)
        ) {
            // Outer ring
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f))
            )
        }
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
fun NewTodaySlotRow(
    slot: TodayPlanSlot,
    onToggle: () -> Unit = {},
    onTap: (() -> Unit)? = null
) {
    val dotColor = when (slot.slotType.uppercase()) {
        "BREAKFAST"   -> SlotBreakfast
        "LUNCH"       -> SlotLunch
        "DINNER"      -> SlotDinner
        else          -> SlotDefault
    }
    val canToggle    = slot.plannedFoods.isNotEmpty() || slot.isLogged
    val hasLoggedFoods = slot.loggedFoods.isNotEmpty()
    val hasPlanFoods = slot.plannedFoods.isNotEmpty()
    var expanded by remember(slot.slotType, slot.loggedFoods.size) { mutableStateOf(false) }

    // Calorie count from planned foods or logged foods
    val calories = when {
        slot.isLogged && hasLoggedFoods ->
            slot.loggedFoods.sumOf { it.calculatedCalories.toInt() }
        hasPlanFoods ->
            slot.plannedFoods.sumOf { it.calculatedCalories.toInt() }
        else -> 0
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    when {
                        onTap != null    -> onTap()
                        hasLoggedFoods || hasPlanFoods -> expanded = !expanded
                    }
                }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Colour dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (slot.isLogged) dotColor else Color(0xFFCCCCCC))
            )

            // Name + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.plannedMealName ?: slot.slotDisplayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (slot.isLogged) TextPrimary else Color(0xFF444444)
                )
                val subtitle = when {
                    slot.isLogged -> "${slot.slotDisplayName} · Logged ✓" + if (hasLoggedFoods) " · tap to see" else ""
                    slot.plannedMealName != null -> "${slot.slotDisplayName} · Not logged yet"
                    else -> slot.slotDisplayName
                }
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = if (slot.isLogged) PrimaryGreen else TextSecondary,
                    fontWeight = if (slot.isLogged) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Calorie count
            if (calories > 0) {
                Text(
                    text = "$calories",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (slot.isLogged) Color(0xFF888888) else Color(0xFFCCCCCC)
                )
            }

            // Tick / circle
            if (slot.isLogged) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5EE))
                        .then(if (canToggle) Modifier.clickable(onClick = onToggle) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Logged", tint = PrimaryGreen, modifier = Modifier.size(12.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF5F5F5))
                        .then(if (canToggle) Modifier.clickable(onClick = onToggle) else Modifier),
                    contentAlignment = Alignment.Center
                ) { /* empty circle */ }
            }
        }

        // Expandable ingredient chips
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            val foods = if (hasLoggedFoods) slot.loggedFoods.map { "${it.food.name} ${it.loggedFood.quantity.toInt()}${it.loggedFood.unit.name.take(1).lowercase()}" }
                        else slot.plannedFoods.map { "${it.food.name} ${it.mealFoodItem.quantity.toInt()}${it.mealFoodItem.unit.name.take(1).lowercase()}" }
            if (foods.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    // Wrap chips manually (FlowRow unavailable without ExperimentalApi, use wrapping Column+Row)
                    IngredientChips(foods)
                }
            }
        }
    }
}

@Composable
private fun IngredientChips(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                rowItems.forEach { label ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CardBg,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .background(CardBg)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                color = Color(0xFF555555),
                                modifier = Modifier
                                    .background(Color(0xFFF8F8F8))
                                    .clip(RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── This week (restyled) ───────────────────────────────────────────────────────

@Composable
fun ThisWeekCard(
    weekDays: List<WeekDayInfo>,
    weekOffset: Int = 0,
    onDayClick: (LocalDate) -> Unit = {},
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val displayDays = if (weekDays.isEmpty()) {
        (6 downTo 0).map { WeekDayInfo(today.minusDays(it.toLong()), null, WeekDayState.NO_DATA) }
    } else weekDays

    val weekLabel = if (displayDays.isNotEmpty()) {
        val start = displayDays.first().date
        val end   = displayDays.last().date
        val sm = start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val em = end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        if (start.month == end.month) "$sm ${start.year}" else "$sm – $em ${end.year}"
    } else {
        today.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " ${today.year}"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("This week", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousWeek, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                    Text(weekLabel, fontSize = 12.sp, color = TextSecondary)
                    IconButton(onClick = onNextWeek, enabled = weekOffset < 0, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = if (weekOffset < 0) TextSecondary else TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                displayDays.forEach { info ->
                    val letter = info.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1).uppercase()
                    Text(letter, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                displayDays.forEach { info ->
                    val isToday = info.date == today
                    val bgColor = when {
                        isToday -> PrimaryGreen
                        info.state == WeekDayState.COMPLETED -> PrimaryGreen
                        info.state == WeekDayState.PLANNED_FUTURE -> Color(0xFFF57C00)
                        info.state == WeekDayState.MISSED -> Color(0xFFD32F2F)
                        else -> Color.Transparent
                    }
                    val textColor = if (bgColor == Color.Transparent) Color(0xFFCCCCCC) else Color.White
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor)
                                .then(if (info.state != WeekDayState.NO_DATA) Modifier.clickable { onDayClick(info.date) } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(info.date.dayOfMonth.toString(), fontSize = 12.sp, fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal, color = textColor)
                        }
                        if (info.dietLabel != null) {
                            Text(info.dietLabel, fontSize = 9.sp, color = PrimaryGreen, textAlign = TextAlign.Center, maxLines = 1)
                        } else {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(color = PrimaryGreen, label = "Done")
                LegendDot(color = Color(0xFFF57C00), label = "Planned")
                LegendDot(color = Color(0xFFD32F2F), label = "Missed")
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = TextSecondary)
    }
}

// ── Kept for other screens that still reference them ──────────────────────────

@Composable
fun BloodGlucoseCard(
    latestSugar: HealthMetric?,
    glucoseHistory: List<HealthMetric>,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Blood Glucose", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                TextButton(onClick = onDetailsClick) { Text("Details ›", color = PrimaryGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (latestSugar != null) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(latestSugar.value.toInt().toString(), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("mg/dL", fontSize = 14.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
                }
                val inRange = latestSugar.value in 80.0..130.0
                Surface(shape = RoundedCornerShape(20.dp), color = if (inRange) Color(0xFFE8F5EE) else Color(0xFFFFEBEE)) {
                    Text(
                        if (inRange) "In Range" else "Out of Range",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        color = if (inRange) PrimaryGreen else Color(0xFFD32F2F),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (glucoseHistory.size >= 2) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlucoseChart(history = glucoseHistory)
                }
            } else {
                Text("No glucose reading today.\nLog a reading in Health.", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun GlucoseChart(history: List<HealthMetric>) {
    val sorted   = remember(history) { history.sortedBy { it.date } }
    val entries  = remember(sorted) { sorted.mapIndexed { i, m -> entryOf(i.toFloat(), m.value.toFloat()) } }
    val producer = remember(entries) { ChartEntryModelProducer(entries) }
    val labels   = remember(sorted) { val fmt = DateTimeFormatter.ofPattern("dd/MM"); sorted.map { it.date.toLocalDate().format(fmt) } }
    val xSpacing = remember(sorted.size) { maxOf(1, sorted.size / 4) }
    val formatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ -> labels.getOrElse(value.toInt()) { "" } }
    ProvideChartStyle(m3ChartStyle()) {
        Chart(
            chart = lineChart(), chartModelProducer = producer,
            startAxis = rememberStartAxis(itemPlacer = AxisItemPlacer.Vertical.default(maxItemCount = 4)),
            bottomAxis = rememberBottomAxis(valueFormatter = formatter, itemPlacer = remember(xSpacing) { AxisItemPlacer.Horizontal.default(spacing = xSpacing) }),
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )
    }
}

@Composable
fun StatsRow(latestHba1c: HealthMetric?, latestWeight: HealthMetric?, dayStreak: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(emoji = "🏅", value = latestHba1c?.let { "${it.value}%" } ?: "--", label = "A1C Level", iconBg = Color(0xFFEDE7F6), modifier = Modifier.weight(1f))
        StatCard(emoji = "⚡", value = latestWeight?.let { "${it.value} lbs" } ?: "--", label = "Weight", iconBg = Color(0xFFE3F2FD), modifier = Modifier.weight(1f))
        StatCard(emoji = "🕐", value = "$dayStreak", label = "Streak", iconBg = Color(0xFFFFF8E1), modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(emoji: String, value: String, label: String, iconBg: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Text(emoji, fontSize = 16.sp)
            }
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
            Text(label, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

// Kept for HealthScreen which references these
@Composable
fun MacroRingsCard(calories: Int, calorieGoal: Int, protein: Int, carbs: Int, fat: Int) {
    MacroProgressCard(calories = calories, calorieGoal = calorieGoal, protein = protein, carbs = carbs, fat = fat)
}
