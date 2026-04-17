package com.mealplanplus.ui.screens.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.mealplanplus.data.model.*
import com.mealplanplus.ui.components.CalendarDayCell
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DarkGreen
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.LocalIsDarkTheme
import com.mealplanplus.ui.theme.TagPurple
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.util.toEpochMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlin.math.roundToInt

// Slot dot colours
internal fun planSlotColor(slotName: String): Color = when (slotName.uppercase()) {
    "BREAKFAST"    -> Color(0xFFF59E0B)
    "NOON"         -> Color(0xFF888888)
    "LUNCH"        -> Color(0xFF2E7D52)
    "DINNER"       -> Color(0xFF7C3AED)
    "EVENING_SNACK"-> Color(0xFF2196F3)
    "EARLY_MORNING"-> Color(0xFF607D8B)
    "MID_MORNING"  -> Color(0xFFF59E0B)
    "PRE_WORKOUT"  -> Color(0xFFF44336)
    "EVENING"      -> Color(0xFF3F51B5)
    "POST_WORKOUT" -> Color(0xFF009688)
    "POST_DINNER"  -> Color(0xFF607D8B)
    else           -> Color(0xFF888888)
}

// Calendar-specific background tints — adapt for dark mode
private val LightGreenBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF1A2D23) else Color(0xFFE8F5E9)
private val YellowBg: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2D2100) else Color(0xFFFFFDE7)
private val PlannedYellow: Color
    @Composable get() = if (LocalIsDarkTheme.current) Color(0xFF2A1E00) else Color(0xFFFFF9C4)
