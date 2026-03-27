package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.MealFoodItemWithDetails
import kotlin.math.roundToInt

private val DetailGreen = Color(0xFF2E7D52)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    onNavigateBack: () -> Unit,
    readOnly: Boolean = false,
    viewModel: MealDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.slotLabel, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DetailGreen,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.allFoods.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No meal added to this slot yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF5F5F5)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Instructions card
                    if (uiState.instructions.isNotBlank()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Instructions",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DetailGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = uiState.instructions,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Ingredients header + sort chips
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Ingredients",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DetailGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SortChip(
                                            label = "A–Z",
                                            selected = uiState.sortOrder == IngredientSortOrder.ALPHABETICAL,
                                            onClick = { viewModel.setSortOrder(IngredientSortOrder.ALPHABETICAL) }
                                        )
                                        SortChip(
                                            label = "Qty",
                                            selected = uiState.sortOrder == IngredientSortOrder.QUANTITY,
                                            onClick = { viewModel.setSortOrder(IngredientSortOrder.QUANTITY) }
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE))

                                // Progress indicator (interactive mode only)
                                if (!readOnly) {
                                    val checkedCount = uiState.checkedFoodIds.size
                                    val totalCount = uiState.sortedFoods.size
                                    if (totalCount > 0) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "$checkedCount of $totalCount prepared",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (checkedCount > 0) {
                                                    Text(
                                                        "Clear all",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DetailGreen,
                                                        modifier = Modifier.clickable {
                                                            uiState.checkedFoodIds.forEach { viewModel.toggleChecked(it) }
                                                        }
                                                    )
                                                }
                                            }
                                            LinearProgressIndicator(
                                                progress = { if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp),
                                                color = DetailGreen,
                                                trackColor = Color(0xFFE0E0E0)
                                            )
                                        }
                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                    }
                                }

                                uiState.sortedFoods.forEachIndexed { idx, food ->
                                    IngredientCheckRow(
                                        item = food,
                                        checked = food.mealFoodItem.foodId in uiState.checkedFoodIds,
                                        onToggle = { viewModel.toggleChecked(food.mealFoodItem.foodId) },
                                        readOnly = readOnly
                                    )
                                    if (idx < uiState.sortedFoods.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Color(0xFFF0F0F0)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Totals card
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Totals",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DetailGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TotalMacroCell("🔥", "${uiState.totalCalories.roundToInt()}", "kcal", Color(0xFFE65100))
                                    TotalMacroCell("💪", "${uiState.totalProtein.roundToInt()}g", "protein", Color(0xFF2E7D32))
                                    TotalMacroCell("🍞", "${uiState.totalCarbs.roundToInt()}g", "carbs", Color(0xFF1565C0))
                                    TotalMacroCell("🥑", "${uiState.totalFat.roundToInt()}g", "fat", Color(0xFF6A1B9A))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = DetailGreen,
            selectedLabelColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun IngredientCheckRow(
    item: MealFoodItemWithDetails,
    checked: Boolean,
    onToggle: () -> Unit,
    readOnly: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!readOnly) Modifier.clickable { onToggle() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!readOnly) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = DetailGreen,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (!readOnly && checked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (!readOnly && checked) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${item.mealFoodItem.quantity.toInt()} ${item.mealFoodItem.unit.shortLabel}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "${item.calculatedCalories.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE65100)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MacroPill("P ${item.calculatedProtein.roundToInt()}g", Color(0xFF2E7D32))
                MacroPill("C ${item.calculatedCarbs.roundToInt()}g", Color(0xFF1565C0))
                MacroPill("F ${item.calculatedFat.roundToInt()}g", Color(0xFF6A1B9A))
            }
        }
    }
}

@Composable
private fun MacroPill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun TotalMacroCell(icon: String, value: String, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
