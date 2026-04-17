package com.mealplanplus.ui.screens.log

import androidx.compose.foundation.background
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
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogMealPickerScreen(
    slotType: String,
    onNavigateBack: () -> Unit,
    onMealSelected: (Long, Double) -> Unit,
    onNavigateHome: () -> Unit = {},
    viewModel: LogMealPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedMealForQuantity by remember { mutableStateOf<MealWithFoods?>(null) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(slotType) {
        viewModel.setSlotType(slotType)
    }

    // Show success snackbar
    LaunchedEffect(showSuccessSnackbar) {
        if (showSuccessSnackbar) {
            val result = snackbarHostState.showSnackbar(
                message = "Meal logged successfully!",
                actionLabel = "Go Home",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onNavigateHome()
            }
            showSuccessSnackbar = false
        }
    }

    Scaffold(
        containerColor = BgPage,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    val slot = DefaultMealSlot.entries.find { it.name == slotType }
                    Text("Add ${slot?.displayName ?: "Meal"}", color = TextPrimary)
                },
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search meals...") },
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

            // Filter chips for slot type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.filterSlot == null,
                    onClick = { viewModel.setFilterSlot(null) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.filterSlot == slotType,
                    onClick = { viewModel.setFilterSlot(slotType) },
                    label = {
                        val slot = DefaultMealSlot.entries.find { it.name == slotType }
                        Text(slot?.displayName ?: slotType)
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Results count
            Text(
                text = "${uiState.filteredMeals.size} meals",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Meals list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.filteredMeals.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (uiState.searchQuery.isNotEmpty()) "No meals match your search" else "No meals available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMuted
                            )
                        }
                    }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredMeals.distinctBy { it.meal.id }, key = { it.meal.id }) { mealWithFoods ->
                        MealCard(
                            mealWithFoods = mealWithFoods,
                            onClick = { selectedMealForQuantity = mealWithFoods }
                        )
                    }
                }
            }
        }
    }

    // Quantity picker dialog
    selectedMealForQuantity?.let { mealWithFoods ->
        QuantityPickerDialog(
            mealName = mealWithFoods.meal.name,
            calories = mealWithFoods.totalCalories.toInt(),
            onConfirm = { quantity ->
                onMealSelected(mealWithFoods.meal.id, quantity)
                showSuccessSnackbar = true
            },
            onDismiss = { selectedMealForQuantity = null }
        )
    }
}

@Composable
fun MealCard(
    mealWithFoods: MealWithFoods,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mealWithFoods.meal.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "${mealWithFoods.totalCalories.toInt()} cal",
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignGreen
                )
            }

            if (mealWithFoods.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = mealWithFoods.items.take(3).joinToString(", ") { it.food.name } +
                            if (mealWithFoods.items.size > 3) " +${mealWithFoods.items.size - 3} more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "P: ${mealWithFoods.totalProtein.toInt()}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChartProtein
                )
                Text(
                    text = "C: ${mealWithFoods.totalCarbs.toInt()}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChartCarbs
                )
                Text(
                    text = "F: ${mealWithFoods.totalFat.toInt()}g",
                    style = MaterialTheme.typography.labelMedium,
                    color = ChartFat
                )
            }
        }
    }
}

@Composable
fun QuantityPickerDialog(
    mealName: String,
    calories: Int,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(1.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add $mealName", color = TextPrimary) },
        containerColor = CardBg,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${(calories * quantity).toInt()} cal",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DesignGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (quantity > 0.5) quantity -= 0.5 },
                        enabled = quantity > 0.5
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Decrease")
                    }
                    Text(
                        text = "%.1f servings".format(quantity),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    IconButton(onClick = { quantity += 0.5 }) {
                        Icon(Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(quantity); onDismiss() }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
