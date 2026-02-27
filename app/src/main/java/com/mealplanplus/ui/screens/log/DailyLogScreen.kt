package com.mealplanplus.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.DailyLogWithFoods
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.LoggedFoodWithDetails
import com.mealplanplus.data.model.MealFoodItemWithDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min

// ── Colours ─────────────────────────────────────────────────────────────────
private val TopBarGreen = Color(0xFF2E7D52)
private val CaloriesColor = Color(0xFF4CAF50)
private val CarbsColor = Color(0xFFFF9800)
private val ProteinColor = Color(0xFF2196F3)
private val FatColor = Color(0xFFE91E63)
private val OverColor = Color(0xFFFF9800)

// ── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    date: String?,
    onNavigateBack: () -> Unit,
    onNavigateToMealPicker: (String, String) -> Unit = { _, _ -> }, // kept for NavHost compat
    onNavigateToDietPicker: (String) -> Unit = { _ -> },
    onNavigateHome: () -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: DailyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val expandedSlots = remember { mutableStateOf(setOf(DefaultMealSlot.BREAKFAST.name)) }

    LaunchedEffect(date) { viewModel.setDateFromString(date) }

    LaunchedEffect(uiState.finishCompleted) {
        if (uiState.finishCompleted) {
            val result = snackbarHostState.showSnackbar(
                message = "Day completed!",
                actionLabel = "Go Home",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onNavigateHome()
            viewModel.clearFinishCompleted()
        }
    }

    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<Long>("selected_diet_id")?.let { dietId ->
            val selectedDate = savedStateHandle.get<String>("selected_date")
            viewModel.applyDietById(dietId, selectedDate)
            savedStateHandle.remove<Long>("selected_diet_id")
            savedStateHandle.remove<String>("selected_date")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            FoodLogTopBar(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onNavigateToDietPicker = onNavigateToDietPicker,
                onClear = { viewModel.clearPlan() },
                onFinish = { viewModel.finishPlan() },
                onReopen = { viewModel.reopenPlan() },
                onToday = { viewModel.goToToday() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF0F9F4))
        ) {
            DateNavigatorPill(
                date = uiState.date,
                onPrevious = { viewModel.goToPreviousDay() },
                onNext = { viewModel.goToNextDay() }
            )
            MacroSummaryCard(
                comparison = uiState.comparison,
                selectedTab = uiState.selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CaloriesColor)
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> DailyLogTab(
                        logWithFoods = uiState.logWithFoods,
                        plannedDiet = uiState.plannedDiet,
                        expandedSlots = expandedSlots.value,
                        onToggleSlot = { slot ->
                            expandedSlots.value = if (slot.name in expandedSlots.value)
                                expandedSlots.value - slot.name
                            else
                                expandedSlots.value + slot.name
                        },
                        onAddFood = { slot -> viewModel.showFoodPickerFor(slot) },
                        onDeleteFood = { id -> viewModel.deleteLoggedFood(id) },
                        onToggleSlotLogged = { slot -> viewModel.toggleSlotLogged(slot) }
                    )
                    1 -> PlanVsActualTab(comparison = uiState.comparison)
                }
            }
        }

        if (uiState.showFoodPicker) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.hideFoodPicker() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                FoodPickerSheetContent(
                    slot = uiState.selectedSlot ?: DefaultMealSlot.BREAKFAST,
                    foods = allFoods,
                    onLogFood = { foodId, qty -> viewModel.logFood(foodId, qty) },
                    onDismiss = { viewModel.hideFoodPicker() }
                )
            }
        }
    }
}

