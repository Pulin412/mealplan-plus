package com.mealplanplus.ui.screens.foods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.Tag
import com.mealplanplus.ui.components.TagChip
import com.mealplanplus.ui.screens.diets.TagFilterRow
import com.mealplanplus.ui.screens.diets.TagsManagementDialog

private val FoodGreen = Color(0xFF2E7D52)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(
    onNavigateToAddFood: () -> Unit = {},
    onNavigateToScanner: () -> Unit = {},
    onNavigateToOnlineSearch: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    pickerMode: Boolean = false,
    onFoodSelected: ((FoodItem, Double) -> Unit)? = null,
    viewModel: FoodsViewModel = hiltViewModel()
) {
    val foods by viewModel.foods.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()
    var showTagsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            FoodsTopBar(
                totalCount = foods.size,
                searchQuery = searchQuery,
                onSearchChange = viewModel::onSearchQueryChange,
                onNavigateBack = onNavigateBack,
                onNewFood = if (!pickerMode) onNavigateToAddFood else null,
                onTagsSettings = if (!pickerMode) { { showTagsDialog = true } } else null,
                onNavigateToOnlineSearch = if (!pickerMode) onNavigateToOnlineSearch else null,
                onNavigateToScanner = if (!pickerMode) onNavigateToScanner else null,
                pickerMode = pickerMode
            )
        },
        floatingActionButton = {
            if (!pickerMode) {
                Column(horizontalAlignment = Alignment.End) {
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            // Tag filter row
            TagFilterRow(
                tags = tags,
                selectedTagIds = selectedTagIds,
                tagCountMap = emptyMap(),
                totalCount = foods.size,
                onTagClick = viewModel::toggleTag,
                onAllClick = viewModel::clearTagFilter
            )

            // Tab row
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.White,
                contentColor = FoodGreen
            ) {
                Tab(
                    selected = selectedTab == FoodTab.ALL,
                    onClick = { viewModel.selectTab(FoodTab.ALL) },
                    text = { Text("All") }
                )
                Tab(
                    selected = selectedTab == FoodTab.FAVORITES,
                    onClick = { viewModel.selectTab(FoodTab.FAVORITES) },
                    text = { Text("Favorites") }
                )
                Tab(
                    selected = selectedTab == FoodTab.RECENT,
                    onClick = { viewModel.selectTab(FoodTab.RECENT) },
                    text = { Text("Recent") }
                )
            }

            HorizontalDivider()

            when {
                foods.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val emptyText = when {
                            searchQuery.isNotBlank() -> "No foods found"
                            selectedTagIds.isNotEmpty() -> "No foods match selected tags"
                            selectedTab == FoodTab.FAVORITES -> "No favorites yet.\nTap ♡ to add favorites!"
                            selectedTab == FoodTab.RECENT -> "No recent foods."
                            else -> "No foods yet.\nTap + to add or scan a barcode!"
                        }
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(foods, key = { it.id }) { food ->
                            FoodCard(
                                food = food,
                                tags = tags.filter { /* populated by getTagsForFood in expanded view */ false },
                                pickerMode = pickerMode,
                                onToggleFavorite = { viewModel.toggleFavorite(food) },
                                onDelete = { viewModel.deleteFood(food) },
                                onFoodSelected = { qty -> onFoodSelected?.invoke(food, qty) },
                                onAddTag = { tagId -> viewModel.addTagToFood(food.id, tagId) },
                                onRemoveTag = { tagId -> viewModel.removeTagFromFood(food.id, tagId) },
                                allTags = tags,
                                getFoodTags = { viewModel.getTagsForFood(food.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTagsDialog) {
        TagsManagementDialog(
            tags = tags,
            onDismiss = { showTagsDialog = false },
            onCreateTag = { /* tags are shared; managed from DietsScreen */ },
            onDeleteTag = { },
            onCreateTagWithColor = { _, _ -> }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsTopBar(
    totalCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNewFood: (() -> Unit)? = null,
    onTagsSettings: (() -> Unit)? = null,
    onNavigateToOnlineSearch: (() -> Unit)? = null,
    onNavigateToScanner: (() -> Unit)? = null,
    pickerMode: Boolean = false
) {
    Surface(color = FoodGreen, shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pickerMode) "Select Food" else "My Foods",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "$totalCount items",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                if (onTagsSettings != null) {
                    TextButton(onClick = onTagsSettings) {
                        Text("Tags", color = Color.White, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                if (onNewFood != null) {
                    IconButton(onClick = onNewFood) {
                        Icon(Icons.Default.Add, contentDescription = "Add Food", tint = Color.White)
                    }
                }
            }
            // Embedded search
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search foods…", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.2f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCard(
    food: FoodItem,
    tags: List<Tag>,
    pickerMode: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onFoodSelected: (Double) -> Unit,
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    allTags: List<Tag>,
    getFoodTags: () -> kotlinx.coroutines.flow.Flow<List<Tag>>
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showQuantityDialog by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    val foodTags by getFoodTags().collectAsState(initial = emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = food.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (food.barcode != null) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    food.brand?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${food.servingSize.toInt()} ${food.servingUnit}  ·  " +
                        "${food.calories.toInt()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = FoodGreen,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pickerMode) {
                        Button(
                            onClick = { showQuantityDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = FoodGreen),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Select", fontSize = 13.sp)
                        }
                    } else {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                            Icon(
                                if (food.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                tint = if (food.isFavorite) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tags row
            if (foodTags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(foodTags) { tag -> TagChip(tag = tag) }
                }
            }

            // Expanded details
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    // Macros
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacroDetail("Protein", "${food.protein.toInt()}g", Color(0xFF2196F3))
                        MacroDetail("Carbs", "${food.carbs.toInt()}g", Color(0xFFFF9800))
                        MacroDetail("Fat", "${food.fat.toInt()}g", Color(0xFFE91E63))
                        food.glycemicIndex?.let {
                            MacroDetail("GI", "$it", when {
                                it <= 55 -> FoodGreen
                                it <= 69 -> Color(0xFFFF9800)
                                else -> Color(0xFFE53935)
                            })
                        }
                    }

                    if (!pickerMode) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        // Tag management
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Tags", style = MaterialTheme.typography.labelMedium,
                                color = Color.Gray)
                            TextButton(onClick = { showTagPicker = !showTagPicker }) {
                                Text("+ Add tag", color = FoodGreen,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        if (foodTags.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(foodTags) { tag ->
                                    InputChip(
                                        selected = true,
                                        onClick = { onRemoveTag(tag.id) },
                                        label = { Text(tag.name, fontSize = 12.sp) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove",
                                                modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                        if (showTagPicker && allTags.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(allTags.filter { t -> foodTags.none { it.id == t.id } }) { tag ->
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            onAddTag(tag.id)
                                            showTagPicker = false
                                        },
                                        label = { Text(tag.name, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null,
                                modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", style = MaterialTheme.typography.labelMedium)
                        }
                    }
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
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuantityDialog) {
        FoodQuantityDialog(
            food = food,
            onConfirm = { qty ->
                onFoodSelected(qty)
                showQuantityDialog = false
            },
            onDismiss = { showQuantityDialog = false }
        )
    }
}

@Composable
fun MacroDetail(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodQuantityDialog(
    food: FoodItem,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var qty by remember { mutableStateOf(food.servingSize.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add ${food.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Serving: ${food.servingSize.toInt()} ${food.servingUnit}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Quantity (${food.servingUnit})") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val q = qty.toDoubleOrNull() ?: food.servingSize
                onConfirm(q)
            }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
