package com.mealplanplus.ui.screens.log

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.ui.screens.diets.AdvancedFilterSection
import com.mealplanplus.ui.screens.diets.DietCard
import com.mealplanplus.ui.screens.diets.DietsTopBar
import com.mealplanplus.ui.screens.diets.DietsViewModel
import com.mealplanplus.ui.screens.diets.TagFilterRow
import java.time.LocalDate
import com.mealplanplus.ui.theme.BgPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietPickerScreen(
    date: String,
    onNavigateBack: () -> Unit,
    onDietSelected: (Long, String) -> Unit,
    onNavigateHome: () -> Unit = {},
    viewModel: DietsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDietId by remember { mutableStateOf<Long?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showAdvancedFilters by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val parsedDate = try { LocalDate.parse(date) } catch (e: Exception) { LocalDate.now() }
    val isFutureDate = parsedDate.isAfter(LocalDate.now())
    val actionText = if (isFutureDate) "Plan" else "Log"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DietsTopBar(
                totalCount = uiState.totalDietCount,
                shownCount = uiState.diets.size,
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::updateSearchQuery,
                onNewDiet = null,  // no new diet in picker
                onNavigateBack = onNavigateBack,
                onTagsSettings = null,
                onFavouritesToggle = viewModel::toggleFavouritesFilter,
                favouriteCount = uiState.favouriteDiets.size,
                showFavouritesOnly = uiState.showFavouritesOnly,
                title = "Select Diet"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPage)
        ) {
            // Tag filter row
            TagFilterRow(
                tags = uiState.allTags,
                selectedTagIds = uiState.selectedTagIds,
                tagCountMap = uiState.tagCountMap,
                totalCount = uiState.totalDietCount,
                onTagClick = viewModel::toggleTagFilter,
                onAllClick = viewModel::clearTagFilters
            )

            // Advanced filters toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                    Text(
                        text = if (showAdvancedFilters) "Hide Filters" else "More Filters",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                AdvancedFilterSection(
                    foodFilter = uiState.foodFilter,
                    slotFilter = uiState.slotFilter,
                    onFoodFilterChange = viewModel::updateFoodFilter,
                    onSlotToggle = viewModel::toggleSlotFilter,
                    onClearAll = { viewModel.clearAllFilters(); showAdvancedFilters = false }
                )
            }

            HorizontalDivider()

            // Content
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.diets.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                uiState.showFavouritesOnly -> "No favourite diets yet.\nTap ⭐ on a diet to mark it."
                                uiState.searchQuery.isBlank() && uiState.selectedTagIds.isEmpty() -> "No diets available"
                                else -> "No diets match your filters"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.diets, key = { it.diet.id }) { item ->
                            DietCard(
                                item = item,
                                onSelect = {
                                    selectedDietId = item.diet.id
                                    showConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog && selectedDietId != null) {
        val selectedItem = uiState.diets.find { it.diet.id == selectedDietId }
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false; selectedDietId = null },
            title = { Text("$actionText Diet") },
            text = {
                Column {
                    Text("$actionText \"${selectedItem?.diet?.name}\" for $date?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${selectedItem?.totalCalories ?: 0} cal · ${selectedItem?.mealCount ?: 0} meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedDietId?.let { id ->
                        onDietSelected(id, date)
                        showConfirmDialog = false
                    }
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false; selectedDietId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
