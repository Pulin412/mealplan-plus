package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
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

            uiState.foods.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No meal added to this slot yet.",
                        style = MaterialTheme.typography.bodyMedium,
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Instructions card (only if set)
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
                                        "How to prepare",
                                        style = MaterialTheme.typography.titleSmall,
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

                    // Ingredients header + list
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "Ingredients",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DetailGreen,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                // Macro header row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF9F9F9))
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Food",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(2f),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    MacroHeader("kcal", Modifier.weight(1f))
                                    MacroHeader("P", Modifier.weight(1f))
                                    MacroHeader("C", Modifier.weight(1f))
                                    MacroHeader("F", Modifier.weight(1f))
                                }
                                HorizontalDivider(color = Color(0xFFEEEEEE))
                                uiState.foods.forEachIndexed { idx, food ->
                                    MealDetailFoodRow(food)
                                    if (idx < uiState.foods.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = Color(0xFFF5F5F5)
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
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DetailGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(10.dp))
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
private fun MacroHeader(label: String, modifier: Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun MealDetailFoodRow(item: MealFoodItemWithDetails) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = item.food.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.mealFoodItem.quantity.toInt()} ${item.mealFoodItem.unit.shortLabel}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MacroValue("${item.calculatedCalories.roundToInt()}", Color(0xFFE65100), Modifier.weight(1f))
        MacroValue("${item.calculatedProtein.roundToInt()}g", Color(0xFF2E7D32), Modifier.weight(1f))
        MacroValue("${item.calculatedCarbs.roundToInt()}g", Color(0xFF1565C0), Modifier.weight(1f))
        MacroValue("${item.calculatedFat.roundToInt()}g", Color(0xFF6A1B9A), Modifier.weight(1f))
    }
}

@Composable
private fun MacroValue(value: String, color: Color, modifier: Modifier) {
    Text(
        value,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = modifier,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun TotalMacroCell(icon: String, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(icon, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
