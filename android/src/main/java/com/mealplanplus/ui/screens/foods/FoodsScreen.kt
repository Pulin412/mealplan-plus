package com.mealplanplus.ui.screens.foods

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.FoodItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(
    onNavigateToAddFood: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToOnlineSearch: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: FoodsViewModel = hiltViewModel()
) {
    val foods by viewModel.foods.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Foods") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToOnlineSearch) {
                        Icon(Icons.Default.Search, contentDescription = "Search Online")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                SmallFloatingActionButton(
                    onClick = onNavigateToOnlineSearch,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Online")
                }
                Spacer(Modifier.height(8.dp))
                SmallFloatingActionButton(
                    onClick = onNavigateToScanner,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Info, contentDescription = "Scan Barcode")
                }
                Spacer(Modifier.height(8.dp))
                FloatingActionButton(onClick = onNavigateToAddFood) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == FoodTab.ALL,
                    onClick = { viewModel.selectTab(FoodTab.ALL) },
                    text = { Text("All") },
                    icon = { Icon(Icons.Default.List, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == FoodTab.FAVORITES,
                    onClick = { viewModel.selectTab(FoodTab.FAVORITES) },
                    text = { Text("Favorites") },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == FoodTab.RECENT,
                    onClick = { viewModel.selectTab(FoodTab.RECENT) },
                    text = { Text("Recent") },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                )
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search foods...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (foods.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyText = when {
                        searchQuery.isNotBlank() -> "No foods found"
                        selectedTab == FoodTab.FAVORITES -> "No favorites yet.\nTap the heart to add favorites!"
                        selectedTab == FoodTab.RECENT -> "No recent foods.\nFoods you log will appear here."
                        else -> "No foods yet.\nTap + to add or scan a barcode!"
                    }
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(foods, key = { it.id }) { food ->
                        FoodItemCard(
                            food = food,
                            onToggleFavorite = { viewModel.toggleFavorite(food) },
                            onDelete = { viewModel.deleteFood(food) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemCard(
    food: FoodItem,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (food.barcode != null) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Has barcode",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                food.brand?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${food.servingSize.toInt()} ${food.servingUnit}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Cal: ${food.calories.toInt()} | P: ${food.protein.toInt()}g | C: ${food.carbs.toInt()}g | F: ${food.fat.toInt()}g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                food.glycemicIndex?.let {
                    Text(
                        text = "GI: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = when {
                            it <= 55 -> MaterialTheme.colorScheme.primary
                            it <= 69 -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
            Column {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (food.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (food.isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (food.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Food") },
            text = { Text("Delete ${food.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
