package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.components.*
import com.mealplanplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToFoodPicker: () -> Unit,
    onNavigateToMealDetail: (Long, String) -> Unit = { _, _ -> },
    onNavigateToMealPicker: (slotType: String) -> Unit = {},
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
                    val qty  = handle.get<Double>("usda_food_quantity_$i") ?: 1.0
                    val unit = handle.get<String>("usda_food_unit_$i")?.let {
                        runCatching { FoodUnit.valueOf(it) }.getOrNull()
                    } ?: FoodUnit.GRAM
                    viewModel.addUsdaFood(usdaFood, qty, unit)
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

    // Handle meal selection result from DietMealPickerScreen (VIEW MODE slot taps)
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow<Long?>("selected_meal_id", null)?.collect { mealId ->
            if (mealId != null) {
                val slotType = savedStateHandle.get<String>("selected_slot_type") ?: return@collect
                viewModel.assignMealToSlotById(slotType, mealId)
                savedStateHandle.remove<Long>("selected_meal_id")
                savedStateHandle.remove<String>("selected_slot_type")
            }
        }
    }

    // Loading / not found states
    when {
        uiState.isLoading -> {
            Box(Modifier.fillMaxSize().background(BgPage), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DesignGreen)
            }
            return
        }
        uiState.diet == null -> {
            Box(Modifier.fillMaxSize().background(BgPage), contentAlignment = Alignment.Center) {
                Text("Diet not found", color = TextMuted)
            }
            return
        }
    }

    val bgPage = BgPage
    val cardBg = CardBg
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    val textSecondary = TextSecondary
    val dividerColor = DividerColor
    val designGreen = DesignGreen

    val diet = uiState.diet!!
    val dwm = uiState.dietWithMeals
    val isEditing = uiState.isEditing
    val customSlotTypes = uiState.customSlotTypes
    var showAddSlotDialog by remember { mutableStateOf(false) }

    // Compute meal names for subtitle
    val mealNames = remember(dwm) {
        DefaultMealSlot.entries
            .mapNotNull { dwm?.meals?.get(it.name)?.meal?.name }
            .take(3)
            .joinToString(", ")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgPage)
    ) {
        if (isEditing) {
            // ─── EDIT MODE header ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.cancelEditing() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel", tint = textMuted)
                }
                Text(
                    "Edit Diet",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                PrimaryButton(
                    text = "Save",
                    onClick = viewModel::saveDiet,
                    modifier = Modifier.width(80.dp)
                )
            }

            // Edit form
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                item {
                    val totalCal = dwm?.totalCalories?.toInt() ?: 0
                    val totalPro = dwm?.totalProtein?.toInt() ?: 0
                    val totalCarb = dwm?.totalCarbs?.toInt() ?: 0
                    val totalFat = dwm?.totalFat?.toInt() ?: 0
                    EstimatedTotalsCard(totalCal, totalPro, totalCarb, totalFat, glycemicLoad = dwm?.totalGlycemicLoad)
                }

                val slots = slotsToShow(DefaultMealSlot.entries.associateWith { slot ->
                    dwm?.meals?.get(slot.name)?.items ?: emptyList()
                })
                items(slots.size) { index ->
                    val slot = slots[index]
                    val foods = dwm?.meals?.get(slot.name)?.items ?: emptyList()
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
                        isEditing = true,
                        instructions = uiState.editSlotInstructions[slot.name] ?: "",
                        onInstructionsChange = { text -> viewModel.updateSlotInstructions(slot, text) },
                        onChangeMeal = {
                            viewModel.setPickingSlot(slot)
                            onNavigateToMealPicker(slot.name)
                        }
                    )
                }

                items(customSlotTypes.size) { index ->
                    val slotType = customSlotTypes[index]
                    val displayName = slotType.removePrefix("CUSTOM:")
                    val foods = dwm?.meals?.get(slotType)?.items ?: emptyList()
                    CustomDietSlotSection(
                        slotType = slotType,
                        displayName = displayName,
                        foods = foods,
                        isEditing = true,
                        instructions = uiState.editSlotInstructions[slotType] ?: "",
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
                        onInstructionsChange = { text -> viewModel.updateSlotInstructionsForType(slotType, text) },
                        onRemoveSlot = { viewModel.removeCustomSlot(slotType) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = { showAddSlotDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Custom Meal Slot")
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        } else {
            // ─── VIEW MODE ────────────────────────────────────────────────────
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textMuted)
                }
                Text(
                    "Diet Detail",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                // Type tag chips (up to 2)
                uiState.allTags.filter { it.id in uiState.dietTagIds }.take(2).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(TagGreenBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = designGreen)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                // Edit button
                IconButton(onClick = { viewModel.startEditing() }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = textMuted)
                }
                // Delete button
                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = textMuted)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // Diet name + subtitle
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgPage)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            diet.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = textPrimary
                        )
                        if (mealNames.isNotEmpty()) {
                            Text(
                                mealNames,
                                fontSize = 12.sp,
                                color = textMuted,
                                modifier = Modifier.padding(top = 3.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        diet.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            Text(
                                desc,
                                fontSize = 12.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Macro strip
                if (dwm != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(cardBg)
                        ) {
                            DetailMacroCol("${dwm.totalCalories.toInt()}", "kcal", Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                            DetailMacroCol("${dwm.totalProtein.toInt()}g", "protein", Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                            DetailMacroCol("${dwm.totalCarbs.toInt()}g", "carbs", Modifier.weight(1f))
                            Box(modifier = Modifier.width(1.dp).height(56.dp).background(dividerColor).align(Alignment.CenterVertically))
                            DetailMacroCol("${dwm.totalFat.toInt()}g", "fat", Modifier.weight(1f))
                        }
                    }
                }

                // Meal Slots section
                item {
                    FormSectionLabel(
                        "Meal Slots",
                        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 10.dp)
                    )
                }

                // Slot rows card
                item {
                    val allSlots = DefaultMealSlot.entries.filter { slot ->
                        dwm?.meals?.get(slot.name) != null
                    }
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                    ) {
                        if (allSlots.isEmpty() && customSlotTypes.isEmpty()) {
                            Text(
                                "No meals assigned. Tap Edit to assign meals to slots.",
                                fontSize = 13.sp,
                                color = textMuted,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                        allSlots.forEachIndexed { index, slot ->
                            val mealWithFoods = dwm?.meals?.get(slot.name)
                            DietViewSlotRow(
                                slot = slot,
                                mealWithFoods = mealWithFoods,
                                onClick = { onNavigateToMealPicker(slot.name) },
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )
                            if (index < allSlots.lastIndex || customSlotTypes.isNotEmpty()) {
                                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                            }
                        }

                        // Custom slots
                        customSlotTypes.forEachIndexed { index, slotType ->
                            val mealWithFoods = dwm?.meals?.get(slotType)
                            DietViewCustomSlotRow(
                                displayName = slotType.removePrefix("CUSTOM:"),
                                slotType = slotType,
                                mealWithFoods = mealWithFoods,
                                onClick = { onNavigateToMealPicker(slotType) },
                                textPrimary = textPrimary,
                                textMuted = textMuted
                            )
                            if (index < customSlotTypes.lastIndex) {
                                HorizontalDivider(color = dividerColor, thickness = 1.dp)
                            }
                        }
                    }
                }

                // Error
                uiState.error?.let { error ->
                    item {
                        LaunchedEffect(error) {
                            kotlinx.coroutines.delay(3000)
                            viewModel.clearError()
                        }
                        Text(
                            error,
                            fontSize = 12.sp,
                            color = TextDestructive,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
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
            text = { Text("Delete \"${diet.name}\"?") },
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

    // Add custom slot dialog (edit mode)
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
                    modifier = Modifier.fillMaxWidth()
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
                TextButton(onClick = { showAddSlotDialog = false; newSlotName = "" }) { Text("Cancel") }
            }
        )
    }
}

// ─── Macro column for detail strip ───────────────────────────────────────────

@Composable
private fun DetailMacroCol(value: String, label: String, modifier: Modifier = Modifier) {
    val textPrimary = TextPrimary
    val textMuted = TextMuted
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(label, fontSize = 10.sp, color = textMuted, modifier = Modifier.padding(top = 1.dp))
    }
}

// ─── Slot row for view mode ───────────────────────────────────────────────────

@Composable
private fun DietViewSlotRow(
    slot: DefaultMealSlot,
    mealWithFoods: MealWithFoods?,
    onClick: () -> Unit,
    textPrimary: Color,
    textMuted: Color
) {
    val dotColor = slotDotColor(slot.name)
    val hasMeal = mealWithFoods != null && mealWithFoods.items.isNotEmpty()
    val ingredientNames = mealWithFoods?.items
        ?.take(5)
        ?.joinToString(" · ") { it.food.name }
        ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            slot.displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
            color = textMuted,
            modifier = Modifier.width(72.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            if (mealWithFoods != null) {
                Text(
                    mealWithFoods.meal.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ingredientNames.isNotEmpty()) {
                    Text(
                        ingredientNames,
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            } else {
                Text(
                    "Tap to assign a meal",
                    fontSize = 13.sp,
                    color = textMuted,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        if (hasMeal) {
            Text(
                "${mealWithFoods!!.totalCalories.toInt()}",
                fontSize = 12.sp,
                color = textMuted,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text("+", fontSize = 14.sp, color = textMuted)
        }
    }
}

@Composable
private fun DietViewCustomSlotRow(
    displayName: String,
    @Suppress("UNUSED_PARAMETER") slotType: String,
    mealWithFoods: MealWithFoods?,
    onClick: () -> Unit,
    textPrimary: Color,
    textMuted: Color
) {
    val dotColor = SlotDefault
    val hasMeal = mealWithFoods != null && mealWithFoods.items.isNotEmpty()
    val ingredientNames = mealWithFoods?.items
        ?.take(5)
        ?.joinToString(" · ") { it.food.name }
        ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            displayName,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.4.sp,
            color = textMuted,
            modifier = Modifier.width(72.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            if (mealWithFoods != null) {
                Text(
                    mealWithFoods.meal.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (ingredientNames.isNotEmpty()) {
                    Text(
                        ingredientNames,
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            } else {
                Text(
                    "Tap to assign a meal",
                    fontSize = 13.sp,
                    color = textMuted,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        if (hasMeal) {
            Text(
                "${mealWithFoods!!.totalCalories.toInt()}",
                fontSize = 12.sp,
                color = textMuted,
                fontWeight = FontWeight.Medium
            )
        } else {
            Text("+", fontSize = 14.sp, color = textMuted)
        }
    }
}

// ─── Slot dot color mapping ────────────────────────────────────────────────────

private fun slotDotColor(slotName: String): Color = when (slotName) {
    "EARLY_MORNING" -> Color(0xFF9C27B0)
    "BREAKFAST"     -> Color(0xFFF59E0B)
    "MID_MORNING"   -> Color(0xFF795548)
    "NOON"          -> Color(0xFF888888)
    "LUNCH"         -> Color(0xFF2E7D52)
    "PRE_WORKOUT"   -> Color(0xFF2196F3)
    "EVENING"       -> Color(0xFFFBBF24)
    "EVENING_SNACK" -> Color(0xFF4CAF50)
    "POST_WORKOUT"  -> Color(0xFF03A9F4)
    "DINNER"        -> Color(0xFF7C3AED)
    "POST_DINNER"   -> Color(0xFF009688)
    else            -> Color(0xFF888888)
}
