package com.mealplanplus.ui.screens.foods

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.components.CalorieValue
import com.mealplanplus.ui.components.FavoriteStar
import com.mealplanplus.ui.components.MacroText
import com.mealplanplus.ui.components.SegmentedControl
import com.mealplanplus.ui.components.VerifiedBadge
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.CarbsBg
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DeleteColor
import com.mealplanplus.ui.theme.DisabledFill
import com.mealplanplus.ui.theme.DisabledText
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.FatBg
import com.mealplanplus.ui.theme.FavoriteGold
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.OnlineAdd
import com.mealplanplus.ui.theme.ProteinBg
import com.mealplanplus.ui.theme.Scrim
import com.mealplanplus.ui.theme.SearchBg
import com.mealplanplus.ui.theme.Success
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsScreen(onBack: () -> Unit, viewModel: FoodViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }

    if (state.activeSheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeSheet() },
            sheetState       = sheetState,
            shape            = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            containerColor   = Surface,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderCool)
                )
            }
        ) {
            when (state.activeSheet) {
                FoodSheet.MANUAL  -> ManualEntrySheet(state = state, viewModel = viewModel)
                FoodSheet.ONLINE  -> OnlineSearchSheet(state = state, viewModel = viewModel)
                FoodSheet.BARCODE -> BarcodeScanSheet(state = state, viewModel = viewModel)
                null -> Unit
            }
        }
    }

    Scaffold(
        containerColor = AppBg,
        topBar = {
            FoodsTopBar(
                count  = state.foods.size,
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FoodsFab(
                fanOpen   = state.fanOpen,
                onToggle  = { if (state.fanOpen) viewModel.closeFan() else viewModel.openFan() },
                onManual  = { viewModel.openSheet(FoodSheet.MANUAL) },
                onOnline  = { viewModel.openSheet(FoodSheet.ONLINE) },
                onBarcode = { viewModel.openSheet(FoodSheet.BARCODE) },
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            FoodsSearchBar(
                query         = state.searchQuery,
                onQueryChange = viewModel::setSearch,
            )
            Spacer(Modifier.height(10.dp))
            FoodsToolbar(
                sortMode       = state.sortMode,
                onSortClick    = { sortMenuOpen = true },
                favOnly        = state.favOnly,
                favCount       = state.favCount,
                onFavToggle    = viewModel::toggleFavOnly,
                viewMode       = state.viewMode,
                onViewToggle   = viewModel::setViewMode,
                sortOpen       = sortMenuOpen,
                onSortDismiss  = { sortMenuOpen = false },
                onSortPick     = { sort ->
                    viewModel.setSort(sort)
                    sortMenuOpen = false
                },
            )

            // Category filter chips — only shown once some foods have categories.
            if (state.usedCategories.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.usedCategories.forEach { cat ->
                        val on = state.categoryFilter == cat
                        Text(
                            cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (on) OnAccent else MutedDark,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (on) Teal else Color.Transparent)
                                .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                                .clickable { viewModel.setCategoryFilter(cat) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            val foods = state.filteredFoods
            if (foods.isEmpty() && state.favOnly) {
                EmptyFavouritesState()
            } else {
                when (state.viewMode) {
                    FoodViewMode.LIST -> FoodListView(
                        foods           = foods,
                        expandedIds     = state.expandedIds,
                        onToggleExpand  = viewModel::toggleExpand,
                        onToggleFav     = viewModel::toggleFavorite,
                        onDelete        = viewModel::deleteFood,
                        onEdit          = viewModel::openEditFood,
                    )
                    FoodViewMode.COMPACT -> FoodCompactView(
                        foods           = foods,
                        expandedIds     = state.expandedIds,
                        onToggleExpand  = viewModel::toggleExpand,
                        onToggleFav     = viewModel::toggleFavorite,
                        onDelete        = viewModel::deleteFood,
                        onEdit          = viewModel::openEditFood,
                    )
                }
            }
        }

        if (state.fanOpen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Scrim)
                    .clickable { viewModel.closeFan() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodsTopBar(count: Int, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                "Food items",
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Ink,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
        },
        actions = {
            Text(
                "$count saved",
                fontSize  = 12.sp,
                color     = Muted,
                modifier  = Modifier.padding(end = 16.dp),
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
    )
}

@Composable
fun FoodsSearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(SearchBg)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        androidx.compose.foundation.text.BasicTextField(
            value       = query,
            onValueChange = onQueryChange,
            singleLine  = true,
            textStyle   = TextStyle(fontSize = 13.sp, color = Ink),
            modifier    = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text("Search your foods…", fontSize = 13.sp, color = Muted)
                }
                inner()
            }
        )
    }
}

@Composable
fun FoodsToolbar(
    sortMode: FoodSort,
    onSortClick: () -> Unit,
    favOnly: Boolean,
    favCount: Int,
    onFavToggle: () -> Unit,
    viewMode: FoodViewMode,
    onViewToggle: (FoodViewMode) -> Unit,
    sortOpen: Boolean,
    onSortDismiss: () -> Unit,
    onSortPick: (FoodSort) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Sort dropdown button
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, BorderCool, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSortClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("↕ ${sortMode.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Muted, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(
                expanded        = sortOpen,
                onDismissRequest = onSortDismiss,
            ) {
                FoodSort.values().forEach { sort ->
                    DropdownMenuItem(
                        text    = { Text(sort.label, fontSize = 13.sp) },
                        onClick = { onSortPick(sort) },
                    )
                }
            }
        }

        // Favourites filter chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (favOnly) Teal else BorderCool, RoundedCornerShape(8.dp))
                .background(if (favOnly) Teal.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onFavToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                "${if (favOnly) "★" else "☆"} $favCount",
                fontSize   = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (favOnly) Teal else Ink,
            )
        }

        Spacer(Modifier.weight(1f))

        // List / Compact toggle
        val modes = listOf(FoodViewMode.LIST to "☰", FoodViewMode.COMPACT to "≣")
        SegmentedControl(
            optionCount = modes.size,
            selectedIndex = modes.indexOfFirst { it.first == viewMode },
            onSelect = { onViewToggle(modes[it].first) },
        ) { i, selected ->
            Text(modes[i].second, fontSize = 14.sp, color = if (selected) Surface else Muted)
        }
    }
}

