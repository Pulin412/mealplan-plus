package com.mealplanplus.ui.screens.foods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
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
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPlaceholder
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary

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
        containerColor = BgPage,
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BgPage)
        ) {
            // Filter chips row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FoodFilterChip("All", selectedTab == FoodTab.ALL) { viewModel.selectTab(FoodTab.ALL) }
                }
                item {
                    FoodFilterChip("Favorites", selectedTab == FoodTab.FAVORITES) { viewModel.selectTab(FoodTab.FAVORITES) }
                }
                item {
                    FoodFilterChip("Recent", selectedTab == FoodTab.RECENT) { viewModel.selectTab(FoodTab.RECENT) }
                }
            }
            HorizontalDivider(color = DividerColor)

            when {
                foods.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val emptyText = when {
                            searchQuery.isNotBlank() -> "No foods found"
                            selectedTagIds.isNotEmpty() -> "No foods match selected tags"
                            selectedTab == FoodTab.FAVORITES -> "No favorites yet.\nTap ♡ to add!"
                            selectedTab == FoodTab.RECENT -> "No recent foods."
                            else -> "No foods yet.\nTap + to add or scan a barcode!"
                        }
                        Text(text = emptyText, fontSize = 14.sp, color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(BgPage),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(foods, key = { it.id }) { food ->
                            FoodCard(
                                food = food,
                                tags = tags.filter { false },
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (pickerMode) "Select Food" else "Foods",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
                Text(text = "$totalCount items", fontSize = 12.sp, color = TextSecondary)
            }
            if (onNavigateToOnlineSearch != null) {
                IconButton(onClick = onNavigateToOnlineSearch, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Search, contentDescription = "Online Search", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
            if (onNavigateToScanner != null) {
                IconButton(onClick = onNavigateToScanner, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan Barcode", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
            if (onNewFood != null) {
                IconButton(onClick = onNewFood, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Add, contentDescription = "Add Food", tint = TextPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
        // Flat search bar
        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = { Text("Search foods…", fontSize = 14.sp, color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = TagGrayBg,
                unfocusedContainerColor = TagGrayBg,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = DesignGreen
            )
        )
        HorizontalDivider(color = DividerColor, thickness = 1.dp)
    }
}

@Composable
private fun FoodFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else TagGrayBg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary
        )
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

    // Icon background tint based on food category
    val iconBg = when {
        food.calories > 400 -> Color(0xFFFFF8E6)
        food.protein > 20   -> Color(0xFFF5FFF5)
        food.carbs > 40     -> Color(0xFFF0F8FF)
        else                -> Color(0xFFF5F5F5)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon well
                Box(
                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🥗", fontSize = 18.sp)
                }
                // Name + subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(food.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                    Text(
                        text = food.brand?.let { "${it} · " }.orEmpty() + "${food.servingSize.toInt()} ${food.servingUnit}",
                        fontSize = 11.sp, color = TextMuted, maxLines = 1
                    )
                }
                // Right column: picker button or kcal
                if (pickerMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TextPrimary)
                            .clickable { showQuantityDialog = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${food.calories.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("kcal", fontSize = 10.sp, color = TextMuted)
                    }
                }
            }

            // Expanded detail section
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column {
                    HorizontalDivider(color = DividerColor)
                    // Macro row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MacroDetail("Protein", "${food.protein.toInt()}g", Color(0xFF2196F3))
                        MacroDetail("Carbs", "${food.carbs.toInt()}g", Color(0xFFFF9800))
                        MacroDetail("Fat", "${food.fat.toInt()}g", Color(0xFFE91E63))
                        food.glycemicIndex?.let {
                            MacroDetail("GI", "$it", when {
                                it <= 55 -> DesignGreen
                                it <= 69 -> Color(0xFFFF9800)
                                else -> Color(0xFFE53935)
                            })
                        }
                    }
                    if (!pickerMode) {
                        HorizontalDivider(color = DividerColor)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onToggleFavorite, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    if (food.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (food.isFavorite) Color(0xFFE53E3E) else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            TextButton(onClick = { showDeleteDialog = true }) {
                                Text("Delete", color = Color(0xFFE53E3E), fontSize = 13.sp)
                            }
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
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
