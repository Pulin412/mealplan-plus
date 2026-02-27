package com.mealplanplus.ui.screens.diets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Tag
import com.mealplanplus.ui.components.TagChip

private val DietGreen = Color(0xFF2E7D52)
private val DietGreenLight = Color(0xFFE8F5EE)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietsScreen(
    onNavigateToAddDiet: () -> Unit,
    onNavigateToDietDetail: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: DietsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdvancedFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            DietsTopBar(
                totalCount = uiState.totalDietCount,
                shownCount = uiState.diets.size,
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::updateSearchQuery,
                onNewDiet = onNavigateToAddDiet,
                onNavigateBack = onNavigateBack,
                onTagsSettings = viewModel::showTagsManagement,
                title = "My Diets"
            )
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
                    .clickable { showAdvancedFilters = !showAdvancedFilters }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val hasAdvanced = uiState.foodFilter.isNotBlank() || uiState.slotFilter.isNotEmpty()
                Text(
                    text = if (hasAdvanced) "More Filters (active)" else "More Filters",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasAdvanced) DietGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasAdvanced) FontWeight.Bold else FontWeight.Normal
                )
                Icon(
                    imageVector = if (showAdvancedFilters) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Advanced filter panel
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
                        CircularProgressIndicator(color = DietGreen)
                    }
                }
                uiState.diets.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (uiState.searchQuery.isBlank() && uiState.selectedTagIds.isEmpty() &&
                                uiState.foodFilter.isBlank() && uiState.slotFilter.isEmpty())
                                "No diets yet.\nTap + New Diet to create one!"
                            else
                                "No diets match your filters",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                onEdit = { onNavigateToDietDetail(item.diet.id) },
                                onDuplicate = { viewModel.duplicateDiet(item.diet) },
                                onDelete = { viewModel.deleteDiet(item.diet) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Tags Management Dialog
    if (uiState.showTagsDialog) {
        TagsManagementDialog(
            tags = uiState.allTags,
            onDismiss = { viewModel.hideTagsManagement() },
            onCreateTag = { viewModel.createTag(it) },
            onDeleteTag = { viewModel.deleteTag(it) },
            onCreateTagWithColor = { name, color -> viewModel.createTagWithColor(name, color) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietsTopBar(
    totalCount: Int,
    shownCount: Int,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNewDiet: (() -> Unit)? = null,
    onNavigateBack: () -> Unit,
    onTagsSettings: (() -> Unit)? = null,
    title: String = "My Diets"
) {
    Surface(color = DietGreen, shadowElevation = 4.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: back + title + settings
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
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = if (totalCount == shownCount)
                            "$totalCount diets"
                        else
                            "$totalCount diets · $shownCount shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
                // Settings icon for tag management (optional)
                if (onTagsSettings != null) {
                    IconButton(onClick = onTagsSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Tags", tint = Color.White)
                    }
                }
                // + New Diet pill button (optional)
                if (onNewDiet != null) OutlinedButton(
                    onClick = onNewDiet,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Diet", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search diets...", color = Color.White.copy(alpha = 0.7f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.7f)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.White)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.5f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White
                )
            )
        }
    }
}

@Composable
fun TagFilterRow(
    tags: List<Tag>,
    selectedTagIds: Set<Long>,
    tagCountMap: Map<Long, Int>,
    totalCount: Int,
    onTagClick: (Long) -> Unit,
    onAllClick: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All N" chip
        item {
            FilterChip(
                selected = selectedTagIds.isEmpty(),
                onClick = onAllClick,
                label = { Text("All $totalCount") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DietGreen,
                    selectedLabelColor = Color.White
                )
            )
        }
        items(tags) { tag ->
            val count = tagCountMap[tag.id] ?: 0
            FilterChip(
                selected = tag.id in selectedTagIds,
                onClick = { onTagClick(tag.id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(tag.name)
                        if (count > 0) {
                            Surface(
                                shape = CircleShape,
                                color = if (tag.id in selectedTagIds) Color.White.copy(alpha = 0.3f) else DietGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tag.id in selectedTagIds) Color.White else DietGreen,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DietGreen,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun AdvancedFilterSection(
    foodFilter: String,
    slotFilter: Set<String>,
    onFoodFilterChange: (String) -> Unit,
    onSlotToggle: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Surface(color = Color(0xFFF0F7F3)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            // Food filter
            OutlinedTextField(
                value = foodFilter,
                onValueChange = onFoodFilterChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Contains food (e.g. chicken)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (foodFilter.isNotEmpty()) {
                        IconButton(onClick = { onFoodFilterChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                label = { Text("Food Filter") }
            )

            Spacer(Modifier.height(10.dp))

            Text("Slot presence:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))

            // Slot filter chips in a wrapping row
            val slots = DefaultMealSlot.entries.sortedBy { it.order }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                slots.forEach { slot ->
                    FilterChip(
                        selected = slot.name in slotFilter,
                        onClick = { onSlotToggle(slot.name) },
                        label = { Text(slot.displayName, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DietGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (foodFilter.isNotBlank() || slotFilter.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClearAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Clear all filters", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DietCard(
    item: DietDisplayItem,
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onSelect: (() -> Unit)? = null  // picker mode: tap card to select
) {
    var expanded by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isPickerMode = onSelect != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header (always visible) — tap to expand or select
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (isPickerMode) onSelect?.invoke() else expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Name row + badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = item.diet.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.diet.isSystemDiet) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE0E0E0)
                            ) {
                                Text(
                                    "Built-in",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF616161),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        item.tags.take(2).forEach { tag -> TagChip(tag = tag) }
                        if (item.tags.size > 2) {
                            Text(
                                "+${item.tags.size - 2}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Description
                    item.diet.description?.let { desc ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // Macro pills row
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MacroPill(icon = "🔥", value = "${item.totalCalories}", unit = "kcal", color = Color(0xFFE65100))
                        MacroPill(icon = "🍞", value = "${item.totalCarbs}g", unit = "carbs", color = Color(0xFF1565C0))
                        MacroPill(icon = "💪", value = "${item.totalProtein}g", unit = "protein", color = Color(0xFF2E7D32))
                        MacroPill(icon = "🔥", value = "${item.totalFat}g", unit = "fat", color = Color(0xFF6A1B9A))
                    }
                }

                if (isPickerMode) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Select",
                        tint = DietGreen,
                        modifier = Modifier.padding(start = 8.dp).size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp).size(20.dp)
                    )
                }
            }

            // Expanded section: Edit + Delete (only in non-picker mode)
            AnimatedVisibility(
                visible = expanded && !isPickerMode,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(color = Color(0xFFEEEEEE))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Edit", color = DietGreen)
                        }
                        TextButton(onClick = { onDuplicate() }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Duplicate", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Diet") },
            text = { Text("Delete \"${item.diet.name}\"?") },
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
}

@Composable
fun MacroPill(icon: String, value: String, unit: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text = "$icon $value", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(text = unit, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

// Keep TagsManagementDialog + ColorPicker for tag management from settings icon
@Composable
fun TagsManagementDialog(
    tags: List<Tag>,
    onDismiss: () -> Unit,
    onCreateTag: (String) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onCreateTagWithColor: ((String, String) -> Unit)? = null
) {
    var newTagName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Tag.COLOR_PALETTE[0]) }
    var tagToDelete by remember { mutableStateOf<Tag?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("New tag name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                Text("Select color:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                ColorPicker(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onCreateTagWithColor?.invoke(newTagName, selectedColor) ?: onCreateTag(newTagName)
                            newTagName = ""
                            selectedColor = Tag.COLOR_PALETTE[0]
                        }
                    },
                    enabled = newTagName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Tag")
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text("Existing Tags", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                if (tags.isEmpty()) {
                    Text("No tags yet", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(tags) { tag ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TagChip(tag = tag)
                                IconButton(onClick = { tagToDelete = tag }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    tagToDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { tagToDelete = null },
            title = { Text("Delete Tag") },
            text = { Text("Delete tag '${tag.name}'? This will remove it from all diets.") },
            confirmButton = {
                TextButton(onClick = { onDeleteTag(tag); tagToDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { tagToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ColorPicker(selectedColor: String, onColorSelected: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Tag.COLOR_PALETTE.forEach { color ->
            val parsedColor = com.mealplanplus.ui.components.parseColor(color)
            val isSelected = color == selectedColor
            Surface(
                color = parsedColor,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(28.dp).clickable { onColorSelected(color) }
            ) {
                if (isSelected) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            modifier = Modifier.size(16.dp),
                            tint = if (parsedColor.luminance() > 0.5f) Color.Black else Color.White
                        )
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
