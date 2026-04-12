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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

// ── Slot colour helpers ────────────────────────────────────────────────────────

private fun slotAccent(slotType: String): Color = when (slotType.uppercase()) {
    "BREAKFAST", "EARLY_MORNING", "MID_MORNING" -> SlotBreakfast
    "NOON"                                       -> SlotNoon
    "LUNCH"                                      -> SlotLunch
    "EVENING", "EVENING_SNACK", "PRE_WORKOUT"    -> SlotEvening
    "DINNER", "POST_WORKOUT", "POST_DINNER"      -> SlotDinner
    else                                          -> SlotNoon
}

/** Estimated macro targets from calorie goal (replaced by diet-plan values in Phase 1+). */
private data class MacroTargets(val protein: Int, val carbs: Int, val fat: Int, val calories: Int)
private fun estimateTargets(calorieGoal: Int) = MacroTargets(
    protein  = (calorieGoal * 0.30f / 4f).toInt(),
    carbs    = (calorieGoal * 0.45f / 4f).toInt(),
    fat      = (calorieGoal * 0.25f / 9f).toInt(),
    calories = calorieGoal
)

// ── Screen ─────────────────────────────────────────────────────────────────────

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
    val uiState     by viewModel.uiState.collectAsState()
    val weekOffset  by viewModel.weekOffset.collectAsState()
    val scope       = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.finishCompleted) {
        if (uiState.finishCompleted) {
            snackbarHostState.showSnackbar(
                "Day completed! Great work 🎉",
                duration = SnackbarDuration.Short
            )
            viewModel.clearFinishCompleted()
        }
    }

    // Diet picker result from back-stack
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
        snackbarHost    = { SnackbarHost(snackbarHostState) },
        containerColor  = AppBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top bar — greeting + notification icon
            HomeTopBar(
                userName           = uiState.userName,
                onNotificationClick = onNavigateToSettings
            )

            // 2. Inline stat strip
            StatStrip(
                dayStreak = uiState.dayStreak,
                kcalToday = uiState.todaySummary.calories
            )

            // 3. Macros card — logged vs estimated target
            val targets = estimateTargets(uiState.calorieGoal)
            MacroProgressCard(
                proteinLogged  = uiState.todaySummary.protein,
                carbsLogged    = uiState.todaySummary.carbs,
                fatLogged      = uiState.todaySummary.fat,
                caloriesLogged = uiState.todaySummary.calories,
                targets        = targets
            )

            // 4. AI insight strip (Phase 4/5 placeholder)
            AiInsightStrip()

            // 5. Today's meals section
            TodaySlotsSection(
                slots              = uiState.todayPlanSlots,
                hasDietToday       = uiState.hasDietToday,
                isTodayCompleted   = uiState.isTodayCompleted,
                onPlanOrChangeDiet = onNavigateToDietPickerForToday,
                onSlotToggle       = { slot -> viewModel.toggleSlotLogged(slot) },
                onSlotTap          = { slot ->
                    val dId = slot.dietId
                    if (dId != null && slot.plannedMealId != null) {
                        onNavigateToMealDetail(dId, slot.slotType)
                    }
                },
                onFinishDay        = { viewModel.finishTodayPlan() },
                onReopenDay        = { viewModel.reopenTodayPlan() }
            )

            // 6. This week mini strip
            ThisWeekStrip(
                weekDays       = uiState.weekDays,
                weekOffset     = weekOffset,
                onDayClick     = { date -> onNavigateToLogWithDate(date.toString()) },
                onPreviousWeek = viewModel::previousWeek,
                onNextWeek     = viewModel::nextWeek
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeTopBar(userName: String, onNotificationClick: () -> Unit) {
    val greeting = when (java.time.LocalTime.now().hour) {
        in 5..11  -> "Good morning"
        in 12..16 -> "Good afternoon"
        else      -> "Good evening"
    }
    val today = LocalDate.now()
    val dateLabel = today.format(
        DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = "$greeting, ${userName.ifBlank { "there" }}",
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
                color      = AppOnSurface,
                letterSpacing = (-0.3).sp
            )
            Text(
                text     = dateLabel,
                fontSize = 12.sp,
                color    = AppMuted
            )
        }
        IconButton(
            onClick   = onNotificationClick,
            modifier  = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(TagGreyBg)
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Settings",
                tint               = AppMuted,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

// ── Stat strip ─────────────────────────────────────────────────────────────────

@Composable
private fun StatStrip(dayStreak: Int, kcalToday: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .padding(horizontal = 20.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(emoji = "🔥", value = "$dayStreak", unit = "days")
        VerticalDividerThin()
        StatItem(emoji = "⚡", value = "%,d".format(kcalToday), unit = "kcal")
        VerticalDividerThin()
        // Workout count — Phase 2; shows 0 until workout logging is live
        StatItem(emoji = "💪", value = "0", unit = "sessions")
    }
}

@Composable
private fun StatItem(emoji: String, value: String, unit: String) {
    Row(
        modifier          = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(emoji, fontSize = 13.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppOnSurface,
             letterSpacing = (-0.3).sp)
        Text(unit, fontSize = 11.sp, color = AppSubtle)
    }
}

@Composable
private fun VerticalDividerThin() {
    Box(
        modifier = Modifier
            .padding(horizontal = 14.dp)
            .width(1.dp)
            .height(16.dp)
            .background(AppBorder)
    )
}

// ── Macros card ────────────────────────────────────────────────────────────────

@Composable
private fun MacroProgressCard(
    proteinLogged: Int,
    carbsLogged: Int,
    fatLogged: Int,
    caloriesLogged: Int,
    targets: MacroTargets
) {
    val isOnTrack = caloriesLogged <= targets.calories
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom
            ) {
                Column {
                    Text("Today vs plan", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                         color = AppOnSurface)
                    Text("Target estimated from daily goal",
                         fontSize = 10.sp, color = AppMuted)
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnTrack) TagGreenBg else TagOrangeBg
                ) {
                    Text(
                        text     = if (isOnTrack) "On track" else "Over target",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color    = if (isOnTrack) TagGreenText else TagOrangeText,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2 × 2 grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroProgressItem(
                    label  = "Protein",
                    logged = proteinLogged,
                    target = targets.protein,
                    unit   = "g",
                    color  = MacroProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressItem(
                    label  = "Carbs",
                    logged = carbsLogged,
                    target = targets.carbs,
                    unit   = "g",
                    color  = MacroCarbs,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroProgressItem(
                    label  = "Fat",
                    logged = fatLogged,
                    target = targets.fat,
                    unit   = "g",
                    color  = MacroFat,
                    modifier = Modifier.weight(1f)
                )
                MacroProgressItem(
                    label  = "Calories",
                    logged = caloriesLogged,
                    target = targets.calories,
                    unit   = "kcal",
                    color  = MacroCalories,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroProgressItem(
    label: String,
    logged: Int,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (logged.toFloat() / target).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Bottom
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = AppOnSurface.copy(alpha = 0.75f))
            Text(
                text  = "$logged / ${target}$unit",
                fontSize = 11.sp,
                color = AppMuted
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        LinearProgressIndicator(
            progress       = { progress },
            modifier       = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color          = color,
            trackColor     = color.copy(alpha = 0.12f)
        )
    }
}

// ── AI insight strip ───────────────────────────────────────────────────────────

@Composable
private fun AiInsightStrip() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .clickable { /* Phase 4/5: open AI overlay */ }
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier          = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(TagPurpleBg),
            contentAlignment  = Alignment.Center
        ) {
            Text("✦", fontSize = 13.sp, color = AIAccent)
        }
        Text(
            text     = "AI Coach available in Phase 4/5 — protein on track 💪",
            fontSize = 12.sp,
            color    = AppOnSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("›", fontSize = 16.sp, color = AppSubtle)
    }
}

// ── Today's slots section ──────────────────────────────────────────────────────

@Composable
private fun TodaySlotsSection(
    slots: List<TodayPlanSlot>,
    hasDietToday: Boolean,
    isTodayCompleted: Boolean,
    onPlanOrChangeDiet: () -> Unit,
    onSlotToggle: (TodayPlanSlot) -> Unit,
    onSlotTap: (TodayPlanSlot) -> Unit,
    onFinishDay: () -> Unit,
    onReopenDay: () -> Unit
) {
    val loggedCount  = slots.count { it.isLogged }
    val totalCount   = slots.size
    val allLogged    = totalCount > 0 && loggedCount == totalCount

    // Section header row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text      = "Today",
            fontSize  = 10.sp,
            fontWeight = FontWeight.Bold,
            color     = AppMuted,
            letterSpacing = 0.8.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Counter label — hidden when day is complete
            AnimatedVisibility(visible = !isTodayCompleted) {
                Text(
                    text      = if (totalCount > 0) "$loggedCount / $totalCount logged" else "No diet planned",
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color     = AppMuted,
                    modifier  = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(TagGreyBg)
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
            // Completion tick: gray when not all logged, green when all logged or day completed
            val tickEnabled = allLogged || isTodayCompleted
            val tickBg     = if (tickEnabled) StatusSuccess else AppBorder.copy(alpha = 0.4f)
            val tickIcon   = if (tickEnabled) AppSurface else AppBorder
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(tickBg)
                    .then(
                        if (tickEnabled) Modifier.clickable {
                            if (isTodayCompleted) onReopenDay() else onFinishDay()
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = if (isTodayCompleted) "Day complete – tap to reopen"
                                         else if (allLogged) "Mark day complete"
                                         else "Not all meals logged",
                    tint     = tickIcon,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }

    if (!hasDietToday || slots.isEmpty()) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No diet planned for today", fontSize = 13.sp, color = AppMuted)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onPlanOrChangeDiet) {
                    Text("Assign a diet →", color = StatusSuccess, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        return
    }

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column {
            slots.forEachIndexed { index, slot ->
                TodaySlotItem(
                    slot         = slot,
                    isCompleted  = isTodayCompleted,
                    onToggle     = { onSlotToggle(slot) },
                    onTap        = {
                        if (slot.dietId != null && slot.plannedMealId != null) onSlotTap(slot)
                    }
                )
                if (index < slots.lastIndex) {
                    HorizontalDivider(
                        color     = AppDivider,
                        thickness = 0.5.dp,
                        modifier  = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TodaySlotItem(
    slot: TodayPlanSlot,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onTap: () -> Unit
) {
    val accent       = slotAccent(slot.slotType)
    val hasIngredients = slot.plannedFoods.isNotEmpty()
    var expanded     by remember(slot.slotType) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (hasIngredients) expanded = !expanded else onTap()
            }
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Coloured dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (slot.isLogged) accent else AppBorder)
            )
            // Slot content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = slot.plannedMealName ?: slot.slotDisplayName,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (slot.isLogged) AppOnSurface else AppMuted,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = buildString {
                        append(slot.slotDisplayName)
                        if (slot.isLogged) append(" · Logged ✓") else append(" · Not logged")
                        if (hasIngredients) append(" · tap to ${if (expanded) "collapse" else "see ingredients"}")
                    },
                    fontSize = 11.sp,
                    color    = if (slot.isLogged) StatusSuccess else AppSubtle
                )
            }
            // Calorie indicator
            Text(
                text       = "${slot.plannedFoods.sumOf { it.calculatedCalories }.toInt()} kcal",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                color      = if (slot.isLogged) AppMuted else AppSubtle
            )
            // Log / un-log tick
            val canToggle = (slot.plannedFoods.isNotEmpty() || slot.isLogged) && !isCompleted
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (slot.isLogged) TagGreenBg else TagGreyBg)
                    .then(if (canToggle) Modifier.clickable(onClick = onToggle) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = if (slot.isLogged) "Logged" else "Not logged",
                    tint     = if (slot.isLogged) StatusSuccess else AppSubtle,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // Expandable ingredient chips
        AnimatedVisibility(
            visible = expanded && hasIngredients,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 30.dp, end = 14.dp, bottom = 10.dp)
            ) {
                // Wrap chips — use FlowRow equivalent with wrapping Row trick
                Column {
                    val chunked = slot.plannedFoods.chunked(3)
                    chunked.forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(bottom = 5.dp)
                        ) {
                            rowItems.forEach { food ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AppBackground,
                                    tonalElevation = 0.dp
                                ) {
                                    Text(
                                        text     = "${food.food.name} ${food.mealFoodItem.quantity.toInt()}${food.mealFoodItem.unit}",
                                        fontSize = 11.sp,
                                        color    = AppOnSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── This week mini strip ───────────────────────────────────────────────────────

@Composable
private fun ThisWeekStrip(
    weekDays: List<WeekDayInfo>,
    weekOffset: Int,
    onDayClick: (LocalDate) -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val today = LocalDate.now()
    val displayDays = if (weekDays.isEmpty()) {
        (6 downTo 0).map { WeekDayInfo(today.minusDays(it.toLong()), null, WeekDayState.NO_DATA) }
    } else weekDays

    val weekLabel = if (displayDays.isNotEmpty()) {
        val start = displayDays.first().date
        val end   = displayDays.last().date
        val startMonth = start.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        val endMonth   = end.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
        if (start.month == end.month) "$startMonth ${start.year}" else "$startMonth – $endMonth"
    } else ""

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 10.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(weekLabel, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AppOnSurface)
                Row {
                    IconButton(onClick = onPreviousWeek, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ChevronLeft, null, tint = AppMuted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick  = onNextWeek,
                        enabled  = weekOffset < 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, null,
                             tint = if (weekOffset < 0) AppMuted else AppBorder,
                             modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                displayDays.forEach { info ->
                    val isToday   = info.date == today
                    val dotColor  = when (info.state) {
                        WeekDayState.COMPLETED      -> StatusSuccess
                        WeekDayState.MISSED         -> StatusError
                        WeekDayState.PLANNED_FUTURE -> StatusWarning
                        else                         -> Color.Transparent
                    }
                    Column(
                        modifier            = Modifier
                            .weight(1f)
                            .clickable(enabled = info.state != WeekDayState.NO_DATA) {
                                onDayClick(info.date)
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text      = info.date.dayOfWeek
                                .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .take(1).uppercase(),
                            fontSize  = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color     = if (isToday) AppOnSurface else AppSubtle,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isToday && info.state == WeekDayState.COMPLETED -> StatusSuccess
                                        isToday -> AppOnSurface
                                        else     -> Color.Transparent
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text       = info.date.dayOfMonth.toString(),
                                fontSize   = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color      = when {
                                    isToday -> AppSurface
                                    info.state == WeekDayState.NO_DATA -> AppSubtle
                                    else    -> AppOnSurface
                                },
                                textAlign  = TextAlign.Center
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WeekLegendItem(StatusSuccess, "Logged")
                WeekLegendItem(StatusWarning, "Planned")
                WeekLegendItem(StatusError,   "Missed")
            }
        }
    }
}

@Composable
private fun WeekLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = AppMuted)
    }
}
