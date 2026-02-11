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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealWithFoods

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMealSlot: (String) -> Unit,
    viewModel: DietDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isEditing) {
                        Text("Edit Diet")
                    } else {
                        Text(uiState.diet?.name ?: "Diet Details")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.isEditing) {
                            viewModel.cancelEditing()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            if (uiState.isEditing) Icons.Default.Close else Icons.Default.ArrowBack,
                            contentDescription = if (uiState.isEditing) "Cancel" else "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = { viewModel.saveDiet() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    } else {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.diet == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Diet not found")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Edit mode fields
                    if (uiState.isEditing) {
                        item {
                            OutlinedTextField(
                                value = uiState.editName,
                                onValueChange = viewModel::updateName,
                                label = { Text("Diet Name *") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = uiState.editDescription,
                                onValueChange = viewModel::updateDescription,
                                label = { Text("Description") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                        }
                        // Tags editor
                        item {
                            Column {
                                Text(
                                    text = "Tags",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    DietTag.entries.filter { it != DietTag.CUSTOM }.forEach { tag ->
                                        FilterChip(
                                            selected = uiState.editTags.contains(tag),
                                            onClick = { viewModel.toggleTag(tag) },
                                            label = { Text(tag.displayName) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // View mode header
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                uiState.diet?.description?.let { desc ->
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Show tags in view mode
                                val tags = uiState.diet?.getTagList() ?: emptyList()
                                if (tags.isNotEmpty() && tags.any { it != DietTag.CUSTOM }) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        tags.filter { it != DietTag.CUSTOM }.forEach { tag ->
                                            Surface(
                                                color = when (tag) {
                                                    DietTag.REMISSION -> MaterialTheme.colorScheme.primaryContainer
                                                    DietTag.MAINTENANCE -> MaterialTheme.colorScheme.secondaryContainer
                                                    DietTag.SOS -> MaterialTheme.colorScheme.errorContainer
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(
                                                    text = tag.displayName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    color = when (tag) {
                                                        DietTag.REMISSION -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        DietTag.MAINTENANCE -> MaterialTheme.colorScheme.onSecondaryContainer
                                                        DietTag.SOS -> MaterialTheme.colorScheme.onErrorContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Macro summary card
                    item {
                        uiState.dietWithMeals?.let { dwm ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "Daily Totals",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        MacroDisplay("Calories", "${dwm.totalCalories.toInt()}")
                                        MacroDisplay("Protein", "${dwm.totalProtein.toInt()}g")
                                        MacroDisplay("Carbs", "${dwm.totalCarbs.toInt()}g")
                                        MacroDisplay("Fat", "${dwm.totalFat.toInt()}g")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Divider()
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Meal Slots",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Meal slots
                    items(DefaultMealSlot.entries.sortedBy { it.order }) { slot ->
                        val mealWithFoods = uiState.dietWithMeals?.meals?.get(slot.name)
                        MealSlotCard(
                            slot = slot,
                            mealWithFoods = mealWithFoods,
                            onClick = { onNavigateToMealSlot(slot.name) }
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
            text = { Text("Are you sure you want to delete ${uiState.diet?.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDiet { onNavigateBack() }
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error briefly then clear
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
}

@Composable
fun MacroDisplay(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun MealSlotCard(
    slot: DefaultMealSlot,
    mealWithFoods: MealWithFoods?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = slot.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (mealWithFoods != null) {
                    Text(
                        text = mealWithFoods.meal.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${mealWithFoods.totalCalories.toInt()} cal • P:${mealWithFoods.totalProtein.toInt()}g C:${mealWithFoods.totalCarbs.toInt()}g F:${mealWithFoods.totalFat.toInt()}g",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (mealWithFoods.items.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mealWithFoods.items.take(3).joinToString(", ") { it.food.name } +
                                    if (mealWithFoods.items.size > 3) " +${mealWithFoods.items.size - 3} more" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = "Not assigned",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Select meal",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MealPickerDialog(
    slotName: String,
    currentMealId: Long?,
    availableMeals: List<Meal>,
    onMealSelected: (Meal?) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select meal for $slotName") },
        text = {
            LazyColumn {
                // Clear option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMealSelected(null) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Clear / Not assigned",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Divider()
                }

                items(availableMeals.distinctBy { it.id }, key = { it.id }) { meal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMealSelected(meal) }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meal.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            meal.description?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (meal.id == currentMealId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
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
