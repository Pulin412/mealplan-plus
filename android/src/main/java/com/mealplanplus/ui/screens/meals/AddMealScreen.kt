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

    // Handle batch food selection results from FoodPickerScreen
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            val localCount = handle.get<Int>("selected_food_count") ?: 0
            repeat(localCount) { i ->
                handle.get<Long>("selected_food_id_$i")?.let { foodId ->
                    val qty  = handle.get<Double>("selected_quantity_$i") ?: 1.0
                    val unit = handle.get<String>("selected_unit_$i")?.let {
                        runCatching { FoodUnit.valueOf(it) }.getOrNull()
                    } ?: FoodUnit.GRAM
                    viewModel.addFoodById(foodId, qty, unit)
                }
                handle.remove<Long>("selected_food_id_$i")
                handle.remove<Double>("selected_quantity_$i")
                handle.remove<String>("selected_unit_$i")
            }
            if (localCount > 0) handle.remove<Int>("selected_food_count")

            val usdaCount = handle.get<Int>("usda_food_count") ?: 0
            repeat(usdaCount) { i ->
                handle.get<String>("usda_food_name_$i")?.let { name ->
                    val usdaFood = UsdaFoodResult(
                        fdcId       = 0,
                        name        = name,
                        brand       = handle.get<String>("usda_food_brand_$i"),
                        calories    = handle.get<Double>("usda_food_calories_$i") ?: 0.0,
                        protein     = handle.get<Double>("usda_food_protein_$i") ?: 0.0,
                        carbs       = handle.get<Double>("usda_food_carbs_$i") ?: 0.0,
                        fat         = handle.get<Double>("usda_food_fat_$i") ?: 0.0,
                        servingSize = handle.get<Double>("usda_food_serving_size_$i") ?: 100.0,
                        servingUnit = handle.get<String>("usda_food_serving_unit_$i") ?: "g"
                    )
                    val qty = handle.get<Double>("usda_food_quantity_$i") ?: 1.0
                    viewModel.addUsdaFoodWithQuantity(usdaFood, qty)
                }
                handle.remove<String>("usda_food_name_$i")
                handle.remove<String>("usda_food_brand_$i")
                handle.remove<Double>("usda_food_calories_$i")
                handle.remove<Double>("usda_food_protein_$i")
                handle.remove<Double>("usda_food_carbs_$i")
                handle.remove<Double>("usda_food_fat_$i")
                handle.remove<Double>("usda_food_serving_size_$i")
                handle.remove<String>("usda_food_serving_unit_$i")
                handle.remove<Double>("usda_food_quantity_$i")
                handle.remove<String>("usda_food_unit_$i")
            }
            if (usdaCount > 0) handle.remove<Int>("usda_food_count")
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

            // Search & add ingredient button
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .clickable { onNavigateToFoodPicker() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
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

            // Ingredients section label + card (only when there are ingredients)
            if (uiState.selectedFoods.isNotEmpty()) {
                item {
                    FormSectionLabel(
                        "Ingredients (${uiState.selectedFoods.size})",
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                    ) {
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