private val CompletedGreen: Color
    @Composable get() = DesignGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLog: (String) -> Unit,
    onNavigateToDietPicker: (String) -> Unit = {},
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
    onNavigateToDayDetail: (LocalDate) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Observe diet selection result from DietPickerScreen
    val pickedDietId by (savedStateHandle
        ?.getStateFlow("selected_diet_id", -1L)
        ?.collectAsState() ?: remember { mutableStateOf(-1L) })
    LaunchedEffect(pickedDietId) {
        if (pickedDietId != -1L) {
            viewModel.assignDietById(pickedDietId)
            savedStateHandle?.set("selected_diet_id", -1L)
        }
    }

    // Grocery snapshot bottom sheet
    if (uiState.grocerySnapshot != null) {
        GrocerySnapshotSheet(
            dietName = uiState.selectedDiet?.name ?: "",
            items = uiState.grocerySnapshot!!,
            onDismiss = { viewModel.clearGrocerySnapshot() }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(BgPage)) {
        MealPlanTopBar(
            onSelectDietForToday = {
                viewModel.selectDate(LocalDate.now())
                onNavigateToDietPicker(LocalDate.now().toString())
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Mini calendar ──────────────────────────────────────────────
            item {
                PlanMiniCalendar(
                    currentMonth = uiState.currentMonth,
                    selectedDate = uiState.selectedDate,
                    plans = uiState.plans,
                    onDateSelected = { date ->
                        viewModel.selectDate(date)
                        onNavigateToDayDetail(date)
                    },
                    onPreviousMonth = { viewModel.goToPreviousMonth() },
                    onNextMonth = { viewModel.goToNextMonth() }
                )
            }

            // ── Upcoming list ──────────────────────────────────────────────
            item {
                UpcomingSection(
                    plans = uiState.plans,
                    dietNames = uiState.dietNames,
                    selectedDate = uiState.selectedDate,
                    onDaySelected = { date ->
                        viewModel.selectDate(date)
                        onNavigateToDayDetail(date)
                    },
                    onAssignDiet = { date -> onNavigateToDietPicker(date.toString()) }
                )
            }
        }
    }
}

@Composable
private fun MealPlanTopBar(onSelectDietForToday: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Text(
            text = "Plan",
            color = Color(0xFF111111),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        OutlinedButton(
            onClick = onSelectDietForToday,
            modifier = Modifier.align(Alignment.CenterEnd),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = DarkGreen
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkGreen),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text("Select Diet", style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Mini Calendar ─────────────────────────────────────────────────────────────

@Composable
private fun PlanMiniCalendar(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthLabel = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp, bottom = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "‹",
                    fontSize = 20.sp,
                    color = TextMuted,
                    modifier = Modifier.clickable(onClick = onPreviousMonth).padding(4.dp)
                )
                Text(
                    text = monthLabel,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "›",
                    fontSize = 20.sp,
                    color = TextMuted,
                    modifier = Modifier.clickable(onClick = onNextMonth).padding(4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Day-of-week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Text(
                        text = d,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Calendar grid
            PlanCalendarGrid(
                month = currentMonth,
                selectedDate = selectedDate,
                plans = plans,
                onDateSelected = onDateSelected
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(Modifier.height(10.dp))

            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlanLegendDot(Color(0xFF2E7D52), "Diet planned")
                PlanLegendDot(Color(0xFF1E4FBF), "Workout")
                PlanLegendDot(Color(0xFFE53E3E), "Missed log")
            }
        }
    }
}

@Composable
private fun PlanLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = TextSecondary)
    }
}

@Composable
private fun PlanCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    onDateSelected: (LocalDate) -> Unit
) {
    val today       = LocalDate.now()
    val firstDay    = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val totalCells  = startOffset + daysInMonth
    val rows        = (totalCells + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNumber = row * 7 + col - startOffset + 1
                    if (dayNumber in 1..daysInMonth) {
                        val date   = month.atDay(dayNumber)
                        val dateMs = date.toEpochMs()
                        val plan   = plans[dateMs]
                        val hasPlan = plan != null && plan.dietId != null
                        CalendarDayCell(
                            day        = dayNumber,
                            isSelected = date == selectedDate,
                            isToday    = date == today,
                            hasPlan    = hasPlan,
                            isCompleted = plan?.isCompleted ?: false,
                            isPast     = date.isBefore(today),
                            onClick    = { onDateSelected(date) },
                            modifier   = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Upcoming Section ──────────────────────────────────────────────────────────

@Composable
private fun UpcomingSection(
    plans: Map<Long, Plan>,
    dietNames: Map<Long, String>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    onAssignDiet: (LocalDate) -> Unit
) {
    val today     = LocalDate.now()
    val upcoming  = (0..6).map { today.plusDays(it.toLong()) }
    val dayFmt    = java.time.format.DateTimeFormatter.ofPattern("EEE").withLocale(Locale.getDefault())

    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 4.dp)) {
        Text(
            text = "UPCOMING",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.8.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            upcoming.forEachIndexed { index, date ->
                val dateMs   = date.toEpochMs()
                val plan     = plans[dateMs]
                val hasPlan  = plan != null && plan.dietId != null
                val dietName = dietNames[dateMs]
                val isToday  = date == today
                val isSelected = date == selectedDate

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isSelected) Color(0xFFF7F7F7) else Color.Transparent)
                        .clickable { onDaySelected(date) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Date column
                    Column(
                        modifier = Modifier.width(36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = date.format(dayFmt).uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!hasPlan && !isToday) TextMuted else TextPrimary,
                            lineHeight = 18.sp
                        )
                    }

                    // Diet info
                    Column(modifier = Modifier.weight(1f)) {
                        if (hasPlan && dietName != null) {
                            Text(dietName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            // Meal preview from dietNames if available, otherwise empty
                            Text("Tap to see meals", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(top = 1.dp))
                        } else {
                            Text("Not planned", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                            Text("Tap to assign a diet", fontSize = 11.sp, color = Color(0xFFDDDDDD), modifier = Modifier.padding(top = 1.dp))
                        }
                    }

                    // Tag / action
                    when {
                        isToday -> Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F5EE))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                        }
                        !hasPlan -> Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF5F5F5))
                                .clickable { onAssignDiet(date) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+ Plan", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC05200))
                        }
                    }
                }

                if (index < upcoming.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = Color(0xFFF5F5F5)
                    )
                }
            }
        }
    }
}

// Keep the old CalendarCard name as a no-op so nothing else breaks
@Composable
private fun CalendarCard(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    dietNames: Map<Long, String>,
    isWeekView: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleView: () -> Unit
) {
    // Replaced by PlanMiniCalendar — kept for compile compatibility
    PlanMiniCalendar(currentMonth, selectedDate, plans, onDateSelected, onPreviousMonth, onNextMonth)
}