private val FoodSort.label: String get() = when (this) {
    FoodSort.RECENT   -> "Recent"
    FoodSort.NAME     -> "Name"
    FoodSort.CALORIES -> "Calories"
    FoodSort.PROTEIN  -> "Protein"
}

// ── List view ──────────────────────────────────────────────────────────────

@Composable
fun FoodListView(
    foods: List<Food>,
    expandedIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onToggleFav: (Food) -> Unit,
    onDelete: (Food) -> Unit,
    onEdit: (Food) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(foods, key = { it.id }) { food ->
            FoodListCard(
                food          = food,
                expanded      = food.id in expandedIds,
                onToggleExpand = { onToggleExpand(food.id) },
                onToggleFav   = { onToggleFav(food) },
                onDelete      = { onDelete(food) },
                onEdit        = { onEdit(food) },
            )
        }
    }
}

@Composable
fun FoodListCard(
    food: Food,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    AppCard(modifier = Modifier.clickable(onClick = onToggleExpand)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    food.name,
                    fontSize    = 12.5.sp,
                    fontWeight  = FontWeight.Bold,
                    color       = Ink,
                    maxLines    = 1,
                    overflow    = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(food.brand, servingLabel(food.servingLabel)).joinToString(" · ")
                Text(sub, fontSize = 10.5.sp, color = Muted)
            }
            Spacer(Modifier.width(8.dp))

            // Star (tight inline — matches design density)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onToggleFav),
            ) { FavoriteStar(active = food.isFavorite, size = 17.dp) }

            Spacer(Modifier.width(8.dp))

            // Calories
            CalorieValue(kcal = food.caloriesPer100.toInt())

            // Delete — seed/system foods are read-only (no delete)
            if (!food.isSystemFood) {
                Spacer(Modifier.width(10.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onDelete),
                ) { Icon(Icons.Default.Close, contentDescription = "Delete", tint = DeleteColor, modifier = Modifier.size(15.dp)) }
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    MacroText(food.proteinPer100, food.carbsPer100, food.fatPer100, fontSize = 10.5.sp)
                }
                if (!food.isSystemFood) {   // seed/system foods are read-only
                    Spacer(Modifier.width(8.dp))
                    Text("✎ Edit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                        modifier = Modifier.clickable(onClick = onEdit).padding(end = 10.dp))
                }
                VerifiedBadge(food.verified)
            }
        }
    }
}

