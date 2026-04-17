package com.mealplanplus.ui.screens.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.*
import com.mealplanplus.ui.screens.foods.UsdaFoodResultRow
import com.mealplanplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: AddMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingFood by remember { mutableStateOf<SelectedFood?>(null) }

    // Handle food selection results from FoodPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.get<Long>("selected_food_id")?.let { foodId ->
                val quantity = handle.get<Double>("selected_quantity") ?: 1.0
                val unit = handle.get<String>("selected_unit")?.let {
                    runCatching { FoodUnit.valueOf(it) }.getOrNull()
                } ?: FoodUnit.GRAM
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
                viewModel.addUsdaFoodWithQuantity(usdaFood, quantity)
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

    val bgPage = BgPage
    val cardBg = CardBg
    val textMuted = TextMuted
    val dividerColor = DividerColor
    val iconBgGray = IconBgGray

    val totalCal = uiState.selectedFoods.sumOf { it.food.calculateCalories(it.quantity, it.unit) }
    val totalP = uiState.selectedFoods.sumOf { it.food.calculateProtein(it.quantity, it.unit) }
    val totalC = uiState.selectedFoods.sumOf { it.food.calculateCarbs(it.quantity, it.unit) }
    val totalF = uiState.selectedFoods.sumOf { it.food.calculateFat(it.quantity, it.unit) }

    Column(modifier = Modifier.fillMaxSize().background(bgPage)) {
        // ── Header ──
        ScreenCloseHeader(title = "Create Meal", onClose = onNavigateBack)
        ScreenSubtitle(text = "Combine foods into a meal")

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Meal name
            item {
                FormGroup(label = "Meal Name *") {
                    DesignTextField(
                        value = uiState.name,
                        onValueChange = viewModel::updateName,
                        placeholder = "e.g. Paneer Corn Stir Fry",
                        imeAction = ImeAction.Next
                    )
                }
            }

            // Ingredients section label
            item {
                FormSectionLabel(
                    "Ingredients",
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // Ingredient card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                ) {
                    if (uiState.selectedFoods.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No ingredients added yet",
                                fontSize = 13.sp,
                                color = textMuted,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    } else {
                        uiState.selectedFoods.forEachIndexed { index, sf ->
                            IngredientRow(
                                food = sf.food,
                                quantity = sf.quantity,
                                unit = sf.unit,
                                onEdit = { editingFood = sf },
                                onRemove = { viewModel.removeFood(sf.food.id) }
                            )
                            if (index < uiState.selectedFoods.lastIndex) {
                                HorizontalDivider(color = dividerColor, thickness = 1.dp, modifier = Modifier.padding(start = 62.dp))
                            }
                        }
                    }

                    // Add ingredient row
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToFoodPicker() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(iconBgGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", fontSize = 20.sp, color = textMuted, fontWeight = FontWeight.Light)
                        }
                        Text(
                            "Search & add ingredient…",
                            fontSize = 13.sp,
                            color = textMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Macro summary strip (shown when there are foods)
            if (uiState.selectedFoods.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                    ) {
                        MacroColumn("${totalCal.toInt()}", "kcal", Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                        MacroColumn("${totalP.toInt()}g", "protein", Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                        MacroColumn("${totalC.toInt()}g", "carbs", Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                        MacroColumn("${totalF.toInt()}g", "fat", Modifier.weight(1f))
                    }
                }
            }

            // Error
            uiState.error?.let { error ->
                item {
                    Text(
                        error,
                        fontSize = 12.sp,
                        color = TextDestructive
                    )
                }
            }

            // Save button
            item {
                PrimaryButton(
                    text = "Save Meal",
                    onClick = viewModel::saveMeal,
                    isLoading = uiState.isLoading,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Food detail sheet for editing
    editingFood?.let { sf ->
        com.mealplanplus.ui.components.FoodDetailSheet(
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

// ─── Ingredient row ───────────────────────────────────────────────────────────

@Composable
private fun IngredientRow(
    food: FoodItem,
    quantity: Double,
    unit: FoodUnit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val iconBgGray = IconBgGray

    val quantityLabel = buildString {
        append(if (quantity == quantity.toLong().toDouble()) "${quantity.toInt()}" else "%.1f".format(quantity))
        append(unit.shortLabel)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Food icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBgGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = textMuted, modifier = Modifier.size(18.dp))
        }

        // Name
        Text(
            food.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )

        // Quantity badge + remove
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgGray)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    quantityLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary
                )
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ─── Macro column for summary strip ──────────────────────────────────────────

@Composable
private fun MacroColumn(value: String, label: String, modifier: Modifier = Modifier) {
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(label, fontSize = 10.sp, color = textMuted, modifier = Modifier.padding(top = 1.dp))
    }
}

// Keep SelectedFoodCard for backward compat (not used in new UI but referenced from NavHost if any)
@Composable
fun SelectedFoodCard(
    food: FoodItem,
    quantity: Double,
    unit: FoodUnit = FoodUnit.GRAM,
    @Suppress("UNUSED_PARAMETER") onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    IngredientRow(
        food = food,
        quantity = quantity,
        unit = unit,
        onEdit = onEdit,
        onRemove = onRemove
    )
}
