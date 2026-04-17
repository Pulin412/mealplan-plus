package com.mealplanplus.ui.screens.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.FoodDetailSheet
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DesignGreenLight
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMealScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: EditMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var slotExpanded by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<SelectedFood?>(null) }

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

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("Edit Meal", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.name.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPage)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BgPage)
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
                    HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 8.dp))
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
                            color = TextSecondary
                        )
                    }
                }

                items(uiState.selectedFoods, key = { it.food.id }) { sf ->
                    SelectedFoodCard(
                        food = sf.food,
                        quantity = sf.quantity,
                        onQuantityChange = { viewModel.updateFoodQuantity(sf.food.id, it) },
                        onRemove = { viewModel.removeFood(sf.food.id) },
                        onEdit = { editingFood = sf }
                    )
                }

                if (uiState.selectedFoods.isNotEmpty()) {
                    item {
                        val totalCal = uiState.selectedFoods.sumOf { it.food.calories * it.quantity }
                        val totalP = uiState.selectedFoods.sumOf { it.food.protein * it.quantity }
                        val totalC = uiState.selectedFoods.sumOf { it.food.carbs * it.quantity }
                        val totalF = uiState.selectedFoods.sumOf { it.food.fat * it.quantity }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = DesignGreenLight)
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
                                color = Color.White
                            )
                        } else {
                            Text("Update Meal")
                        }
                    }
                }

                uiState.error?.let { error ->
                    item {
                        Text(
                            text = error,
                            color = TextDestructive,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }

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