// ── Compact view ───────────────────────────────────────────────────────────

@Composable
fun FoodCompactView(
    foods: List<Food>,
    expandedIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onToggleFav: (Food) -> Unit,
    onDelete: (Food) -> Unit,
    onEdit: (Food) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 88.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
            ) {
                foods.forEachIndexed { index, food ->
                    FoodCompactRow(
                        food          = food,
                        expanded      = food.id in expandedIds,
                        onToggleExpand = { onToggleExpand(food.id) },
                        onToggleFav   = { onToggleFav(food) },
                        onDelete      = { onDelete(food) },
                        onEdit        = { onEdit(food) },
                    )
                    if (index < foods.lastIndex) {
                        HorizontalDivider(color = SearchBg, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun FoodCompactRow(
    food: Food,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFav: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggleExpand)
            .padding(horizontal = 11.dp, vertical = 7.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    food.name,
                    fontSize    = 12.sp,
                    fontWeight  = FontWeight.Bold,
                    color       = Ink,
                    maxLines    = 1,
                    overflow    = TextOverflow.Ellipsis,
                )
                val sub = listOfNotNull(food.brand, servingLabel(food.servingLabel)).joinToString(" · ")
                Text(sub, fontSize = 9.5.sp, color = MutedFaint)
            }
            Spacer(Modifier.width(6.dp))

            // Star
            IconButton(onClick = onToggleFav, modifier = Modifier.size(22.dp)) {
                FavoriteStar(active = food.isFavorite, size = 13.dp)
            }

            Spacer(Modifier.width(4.dp))

            // Kcal + superscript
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    "${food.caloriesPer100.toInt()}",
                    fontFamily = DmMono,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color      = Ink,
                )
                Text(
                    "kcal",
                    fontSize  = 8.sp,
                    color     = Muted,
                    modifier  = Modifier.padding(top = 1.dp, start = 1.dp),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    MacroText(food.proteinPer100, food.carbsPer100, food.fatPer100, fontSize = 10.sp)
                }
                if (!food.isSystemFood) {   // seed/system foods are read-only
                    Spacer(Modifier.width(8.dp))
                    Text("✎ Edit", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                        modifier = Modifier.clickable(onClick = onEdit))
                }
                Spacer(Modifier.width(8.dp))
                VerifiedBadge(food.verified)
                if (!food.isSystemFood) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "✕ Remove",
                        fontSize  = 10.sp,
                        color     = Muted,
                        modifier  = Modifier.clickable(onClick = onDelete),
                    )
                }
            }
        }
    }
}

// ── Shared ─────────────────────────────────────────────────────────────────

private fun Double.fmt(): String {
    return if (this == kotlin.math.floor(this)) this.toInt().toString()
    else "%.1f".format(this)
}

/** Serving shown under the food name: bare numbers get a "gm" unit; blank falls back to the per-100g basis. */
private fun servingLabel(raw: String?): String = when {
    raw.isNullOrBlank()          -> "100gm"
    raw.toDoubleOrNull() != null -> "${raw}gm"
    else                         -> raw
}

