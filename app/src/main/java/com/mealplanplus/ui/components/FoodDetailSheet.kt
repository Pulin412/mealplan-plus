package com.mealplanplus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.UsdaFoodResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailSheet(
    food: FoodItem?,
    usdaFood: UsdaFoodResult?,
    initialQuantity: Double = 1.0,
    onConfirm: (quantity: Double) -> Unit,
    onDismiss: () -> Unit
) {
    val name = food?.name ?: usdaFood?.name ?: ""
    val brand = food?.brand ?: usdaFood?.brand
    val baseCalories = food?.calories ?: usdaFood?.calories ?: 0.0
    val baseProtein = food?.protein ?: usdaFood?.protein ?: 0.0
    val baseCarbs = food?.carbs ?: usdaFood?.carbs ?: 0.0
    val baseFat = food?.fat ?: usdaFood?.fat ?: 0.0
    val servingSize = food?.servingSize ?: usdaFood?.servingSize ?: 100.0
    val servingUnit = food?.servingUnit ?: usdaFood?.servingUnit ?: "g"

    var quantityText by remember { mutableStateOf(initialQuantity.toString()) }
    val quantity = quantityText.toDoubleOrNull() ?: 1.0

    // Calculate adjusted macros
    val calories = baseCalories * quantity
    val protein = baseProtein * quantity
    val carbs = baseCarbs * quantity
    val fat = baseFat * quantity

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            brand?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Quantity input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Servings") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Quick quantity buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.5, 1.0, 1.5, 2.0).forEach { q ->
                        FilterChip(
                            selected = quantity == q,
                            onClick = { quantityText = if (q == q.toLong().toDouble()) q.toInt().toString() else q.toString() },
                            label = { Text(if (q == q.toLong().toDouble()) q.toInt().toString() else q.toString()) }
                        )
                    }
                }
            }

            Text(
                text = "1 serving = ${servingSize.toInt()} $servingUnit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Macro breakdown
            Text(
                "Nutrition (${quantity}x serving)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            // Calories highlight
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Calories", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${calories.toInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Macros row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroCard(
                    label = "Protein",
                    value = protein,
                    unit = "g",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Carbs",
                    value = carbs,
                    unit = "g",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Fat",
                    value = fat,
                    unit = "g",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))

            // Add button
            Button(
                onClick = { onConfirm(quantity) },
                modifier = Modifier.fillMaxWidth(),
                enabled = quantity > 0
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add to Meal")
            }
        }
    }
}

@Composable
fun MacroCard(
    label: String,
    value: Double,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.1f".format(value)}$unit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
