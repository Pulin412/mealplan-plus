package com.mealplanplus.ui.screens.meals

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
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.FoodDetailSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: AddMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var slotExpanded by remember { mutableStateOf(false) }

    // Editing food state
    var editingFood by remember { mutableStateOf<SelectedFood?>(null) }

    // Handle food selection results from FoodPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            // Local food selected
            handle.get<Long>("selected_food_id")?.let { foodId ->
                val quantity = handle.get<Double>("selected_quantity") ?: 1.0
                val unit = handle.get<String>("selected_unit")?.let { runCatching { com.mealplanplus.data.model.FoodUnit.valueOf(it) }.getOrNull() } ?: com.mealplanplus.data.model.FoodUnit.GRAM
                viewModel.addFoodById(foodId, quantity, unit)
                handle.remove<Long>("selected_food_id")
                handle.remove<Double>("selected_quantity")
                handle.remove<String>("selected_unit")
            }

            // USDA food selected
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
                viewModel.addUsdaFoodWithQuantity(usdaFood, quantity)

                // Clear all USDA keys
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

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Meal") },
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
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Meal Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = slotExpanded,
                    onExpandedChange = { slotExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedSlot.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Meal Slot") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = slotExpanded,
                        onDismissRequest = { slotExpanded = false }
                    ) {
                        DefaultMealSlot.entries.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text(slot.displayName) },
                                onClick = {
                                    viewModel.updateSlot(slot)
                                    slotExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Food Items", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onNavigateToFoodPicker) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Food")
                    }
                }
            }

            if (uiState.selectedFoods.isEmpty()) {
                item {
                    Text(
                        text = "No foods added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.selectedFoods, key = { it.food.id }) { sf ->
                SelectedFoodCard(
                    food = sf.food,
                    quantity = sf.quantity,
                    unit = sf.unit,
                    onQuantityChange = { viewModel.updateFoodQuantity(sf.food.id, it) },
                    onRemove = { viewModel.removeFood(sf.food.id) },
                    onEdit = { editingFood = sf }
                )
            }

            if (uiState.selectedFoods.isNotEmpty()) {
                item {
                    val totalCal = uiState.selectedFoods.sumOf { it.food.calculateCalories(it.quantity, it.unit) }
                    val totalP = uiState.selectedFoods.sumOf { it.food.calculateProtein(it.quantity, it.unit) }
                    val totalC = uiState.selectedFoods.sumOf { it.food.calculateCarbs(it.quantity, it.unit) }
                    val totalF = uiState.selectedFoods.sumOf { it.food.calculateFat(it.quantity, it.unit) }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Macros", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Cal: ${totalCal.toInt()} | P: ${totalP.toInt()}g | C: ${totalC.toInt()}g | F: ${totalF.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::saveMeal,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Meal")
                    }
                }
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    // Food detail sheet for editing existing food
    editingFood?.let { sf ->
        FoodDetailSheet(
            food = sf.food,
            usdaFood = null,
            initialQuantity = sf.quantity,
            onConfirm = { quantity ->
                viewModel.updateFoodQuantity(sf.food.id, quantity)
                editingFood = null
            },
            onDismiss = { editingFood = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedFoodCard(
    food: FoodItem,
    quantity: Double,
    unit: FoodUnit = FoodUnit.GRAM,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    val calories = food.calculateCalories(quantity, unit)
    val protein = food.calculateProtein(quantity, unit)
    val carbs = food.calculateCarbs(quantity, unit)
    val fat = food.calculateFat(quantity, unit)

    Card(
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${quantity.toInt()}${unit.shortLabel} • ${calories.toInt()} cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "P:${protein.toInt()}g C:${carbs.toInt()}g F:${fat.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (quantity > 10) onQuantityChange(quantity - 10) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease")
                }
                Text(
                    text = if (quantity == quantity.toLong().toDouble()) "${quantity.toInt()}" else "%.1f".format(quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 10) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
    }
}

