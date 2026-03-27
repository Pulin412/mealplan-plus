package com.mealplanplus.ui.screens.diets

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealFoodItemWithDetails
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.FoodDetailSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietMealSlotScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    onNavigateToMealPicker: (String) -> Unit = {},
    savedStateHandle: SavedStateHandle? = null,
    viewModel: DietMealSlotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingFoodItem by remember { mutableStateOf<MealFoodItemWithDetails?>(null) }

    // Handle meal selection result
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.getStateFlow<Long?>("selected_meal_id", null).collect { mealId ->
                if (mealId != null) {
                    viewModel.changeMealById(mealId)
                    handle.remove<Long>("selected_meal_id")
                    handle.remove<String>("selected_slot_type")
                }
            }
        }
    }

    // Handle food selection results from FoodPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.get<Long>("selected_food_id")?.let { foodId ->
                val quantity = handle.get<Double>("selected_quantity") ?: 1.0
                val unit = handle.get<String>("selected_unit")?.let { runCatching { com.mealplanplus.data.model.FoodUnit.valueOf(it) }.getOrNull() } ?: com.mealplanplus.data.model.FoodUnit.GRAM
                viewModel.addFoodById(foodId, quantity, unit)
                handle.remove<Long>("selected_food_id")
                handle.remove<Double>("selected_quantity")
                handle.remove<String>("selected_unit")
            }

            handle.get<String>("usda_food_name")?.let { name ->
                val usdaFood = UsdaFoodResult(
                    fdcId = 0,
                    name = name,
                    brand = handle.get<String>("usda_food_brand"),
                    calories = handle.get<Double>("usda_food_calories") ?: 0.0,
                    protein = handle.get<Double>("usda_food_protein") ?: 0.0,
                    carbs = handle.get<Double>("usda_food_carbs") ?: 0.0,
                    fat = handle.get<Double>("usda_food_fat") ?: 0.0,
                    servingSize = handle.get<Double>("usda_food_serving_size") ?: 100.0,
                    servingUnit = handle.get<String>("usda_food_serving_unit") ?: "g"
                )
                val quantity = handle.get<Double>("selected_quantity") ?: 1.0
                val unit = handle.get<String>("selected_unit")?.let { runCatching { com.mealplanplus.data.model.FoodUnit.valueOf(it) }.getOrNull() } ?: com.mealplanplus.data.model.FoodUnit.GRAM
                viewModel.addUsdaFood(usdaFood, quantity, unit)

                handle.remove<String>("usda_food_name")
                handle.remove<String>("usda_food_brand")
                handle.remove<Double>("usda_food_calories")
                handle.remove<Double>("usda_food_protein")
                handle.remove<Double>("usda_food_carbs")
                handle.remove<Double>("usda_food_fat")
                handle.remove<Double>("usda_food_serving_size")
                handle.remove<String>("usda_food_serving_unit")
                handle.remove<Double>("selected_quantity")
                handle.remove<String>("selected_unit")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.slotDisplayName)
                        if (uiState.dietName.isNotEmpty()) {
                            Text(
                                text = uiState.dietName,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (uiState.currentMeal != null) {
                FloatingActionButton(onClick = onNavigateToFoodPicker) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food")
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Meal selector - navigate to picker
                    item {
                        MealSelectorCard(
                            currentMeal = uiState.currentMeal,
                            onClick = { onNavigateToMealPicker(uiState.slotType) }
                        )
                    }

                    // Macro summary
                    uiState.mealWithFoods?.let { mealWithFoods ->
                        item {
                            MacroSummaryCard(
                                calories = mealWithFoods.totalCalories,
                                protein = mealWithFoods.totalProtein,
                                carbs = mealWithFoods.totalCarbs,
                                fat = mealWithFoods.totalFat
                            )
                        }

                        // Food items header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Food Items (${mealWithFoods.items.size})",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }

                        // Food items list
                        if (mealWithFoods.items.isEmpty()) {
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "No food items yet",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "Tap + to add foods",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            items(mealWithFoods.items, key = { it.food.id }) { item ->
                                FoodItemCard(
                                    item = item,
                                    onEdit = { editingFoodItem = item },
                                    onRemove = { viewModel.removeFood(item.food.id) }
                                )
                            }
                        }
                    }

                    // Empty state when no meal assigned
                    if (uiState.currentMeal == null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            "No meal assigned",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Select a meal above to see its contents",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    // Food quantity editor
    editingFoodItem?.let { item ->
        FoodDetailSheet(
            food = item.food,
            usdaFood = null,
            initialQuantity = item.mealFoodItem.quantity,
            onConfirm = { quantity ->
                viewModel.updateFoodQuantity(item.food.id, quantity)
                editingFoodItem = null
            },
            onDismiss = { editingFoodItem = null }
        )
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
fun MealSelectorCard(
    currentMeal: Meal?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Current Meal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = currentMeal?.name ?: "Not assigned",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Change meal",
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Meal Totals",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroItem("Calories", "${calories.toInt()}", MaterialTheme.colorScheme.primary)
                MacroItem("Protein", "${protein.toInt()}g", MaterialTheme.colorScheme.tertiary)
                MacroItem("Carbs", "${carbs.toInt()}g", MaterialTheme.colorScheme.secondary)
                MacroItem("Fat", "${fat.toInt()}g", MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun MacroItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FoodItemCard(
    item: MealFoodItemWithDetails,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.food.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${item.mealFoodItem.quantity.toInt()}${item.mealFoodItem.unit.shortLabel} • ${item.calculatedCalories.toInt()} cal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "P: ${item.calculatedProtein.toInt()}g • C: ${item.calculatedCarbs.toInt()}g • F: ${item.calculatedFat.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