// ── Empty favourites state ──────────────────────────────────────────────────

@Composable
fun EmptyFavouritesState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("★", fontSize = 48.sp, color = FavoriteGold)
            Spacer(Modifier.height(12.dp))
            Text("No favourites yet", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text(
                "Tap the ☆ on any food to save it here.",
                fontSize = 13.sp,
                color    = Muted,
            )
        }
    }
}

// ── FAB speed-dial ─────────────────────────────────────────────────────────

@Composable
fun FoodsFab(
    fanOpen: Boolean,
    onToggle: () -> Unit,
    onManual: () -> Unit,
    onOnline: () -> Unit,
    onBarcode: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.End) {
        if (fanOpen) {
            FabOption(
                label     = "Scan barcode",
                iconText  = "▦",
                bgColor   = FatBg,
                onClick   = onBarcode,
            )
            Spacer(Modifier.height(8.dp))
            FabOption(
                label     = "Search online",
                iconText  = "⌕",
                bgColor   = CarbsBg,
                onClick   = onOnline,
            )
            Spacer(Modifier.height(8.dp))
            FabOption(
                label     = "Enter manually",
                iconText  = "✎",
                bgColor   = ProteinBg,
                onClick   = onManual,
            )
            Spacer(Modifier.height(12.dp))
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Teal)
                .clickable(onClick = onToggle),
        ) {
            val rotation by animateFloatAsState(targetValue = if (fanOpen) 45f else 0f, label = "fab_rotate")
            Text(
                "+",
                fontSize   = 28.sp,
                color      = OnAccent,
                fontWeight = FontWeight.Light,
                modifier   = Modifier.rotate(rotation),
            )
        }
    }
}

@Composable
private fun FabOption(
    label: String,
    iconText: String,
    bgColor: Color,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceMuted)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text(label, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .clickable(onClick = onClick),
        ) {
            Text(iconText, fontSize = 18.sp, color = Ink)
        }
    }
}

// ── Manual entry sheet ──────────────────────────────────────────────────────

