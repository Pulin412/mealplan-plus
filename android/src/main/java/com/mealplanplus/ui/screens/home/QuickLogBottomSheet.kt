package com.mealplanplus.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.PrimaryGreen
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.BgPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickLogBottomSheet(
    uiState: QuickLogUiState,
    onDismiss: () -> Unit,
    onSlotSelect: (DefaultMealSlot) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onLocalFoodSelect: (FoodItem) -> Unit,
    onUsdaFoodSelect: (UsdaFoodResult) -> Unit,
    onDismissFood: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (FoodUnit) -> Unit,
    onGiChange: (String) -> Unit,
    onAddFood: () -> Unit,
    onRemoveItem: (QuickLogEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgPage
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Log", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Slot chips
            Text("Slot", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(DefaultMealSlot.entries) { slot ->
                    FilterChip(
                        selected = uiState.selectedSlot == slot,
                        onClick = { onSlotSelect(slot) },
                        label = { Text(slot.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search row: text field + Search button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a food name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() })
                )
                Button(
                    onClick = onSearch,
                    enabled = uiState.searchQuery.trim().length >= 2,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text("Search", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Selected food — quantity + GI entry
            val preview = uiState.selectedFoodPreview
            if (preview != null) {
                Spacer(modifier = Modifier.height(12.dp))
                SelectedFoodRow(
                    food = preview,
                    quantityInput = uiState.quantityInput,
                    selectedUnit = uiState.selectedUnit,
                    giInput = uiState.giInput,
                    glContribution = uiState.glContribution,
                    isAdding = uiState.isAdding,
                    onQuantityChange = onQuantityChange,
                    onUnitChange = onUnitChange,
                    onGiChange = onGiChange,
                    onAdd = onAddFood,
                    onDismiss = onDismissFood
                )
            }

            // Local results
            if (uiState.localResults.isNotEmpty() && preview == null) {
                Spacer(modifier = Modifier.height(12.dp))
                ResultSectionHeader(title = "From your library", count = uiState.localResults.size)
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        uiState.localResults.forEachIndexed { index, food ->
                            FoodResultRow(
                                name = food.name,
                                brand = food.brand,
                                calPer100 = food.caloriesPer100,
                                isOnline = false,
                                onClick = { onLocalFoodSelect(food) }
                            )
                            if (index < uiState.localResults.lastIndex) {
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            // USDA results
            if (preview == null) {
                if (uiState.isSearchingUsda) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        Text("Searching online...", fontSize = 13.sp, color = TextSecondary)
                    }
                } else if (uiState.usdaError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(uiState.usdaError, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                } else if (uiState.usdaResults.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ResultSectionHeader(title = "Online results", count = uiState.usdaResults.size)
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column {
                            uiState.usdaResults.forEachIndexed { index, usda ->
                                FoodResultRow(
                                    name = usda.name,
                                    brand = usda.brand,
                                    calPer100 = if (usda.servingSize > 0) usda.calories * 100.0 / usda.servingSize else usda.calories,
                                    isOnline = true,
                                    onClick = { onUsdaFoodSelect(usda) }
                                )
                                if (index < uiState.usdaResults.lastIndex) {
                                    HorizontalDivider(color = TextMuted.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Added items
            if (uiState.addedItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Added (${uiState.addedItems.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "${uiState.addedItems.sumOf { it.calories }} kcal",
                        fontSize = 11.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column {
                        uiState.addedItems.forEachIndexed { index, entry ->
                            AddedItemRow(entry = entry, onRemove = { onRemoveItem(entry) })
                            if (index < uiState.addedItems.lastIndex) {
                                HorizontalDivider(color = TextMuted.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 12.dp))
                            }
                        }
                    }
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(uiState.error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ResultSectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        letterSpacing = 0.5.sp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedFoodRow(
    food: FoodItem,
    quantityInput: String,
    selectedUnit: FoodUnit,
    giInput: String,
    glContribution: Double?,
    isAdding: Boolean,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (FoodUnit) -> Unit,
    onGiChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    var unitMenuExpanded by remember { mutableStateOf(false) }
    val previewCal = quantityInput.toDoubleOrNull()
        ?.let { food.calculateCalories(it, selectedUnit).toInt() } ?: 0

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Food name + calorie/GL preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    val statLine = buildString {
                        if (previewCal > 0) append("$previewCal kcal")
                        if (glContribution != null) {
                            if (isNotEmpty()) append(" · ")
                            append("GL ${String.format("%.1f", glContribution)}")
                        }
                    }
                    if (statLine.isNotEmpty()) {
                        Text(statLine, fontSize = 11.sp, color = PrimaryGreen)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Clear, contentDescription = "Cancel", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity + unit + add button
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantityInput,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.width(90.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                ExposedDropdownMenuBox(
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                    modifier = Modifier.width(100.dp)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.shortLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                        modifier = Modifier.menuAnchor(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                    )
                    ExposedDropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        FoodUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text("${unit.shortLabel} (${unit.label})", fontSize = 13.sp) },
                                onClick = { onUnitChange(unit); unitMenuExpanded = false }
                            )
                        }
                    }
                }
                Button(
                    onClick = onAdd,
                    enabled = !isAdding && quantityInput.toDoubleOrNull()?.let { it > 0 } == true,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GI input row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = giInput,
                    onValueChange = onGiChange,
                    modifier = Modifier.width(80.dp),
                    label = { Text("GI", fontSize = 11.sp) },
                    placeholder = { Text("0–100", fontSize = 12.sp, color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
                )
                Text(
                    text = if (food.glycemicIndex != null && giInput.isEmpty())
                        "Glycemic Index · current: ${food.glycemicIndex}"
                    else
                        "Glycemic Index (optional, 0–100)\nSaved to this food for future logs",
                    fontSize = 10.sp,
                    color = TextMuted,
                    lineHeight = 14.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FoodResultRow(
    name: String,
    brand: String?,
    calPer100: Double,
    isOnline: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            if (!brand.isNullOrBlank()) {
                Text(brand, fontSize = 11.sp, color = TextSecondary)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isOnline) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = PrimaryGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        "online",
                        fontSize = 9.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
            Text("${calPer100.toInt()} kcal/100g", fontSize = 11.sp, color = TextMuted)
        }
    }
}

@Composable
private fun AddedItemRow(
    entry: QuickLogEntry,
    onRemove: () -> Unit
) {
    val gl = entry.food.glycemicIndex?.let { gi ->
        val grams = entry.food.toGrams(entry.quantity, entry.unit)
        val carbs = entry.food.carbsPer100 * grams / 100.0
        gi * carbs / 100.0
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.food.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(
                buildString {
                    append("${entry.quantity.toInt()}${entry.unit.shortLabel} · ${entry.calories} kcal")
                    if (gl != null) append(" · GL ${String.format("%.1f", gl)}")
                },
                fontSize = 11.sp,
                color = TextSecondary
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Clear,
                contentDescription = "Remove",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
