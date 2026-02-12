package com.mealplanplus.ui.screens.log

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.LoggedMealWithDetails
import com.mealplanplus.data.model.Meal
import com.mealplanplus.ui.components.GradientBackground
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    date: String?,
    onNavigateBack: () -> Unit,
    onNavigateToMealPicker: (String, String) -> Unit = { _, _ -> },
    onNavigateToDietPicker: (String) -> Unit = { _ -> },
    onNavigateHome: () -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: DailyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableMeals by viewModel.availableMeals.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(date) {
        viewModel.setDateFromString(date)
    }

    // Handle finish completion - show snackbar and navigate home
    LaunchedEffect(uiState.finishCompleted) {
        if (uiState.finishCompleted) {
            val result = snackbarHostState.showSnackbar(
                message = "Day completed!",
                actionLabel = "Go Home",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateHome()
            }
            viewModel.clearFinishCompleted()
        }
    }

    // Handle meal selection results from LogMealPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.get<Long>("selected_meal_id")?.let { mealId ->
                val quantity = handle.get<Double>("selected_meal_quantity") ?: 1.0
                val slotType = handle.get<String>("selected_slot_type") ?: ""
                viewModel.logMeal(mealId, slotType, quantity)
                handle.remove<Long>("selected_meal_id")
                handle.remove<Double>("selected_meal_quantity")
                handle.remove<String>("selected_slot_type")
            }
            // Handle diet selection from DietPickerScreen
            handle.get<Long>("selected_diet_id")?.let { dietId ->
                val selectedDate = handle.get<String>("selected_date")
                viewModel.applyDietById(dietId, selectedDate)
                handle.remove<Long>("selected_diet_id")
                handle.remove<String>("selected_date")
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Daily Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val plan = uiState.planForDate
                    val today = LocalDate.now()
                    val isCompleted = plan?.isCompleted == true
                    val canFinish = plan != null && !isCompleted &&
                            (uiState.date == today || uiState.date.isBefore(today))

                    // Apply Diet button - navigate to full diet picker
                    TextButton(onClick = { onNavigateToDietPicker(uiState.date.toString()) }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Diet", color = MaterialTheme.colorScheme.onPrimary)
                    }

                    // Clear button - shown when there's a plan that's not completed
                    if (plan != null && !isCompleted) {
                        TextButton(onClick = { viewModel.clearPlan() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    // Finish button - only for today or past dates with uncompleted plan
                    if (canFinish) {
                        TextButton(onClick = { viewModel.finishPlan() }) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Finish", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    // Reopen button - shown when day is completed
                    if (isCompleted) {
                        TextButton(onClick = { viewModel.reopenPlan() }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reopen", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }

                    if (uiState.date != LocalDate.now()) {
                        TextButton(onClick = { viewModel.goToToday() }) {
                            Text("Today", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        GradientBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Date navigation
                DateNavigator(
                date = uiState.date,
                onPrevious = { viewModel.goToPreviousDay() },
                onNext = { viewModel.goToNextDay() }
            )

            // Macro summary with comparison
            MacroComparisonCard(
                comparison = uiState.comparison,
                plannedDietName = uiState.plannedDiet?.diet?.name
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // Meal slots
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(DefaultMealSlot.entries.toList()) { slot ->
                        val slotMeals = uiState.logWithMeals?.mealsForSlot(slot.name) ?: emptyList()
                        MealSlotSection(
                            slot = slot,
                            meals = slotMeals,
                            onAddMeal = {
                                // Navigate to meal picker screen
                                val dateStr = uiState.date.toString()
                                onNavigateToMealPicker(dateStr, slot.name)
                            },
                            onRemoveMeal = { viewModel.deleteLoggedMeal(it.loggedMeal.id) },
                            onUpdateQuantity = { meal, qty ->
                                viewModel.updateMealQuantity(meal.loggedMeal, qty)
                            }
                        )
                    }
                }
            }
        }
        }
    }

}

@Composable
fun DateNavigator(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val isToday = date == LocalDate.now()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isToday) "Today" else date.format(formatter),
                style = MaterialTheme.typography.titleMedium
            )
            if (isToday) {
                Text(
                    text = date.format(formatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
fun MacroComparisonCard(
    comparison: MacroComparison,
    plannedDietName: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            if (comparison.hasPlan && plannedDietName != null) {
                Text(
                    text = "Plan: $plannedDietName",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Actual macros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ComparisonMacroItem("Cal", comparison.actualCalories, comparison.plannedCalories)
                ComparisonMacroItem("P", comparison.actualProtein, comparison.plannedProtein, "g")
                ComparisonMacroItem("C", comparison.actualCarbs, comparison.plannedCarbs, "g")
                ComparisonMacroItem("F", comparison.actualFat, comparison.plannedFat, "g")
            }
        }
    }
}

@Composable
fun ComparisonMacroItem(label: String, actual: Int, planned: Int, unit: String = "") {
    val diff = actual - planned
    val diffColor = when {
        !planned.let { it > 0 } -> MaterialTheme.colorScheme.onPrimaryContainer
        diff > 0 -> MaterialTheme.colorScheme.error
        diff < 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$actual$unit",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (planned > 0) {
            Text(
                text = "/ $planned$unit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            )
            val diffText = if (diff >= 0) "+$diff" else "$diff"
            Text(
                text = diffText,
                style = MaterialTheme.typography.labelSmall,
                color = diffColor
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MealSlotSection(
    slot: DefaultMealSlot,
    meals: List<LoggedMealWithDetails>,
    onAddMeal: () -> Unit,
    onRemoveMeal: (LoggedMealWithDetails) -> Unit,
    onUpdateQuantity: (LoggedMealWithDetails, Double) -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = onAddMeal) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Meal")
                }
            }

            if (meals.isEmpty()) {
                Text(
                    text = "No meals logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                meals.forEach { mealWithDetails ->
                    LoggedMealItem(
                        meal = mealWithDetails,
                        onRemove = { onRemoveMeal(mealWithDetails) },
                        onQuantityChange = { onUpdateQuantity(mealWithDetails, it) }
                    )
                }

                // Slot subtotal
                val slotCal = meals.sumOf { it.totalCalories }
                Text(
                    text = "Subtotal: ${slotCal.toInt()} cal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LoggedMealItem(
    meal: LoggedMealWithDetails,
    onRemove: () -> Unit,
    onQuantityChange: (Double) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(meal.meal.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${meal.totalCalories.toInt()} cal | P:${meal.totalProtein.toInt()}g C:${meal.totalCarbs.toInt()}g F:${meal.totalFat.toInt()}g",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onQuantityChange(meal.loggedMeal.quantity - 0.5) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "%.1f".format(meal.loggedMeal.quantity),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { onQuantityChange(meal.loggedMeal.quantity + 0.5) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun MealPickerDialog(
    meals: List<Meal>,
    onSelect: (Meal, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMeal by remember { mutableStateOf<Meal?>(null) }
    var quantity by remember { mutableStateOf(1.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedMeal == null) "Select Meal" else "Set Quantity") },
        text = {
            if (selectedMeal == null) {
                if (meals.isEmpty()) {
                    Column {
                        Text("No meals available.")
                        Text(
                            "Create meals first in the Meals section.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn {
                        items(meals.distinctBy { it.id }, key = { it.id }) { meal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMeal = meal }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(meal.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        meal.defaultSlot?.displayName ?: "Custom",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column {
                    Text(selectedMeal!!.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = { if (quantity > 0.5) quantity -= 0.5 }) {
                            Icon(Icons.Default.Clear, contentDescription = "Decrease")
                        }
                        Text(
                            text = "%.1f servings".format(quantity),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        IconButton(onClick = { quantity += 0.5 }) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMeal != null) {
                TextButton(onClick = { onSelect(selectedMeal!!, quantity) }) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedMeal != null) {
                    selectedMeal = null
                    quantity = 1.0
                } else {
                    onDismiss()
                }
            }) {
                Text(if (selectedMeal != null) "Back" else "Cancel")
            }
        }
    )
}