// ── Top Bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodLogTopBar(
    uiState: DailyLogUiState,
    onNavigateBack: () -> Unit,
    onNavigateToDietPicker: (String) -> Unit,
    onClear: () -> Unit,
    onFinish: () -> Unit,
    onReopen: () -> Unit,
    onToday: () -> Unit
) {
    val plan = uiState.planForDate
    val today = LocalDate.now()
    val isCompleted = plan?.isCompleted == true
    val canFinish = plan != null && !isCompleted &&
            (uiState.date == today || uiState.date.isBefore(today))

    TopAppBar(
        title = { Text("Food Log", fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { onNavigateToDietPicker(uiState.date.toString()) }) {
                Icon(Icons.Default.DateRange, contentDescription = "Diet", tint = Color.White)
            }
            if (plan != null && !isCompleted) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color.White)
                }
            }
            if (canFinish) {
                IconButton(onClick = onFinish) {
                    Icon(Icons.Default.Check, contentDescription = "Finish", tint = Color.White)
                }
            }
            if (isCompleted) {
                IconButton(onClick = onReopen) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reopen", tint = Color.White)
                }
            }
            if (uiState.date != today) {
                TextButton(onClick = onToday) { Text("Today", color = Color.White) }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = TopBarGreen,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

// ── Date Navigator Pill ───────────────────────────────────────────────────────

@Composable
fun DateNavigatorPill(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val label = if (isToday) "Today · ${date.format(formatter)}" else date.format(formatter)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day", tint = TopBarGreen)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1B5E20)
            )
            IconButton(onClick = onNext, enabled = !isToday) {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Next day",
                    tint = if (isToday) Color.LightGray else TopBarGreen
                )
            }
        }
    }
}

// ── Macro Summary Card ────────────────────────────────────────────────────────

@Composable
fun MacroSummaryCard(
    comparison: MacroComparison,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroTile("Calories", comparison.actualCalories, "kcal", comparison.plannedCalories, CaloriesColor)
                MacroTile("Carbs", comparison.actualCarbs, "g", comparison.plannedCarbs, CarbsColor)
                MacroTile("Protein", comparison.actualProtein, "g", comparison.plannedProtein, ProteinColor)
                MacroTile("Fat", comparison.actualFat, "g", comparison.plannedFat, FatColor)
            }
            Spacer(Modifier.height(12.dp))
            // Tab toggle pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF5F5F5))
            ) {
                TabToggleButton("Daily Log", selectedTab == 0, Modifier.weight(1f)) { onTabSelected(0) }
                TabToggleButton("Plan vs Actual", selectedTab == 1, Modifier.weight(1f)) { onTabSelected(1) }
            }
        }
    }
}

