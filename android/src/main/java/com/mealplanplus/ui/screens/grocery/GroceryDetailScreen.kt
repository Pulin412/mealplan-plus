package com.mealplanplus.ui.screens.grocery

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.GroceryItemWithFood
import com.mealplanplus.util.GroceryCategory
import com.mealplanplus.ui.theme.BgPage
import com.mealplanplus.ui.theme.CardBg
import com.mealplanplus.ui.theme.DesignGreen
import com.mealplanplus.ui.theme.DesignGreenLight
import com.mealplanplus.ui.theme.TagGrayBg
import com.mealplanplus.ui.theme.TextDestructive
import com.mealplanplus.ui.theme.TextMuted
import com.mealplanplus.ui.theme.TextPrimary
import com.mealplanplus.ui.theme.TextSecondary
import com.mealplanplus.ui.theme.minimalTopAppBarColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroceryDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroceryDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var collapsedCategories by remember { mutableStateOf(emptySet<String>()) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            uiState.list?.list?.name ?: "Grocery List",
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                        uiState.list?.let { gl ->
                            Text("${gl.items.size} items · ${gl.items.count { it.item.isChecked }} bought",
                                fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareList() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = TextSecondary)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Uncheck all") },
                                onClick = {
                                    viewModel.uncheckAllItems()
                                    showMenu = false
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, null) }
                            )
                        }
                    }
                },
                colors = minimalTopAppBarColors()
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
                ) { CircularProgressIndicator() }
            }
            uiState.list == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("List not found") }
            }
            else -> {
                val list = uiState.list!!

                // Filter by tab
                val displayItems = when (uiState.selectedTab) {
                    1 -> list.items.filter { !it.item.isChecked }
                    else -> list.items
                }

                // Group by category following GroceryCategory.all order
                val itemsByCategory = displayItems.groupBy { it.item.category ?: GroceryCategory.OTHER }
                val groupedItems = GroceryCategory.all.mapNotNull { cat ->
                    val catItems = itemsByCategory[cat]?.sortedWith(
                        compareBy({ it.item.isChecked }, { it.displayName.lowercase() })
                    )
                    if (catItems.isNullOrEmpty()) null else cat to catItems
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Summary row + action
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(DesignGreenLight).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("${list.totalCount} items", fontSize = 12.sp, color = DesignGreen, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                .background(TagGrayBg).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("${list.checkedCount} bought", fontSize = 12.sp, color = TextSecondary) }
                        Spacer(Modifier.weight(1f))
                        if (list.checkedCount > 0) {
                            TextButton(onClick = { viewModel.uncheckAllItems() }, contentPadding = PaddingValues(0.dp)) {
                                Text("Clear bought", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }

                        if (list.totalCount > 0) {
                            LinearProgressIndicator(
                                progress = list.progressPercent,
                                modifier = Modifier.fillMaxWidth().height(3.dp),
                                color = DesignGreen,
                                trackColor = TagGrayBg
                            )
                        }

                    // Tab row + Add Item
                    Row(
                        modifier = Modifier.fillMaxWidth().background(CardBg).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GroceryFilterChip("All (${list.totalCount})", uiState.selectedTab == 0) { viewModel.setSelectedTab(0) }
                        GroceryFilterChip("To Buy (${list.totalCount - list.checkedCount})", uiState.selectedTab == 1) { viewModel.setSelectedTab(1) }
                        Spacer(Modifier.weight(1f))
                        if (uiState.isRegenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (list.list.startDate != null) {
                            IconButton(onClick = { viewModel.regenerateList() }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Regenerate",
                                    tint = DesignGreen
                                )
                            }
                        }
                        TextButton(onClick = { viewModel.showAddItemDialog() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }

                    HorizontalDivider()

                    if (displayItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = TextSecondary
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (uiState.selectedTab == 1) "Nothing left to buy!" else "No items yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                if (uiState.selectedTab == 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Tap Add Item to get started",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            for ((category, catItems) in groupedItems) {
                                val emoji = GroceryCategory.categoryEmoji[category] ?: "\uD83D\uDED2"
                                val checkedInCat = catItems.count { it.item.isChecked }
                                val isCollapsed = category in collapsedCategories

                                stickyHeader(key = "header_$category") {
                                    CategoryHeader(
                                        emoji = emoji,
                                        name = category,
                                        checkedCount = checkedInCat,
                                        totalCount = catItems.size,
                                        isCollapsed = isCollapsed,
                                        onToggleCollapse = {
                                            collapsedCategories = if (isCollapsed)
                                                collapsedCategories - category
                                            else
                                                collapsedCategories + category
                                        }
                                    )
                                }

                                if (!isCollapsed) {
                                    items(catItems, key = { it.item.id }) { item ->
                                        GroceryItemRow(
                                            item = item,
                                            onCheckedChange = {
                                                viewModel.toggleItemChecked(item.item.id, item.item.isChecked)
                                            },
                                            onEdit = { viewModel.showEditItemDialog(item) },
                                            onDelete = { viewModel.deleteItem(item.item.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add item dialog
    if (uiState.showAddItemDialog) {
        AddItemDialog(
            onDismiss = { viewModel.hideAddItemDialog() },
            onAdd = { name, quantity, unit ->
                viewModel.addCustomItem(name, quantity, unit)
            }
        )
    }

    // Edit item dialog
    uiState.showEditItemDialog?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { viewModel.hideEditItemDialog() },
            onSave = { quantity -> viewModel.updateItemQuantity(item.item.id, quantity) },
            onDelete = {
                viewModel.deleteItem(item.item.id)
                viewModel.hideEditItemDialog()
            }
        )
    }
}

@Composable
fun CategoryHeader(
    emoji: String,
    name: String,
    checkedCount: Int,
    totalCount: Int,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit
) {
    Surface(
        color = TagGrayBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapse)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$checkedCount/$totalCount",
                style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                contentDescription = if (isCollapsed) "Expand" else "Collapse",
                modifier = Modifier.size(20.dp),
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun GroceryItemRow(
    item: GroceryItemWithFood,
    onCheckedChange: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(CardBg)
            .clickable(onClick = onCheckedChange)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular checkbox (green when checked)
        Box(
            modifier = Modifier.size(22.dp).clip(CircleShape)
                .background(if (item.item.isChecked) DesignGreen else CardBg)
                .border(1.5.dp, if (item.item.isChecked) DesignGreen else TagGrayBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (item.item.isChecked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (item.item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                color = if (item.item.isChecked) TextMuted else TextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text(text = item.displayQuantity, fontSize = 11.sp, color = TextMuted)
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = TextMuted)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp), tint = TextMuted)
        }
    }
    HorizontalDivider(color = TagGrayBg, modifier = Modifier.padding(start = 46.dp))
}

@Composable
private fun GroceryFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) TextPrimary else CardBg)
            .border(1.dp, if (selected) TextPrimary else TagGrayBg, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, FoodUnit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf(FoodUnit.PIECE) }
    var showUnitMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            onClick = { showUnitMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedUnit.label)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(
                            expanded = showUnitMenu,
                            onDismissRequest = { showUnitMenu = false }
                        ) {
                            FoodUnit.entries.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit.label) },
                                    onClick = {
                                        selectedUnit = unit
                                        showUnitMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: 1.0
                    if (name.isNotBlank()) {
                        onAdd(name, qty, selectedUnit)
                    }
                },
                enabled = name.isNotBlank()
            ) {
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

@Composable
fun EditItemDialog(
    item: GroceryItemWithFood,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit
) {
    var quantity by remember { mutableStateOf(item.item.quantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.displayName) },
        text = {
            Column {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity (${item.item.unit.shortLabel})") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull() ?: item.item.quantity
                    onSave(qty)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextDestructive
                    )
                ) {
                    Text("Delete")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
