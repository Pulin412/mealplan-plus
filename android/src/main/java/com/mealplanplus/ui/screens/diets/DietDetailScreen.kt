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
    onNavigateToEditSlot: (String) -> Unit = {},
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
                        onInstructionsChange = { text -> viewModel.updateSlotInstructions(slot, text) }
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
                    val allSlots = DefaultMealSlot.entries
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardBg)
                    ) {
                        allSlots.forEachIndexed { index, slot ->
                            val mealWithFoods = dwm?.meals?.get(slot.name)
                            DietViewSlotRow(
                                slot = slot,
                                mealWithFoods = mealWithFoods,
                                onClick = { onNavigateToEditSlot(slot.name) },
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
                                onClick = { onNavigateToEditSlot(slotType) },
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
