package com.mealplanplus.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.mealplanplus.util.toEpochMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlin.math.roundToInt

private val DarkGreen = Color(0xFF2E7D52)
private val LightGreenBg = Color(0xFFE8F5E9)
private val YellowBg = Color(0xFFFFFDE7)
private val PlannedYellow = Color(0xFFFFF9C4)
private val CompletedGreen = Color(0xFF2E7D52)
private val TagPurple = Color(0xFF7B1FA2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLog: (String) -> Unit,
    onNavigateToDietPicker: (String) -> Unit = {},
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
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

    Column(modifier = Modifier.fillMaxSize()) {
        // Dark green header
        MealPlanTopBar(
            onSelectDietForToday = {
                viewModel.selectDate(LocalDate.now())
                onNavigateToDietPicker(LocalDate.now().toString())
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp)
        ) {
            // Calendar card
            CalendarCard(
                currentMonth = uiState.currentMonth,
                selectedDate = uiState.selectedDate,
                plans = uiState.plans,
                dietNames = uiState.dietNames,
                isWeekView = uiState.isWeekView,
                onDateSelected = { viewModel.selectDate(it) },
                onPreviousMonth = { viewModel.goToPreviousMonth() },
                onNextMonth = { viewModel.goToNextMonth() },
                onToggleView = { viewModel.toggleView() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            val isPlanCompleted = uiState.plans[uiState.selectedDate.toEpochMs()]?.isCompleted ?: false

            // Selected date detail panel
            SelectedDatePanel(
                date = uiState.selectedDate,
                diet = uiState.selectedDiet,
                dietWithMeals = uiState.selectedDietWithMeals,
                tags = uiState.selectedDietTags,
                isPlanCompleted = isPlanCompleted,
                todayLoggedSlots = uiState.todayLoggedSlots,
                onAssignDiet = { onNavigateToDietPicker(uiState.selectedDate.toString()) },
                onChangeDiet = { onNavigateToDietPicker(uiState.selectedDate.toString()) },
                onRemoveDiet = { viewModel.clearPlan() },
                onViewLog = { onNavigateToLog(uiState.selectedDate.toString()) },
                onSlotToggle = { slotType -> viewModel.toggleSlotLogged(slotType) },
                onNavigateToMealDetail = onNavigateToMealDetail,
                onToggleFavourite = { diet -> viewModel.toggleFavourite(diet) },
                onShowGroceries = { viewModel.generateGroceriesForDiet() },
                isGeneratingGroceries = uiState.isGeneratingGroceries
            )
        }
    }
}

@Composable
private fun MealPlanTopBar(onSelectDietForToday: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkGreen)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Meal Plan",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        OutlinedButton(
            onClick = onSelectDietForToday,
            modifier = Modifier.align(Alignment.CenterEnd),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor = Color.White
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Select Diet", style = MaterialTheme.typography.labelMedium)
        }
    }
}

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
    // For week view, track which week's Monday we're showing (start of week containing selectedDate)
    val weekStart = remember(selectedDate) {
        val dow = (selectedDate.dayOfWeek.value - 1).toLong() // Monday=0
        selectedDate.minusDays(dow)
    }
    // Derive header label from week view context
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
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev arrow: navigate week (week view) or hidden (month view)
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

                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Next arrow: navigate week (week view) or hidden (month view)
                    if (isWeekView) {
                        IconButton(
                            onClick = { onDateSelected(selectedDate.plusWeeks(1)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next week", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    // Toggle pill: label shows what you'll switch TO
                    Box(
                        modifier = Modifier
                            .border(1.dp, DarkGreen, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onToggleView() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = DarkGreen
                            )
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
                LegendItem(color = CompletedGreen, label = "Completed")
                LegendItem(color = Color(0xFFFFC107), label = "Planned")
                LegendItem(color = DarkGreen, label = "Today", isOutline = true)
                LegendItem(color = MaterialTheme.colorScheme.outlineVariant, label = "No plan")
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
                dietName = dietNames[dateMs],
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
                            dietName = dietName,
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
    showCheckbox: Boolean = false,
    isLogged: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    val (emoji, tint) = slotEmojiAndColor(slot)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
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
}

@Composable
private fun CustomMealSlotRow(
    displayName: String,
    mealName: String?,
    showCheckbox: Boolean = false,
    isLogged: Boolean = false,
    onToggle: (() -> Unit)? = null,
    onTap: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onTap != null) Modifier.clickable { onTap() } else Modifier)
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
}

/** Green filled circle with checkmark when logged, empty gray circle when not. */
@Composable
private fun SlotCheckCircle(isLogged: Boolean, onToggle: (() -> Unit)? = null) {
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
private fun GrocerySnapshotSheet(
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

private fun formatQuantity(qty: Double): String {
    val rounded = (qty * 10).toLong().toDouble() / 10
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