@Composable
private fun __OldCalendarCardInternals_Unused(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    dietNames: Map<Long, String>,
    isWeekView: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onToggleView: () -> Unit
) {
    // Legacy internals removed
    val weekStart = remember(selectedDate) {
        val dow = (selectedDate.dayOfWeek.value - 1).toLong()
        selectedDate.minusDays(dow)
    }
    val headerText = if (isWeekView) {
        val weekEnd = weekStart.plusDays(6)
        if (weekStart.month == weekEnd.month)
            "${weekStart.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${weekStart.year}"
        else
            "${weekStart.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} – ${weekEnd.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${weekEnd.year}"
    } else {
        "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWeekView) {
                    IconButton(
                        onClick = { onDateSelected(selectedDate.minusWeeks(1)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev week", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.width(32.dp))
                }
                Text(text = headerText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isWeekView) {
                        IconButton(
                            onClick = { onDateSelected(selectedDate.plusWeeks(1)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next week", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleView() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp), tint = DarkGreen)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isWeekView) "Month" else "Week",
                                style = MaterialTheme.typography.labelSmall,
                                color = DarkGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Day of week headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            if (isWeekView) {
                // Single week row: 7 days starting from weekStart
                WeekRow(
                    weekStart = weekStart,
                    selectedDate = selectedDate,
                    plans = plans,
                    dietNames = dietNames,
                    onDateSelected = onDateSelected
                )
            } else {
                // Full month grid
                MealPlanCalendarGrid(
                    month = currentMonth,
                    selectedDate = selectedDate,
                    plans = plans,
                    dietNames = dietNames,
                    onDateSelected = onDateSelected
                )
            }

            Spacer(Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                // Dead code — kept here only so the outer closing braces remain balanced:
                @Suppress("UNUSED_EXPRESSION") Unit
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isOutline: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .then(
                    if (isOutline) Modifier.border(1.5.dp, color, CircleShape)
                    else Modifier.background(color)
                )
        )
        Spacer(Modifier.width(3.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeekRow(
    weekStart: LocalDate,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    dietNames: Map<Long, String>,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (i in 0..6) {
            val date = weekStart.plusDays(i.toLong())
            val dateMs = date.toEpochMs()
            val plan = plans[dateMs]
            val hasPlan = plan != null && plan.dietId != null
            CalendarDayCell(
                day = date.dayOfMonth,
                isSelected = date == selectedDate,
                isToday = date == LocalDate.now(),
                hasPlan = hasPlan,
                isCompleted = plan?.isCompleted ?: false,
                onClick = { onDateSelected(date) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MealPlanCalendarGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    plans: Map<Long, Plan>,
    dietNames: Map<Long, String>,
    onDateSelected: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1 // Monday = 0
    val daysInMonth = month.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = month.atDay(dayNumber)
                        val dateMs = date.toEpochMs()
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        val plan = plans[dateMs]
                        val hasPlan = plan != null && plan.dietId != null
                        val isCompleted = plan?.isCompleted ?: false
                        val dietName = dietNames[dateMs]

                        CalendarDayCell(
                            day = dayNumber,
                            isSelected = isSelected,
                            isToday = isToday,
                            hasPlan = hasPlan,
                            isCompleted = isCompleted,
                            isPast = date.isBefore(LocalDate.now()),
                            onClick = { onDateSelected(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Plan Day Detail ───────────────────────────────────────────────────────────

@Composable
internal fun PlanDayDetail(
    date: LocalDate,
    diet: Diet?,
    dietWithMeals: DietWithMeals?,
    tags: List<Tag>,
    isPlanCompleted: Boolean = false,
    todayLoggedSlots: Map<String, Boolean> = emptyMap(),
    onAssignDiet: () -> Unit,
    onChangeDiet: () -> Unit,
    onRemoveDiet: () -> Unit,
    onViewLog: () -> Unit,
    onSlotToggle: (String) -> Unit = {},
    onToggleFavourite: (Diet) -> Unit = {},
    onShowGroceries: () -> Unit = {},
    isGeneratingGroceries: Boolean = false
) {
    val today   = LocalDate.now()
    val isToday = date == today
    val isPast  = date.isBefore(today)
    val isFuture = date.isAfter(today)
    val dateFmt = java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM")

    Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 8.dp)) {
        // Section label
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DAY DETAIL · ${date.format(dateFmt).uppercase()}",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (diet != null) {
                    IconButton(onClick = { onToggleFavourite(diet) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (diet.isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (diet.isFavourite) Color(0xFFFFC107) else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onShowGroceries, enabled = !isGeneratingGroceries, modifier = Modifier.size(32.dp)) {
                        if (isGeneratingGroceries) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                // Diet name header
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Text(
                        text = diet?.name ?: "No diet planned",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (diet != null) TextPrimary else TextMuted
                    )
                    if (diet != null && dietWithMeals != null) {
                        Text(
                            text = "${dietWithMeals.totalCalories.roundToInt()} kcal",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    if (tags.isNotEmpty()) {
                        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.take(2).forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFE8F5EE))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkGreen)
                                }
                            }
                        }
                    }

                    // Action buttons row
                    if (isFuture || (isToday && (diet == null || isPlanCompleted))) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (diet == null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TextPrimary)
                                        .clickable(onClick = onAssignDiet)
                                        .padding(horizontal = 16.dp, vertical = 9.dp)
                                ) {
                                    Text("+ Assign diet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF5F5F5))
                                        .clickable(onClick = onChangeDiet)
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Text("Change", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF666666))
                                }
                                if (isFuture) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFEEEEE))
                                            .clickable(onClick = onRemoveDiet)
                                            .padding(horizontal = 14.dp, vertical = 9.dp)
                                    ) {
                                        Text("Remove", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE53E3E))
                                    }
                                }
                            }
                        }
                    }
                }

                if (diet != null && dietWithMeals != null) {
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                    // Macro row — inline, matching design
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PlanMacroCell("${dietWithMeals.totalCalories.roundToInt()}", "kcal")
                        PlanMacroCell("${dietWithMeals.totalProtein.roundToInt()}g", "protein")
                        PlanMacroCell("${dietWithMeals.totalCarbs.roundToInt()}g", "carbs")
                        PlanMacroCell("${dietWithMeals.totalFat.roundToInt()}g", "fat", isLast = true)
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                    // Meal slots
                    val showCheckboxes = isToday && !isPlanCompleted
                    PlanDietSlots(
                        diet = diet,
                        dietWithMeals = dietWithMeals,
                        showCheckboxes = showCheckboxes,
                        slotLoggedState = if (showCheckboxes) todayLoggedSlots else emptyMap(),
                        onSlotToggle = onSlotToggle
                    )
                } else if (diet == null && (isToday || isFuture)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Select a diet above to plan this day.", fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center)
                    }
                } else if (diet != null) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun RowScope.PlanMacroCell(value: String, label: String, isLast: Boolean = false) {
    Column(
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 1.dp))
    }
}

@Composable
internal fun PlanDietSlots(
    diet: Diet,
    dietWithMeals: DietWithMeals,
    showCheckboxes: Boolean,
    slotLoggedState: Map<String, Boolean>,
    onSlotToggle: (String) -> Unit
) {
    Column {
        DefaultMealSlot.entries.forEach { slot ->
            val mealWithFoods = dietWithMeals.meals[slot.name]
            if (mealWithFoods != null) {
                val isLogged = slotLoggedState[slot.name.uppercase()] == true
                PlanSlotRow(
                    slotName = slot.displayName,
                    slotColor = planSlotColor(slot.name),
                    mealName = mealWithFoods.meal?.name,
                    foods = mealWithFoods.items,
                    kcal = mealWithFoods.items.sumOf { it.calculatedCalories }.toInt(),
                    showCheckbox = showCheckboxes && mealWithFoods.items.isNotEmpty(),
                    isLogged = isLogged,
                    onToggle = if (showCheckboxes) { -> onSlotToggle(slot.name) } else null
                )
                HorizontalDivider(color = Color(0xFFF5F5F5))
            }
        }
        // Custom slots
        dietWithMeals.meals.entries
            .filter { it.key.startsWith("CUSTOM:") }
            .sortedBy { it.key }
            .forEach { (slotType, mealWithFoods) ->
                if (mealWithFoods != null) {
                    val displayName = slotType.removePrefix("CUSTOM:")
                    val isLogged = slotLoggedState[slotType.uppercase()] == true
                    PlanSlotRow(
                        slotName = displayName,
                        slotColor = Color(0xFF888888),
                        mealName = mealWithFoods.meal?.name,
                        foods = mealWithFoods.items,
                        kcal = mealWithFoods.items.sumOf { it.calculatedCalories }.toInt(),
                        showCheckbox = showCheckboxes && mealWithFoods.items.isNotEmpty(),
                        isLogged = isLogged,
                        onToggle = if (showCheckboxes) { -> onSlotToggle(slotType) } else null
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }
    }
}

@Composable
internal fun PlanSlotRow(
    slotName: String,
    slotColor: Color,
    mealName: String?,
    foods: List<MealFoodItemWithDetails>,
    kcal: Int,
    showCheckbox: Boolean,
    isLogged: Boolean,
    onToggle: (() -> Unit)?
) {
    val ingredientPreview = foods.take(5).joinToString(" · ") { it.food.name }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dot
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(slotColor)
        )
        // Slot label (fixed 64dp)
        Text(
            text = slotName.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            color = TextMuted,
            modifier = Modifier.width(64.dp),
            maxLines = 1
        )
        // Meal name + ingredient preview
        Column(modifier = Modifier.weight(1f)) {
            if (mealName != null) {
                Text(mealName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (ingredientPreview.isNotEmpty()) {
                    Text(ingredientPreview, fontSize = 11.sp, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                }
            } else {
                Text("—", fontSize = 13.sp, color = TextMuted)
            }
        }
        // Calories
        if (kcal > 0) {
            Text("$kcal", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
        // Checkbox toggle
        if (showCheckbox) {
            SlotCheckCircle(isLogged = isLogged, onToggle = onToggle)
        }
    }
}

// ── Legacy SelectedDatePanel alias ───────────────────────────────────────────

@Composable
private fun SelectedDatePanel(
    date: LocalDate,
    diet: Diet?,
    dietWithMeals: DietWithMeals?,
    tags: List<Tag>,
    isPlanCompleted: Boolean = false,
    todayLoggedSlots: Map<String, Boolean> = emptyMap(),
    onAssignDiet: () -> Unit,
    onChangeDiet: () -> Unit,
    onRemoveDiet: () -> Unit,
    onViewLog: () -> Unit,
    onSlotToggle: (String) -> Unit = {},
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
    onToggleFavourite: (Diet) -> Unit = {},
    onShowGroceries: () -> Unit = {},
    isGeneratingGroceries: Boolean = false
) {
    val today = LocalDate.now()
    val isToday = date == today
    val isPast = date.isBefore(today)
    val isFuture = date.isAfter(today)

    val panelBg = MaterialTheme.colorScheme.surfaceVariant
    val dateLabel = when {
        isToday -> "Today"
        isPast -> "Past"
        else -> "Upcoming"
    }
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")
    val dateDisplay = "$dateLabel · ${date.format(formatter)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = panelBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateDisplay,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = diet?.name ?: "No diet planned",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (diet != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // First tag chip
                    val firstTag = tags.firstOrNull()
                    if (firstTag != null) {
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(TagPurple.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, TagPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = firstTag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = TagPurple,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Right side: diet actions + star + grocery button
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (diet != null) {
                        // Star / favourite toggle
                        IconButton(onClick = { onToggleFavourite(diet) }, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = if (diet.isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (diet.isFavourite) "Remove from favourites" else "Add to favourites",
                                tint = if (diet.isFavourite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        // Grocery list button
                        IconButton(
                            onClick = onShowGroceries,
                            enabled = !isGeneratingGroceries,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isGeneratingGroceries) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "View grocery list",
                                    tint = DarkGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    when {
                        isToday && diet != null && isPlanCompleted -> TextButton(onClick = onChangeDiet) {
                            Text("Change", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        // Today with an incomplete plan: checkboxes are shown inline — no header button needed
                        isFuture && diet != null -> Button(
                            onClick = onChangeDiet,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium)
                        }
                        isFuture && diet == null -> Button(
                            onClick = onAssignDiet,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("+ Plan", style = MaterialTheme.typography.labelMedium)
                        }
                        // Past dates: diet details are shown read-only inline — no log redirect needed
                    }
                }
            }

            if (diet != null && dietWithMeals != null) {
                // Macros + meals section
                // For today's unfinished plan → show interactive checkboxes, otherwise read-only rows
                val showCheckboxes = isToday && !isPlanCompleted
                DietDetailSection(
                    diet = diet,
                    dietWithMeals = dietWithMeals,
                    showCheckboxes = showCheckboxes,
                    slotLoggedState = if (showCheckboxes) todayLoggedSlots else emptyMap(),
                    onSlotToggle = onSlotToggle,
                    onNavigateToMealDetail = onNavigateToMealDetail
                )

                // Remove diet option (future only)
                if (isFuture) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    TextButton(
                        onClick = onRemoveDiet,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Remove diet from this day",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            } else if (diet == null) {
                // Empty state
                if (isFuture || isToday) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(52.dp),
                            tint = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No diet planned for this day yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (isFuture || isToday) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onAssignDiet,
                                colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                                modifier = Modifier.fillMaxWidth(0.7f),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("+ Plan a Diet")
                            }
                        }
                    }
                }
            } else {
                // Diet assigned but details still loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = DarkGreen, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun DietDetailSection(
    diet: Diet,
    dietWithMeals: DietWithMeals,
    showCheckboxes: Boolean = false,
    slotLoggedState: Map<String, Boolean> = emptyMap(),
    onSlotToggle: (String) -> Unit = {},
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // 4-stat macro tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MacroTile(
                value = "${dietWithMeals.totalCalories.roundToInt()}",
                label = "Total kcal",
                valueColor = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            MacroTile(
                value = "${dietWithMeals.totalProtein.roundToInt()}g",
                label = "Protein",
                valueColor = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            MacroTile(
                value = "${dietWithMeals.totalCarbs.roundToInt()}g",
                label = "Carbs",
                valueColor = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            MacroTile(
                value = "${dietWithMeals.totalFat.roundToInt()}g",
                label = "Fat",
                valueColor = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }

        // Diet description
        diet.description?.let { desc ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))

        // Default meal slots
        DefaultMealSlot.entries.forEach { slot ->
            val mealWithFoods = dietWithMeals.meals[slot.name]
            val hasFoods = mealWithFoods != null && mealWithFoods.items.isNotEmpty()
            val isLogged = slotLoggedState[slot.name.uppercase()] == true
            MealSlotRow(
                slot = slot,
                mealName = mealWithFoods?.meal?.name,
                foods = mealWithFoods?.items ?: emptyList(),
                showCheckbox = showCheckboxes && hasFoods,
                isLogged = isLogged,
                onToggle = if (showCheckboxes && hasFoods) { -> onSlotToggle(slot.name) } else null,
                onTap = if (!showCheckboxes && hasFoods) { -> onNavigateToMealDetail(diet.id, slot.name) } else null
            )
        }

        // Custom diet slots
        dietWithMeals.meals.entries
            .filter { it.key.startsWith("CUSTOM:") }
            .sortedBy { it.key }
            .forEach { (slotType, mealWithFoods) ->
                val displayName = slotType.removePrefix("CUSTOM:")
                val hasFoods = mealWithFoods != null && mealWithFoods.items.isNotEmpty()
                val isLogged = slotLoggedState[slotType.uppercase()] == true
                CustomMealSlotRow(
                    displayName = displayName,
                    mealName = mealWithFoods?.meal?.name,
                    foods = mealWithFoods?.items ?: emptyList(),
                    showCheckbox = showCheckboxes && hasFoods,
                    isLogged = isLogged,
                    onToggle = if (showCheckboxes && hasFoods) { -> onSlotToggle(slotType) } else null,
                    onTap = if (!showCheckboxes && hasFoods) { -> onNavigateToMealDetail(diet.id, slotType) } else null
                )
            }
    }
}

@Composable
private fun MacroTile(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = valueColor.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 13.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MealSlotRow(
    slot: DefaultMealSlot,
    mealName: String?,
    foods: List<MealFoodItemWithDetails> = emptyList(),
    showCheckbox: Boolean = false,
    isLogged: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    val (emoji, tint) = slotEmojiAndColor(slot)
    val canExpand = showCheckbox && foods.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(when {
                    canExpand -> Modifier.clickable { expanded = !expanded }
                    onTap != null -> Modifier.clickable { onTap() }
                    else -> Modifier
                })
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = mealName ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (mealName != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (mealName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canExpand) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide ingredients" else "Show ingredients",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (showCheckbox) {
                SlotCheckCircle(isLogged = isLogged, onToggle = onToggle)
            } else if (onTap != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View ingredients",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = canExpand && expanded) {
            MealIngredientList(foods = foods, indentDp = 48)
        }
    }
}

@Composable
private fun CustomMealSlotRow(
    displayName: String,
    mealName: String?,
    foods: List<MealFoodItemWithDetails> = emptyList(),
    showCheckbox: Boolean = false,
    isLogged: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    val canExpand = showCheckbox && foods.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(when {
                    canExpand -> Modifier.clickable { expanded = !expanded }
                    onTap != null -> Modifier.clickable { onTap() }
                    else -> Modifier
                })
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF7B1FA2).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("⭐", fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF7B1FA2).copy(alpha = 0.10f)
                    ) {
                        Text(
                            "custom",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7B1FA2),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = mealName ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (mealName != null) FontWeight.Medium else FontWeight.Normal,
                    color = if (mealName != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canExpand) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Hide ingredients" else "Show ingredients",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (showCheckbox) {
                SlotCheckCircle(isLogged = isLogged, onToggle = onToggle)
            } else if (onTap != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "View ingredients",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        AnimatedVisibility(visible = canExpand && expanded) {
            MealIngredientList(foods = foods, indentDp = 48)
        }
    }
}

/**
 * Compact ingredient list shown below a meal slot row when the user expands it.
 * [indentDp] aligns the list with the meal name text (past the slot emoji + spacer).
 */
@Composable
private fun MealIngredientList(
    foods: List<MealFoodItemWithDetails>,
    indentDp: Int = 48
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indentDp.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        foods.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item.food.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${item.mealFoodItem.quantity.toInt()}g · ${item.calculatedCalories.toInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/** Green filled circle with checkmark when logged, empty gray circle when not. */
@Composable
internal fun SlotCheckCircle(isLogged: Boolean, onToggle: (() -> Unit)? = null) {
    if (isLogged) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DarkGreen)
                .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Logged – tap to undo",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    } else {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .then(if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier)
        )
    }
}

private fun slotEmojiAndColor(slot: DefaultMealSlot): Pair<String, Color> = when (slot) {
    DefaultMealSlot.EARLY_MORNING -> "🌙" to Color(0xFF9C27B0)
    DefaultMealSlot.BREAKFAST -> "🌅" to Color(0xFFFF9800)
    DefaultMealSlot.MID_MORNING -> "☕" to Color(0xFF795548)
    DefaultMealSlot.NOON -> "☀️" to Color(0xFFFFC107)
    DefaultMealSlot.LUNCH -> "☀️" to Color(0xFFFFC107)
    DefaultMealSlot.PRE_WORKOUT -> "💪" to Color(0xFF2196F3)
    DefaultMealSlot.EVENING -> "🌆" to Color(0xFFFF5722)
    DefaultMealSlot.EVENING_SNACK -> "🍎" to Color(0xFF4CAF50)
    DefaultMealSlot.POST_WORKOUT -> "🥤" to Color(0xFF03A9F4)
    DefaultMealSlot.DINNER -> "🌙" to Color(0xFF3F51B5)
    DefaultMealSlot.POST_DINNER -> "🍵" to Color(0xFF009688)
}

@Composable
fun DietPickerDialog(
    diets: List<Diet>,
    selectedDietId: Long?,
    onSelect: (Diet?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Diet") },
        text = {
            if (diets.isEmpty()) {
                Text("No diets available. Create some diet templates first!")
            } else {
                LazyColumn {
                    items(diets.distinctBy { it.id }, key = { it.id }) { diet ->
                        val isSelected = diet.id == selectedDietId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(diet) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(diet.name, style = MaterialTheme.typography.bodyLarge)
                                if (diet.description != null) {
                                    Text(
                                        diet.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = DarkGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GrocerySnapshotSheet(
    dietName: String,
    items: List<GrocerySnapshotItem>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Grocery List",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (dietName.isNotBlank()) {
                        Text(
                            dietName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkGreen.copy(alpha = 0.1f)
                ) {
                    Text(
                        "${items.size} items",
                        style = MaterialTheme.typography.labelMedium,
                        color = DarkGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            if (items.isEmpty()) {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No ingredients found for this diet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(DarkGreen, CircleShape)
                            )
                            Text(
                                text = item.foodName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${formatQuantity(item.quantity)} ${item.unitLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

internal fun formatQuantity(qty: Double): String {
    val rounded = (qty * 10).toLong().toDouble() / 10
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
