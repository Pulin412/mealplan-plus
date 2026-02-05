package com.mealplanplus.ui.screens.diets

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
import com.mealplanplus.data.model.Meal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDietScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddDietViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val availableMeals by viewModel.availableMeals.collectAsState()
    var selectedSlotForPicker by remember { mutableStateOf<DefaultMealSlot?>(null) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Diet") },
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
                    label = { Text("Diet Name *") },
                    placeholder = { Text("e.g., Low Carb Day") },
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
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Meal Slots", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Assign meals to each time slot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(DefaultMealSlot.entries.toList()) { slot ->
                MealSlotCard(
                    slot = slot,
                    selectedMeal = uiState.slotMeals[slot],
                    onSelectClick = { selectedSlotForPicker = slot },
                    onClear = { viewModel.setMealForSlot(slot, null) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::saveDiet,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Save Diet")
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

    selectedSlotForPicker?.let { slot ->
        MealPickerDialog(
            meals = availableMeals.filter { it.slotType == slot.name || it.slotType == "CUSTOM" },
            allMeals = availableMeals,
            selectedMealId = uiState.slotMeals[slot]?.id,
            onSelect = { meal ->
                viewModel.setMealForSlot(slot, meal)
                selectedSlotForPicker = null
            },
            onDismiss = { selectedSlotForPicker = null }
        )
    }
}

@Composable
fun MealSlotCard(
    slot: DefaultMealSlot,
    selectedMeal: Meal?,
    onSelectClick: () -> Unit,
    onClear: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelectClick)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.titleSmall
                )
                if (selectedMeal != null) {
                    Text(
                        text = selectedMeal.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Not assigned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selectedMeal != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            } else {
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = "Select",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MealPickerDialog(
    meals: List<Meal>,
    allMeals: List<Meal>,
    selectedMealId: Long?,
    onSelect: (Meal?) -> Unit,
    onDismiss: () -> Unit
) {
    val mealsToShow = if (meals.isEmpty()) allMeals else meals

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Meal") },
        text = {
            if (mealsToShow.isEmpty()) {
                Text("No meals available. Create some meals first!")
            } else {
                LazyColumn {
                    items(mealsToShow) { meal ->
                        val isSelected = meal.id == selectedMealId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(meal) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(meal.name, style = MaterialTheme.typography.bodyLarge)
                                meal.description?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
