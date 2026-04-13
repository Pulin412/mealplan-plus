package com.mealplanplus.ui.screens.meals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.UsdaFoodResult
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DesignGreenLight
import com.mealplanplus.ui.theme.TagBlue
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

// Unit types for quantity measurement
enum class MeasureUnit(val label: String) {
    SERVING("Servings"),
    GRAM("Grams (g)"),
    ML("Milliliters (ml)"),
    PIECE("Pieces")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPickerScreen(
    onNavigateBack: () -> Unit,
    onFoodSelected: (FoodItem, Double, com.mealplanplus.data.model.FoodUnit) -> Unit,
    onUsdaFoodSelected: (UsdaFoodResult, Double, com.mealplanplus.data.model.FoodUnit) -> Unit,
    viewModel: FoodPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val allFoods by viewModel.allFoods.collectAsState()

    // Detail sheet state
    var selectedLocalFood by remember { mutableStateOf<FoodItem?>(null) }
    var selectedUsdaFood by remember { mutableStateOf<UsdaFoodResult?>(null) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = { Text("Select Food", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search rice, chicken, dal, apple...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Results
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Show search results or all foods
                if (uiState.searchQuery.length < 2) {
                    // Show all local foods
                    item {
                        Text(
                            "Your Foods (${allFoods.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = DesignGreen,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    if (allFoods.isEmpty()) {
                        item {
                            EmptyState(
                                icon = Icons.Default.Info,
                                title = "No foods yet",
                                subtitle = "Type to search, then use 'Search USDA' button"
                            )
                        }
                    } else {
                        items(allFoods.distinctBy { it.id }, key = { it.id }) { food ->
                            FoodSearchCard(
                                name = food.name,
                                subtitle = food.brand,
                                calories = food.calories,
                                protein = food.protein,
                                carbs = food.carbs,
                                fat = food.fat,
                                servingSize = food.servingSize,
                                servingUnit = food.servingUnit,
                                isLocal = true,
                                onClick = { selectedLocalFood = food }
                            )
                        }
                    }
                } else {
                    // Local results
                    if (uiState.localResults.isNotEmpty()) {
                        item {
                            Text(
                                "Your Foods (${uiState.localResults.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = DesignGreen,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(uiState.localResults.distinctBy { it.id }, key = { it.id }) { food ->
                            FoodSearchCard(
                                name = food.name,
                                subtitle = food.brand,
                                calories = food.calories,
                                protein = food.protein,
                                carbs = food.carbs,
                                fat = food.fat,
                                servingSize = food.servingSize,
                                servingUnit = food.servingUnit,
                                isLocal = true,
                                onClick = { selectedLocalFood = food }
                            )
                        }
                    } else if (!uiState.hasSearchedUsda) {
                        item {
                            Text(
                                "No local foods found for \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // USDA Search Button
                    if (!uiState.hasSearchedUsda) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.searchUsda() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isSearchingUsda && uiState.searchQuery.length >= 2
                            ) {
                                if (uiState.isSearchingUsda) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Searching USDA...")
                                } else {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Search USDA Database (300k+ foods)")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    // USDA Results (only shown after button click)
                    if (uiState.hasSearchedUsda) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "USDA Database (${uiState.usdaResults.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TagBlue
                                )
                            }
                        }

                        when {
                            uiState.searchError != null -> {
                                item {
                                    Text(
                                        uiState.searchError!!,
                                        color = TextDestructive,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            uiState.usdaResults.isEmpty() -> {
                                item {
                                    Text(
                                        "No foods found in USDA database",
                                        color = TextSecondary,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            else -> {
                                items(uiState.usdaResults.distinctBy { it.fdcId }, key = { it.fdcId }) { food ->
                                    FoodSearchCard(
                                        name = food.name,
                                        subtitle = food.brand,
                                        calories = food.calories,
                                        protein = food.protein,
                                        carbs = food.carbs,
                                        fat = food.fat,
                                        servingSize = food.servingSize,
                                        servingUnit = food.servingUnit,
                                        isLocal = false,
                                        onClick = { selectedUsdaFood = food }
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }

    // Food detail bottom sheet for quantity selection
    if (selectedLocalFood != null) {
        FoodDetailBottomSheet(
            name = selectedLocalFood!!.name,
            brand = selectedLocalFood!!.brand,
            calories = selectedLocalFood!!.calories,
            protein = selectedLocalFood!!.protein,
            carbs = selectedLocalFood!!.carbs,
            fat = selectedLocalFood!!.fat,
            servingSize = selectedLocalFood!!.servingSize,
            servingUnit = selectedLocalFood!!.servingUnit,
            gramsPerPiece = selectedLocalFood!!.gramsPerPiece,
            onConfirm = { quantity, unit ->
                onFoodSelected(selectedLocalFood!!, quantity, unit)
                selectedLocalFood = null
            },
            onDismiss = { selectedLocalFood = null }
        )
    }

    if (selectedUsdaFood != null) {
        FoodDetailBottomSheet(
            name = selectedUsdaFood!!.name,
            brand = selectedUsdaFood!!.brand,
            calories = selectedUsdaFood!!.calories,
            protein = selectedUsdaFood!!.protein,
            carbs = selectedUsdaFood!!.carbs,
            fat = selectedUsdaFood!!.fat,
            servingSize = selectedUsdaFood!!.servingSize,
            servingUnit = selectedUsdaFood!!.servingUnit,
            gramsPerPiece = null,
            onConfirm = { quantity, unit ->
                onUsdaFoodSelected(selectedUsdaFood!!, quantity, unit)
                selectedUsdaFood = null
            },
            onDismiss = { selectedUsdaFood = null }
        )
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchCard(
    name: String,
    subtitle: String?,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    servingSize: Double,
    servingUnit: String,
    isLocal: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${calories.toInt()} cal per ${servingSize.toInt()}$servingUnit",
                    style = MaterialTheme.typography.bodySmall,
                    color = DesignGreen
                )
                Text(
                    "P:${protein.toInt()}g  C:${carbs.toInt()}g  F:${fat.toInt()}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isLocal) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Local",
                        tint = DesignGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Select",
                    tint = TextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDetailBottomSheet(
    name: String,
    brand: String?,
    calories: Double,
    protein: Double,
    carbs: Double,
    fat: Double,
    servingSize: Double,
    servingUnit: String,
    gramsPerPiece: Double? = null,
    onConfirm: (Double, com.mealplanplus.data.model.FoodUnit) -> Unit,
    onDismiss: () -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf(MeasureUnit.SERVING) }
    var unitDropdownExpanded by remember { mutableStateOf(false) }

    val quantity = quantityText.toDoubleOrNull() ?: 1.0

    // Calculate multiplier for live macro preview only
    // SERVING: 1 serving = servingSize g → multiplier = quantity * servingSize / 100
    // GRAM: multiplier = quantity / 100
    // ML: multiplier = quantity / 100
    // PIECE: multiplier = quantity * (gramsPerPiece ?: 100) / 100
    val multiplier = when (selectedUnit) {
        MeasureUnit.SERVING -> quantity * servingSize / 100.0
        MeasureUnit.GRAM -> quantity / 100.0
        MeasureUnit.ML -> quantity / 100.0
        MeasureUnit.PIECE -> quantity * (gramsPerPiece ?: 100.0) / 100.0
    }

    val adjustedCalories = calories * multiplier
    val adjustedProtein = protein * multiplier
    val adjustedCarbs = carbs * multiplier
    val adjustedFat = fat * multiplier

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            brand?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Spacer(Modifier.height(16.dp))

            // Quantity and Unit selector
            Text("Amount", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    label = { Text("Qty") }
                )

                // Unit dropdown
                ExposedDropdownMenuBox(
                    expanded = unitDropdownExpanded,
                    onExpandedChange = { unitDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = unitDropdownExpanded,
                        onDismissRequest = { unitDropdownExpanded = false }
                    ) {
                        MeasureUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.label) },
                                onClick = {
                                    selectedUnit = unit
                                    // Reset quantity when switching units
                                    quantityText = when (unit) {
                                        MeasureUnit.SERVING -> "1"
                                        MeasureUnit.GRAM -> servingSize.toInt().toString()
                                        MeasureUnit.ML -> servingSize.toInt().toString()
                                        MeasureUnit.PIECE -> "1"
                                    }
                                    unitDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Quick quantity buttons
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val quickValues = when (selectedUnit) {
                    MeasureUnit.SERVING -> listOf(0.5, 1.0, 1.5, 2.0)
                    MeasureUnit.GRAM -> listOf(25.0, 50.0, 100.0, 150.0)
                    MeasureUnit.ML -> listOf(50.0, 100.0, 150.0, 200.0)
                    MeasureUnit.PIECE -> listOf(1.0, 2.0, 3.0, 4.0)
                }
                quickValues.forEach { q ->
                    FilterChip(
                        selected = quantity == q,
                        onClick = { quantityText = if (q == q.toLong().toDouble()) q.toInt().toString() else q.toString() },
                        label = { Text(if (q == q.toLong().toDouble()) q.toInt().toString() else q.toString()) }
                    )
                }
            }

            val hintText = when (selectedUnit) {
                MeasureUnit.PIECE -> if (gramsPerPiece != null) "1 piece ≈ ${gramsPerPiece.toInt()}g" else "1 piece ≈ ~100g"
                else -> "1 serving = ${servingSize.toInt()} $servingUnit"
            }
            Text(
                hintText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(Modifier.height(20.dp))
            Divider()
            Spacer(Modifier.height(16.dp))

            // Macros
            Text("Nutrition", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            // Calories card
            Card(
                colors = CardDefaults.cardColors(containerColor = DesignGreenLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calories", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${adjustedCalories.toInt()} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = DesignGreen
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Macro row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroChip("Protein", adjustedProtein, "g", ChartProtein, Modifier.weight(1f))
                MacroChip("Carbs", adjustedCarbs, "g", ChartCarbs, Modifier.weight(1f))
                MacroChip("Fat", adjustedFat, "g", ChartFat, Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val foodUnit = when (selectedUnit) {
                        MeasureUnit.SERVING -> com.mealplanplus.data.model.FoodUnit.SERVING
                        MeasureUnit.GRAM -> com.mealplanplus.data.model.FoodUnit.GRAM
                        MeasureUnit.ML -> com.mealplanplus.data.model.FoodUnit.ML
                        MeasureUnit.PIECE -> com.mealplanplus.data.model.FoodUnit.PIECE
                    }
                    onConfirm(quantity, foodUnit)
                },
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
fun MacroChip(
    label: String,
    value: Double,
    unit: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(Modifier.height(4.dp))
            Text(
                "${"%.1f".format(value)}$unit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
