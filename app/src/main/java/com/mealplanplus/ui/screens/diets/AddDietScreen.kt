package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.repository.UsdaFoodResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDietScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: AddDietViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle food selection result from picker
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.get<Long>("selected_food_id")?.let { foodId ->
                val quantity = handle.get<Double>("selected_quantity") ?: 100.0
                viewModel.addFoodById(foodId, quantity)
                handle.remove<Long>("selected_food_id")
                handle.remove<Double>("selected_quantity")
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
                val quantity = handle.get<Double>("selected_quantity") ?: 100.0
                viewModel.addUsdaFoodToSlot(usdaFood, quantity)
                handle.remove<String>("usda_food_name")
                handle.remove<String>("usda_food_brand")
                handle.remove<Double>("usda_food_calories")
                handle.remove<Double>("usda_food_protein")
                handle.remove<Double>("usda_food_carbs")
                handle.remove<Double>("usda_food_fat")
                handle.remove<Double>("usda_food_serving_size")
                handle.remove<String>("usda_food_serving_unit")
                handle.remove<Double>("selected_quantity")
            }
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            DietFormTopBar(
                title = "New Diet",
                onNavigateBack = onNavigateBack,
                onSave = viewModel::saveDiet,
                isSaving = uiState.isLoading
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Diet Info card
            item {
                DietInfoCard(
                    name = uiState.name,
                    description = uiState.description,
                    allTags = uiState.allTags,
                    selectedTagIds = uiState.selectedTagIds,
                    onNameChange = viewModel::updateName,
                    onDescriptionChange = viewModel::updateDescription,
                    onTagToggle = viewModel::toggleTag,
                    newTagName = uiState.newTagName,
                    onNewTagNameChange = viewModel::updateNewTagName,
                    onCreateTag = viewModel::createAndSelectTag
                )
            }

            // Estimated Totals card
            item {
                EstimatedTotalsCard(
                    calories = uiState.estimatedCalories,
                    protein = uiState.estimatedProtein,
                    carbs = uiState.estimatedCarbs,
                    fat = uiState.estimatedFat
                )
            }

            // Slot sections
            val slots = slotsToShow(uiState.slotFoodItems)
            items(slots.size) { index ->
                val slot = slots[index]
                val foods = uiState.slotFoodItems[slot] ?: emptyList()
                DietSlotSection(
                    slot = slot,
                    foods = foods,
                    onAddFood = {
                        viewModel.setPickingSlot(slot)
                        onNavigateToFoodPicker()
                    },
                    onRemoveFood = { idx -> viewModel.removeFood(slot, idx) },
                    onIncrement = { idx -> viewModel.incrementQty(slot, idx) },
                    onDecrement = { idx -> viewModel.decrementQty(slot, idx) }
                )
            }

            // Error
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