@Composable
fun ManualEntrySheet(state: FoodsUiState, viewModel: FoodViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        val isEditing = state.editingFoodId != null
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(if (isEditing) "Edit food" else "New food", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            if (isEditing) {
                Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                    modifier = Modifier.clickable {
                        state.foods.find { it.id == state.editingFoodId }?.let { viewModel.deleteFood(it) }
                        viewModel.closeSheet()
                    })
            }
        }
        Spacer(Modifier.height(16.dp))

        SheetTextField(
            label       = "Name",
            value       = state.manualName,
            onValueChange = viewModel::setManualName,
        )
        Spacer(Modifier.height(12.dp))

        Text("Measured in", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            com.mealplanplus.data.repository.FOOD_UNITS.forEach { u ->
                val on = u == state.manualUnit
                Text(
                    com.mealplanplus.data.repository.unitLabel(u),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) OnAccent else MutedDark,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (on) Teal else Color.Transparent)
                        .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                        .clickable { viewModel.setManualUnit(u) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        if (com.mealplanplus.data.repository.isCountUnit(state.manualUnit)) {
            Spacer(Modifier.height(10.dp))
            SheetTextField(
                label       = "Grams per ${com.mealplanplus.data.repository.unitLabel(state.manualUnit)}",
                placeholder = "e.g. 50",
                value       = state.manualGramsPerUnit,
                onValueChange = viewModel::setManualGramsPerUnit,
                keyboardType  = KeyboardType.Number,
            )
        }
        Spacer(Modifier.height(10.dp))
        SheetTextField(
            label       = "Serving size",
            placeholder = "e.g. 250 g",
            value       = state.manualServing,
            onValueChange = viewModel::setManualServing,
        )
        Spacer(Modifier.height(10.dp))
        SheetTextField(
            label       = "Calories (per 100g)",
            value       = state.manualKcal,
            onValueChange = viewModel::setManualKcal,
            keyboardType  = KeyboardType.Number,
        )
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetTextField(
                label       = "Protein",
                value       = state.manualProtein,
                onValueChange = viewModel::setManualProtein,
                keyboardType  = KeyboardType.Number,
                modifier    = Modifier.weight(1f),
            )
            SheetTextField(
                label       = "Carbs",
                value       = state.manualCarbs,
                onValueChange = viewModel::setManualCarbs,
                keyboardType  = KeyboardType.Number,
                modifier    = Modifier.weight(1f),
            )
            SheetTextField(
                label       = "Fat",
                value       = state.manualFat,
                onValueChange = viewModel::setManualFat,
                keyboardType  = KeyboardType.Number,
                modifier    = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(10.dp))
        // Category: type your own or tap a suggestion (suggestions = defaults + already-used).
        SheetTextField(
            label       = "Category",
            placeholder = "Pick one below or type your own",
            value       = state.manualCategory,
            onValueChange = viewModel::setManualCategory,
        )
        Spacer(Modifier.height(8.dp))
        val catSuggestions = (com.mealplanplus.data.DEFAULT_FOOD_CATEGORIES + state.usedCategories).distinct()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            catSuggestions.forEach { cat ->
                val on = cat == state.manualCategory
                Text(
                    cat, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) OnAccent else MutedDark,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (on) Teal else Color.Transparent)
                        .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                        .clickable { viewModel.setManualCategory(if (on) "" else cat) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick  = viewModel::saveManual,
            enabled  = state.isSaveManualEnabled,
            modifier = Modifier.fillMaxWidth(),
            colors   = ButtonDefaults.buttonColors(
                containerColor         = Teal,
                disabledContainerColor = DisabledFill,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                "Save food",
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = if (state.isSaveManualEnabled) OnAccent else DisabledText,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = Muted, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, fontSize = 13.sp, color = Muted) },
            singleLine    = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            textStyle     = TextStyle(fontSize = 13.sp, color = Ink),
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Teal,
                unfocusedBorderColor = BorderCool,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ── Online search sheet ─────────────────────────────────────────────────────

@Composable
fun OnlineSearchSheet(
    state: FoodsUiState,
    viewModel: FoodViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Search online", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value          = state.onlineQuery,
            onValueChange  = viewModel::setOnlineQuery,
            placeholder    = { Text("Search foods…", fontSize = 13.sp, color = Muted) },
            singleLine     = true,
            leadingIcon    = { Icon(Icons.Default.Search, contentDescription = null, tint = Muted) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.searchOnline() }),
            textStyle      = TextStyle(fontSize = 13.sp, color = Ink),
            shape          = RoundedCornerShape(10.dp),
            colors         = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = Teal,
                unfocusedBorderColor = BorderCool,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(12.dp))

        if (state.onlineLoading) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Teal, modifier = Modifier.size(32.dp))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.height(360.dp),
            ) {
                items(state.onlineResults, key = { it.id ?: it.name }) { dto ->
                    OnlineResultRow(dto = dto, onAdd = { viewModel.addOnlineFood(dto) })
                }
            }
        }
    }
}

@Composable
private fun OnlineResultRow(dto: FoodDto, onAdd: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(dto.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(dto.brand, "per 100g", "${dto.caloriesPer100.toInt()} kcal").joinToString(" · ")
            Text(sub, fontSize = 10.sp, color = Muted)
            MacroText(dto.proteinPer100, dto.carbsPer100, dto.fatPer100, fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, BorderCool, RoundedCornerShape(8.dp))
                .clickable(onClick = onAdd)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Text("+ Add", fontSize = 12.sp, color = OnlineAdd, fontWeight = FontWeight.Medium)
        }
    }
}

// Barcode scan sheet lives in BarcodeScanSheet.kt (CameraX + ML Kit → Open Food Facts).
