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
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.ChartCarbs
import com.mealplanplus.ui.theme.ChartFat
import com.mealplanplus.ui.theme.ChartProtein
import com.mealplanplus.ui.theme.DividerColor
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietsScreen(
    onNavigateToAddDiet: () -> Unit,
    onNavigateToDietDetail: (Long) -> Unit,
    onNavigateToDietDetailView: (Long) -> Unit = {},
    onNavigateBack: () -> Unit,
    viewModel: DietsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAdvancedFilters by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            DietsTopBar(
                totalCount = uiState.totalDietCount,
                shownCount = uiState.diets.size,
                searchQuery = uiState.searchQuery,
                onSearchChange = viewModel::updateSearchQuery,
                onNewDiet = onNavigateToAddDiet,
                onNavigateBack = onNavigateBack,
                onTagsSettings = viewModel::showTagsManagement,
                onFavouritesToggle = viewModel::toggleFavouritesFilter,
                favouriteCount = uiState.favouriteDiets.size,
                showFavouritesOnly = uiState.showFavouritesOnly,
                title = "My Diets"
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
                    .clickable { showAdvancedFilters = !showAdvancedFilters }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val hasAdvanced = uiState.foodFilter.isNotBlank() || uiState.slotFilter.isNotEmpty()
                Text(
                    text = if (hasAdvanced) "More Filters (active)" else "More Filters",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (hasAdvanced) DesignGreen else TextSecondary,
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
                        CircularProgressIndicator(color = DesignGreen)
                    }
                }
                uiState.diets.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                uiState.showFavouritesOnly -> "No favourite diets yet.\nTap ⭐ on a diet card to mark it."
                                uiState.searchQuery.isBlank() && uiState.selectedTagIds.isEmpty() &&
                                    uiState.foodFilter.isBlank() && uiState.slotFilter.isEmpty() ->
                                    "No diets yet.\nTap + New Diet to create one!"
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
                        items(uiState.diets.distinctBy { it.diet.id }, key = { it.diet.id }) { item ->
                            DietCard(
                                item = item,
                                onView = { onNavigateToDietDetailView(item.diet.id) },
                                onEdit = { onNavigateToDietDetail(item.diet.id) },
                                onDuplicate = { viewModel.duplicateDiet(item.diet) },
                                onDelete = { viewModel.deleteDiet(item.diet) },
                                onFavourite = { viewModel.toggleFavourite(item.diet) }
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
    onFavouritesToggle: (() -> Unit)? = null,
    favouriteCount: Int = 0,
    showFavouritesOnly: Boolean = false,
    title: String = "My Diets"
) {
    Surface(color = CardBg, shadowElevation = 0.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row: back + title + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (totalCount == shownCount)
                            "$totalCount diets"
                        else
                            "$totalCount diets · $shownCount shown",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                // Favourites filter toggle
                if (onFavouritesToggle != null) {
                    IconButton(onClick = onFavouritesToggle) {
                        BadgedBox(
                            badge = {
                                if (favouriteCount > 0 && !showFavouritesOnly) {
                                    Badge(containerColor = Color(0xFFFFC107)) {
                                        Text("$favouriteCount", color = Color.Black)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (showFavouritesOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (showFavouritesOnly) "Show all diets" else "Show favourites",
                                tint = if (showFavouritesOnly) Color(0xFFFFC107) else DesignGreen
                            )
                        }
                    }
                }
                // Tags text button for tag management (optional)
                if (onTagsSettings != null) {
                    TextButton(onClick = onTagsSettings) {
                        Text(
                            "Tags",
                            color = DesignGreen,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                // + New Diet pill button (optional)
                if (onNewDiet != null) OutlinedButton(
                    onClick = onNewDiet,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DesignGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DesignGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Diet", style = MaterialTheme.typography.labelMedium)
                }
            }

            // Search field
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("Search diets…", fontSize = 14.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
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
            .background(CardBg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            DietsFilterChip(label = "All $totalCount", selected = selectedTagIds.isEmpty(), onClick = onAllClick)
        }
        items(tags) { tag ->
            val count = tagCountMap[tag.id] ?: 0
            DietsFilterChip(
                label = if (count > 0) "${tag.name} $count" else tag.name,
                selected = tag.id in selectedTagIds,
                onClick = { onTagClick(tag.id) }
            )
        }
    }
}

@Composable
private fun DietsFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else TagGrayBg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary)
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
    Surface(color = CardBg) {
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
                            selectedContainerColor = DesignGreen,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            if (foodFilter.isNotBlank() || slotFilter.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onClearAll, contentPadding = PaddingValues(0.dp)) {
                    Text("Clear all filters", color = TextDestructive)
                }
            }
        }
    }
}

@Composable
fun DietCard(
    item: DietDisplayItem,
    onView: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onFavourite: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isPickerMode = onSelect != null
    val primaryAction: () -> Unit = if (isPickerMode) (onSelect ?: {}) else onView

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = primaryAction),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left calorie block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(70.dp)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "${item.totalCalories}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    ),
                    color = TextPrimary
                )
                Text(
                    text = "KCAL",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted
                )
            }

            // Thin vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(DividerColor)
            )

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 13.dp, bottom = 13.dp, end = 4.dp)
            ) {
                // Name row + badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.diet.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.diet.isFavourite) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (item.diet.isSystem) {
                        Surface(shape = RoundedCornerShape(4.dp), color = TagGrayBg) {
                            Text(
                                "built-in",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Tags + meal count
                if (item.tags.isNotEmpty() || item.mealCount > 0) {
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item.tags.take(3).forEach { tag -> TagChip(tag = tag) }
                        if (item.mealCount > 0) {
                            Text(
                                "· ${item.mealCount} meals",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }

                // Inline macros: P / C / F
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DietMacroInline(label = "P", value = "${item.totalProtein}g", color = ChartProtein)
                    DietMacroInline(label = "C", value = "${item.totalCarbs}g", color = ChartCarbs)
                    DietMacroInline(label = "F", value = "${item.totalFat}g", color = ChartFat)
                }
            }

            // Right: overflow menu (top) + chevron (bottom)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(end = 4.dp, top = 6.dp, bottom = 8.dp)
            ) {
                if (!isPickerMode) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showMenu = false; onEdit() },
                                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = { showMenu = false; onDuplicate() },
                                leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) }
                            )
                            if (onFavourite != null) {
                                DropdownMenuItem(
                                    text = { Text(if (item.diet.isFavourite) "Unfavourite" else "Favourite") },
                                    onClick = { showMenu = false; onFavourite() },
                                    leadingIcon = {
                                        Icon(
                                            if (item.diet.isFavourite) Icons.Default.Star else Icons.Default.StarBorder,
                                            null,
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = TextDestructive) },
                                onClick = { showMenu = false; showDeleteDialog = true },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, null, tint = TextDestructive, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.size(32.dp))
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isPickerMode) DesignGreen else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
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
                    Text("Delete", color = TextDestructive)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// Used by DietFormComponents.kt in the same package
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

@Composable
private fun DietMacroInline(label: String, value: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold), color = color)
        Text(value, style = MaterialTheme.typography.labelSmall, color = color)
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
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextDestructive)
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

private fun glColor(gl: Double): Color = when {
    gl <= 10.0 -> Color(0xFF2E7D32)
    gl <= 19.0 -> Color(0xFFF57F17)
    else       -> Color(0xFFB71C1C)
}

private fun glLabel(gl: Double): String = when {
    gl <= 10.0 -> "Low GL"
    gl <= 19.0 -> "Med GL"
    else       -> "High GL"
}

@Composable
private fun GlycemicLoadPill(gl: Double) {
    val color = glColor(gl)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = color.copy(alpha = 0.12f)
        ) {
            Text(
                text = "⚡ ${glLabel(gl)}  ${String.format("%.1f", gl)}",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
