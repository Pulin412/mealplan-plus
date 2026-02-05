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
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.LoggedFoodWithDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyLogScreen(
    date: String?,
    onNavigateBack: () -> Unit,
    onNavigateToFoods: () -> Unit,
    viewModel: DailyLogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableFoods by viewModel.availableFoods.collectAsState()

    LaunchedEffect(date) {
        viewModel.setDateFromString(date)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
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
                        val slotFoods = uiState.log?.foodsForSlot(slot.name) ?: emptyList()
                        MealSlotSection(
                            slot = slot,
                            foods = slotFoods,
                            onAddFood = { viewModel.showFoodPicker(slot.name) },
                            onRemoveFood = { viewModel.deleteLoggedFood(it.loggedFood.id) },
                            onUpdateQuantity = { food, qty ->
                                viewModel.updateQuantity(food.loggedFood, qty)
                            }
                        )
                    }
                }
            }
        }
    }

    // Food picker dialog
    if (uiState.showFoodPicker) {
        LogFoodPickerDialog(
            foods = availableFoods,
            onSelect = { food, qty -> viewModel.logFood(food, qty) },
            onDismiss = { viewModel.hideFoodPicker() },
            onNavigateToFoods = {
                viewModel.hideFoodPicker()
                onNavigateToFoods()
            }
        )
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
fun MacroSummaryCard(
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroItem("Calories", "${calories.toInt()}", "kcal")
            MacroItem("Protein", "${protein.toInt()}", "g")
            MacroItem("Carbs", "${carbs.toInt()}", "g")
            MacroItem("Fat", "${fat.toInt()}", "g")
        }
    }
}

@Composable
fun MacroItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "$label ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
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
    foods: List<LoggedFoodWithDetails>,
    onAddFood: () -> Unit,
    onRemoveFood: (LoggedFoodWithDetails) -> Unit,
    onUpdateQuantity: (LoggedFoodWithDetails, Double) -> Unit
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
                TextButton(onClick = onAddFood) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (foods.isEmpty()) {
                Text(
                    text = "No foods logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                foods.forEach { food ->
                    LoggedFoodItem(
                        food = food,
                        onRemove = { onRemoveFood(food) },
                        onQuantityChange = { onUpdateQuantity(food, it) }
                    )
                }

                // Slot subtotal
                val slotCal = foods.sumOf { it.calculatedCalories }
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
fun LoggedFoodItem(
    food: LoggedFoodWithDetails,
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
            Text(food.food.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${food.calculatedCalories.toInt()} cal",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onQuantityChange(food.loggedFood.quantity - 0.5) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "%.1f".format(food.loggedFood.quantity),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { onQuantityChange(food.loggedFood.quantity + 0.5) },
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
fun LogFoodPickerDialog(
    foods: List<FoodItem>,
    onSelect: (FoodItem, Double) -> Unit,
    onDismiss: () -> Unit,
    onNavigateToFoods: () -> Unit
) {
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var quantity by remember { mutableStateOf(1.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedFood == null) "Select Food" else "Set Quantity") },
        text = {
            if (selectedFood == null) {
                if (foods.isEmpty()) {
                    Column {
                        Text("No foods available.")
                        TextButton(onClick = onNavigateToFoods) {
                            Text("Add Foods")
                        }
                    }
                } else {
                    LazyColumn {
                        items(foods) { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFood = food }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(food.name, style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "${food.calories.toInt()} cal per ${food.servingSize.toInt()} ${food.servingUnit}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Column {
                    Text("${selectedFood!!.name}", style = MaterialTheme.typography.titleMedium)
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
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${(selectedFood!!.calories * quantity).toInt()} calories",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (selectedFood != null) {
                TextButton(onClick = { onSelect(selectedFood!!, quantity) }) {
                    Text("Add")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedFood != null) {
                    selectedFood = null
                    quantity = 1.0
                } else {
                    onDismiss()
                }
            }) {
                Text(if (selectedFood != null) "Back" else "Cancel")
            }
        }
    )
}
