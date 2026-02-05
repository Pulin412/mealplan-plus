package com.mealplanplus.ui.screens.meals

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddMealViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableFoods by viewModel.availableFoods.collectAsState()
    var showFoodPicker by remember { mutableStateOf(false) }
    var slotExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Meal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::updateName,
                    label = { Text("Meal Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = slotExpanded,
                    onExpandedChange = { slotExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedSlot.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Meal Slot") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(slotExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = slotExpanded,
                        onDismissRequest = { slotExpanded = false }
                    ) {
                        DefaultMealSlot.entries.forEach { slot ->
                            DropdownMenuItem(
                                text = { Text(slot.displayName) },
                                onClick = {
                                    viewModel.updateSlot(slot)
                                    slotExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Food Items", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showFoodPicker = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Food")
                    }
                }
            }

            if (uiState.selectedFoods.isEmpty()) {
                item {
                    Text(
                        text = "No foods added yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(uiState.selectedFoods, key = { it.food.id }) { sf ->
                SelectedFoodCard(
                    food = sf.food,
                    quantity = sf.quantity,
                    onQuantityChange = { viewModel.updateFoodQuantity(sf.food.id, it) },
                    onRemove = { viewModel.removeFood(sf.food.id) }
                )
            }

            if (uiState.selectedFoods.isNotEmpty()) {
                item {
                    val totalCal = uiState.selectedFoods.sumOf { it.food.calories * it.quantity }
                    val totalP = uiState.selectedFoods.sumOf { it.food.protein * it.quantity }
                    val totalC = uiState.selectedFoods.sumOf { it.food.carbs * it.quantity }
                    val totalF = uiState.selectedFoods.sumOf { it.food.fat * it.quantity }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Total Macros", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Cal: ${totalCal.toInt()} | P: ${totalP.toInt()}g | C: ${totalC.toInt()}g | F: ${totalF.toInt()}g",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::saveMeal,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Meal")
                    }
                }
            }

            uiState.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showFoodPicker) {
        FoodPickerDialog(
            foods = availableFoods,
            selectedIds = uiState.selectedFoods.map { it.food.id }.toSet(),
            onSelect = { viewModel.addFood(it) },
            onDismiss = { showFoodPicker = false }
        )
    }
}

@Composable
fun SelectedFoodCard(
    food: FoodItem,
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${food.servingSize.toInt()} ${food.servingUnit} • ${(food.calories * quantity).toInt()} cal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (quantity > 0.5) onQuantityChange(quantity - 0.5) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Decrease")
                }
                Text(
                    text = if (quantity == quantity.toLong().toDouble()) "${quantity.toInt()}" else "%.1f".format(quantity),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(
                    onClick = { onQuantityChange(quantity + 0.5) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increase")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
        }
    }
}

@Composable
fun FoodPickerDialog(
    foods: List<FoodItem>,
    selectedIds: Set<Long>,
    onSelect: (FoodItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Food") },
        text = {
            if (foods.isEmpty()) {
                Text("No foods available. Add some foods first!")
            } else {
                LazyColumn {
                    items(foods) { food ->
                        val isSelected = food.id in selectedIds
                        ListItem(
                            headlineContent = { Text(food.name) },
                            supportingContent = {
                                Text("${food.calories.toInt()} cal per ${food.servingSize.toInt()} ${food.servingUnit}")
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.clickable(enabled = !isSelected) {
                                onSelect(food)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
private fun ListItem(
    headlineContent: @Composable () -> Unit,
    supportingContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            headlineContent()
            supportingContent()
        }
        trailingContent()
    }
}

private fun Modifier.clickable(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    if (enabled) this.then(Modifier.clickable(onClick = onClick)) else this
