package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.UsdaFoodResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
    savedStateHandle: SavedStateHandle? = null,
    autoEdit: Boolean = false,
    viewModel: DietDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Auto-enter edit mode when navigated from Edit button
    LaunchedEffect(autoEdit, uiState.diet) {
        if (autoEdit && uiState.diet != null && !uiState.isEditing) {
            viewModel.startEditing()
        }
    }

    // Handle food selection result from picker
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.let { handle ->
            handle.get<Long>("selected_food_id")?.let { foodId ->
                val quantity = handle.get<Double>("selected_quantity") ?: 1.0
                val unit = handle.get<String>("selected_unit")?.let { runCatching { FoodUnit.valueOf(it) }.getOrNull() } ?: FoodUnit.GRAM
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
                val unit = handle.get<String>("selected_unit")?.let { runCatching { FoodUnit.valueOf(it) }.getOrNull() } ?: FoodUnit.GRAM
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

    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        uiState.diet == null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Diet not found")
            }
            return
        }
    }

    val dwm = uiState.dietWithMeals
    val slotFoodItems = DefaultMealSlot.entries.associateWith { slot ->
        dwm?.meals?.get(slot.name)?.items ?: emptyList()
    }
    val customSlotTypes = uiState.customSlotTypes
    val isEditing = uiState.isEditing
    var showAddSlotDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (isEditing) {
                DietFormTopBar(
                    title = "Edit Diet",
                    onNavigateBack = { viewModel.cancelEditing() },
                    onSave = viewModel::saveDiet,
                    isSaving = false
                )
            } else {
                TopAppBar(
                    title = { Text(uiState.diet?.name ?: "Diet Details") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF2E7D52),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
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
            if (isEditing) {
                // Edit mode: show DietInfoCard with editable fields
                item {
                    DietInfoCard(
                        name = uiState.editName,
                        description = uiState.editDescription,
                        allTags = uiState.allTags,
                        selectedTagIds = uiState.selectedTagIds,
                        onNameChange = viewModel::updateName,
                        onDescriptionChange = viewModel::updateDescription,
                        onTagToggle = viewModel::toggleTag,
                        newTagName = uiState.newTagName,
                        onNewTagNameChange = viewModel::updateNewTagName,
                        onCreateTag = viewModel::createNewTagAndSelect
                    )
                }
            } else {
                // View mode: show Diet Info card (read-only)
                item {
                    DietInfoCard(
                        name = uiState.diet?.name ?: "",
                        description = uiState.diet?.description ?: "",
                        allTags = uiState.allTags.filter { it.id in uiState.dietTagIds },
                        selectedTagIds = uiState.dietTagIds,
                        onNameChange = {},
                        onDescriptionChange = {},
                        onTagToggle = {}
                    )
                }
            }

            // Estimated Totals card
            item {
                val totalCal = dwm?.totalCalories?.toInt() ?: 0
                val totalPro = dwm?.totalProtein?.toInt() ?: 0
                val totalCarb = dwm?.totalCarbs?.toInt() ?: 0
                val totalFat = dwm?.totalFat?.toInt() ?: 0
                EstimatedTotalsCard(totalCal, totalPro, totalCarb, totalFat)
            }

            // Default slot sections
            val slots = slotsToShow(slotFoodItems)
            items(slots.size) { index ->
                val slot = slots[index]
                val foods = slotFoodItems[slot] ?: emptyList()
                val dietId = uiState.diet?.id ?: 0L
                DietSlotSection(
                    slot = slot,
                    foods = foods,
                    onAddFood = {
                        viewModel.setPickingSlot(slot)
                        onNavigateToFoodPicker()
                    },
                    onRemoveFood = { idx ->
                        if (idx in foods.indices) viewModel.removeFood(slot, foods[idx])
                    },
                    onIncrement = { idx ->
                        if (idx in foods.indices) viewModel.incrementQty(slot, foods[idx])
                    },
                    onDecrement = { idx ->
                        if (idx in foods.indices) viewModel.decrementQty(slot, foods[idx])
                    },
                    isEditing = isEditing,
                    instructions = if (isEditing)
                        uiState.editSlotInstructions[slot.name] ?: ""
                    else
                        uiState.dietWithMeals?.instructions?.get(slot.name) ?: "",
                    onInstructionsChange = if (isEditing)
                        { text -> viewModel.updateSlotInstructions(slot, text) } else null,
                    onViewDetails = if (!isEditing && foods.isNotEmpty())
                        { -> onNavigateToMealDetail(dietId, slot.name) } else null
                )
            }

            // Custom slot sections
            items(customSlotTypes.size) { index ->
                val slotType = customSlotTypes[index]
                val displayName = slotType.removePrefix("CUSTOM:")
                val foods = dwm?.meals?.get(slotType)?.items ?: emptyList()
                val dietId = uiState.diet?.id ?: 0L
                CustomDietSlotSection(
                    slotType = slotType,
                    displayName = displayName,
                    foods = foods,
                    isEditing = isEditing,
                    instructions = if (isEditing)
                        uiState.editSlotInstructions[slotType] ?: ""
                    else
                        uiState.dietWithMeals?.instructions?.get(slotType) ?: "",
                    onAddFood = {
                        viewModel.setPickingSlotType(slotType)
                        onNavigateToFoodPicker()
                    },
                    onRemoveFood = { idx ->
                        if (idx in foods.indices) viewModel.removeFoodFromSlot(slotType, foods[idx])
                    },
                    onIncrement = { idx ->
                        if (idx in foods.indices) viewModel.incrementQtyInSlot(slotType, foods[idx])
                    },
                    onDecrement = { idx ->
                        if (idx in foods.indices) viewModel.decrementQtyInSlot(slotType, foods[idx])
                    },
                    onInstructionsChange = if (isEditing)
                        { text -> viewModel.updateSlotInstructionsForType(slotType, text) } else null,
                    onRemoveSlot = if (isEditing)
                        { -> viewModel.removeCustomSlot(slotType) } else null,
                    onViewDetails = if (!isEditing && foods.isNotEmpty())
                        { -> onNavigateToMealDetail(dietId, slotType) } else null
                )
            }

            // Add custom slot button (edit mode)
            if (isEditing) {
                item {
                    OutlinedButton(
                        onClick = { showAddSlotDialog = true },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(18.dp))
                        Spacer(androidx.compose.ui.Modifier.width(8.dp))
                        Text("Add Custom Meal Slot")
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Diet") },
            text = { Text("Delete \"${uiState.diet?.name}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDiet { onNavigateBack() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Add custom slot dialog
    if (showAddSlotDialog) {
        var newSlotName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSlotDialog = false; newSlotName = "" },
            title = { Text("Add Custom Meal Slot") },
            text = {
                OutlinedTextField(
                    value = newSlotName,
                    onValueChange = { newSlotName = it },
                    label = { Text("Slot name") },
                    placeholder = { Text("e.g. Pre-Sleep, Evening Tea") },
                    singleLine = true,
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCustomSlot(newSlotName)
                        showAddSlotDialog = false
                        newSlotName = ""
                    },
                    enabled = newSlotName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSlotDialog = false; newSlotName = "" }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}
