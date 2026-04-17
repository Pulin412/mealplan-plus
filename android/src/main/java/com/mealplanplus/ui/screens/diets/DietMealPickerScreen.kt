package com.mealplanplus.ui.screens.diets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.ui.components.DietBrowserSection
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietMealPickerScreen(
    slotType: String,
    onNavigateBack: () -> Unit,
    onMealSelected: (Long) -> Unit,
    viewModel: DietMealPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = {
                    val slot = DefaultMealSlot.entries.find { it.name == slotType }
                    Text("Select ${slot?.displayName ?: "Meal"}", color = TextPrimary)
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
                placeholder = { Text("Search meals or foods...") },
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

            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = uiState.filterSlot == null && !uiState.showDietBrowser,
                        onClick = { viewModel.setFilterSlot(null) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.filterSlot == slotType && !uiState.showDietBrowser,
                        onClick = { viewModel.setFilterSlot(slotType) },
                        label = {
                            val slot = DefaultMealSlot.entries.find { it.name == slotType }
                            Text(slot?.displayName ?: slotType)
                        }
                    )
                }
                // Common slot filters
                DefaultMealSlot.entries
                    .filter { it.name != slotType }
                    .take(3)
                    .forEach { slot ->
                        item {
                            FilterChip(
                                selected = uiState.filterSlot == slot.name && !uiState.showDietBrowser,
                                onClick = { viewModel.setFilterSlot(slot.name) },
                                label = { Text(slot.displayName) }
                            )
                        }
                    }
                item {
                    FilterChip(
                        selected = uiState.showDietBrowser,
                        onClick = { viewModel.setShowDietBrowser(true) },
                        label = { Text("From Diets") },
                        leadingIcon = if (uiState.showDietBrowser) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Content based on mode
            if (uiState.showDietBrowser) {
                // Diet browser mode
                val filteredDiets = viewModel.getFilteredDiets()

                Text(
                    text = "${filteredDiets.size} diets",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                DietBrowserSection(
                    diets = filteredDiets,
                    expandedDietId = uiState.expandedDietId,
                    onDietClick = viewModel::toggleExpandDiet,
                    onMealSelected = onMealSelected,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Meal list mode
                Text(
                    text = "${uiState.filteredMeals.size} meals",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                                text = if (uiState.searchQuery.isNotEmpty())
                                    "No meals match your search"
                                else
                                    "No meals available",
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
                        items(
                            uiState.filteredMeals.distinctBy { it.meal.id },
                            key = { it.meal.id }
                        ) { mealWithFoods ->
                            PickerMealCard(
                                mealWithFoods = mealWithFoods,
                                onClick = { onMealSelected(mealWithFoods.meal.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickerMealCard(
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${mealWithFoods.totalCalories.toInt()} cal",
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignGreen
                )
            }

            if (mealWithFoods.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mealWithFoods.items.take(3).joinToString(", ") { it.food.name } +
                            if (mealWithFoods.items.size > 3) " +${mealWithFoods.items.size - 3} more" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