@Composable
private fun TabToggleButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) TopBarGreen else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else Color.Gray,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun MacroTile(label: String, actual: Int, unit: String, planned: Int, color: Color) {
    val fraction = if (planned > 0) min(1f, actual.toFloat() / planned.toFloat()) else 0f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text("$actual", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        Text(unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

// ── Daily Log Tab ─────────────────────────────────────────────────────────────

@Composable
fun DailyLogTab(
    logWithFoods: DailyLogWithFoods?,
    plannedDiet: DietWithMeals? = null,
    expandedSlots: Set<String>,
    onToggleSlot: (DefaultMealSlot) -> Unit,
    onAddFood: (DefaultMealSlot) -> Unit,
    onDeleteFood: (Long) -> Unit,
    onToggleSlotLogged: ((DefaultMealSlot) -> Unit)? = null
) {
    val mainSlots = setOf(
        DefaultMealSlot.BREAKFAST, DefaultMealSlot.LUNCH,
        DefaultMealSlot.DINNER, DefaultMealSlot.EVENING_SNACK
    )
    val foodSlots = logWithFoods?.foods
        ?.mapNotNull { DefaultMealSlot.fromString(it.loggedFood.slotType) }
        ?.toSet() ?: emptySet()
    val plannedSlots = plannedDiet?.meals?.keys
        ?.mapNotNull { DefaultMealSlot.fromString(it) }
        ?.toSet() ?: emptySet()
    val slotsToShow = (mainSlots + foodSlots + plannedSlots).sortedBy { it.order }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(slotsToShow) { slot ->
            val slotFoods = logWithFoods?.foodsForSlot(slot.name) ?: emptyList()
            val plannedItems = plannedDiet?.meals?.get(slot.name)?.items ?: emptyList()
            MealSlotCard(
                slot = slot,
                foods = slotFoods,
                plannedItems = plannedItems,
                isExpanded = slot.name in expandedSlots,
                onToggleExpand = { onToggleSlot(slot) },
                onAddFood = { onAddFood(slot) },
                onDeleteFood = onDeleteFood,
                onToggleSlotLogged = onToggleSlotLogged?.let { fn -> { fn(slot) } }
            )
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Meal Slot Card ─────────────────────────────────────────────────────────────

@Composable
fun MealSlotCard(
    slot: DefaultMealSlot,
    foods: List<LoggedFoodWithDetails>,
    plannedItems: List<MealFoodItemWithDetails> = emptyList(),
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddFood: () -> Unit,
    onDeleteFood: (Long) -> Unit,
    onToggleSlotLogged: (() -> Unit)? = null
) {
    val loggedKcal = foods.sumOf { it.calculatedCalories }.toInt()
    val plannedKcal = plannedItems.sumOf { it.calculatedCalories }.toInt()
    val subtitle = when {
        foods.isNotEmpty() && plannedItems.isNotEmpty() ->
            "${foods.size} logged · ${plannedItems.size} planned · $loggedKcal kcal"
        foods.isNotEmpty() -> "${foods.size} logged · $loggedKcal kcal"
        plannedItems.isNotEmpty() -> "${plannedItems.size} planned · $plannedKcal kcal"
        else -> "Nothing logged"
    }
    val color = slotColor(slot)
    val isSlotLogged = foods.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(slotEmoji(slot), style = MaterialTheme.typography.titleSmall)
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(slot.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                // Slot-level tick toggle (only when diet is assigned for this slot)
                if (onToggleSlotLogged != null && plannedItems.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isSlotLogged) CaloriesColor else Color(0xFFE0E0E0))
                            .clickable { onToggleSlotLogged() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSlotLogged) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Logged – tap to undo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    if (foods.isEmpty() && plannedItems.isEmpty()) {
                        Text(
                            "No foods logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                    // Planned items (grey circle) — shown when diet assigned
                    plannedItems.forEach { item ->
                        PlannedFoodRow(item = item)
                        HorizontalDivider(color = Color(0xFFF8F8F8), thickness = 0.5.dp)
                    }
                    // Individually logged foods (green tick)
                    foods.forEach { food ->
                        FoodRow(food = food, onDelete = { onDeleteFood(food.loggedFood.id) })
                        HorizontalDivider(color = Color(0xFFF8F8F8), thickness = 0.5.dp)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        TextButton(onClick = onAddFood) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = TopBarGreen)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Food", color = TopBarGreen, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodRow(food: LoggedFoodWithDetails, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(CaloriesColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    food.food.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                food.food.glycemicIndex?.let { gi ->
                    Spacer(Modifier.width(6.dp))
                    GiBadge(gi)
                }
            }
            Text(
                "${food.loggedFood.quantity.toInt()}g · ${food.calculatedCarbs.toInt()}g carbs",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            "${food.calculatedCalories.toInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp), tint = Color.Gray)
        }
    }
}

@Composable
fun PlannedFoodRow(item: MealFoodItemWithDetails) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Circle, contentDescription = null, tint = Color.Gray.copy(alpha = 0.4f), modifier = Modifier.size(8.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.food.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                item.food.glycemicIndex?.let { gi ->
                    Spacer(Modifier.width(6.dp))
                    GiBadge(gi)
                }
            }
            Text(
                "${item.mealFoodItem.quantity.toInt()}g · ${item.calculatedCarbs.toInt()}g carbs",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray.copy(alpha = 0.7f)
            )
        }
        Text(
            "${item.calculatedCalories.toInt()} kcal",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(Modifier.width(36.dp)) // align with FoodRow delete button space
    }
}

@Composable
fun GiBadge(gi: Int) {
    val bgColor = when {
        gi <= 55 -> Color(0xFF4CAF50)
        gi <= 69 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
    Text(
        text = "GI $gi",
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

// ── Plan vs Actual Tab ────────────────────────────────────────────────────────

@Composable
fun PlanVsActualTab(comparison: MacroComparison) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendItem(Color.Gray.copy(alpha = 0.4f), "Planned")
                        LegendItem(CaloriesColor, "Actual")
                        LegendItem(OverColor, "Over target")
                    }
                    Spacer(Modifier.height(16.dp))
                    MacroComparisonRow("Calories", comparison.actualCalories, comparison.plannedCalories, CaloriesColor, "kcal")
                    MacroComparisonRow("Carbs", comparison.actualCarbs, comparison.plannedCarbs, CarbsColor, "g")
                    MacroComparisonRow("Protein", comparison.actualProtein, comparison.plannedProtein, ProteinColor, "g")
                    MacroComparisonRow("Fat", comparison.actualFat, comparison.plannedFat, FatColor, "g")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun MacroComparisonRow(label: String, actual: Int, planned: Int, color: Color, unit: String) {
    val diff = actual - planned
    val isOver = diff > 0 && planned > 0
    val barColor = if (isOver) OverColor else color
    val fraction = if (planned > 0) min(1f, actual.toFloat() / planned.toFloat()) else 0f

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            Text("Plan: $planned$unit", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Spacer(Modifier.width(8.dp))
            Text("$actual$unit", style = MaterialTheme.typography.labelSmall, color = barColor, fontWeight = FontWeight.SemiBold)
            if (planned > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    if (diff >= 0) "+$diff" else "$diff",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOver) OverColor else CaloriesColor
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Gray.copy(alpha = 0.15f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(barColor)
            )
        }
    }
}

// ── Food Picker Sheet ─────────────────────────────────────────────────────────

@Composable
fun FoodPickerSheetContent(
    slot: DefaultMealSlot,
    foods: List<FoodItem>,
    onLogFood: (foodId: Long, quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var quantityText by remember { mutableStateOf("100") }

    val filtered = remember(searchText, foods) {
        if (searchText.isEmpty()) foods
        else foods.filter { it.name.contains(searchText, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
    ) {
        if (selectedFood == null) {
            Text(
                "Add Food to ${slot.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search foods…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(filtered, key = { it.id }) { food ->
                    ListItem(
                        headlineContent = { Text(food.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = {
                            Text(
                                "${food.caloriesPer100.toInt()} kcal/100g · P:${food.proteinPer100.toInt()}g C:${food.carbsPer100.toInt()}g",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        },
                        trailingContent = { food.glycemicIndex?.let { GiBadge(it) } },
                        modifier = Modifier.clickable { selectedFood = food }
                    )
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { selectedFood = null; quantityText = "100" }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    selectedFood!!.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            val qty = quantityText.toDoubleOrNull() ?: 100.0
            val food = selectedFood!!
            Text(
                "${(food.caloriesPer100 * qty / 100).toInt()} kcal · P:${(food.proteinPer100 * qty / 100).toInt()}g C:${(food.carbsPer100 * qty / 100).toInt()}g F:${(food.fatPer100 * qty / 100).toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Quantity (grams)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onLogFood(food.id, qty) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = TopBarGreen)
            ) {
                Text("Log Food", color = Color.White)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

fun slotEmoji(slot: DefaultMealSlot): String = when (slot) {
    DefaultMealSlot.EARLY_MORNING -> "🌙"
    DefaultMealSlot.BREAKFAST -> "🌅"
    DefaultMealSlot.MID_MORNING -> "☕"
    DefaultMealSlot.NOON -> "☀️"
    DefaultMealSlot.LUNCH -> "🍽️"
    DefaultMealSlot.PRE_WORKOUT -> "💪"
    DefaultMealSlot.EVENING -> "🌆"
    DefaultMealSlot.EVENING_SNACK -> "🍎"
    DefaultMealSlot.POST_WORKOUT -> "🥤"
    DefaultMealSlot.DINNER -> "🌙"
    DefaultMealSlot.POST_DINNER -> "🍵"
}

fun slotColor(slot: DefaultMealSlot): Color = when (slot) {
    DefaultMealSlot.BREAKFAST -> Color(0xFFFF9800)
    DefaultMealSlot.LUNCH -> Color(0xFF2196F3)
    DefaultMealSlot.DINNER -> Color(0xFF9C27B0)
    DefaultMealSlot.EVENING_SNACK -> Color(0xFF4CAF50)
    DefaultMealSlot.EARLY_MORNING -> Color(0xFF607D8B)
    DefaultMealSlot.MID_MORNING -> Color(0xFF795548)
    DefaultMealSlot.NOON -> Color(0xFFFFC107)
    DefaultMealSlot.PRE_WORKOUT -> Color(0xFFF44336)
    DefaultMealSlot.EVENING -> Color(0xFF3F51B5)
    DefaultMealSlot.POST_WORKOUT -> Color(0xFF009688)
    DefaultMealSlot.POST_DINNER -> Color(0xFF607D8B)
}
