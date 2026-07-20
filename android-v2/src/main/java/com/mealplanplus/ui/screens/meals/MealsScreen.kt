package com.mealplanplus.ui.screens.meals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.MealItem
import com.mealplanplus.data.repository.FOOD_UNITS
import com.mealplanplus.data.repository.MealItemUi
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.repository.defaultQtyFor
import com.mealplanplus.data.repository.gramsPerUnit
import com.mealplanplus.data.repository.isCountUnit
import com.mealplanplus.data.repository.unitLabel
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.components.CalorieValue
import com.mealplanplus.ui.components.FavoriteStar
import com.mealplanplus.ui.components.MacroText
import com.mealplanplus.ui.components.SegmentedControl
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.DeleteColor
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.DashedStroke
import com.mealplanplus.ui.theme.MealItemName
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun MealsScreen(
    onBack: () -> Unit = {},
    viewModel: MealViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.newMealOpen) {
        NewMealSheet(viewModel)
        return
    }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            // App bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Text("Meals", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${state.meals.size} saved", fontSize = 13.sp, color = MutedLight)
            }

            MealsSearchBar(state.searchQuery, viewModel::setSearch)

            var sortOpen by remember { mutableStateOf(false) }
            MealsToolbar(
                sortMode = state.sortMode,
                sortOpen = sortOpen,
                onSortClick = { sortOpen = true },
                onSortDismiss = { sortOpen = false },
                onSortPick = { viewModel.setSort(it); sortOpen = false },
                favOnly = state.favOnly,
                favCount = state.favCount,
                onFavToggle = viewModel::toggleFavOnly,
                viewMode = state.viewMode,
                onViewToggle = viewModel::setViewMode,
            )

            SlotFilterRow(state.allSlots, state.slotFilter, viewModel::setSlotFilter)

            val meals = state.filteredMeals
            when {
                meals.isEmpty() && state.favOnly ->
                    EmptyState("★", "No favourite meals yet", "Tap the ☆ on any meal to save it here.")
                meals.isEmpty() ->
                    EmptyState("🍲", "No meals yet", "Tap + to build a meal from your foods.")
                state.viewMode == MealViewMode.LIST -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    items(meals, key = { it.meal.id }) { m ->
                        MealListCard(
                            m = m,
                            expanded = m.meal.id in state.expandedIds,
                            onToggleExpand = { viewModel.toggleExpand(m.meal.id) },
                            onToggleFav = { viewModel.toggleFavorite(m.meal) },
                            onDelete = { viewModel.deleteMeal(m.meal) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .border(1.dp, CardBorder, RoundedCornerShape(12.dp)).background(Surface),
                        ) {
                            meals.forEachIndexed { i, m ->
                                MealCompactRow(
                                    m = m,
                                    expanded = m.meal.id in state.expandedIds,
                                    isLast = i == meals.lastIndex,
                                    onToggleExpand = { viewModel.toggleExpand(m.meal.id) },
                                    onToggleFav = { viewModel.toggleFavorite(m.meal) },
                                    onDelete = { viewModel.deleteMeal(m.meal) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                .size(56.dp).clip(CircleShape).background(Teal).clickable(onClick = viewModel::openNewMeal),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New meal", tint = OnAccent, modifier = Modifier.size(28.dp))
        }
    }
}

// ── Search + toolbar ─────────────────────────────────────────────────────────
@Composable
private fun MealsSearchBar(query: String, onChange: (String) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceMuted)
                .padding(horizontal = 12.dp, vertical = 11.dp),
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MutedLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text("Search your meals…", fontSize = 13.sp, color = MutedLight)
                androidx.compose.foundation.text.BasicTextField(
                    value = query, onValueChange = onChange,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink),
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MealsToolbar(
    sortMode: MealSort, sortOpen: Boolean,
    onSortClick: () -> Unit, onSortDismiss: () -> Unit, onSortPick: (MealSort) -> Unit,
    favOnly: Boolean, favCount: Int, onFavToggle: () -> Unit,
    viewMode: MealViewMode, onViewToggle: (MealViewMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSortClick).padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("↕ ${sortMode.label}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MutedLight, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = onSortDismiss) {
                MealSort.values().forEach { s ->
                    DropdownMenuItem(text = { Text(s.label, fontSize = 13.sp) }, onClick = { onSortPick(s) })
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (favOnly) Teal else CardBorder, RoundedCornerShape(8.dp))
                .background(if (favOnly) Teal.copy(alpha = 0.08f) else Color.Transparent)
                .clickable(onClick = onFavToggle).padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                "${if (favOnly) "★" else "☆"} $favCount",
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (favOnly) Teal else Ink,
            )
        }
        Spacer(Modifier.weight(1f))
        val modes = listOf(MealViewMode.LIST to "☰", MealViewMode.COMPACT to "≣")
        SegmentedControl(
            optionCount = modes.size,
            selectedIndex = modes.indexOfFirst { it.first == viewMode },
            onSelect = { onViewToggle(modes[it].first) },
        ) { i, selected ->
            Text(modes[i].second, fontSize = 14.sp, color = if (selected) Surface else MutedLight)
        }
    }
}

// ── List card ────────────────────────────────────────────────────────────────
@Composable
private fun MealListCard(
    m: MealUi, expanded: Boolean,
    onToggleExpand: () -> Unit, onToggleFav: () -> Unit, onDelete: () -> Unit,
) {
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.meal.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    m.meal.slots.firstOrNull()?.let { slot ->
                        Spacer(Modifier.width(6.dp))
                        SlotBadge(slot)
                        if (m.meal.slots.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            Text("+${m.meal.slots.size - 1}", fontSize = 9.sp, color = MutedFaint)
                        }
                    }
                }
                Text(m.itemsSummary, fontSize = 10.5.sp, color = MutedLight,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onToggleFav)) {
                FavoriteStar(active = m.meal.isFavorite, size = 17.dp)
            }
            Spacer(Modifier.width(8.dp))
            CalorieValue(kcal = m.totalKcal)
            Spacer(Modifier.width(10.dp))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp).clip(CircleShape).background(SurfaceMuted).clickable(onClick = onToggleExpand)) {
                Icon(Icons.Default.KeyboardArrowDown, "Expand", tint = MutedDark, modifier = Modifier.size(16.dp).rotate(rotation))
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 9.dp)) {
                m.items.forEach { ExpandedItemRow(it) }
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    MacroText(m.totalProtein, m.totalCarbs, m.totalFat, fontSize = 10.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✕ Remove", fontSize = 12.sp, color = DeleteColor, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
        }
    }
}

@Composable
private fun ExpandedItemRow(it: MealItemUi) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(it.name, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = MealItemName,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text(it.meta, fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
    }
}

// ── Compact row ──────────────────────────────────────────────────────────────
@Composable
private fun MealCompactRow(
    m: MealUi, expanded: Boolean, isLast: Boolean,
    onToggleExpand: () -> Unit, onToggleFav: () -> Unit, onDelete: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onToggleExpand)
            .padding(horizontal = 11.dp, vertical = 7.dp)
            .then(if (!isLast) Modifier else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(m.meal.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false))
                    m.meal.slots.firstOrNull()?.let { slot ->
                        Spacer(Modifier.width(6.dp))
                        SlotBadge(slot)
                        if (m.meal.slots.size > 1) {
                            Spacer(Modifier.width(4.dp))
                            Text("+${m.meal.slots.size - 1}", fontSize = 9.sp, color = MutedFaint)
                        }
                    }
                }
                Text("${m.items.size} items", fontSize = 9.5.sp, color = MutedFaint)
            }
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onToggleFav)) {
                FavoriteStar(active = m.meal.isFavorite, size = 13.dp)
            }
            Spacer(Modifier.width(4.dp))
            CalorieValue(kcal = m.totalKcal, fontSize = 12.sp)
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                m.items.forEach { ExpandedItemRow(it) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    MacroText(m.totalProtein, m.totalCarbs, m.totalFat, fontSize = 9.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✕ Remove", fontSize = 11.sp, color = DeleteColor, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
        }
        if (!isLast) Spacer(Modifier.height(0.dp))
    }
}

// ── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun SlotBadge(slot: String) {
    Text(
        slot.uppercase(), fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SlotFilterRow(slots: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    if (slots.isEmpty()) return
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        SlotFilterChip("All", selected == null) { onSelect(null) }
        slots.forEach { s -> SlotFilterChip(s, selected == s) { onSelect(if (selected == s) null else s) } }
    }
}

@Composable
private fun SlotFilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    Text(
        label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (on) OnAccent else MutedDark,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) Ink else SurfaceMuted)
            .clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
    ) {
        Text(icon, fontSize = 30.sp)
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(top = 8.dp))
        Text(subtitle, fontSize = 11.5.sp, color = MutedLight, modifier = Modifier.padding(top = 3.dp))
    }
}

// ── New meal builder (design-exact) ──────────────────────────────────────────
private val MEAL_SLOTS = listOf(
    "Early Breakfast", "Breakfast", "Noon", "Pre-Lunch", "Post-Lunch", "Evening",
    "Pre-workout", "Post-workout", "Pre-dinner", "Dinner", "Post Dinner",
)

private enum class AddMode { NONE, SEARCH, ONLINE, MANUAL }

private data class BuildItem(
    val foodId: String,
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val unit: String,
    val gramsPerUnit: Double,   // grams one [unit] weighs; 1.0 for GRAM/ML
    val quantity: Double,       // in [unit]
) {
    /** Grams this item contributes, for the per-100g macro math. */
    val grams: Double get() = quantity * gramsPerUnit
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NewMealSheet(viewModel: MealViewModel) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    val slots = remember { mutableStateListOf<String>() }
    val items = remember { mutableStateListOf<BuildItem>() }
    var addMode by remember { mutableStateOf(AddMode.NONE) }

    val totalKcal = items.sumOf { (it.kcalPer100 * it.grams / 100.0) }.roundToInt()
    val p = items.sumOf { it.proteinPer100 * it.grams / 100.0 }
    val c = items.sumOf { it.carbsPer100 * it.grams / 100.0 }
    val f = items.sumOf { it.fatPer100 * it.grams / 100.0 }

    fun addItem(bi: BuildItem) {
        if (items.none { it.foodId == bi.foodId }) items.add(bi)
    }
    fun setQty(foodId: String, q: Double) {
        val idx = items.indexOfFirst { it.foodId == foodId }
        if (idx >= 0) items[idx] = items[idx].copy(quantity = q)
    }

    Column(Modifier.fillMaxSize().background(AppBg)) {
        if (addMode == AddMode.NONE) {
            // ── Builder body ─────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                IconButton(onClick = viewModel::closeNewMeal) { Icon(Icons.Default.Close, "Close", tint = Ink) }
                Text("New meal", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Label("Meal name")
                OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true,
                    placeholder = { Text("e.g. Chicken rice bowl", fontSize = 13.sp, color = MutedLight) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Label("Meal slot")
                    Text("  · optional", fontSize = 11.sp, color = MutedFaint,
                        modifier = Modifier.padding(bottom = 7.dp))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    MEAL_SLOTS.forEach { slot ->
                        val on = slot in slots
                        Text(slot, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (on) OnAccent else MutedDark,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (on) Teal else Color.Transparent)
                                .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                                .clickable { if (on) slots.remove(slot) else slots.add(slot) }
                                .padding(horizontal = 11.dp, vertical = 6.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Food items", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("$totalKcal kcal", fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                }
                if (items.isEmpty()) {
                    DashedBox("No items yet — add food to build this meal.")
                } else {
                    items.forEach { bi ->
                        val kcal = (bi.kcalPer100 * bi.grams / 100.0).roundToInt()
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                            Column(Modifier.weight(1f)) {
                                Text(bi.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                Text("$kcal kcal", fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
                            }
                            QtyField(bi.foodId, bi.quantity, bi.unit) { setQty(bi.foodId, it) }
                            Spacer(Modifier.width(8.dp))
                            Text("✕", fontSize = 13.sp, color = DeleteColor,
                                modifier = Modifier.clickable { items.removeAll { it.foodId == bi.foodId } })
                        }
                        Divider(color = SurfaceMuted)
                    }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).dashedBorder(DashedStroke).clickable { addMode = AddMode.SEARCH }
                    .padding(vertical = 12.dp)) {
                    Text("＋ Add food item", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                }
                if (items.isNotEmpty()) {
                    Text("P${p.r1()} · C${c.r1()} · F${f.r1()}", fontFamily = DmMono, fontSize = 10.5.sp, color = MutedFaint,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
            }
            val canSave = name.isNotBlank() && items.isNotEmpty()
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)
                .clip(RoundedCornerShape(12.dp)).background(if (canSave) Teal else BorderCool)
                .clickable(enabled = canSave) {
                    viewModel.createMeal(name.trim(), slots.toList(),
                        items.map { MealItem(foodServerId = it.foodId, quantity = it.quantity, unit = it.unit) })
                }.padding(vertical = 14.dp)) {
                Text("Save meal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) OnAccent else MutedLight)
            }
        } else {
            // ── Add-food panel ───────────────────────────────────────────────
            AddFoodPanel(
                mode = addMode,
                foods = state.foods,
                items = items,
                onModeChange = { addMode = it },
                onAdd = ::addItem,
                onQty = ::setQty,
                onDone = { addMode = AddMode.NONE },
                searchOnline = { q -> viewModel.searchOnline(q) },
                saveOnline = { dto -> viewModel.saveOnlineFood(dto) },
                createManual = { n, k, pr, cb, ft, sv, u, gpu -> viewModel.createManualFood(n, k, pr, cb, ft, sv, u, gpu) },
                scope = scope,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddFoodPanel(
    mode: AddMode,
    foods: List<Food>,
    items: List<BuildItem>,
    onModeChange: (AddMode) -> Unit,
    onAdd: (BuildItem) -> Unit,
    onQty: (String, Double) -> Unit,
    onDone: () -> Unit,
    searchOnline: suspend (String) -> List<com.mealplanplus.data.generated.model.FoodDto>,
    saveOnline: suspend (com.mealplanplus.data.generated.model.FoodDto) -> String,
    createManual: suspend (String, Double, Double, Double, Double, String?, String, Double?) -> String,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    var query by remember(mode) { mutableStateOf("") }
    var online by remember { mutableStateOf<List<com.mealplanplus.data.generated.model.FoodDto>>(emptyList()) }
    val title = when (mode) { AddMode.ONLINE -> "Search online"; AddMode.MANUAL -> "New food"; else -> "Add food" }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("‹", fontSize = 22.sp, color = Ink, modifier = Modifier.clickable { onModeChange(AddMode.NONE) })
            Spacer(Modifier.width(8.dp))
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Spacer(Modifier.weight(1f))
            if (mode == AddMode.SEARCH) {
                ModeIcon("⌕") { onModeChange(AddMode.ONLINE) }
                Spacer(Modifier.width(6.dp))
                ModeIcon("✎") { onModeChange(AddMode.MANUAL) }
            }
        }

        when (mode) {
            AddMode.SEARCH -> {
                SearchField(query, { query = it }, "Search your foods…")
                val list = foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                LazyColumn(Modifier.weight(1f)) {
                    items(list, key = { it.id }) { food ->
                        val bi = items.firstOrNull { it.foodId == food.id }
                        PickRow(food.name, "${food.caloriesPer100.toInt()} kcal / 100g", bi,
                            onAdd = { onAdd(food.toBuildItem()) },
                            onQty = { onQty(food.id, it) })
                    }
                }
                DoneButton(items.size, onDone)
            }
            AddMode.ONLINE -> {
                SearchField(query, { query = it; scope.launch { online = searchOnline(it) } }, "Search foods & brands…")
                LazyColumn(Modifier.weight(1f)) {
                    items(online, key = { it.serverId ?: it.name }) { dto ->
                        PickRow(dto.name, "${dto.caloriesPer100.toInt()} kcal / 100g", null,
                            onAdd = {
                                scope.launch {
                                    val id = saveOnline(dto)
                                    onAdd(dto.toBuildItem(id))
                                }
                            }, onQty = {})
                    }
                }
                DoneButton(items.size, onDone)
            }
            AddMode.MANUAL -> {
                var mn by remember { mutableStateOf("") }
                var serving by remember { mutableStateOf("") }
                var kcal by remember { mutableStateOf("") }
                var pr by remember { mutableStateOf("") }
                var cb by remember { mutableStateOf("") }
                var ft by remember { mutableStateOf("") }
                var unit by remember { mutableStateOf("GRAM") }
                var gpu by remember { mutableStateOf("") }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Label("Name")
                    OutlinedTextField(mn, { mn = it }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp))
                    Label("Measured in")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp)) {
                        FOOD_UNITS.forEach { u ->
                            val on = u == unit
                            Text(unitLabel(u), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (on) OnAccent else MutedDark,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(if (on) Teal else Color.Transparent)
                                    .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                                    .clickable { unit = u }
                                    .padding(horizontal = 11.dp, vertical = 6.dp))
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 11.dp)) {
                        Column(Modifier.weight(1.4f)) {
                            if (isCountUnit(unit)) { Label("Grams per ${unitLabel(unit)}"); NumField(gpu) { gpu = it } }
                            else { Label("Serving (g)"); NumField(serving) { serving = it } }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) { Label("Calories /100g"); NumField(kcal) { kcal = it } }
                    }
                    Row(Modifier.fillMaxWidth().padding(bottom = 11.dp)) {
                        Column(Modifier.weight(1f)) { Label("Protein"); NumField(pr) { pr = it } }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Label("Carbs"); NumField(cb) { cb = it } }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) { Label("Fat"); NumField(ft) { ft = it } }
                    }
                }
                val gramsPerUnit = gpu.toDoubleOrNull()
                val ok = mn.isNotBlank() && kcal.isNotBlank() && (!isCountUnit(unit) || (gramsPerUnit ?: 0.0) > 0.0)
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).background(if (ok) Teal else BorderCool)
                    .clickable(enabled = ok) {
                        scope.launch {
                            val k = kcal.toDoubleOrNull() ?: 0.0
                            val p = pr.toDoubleOrNull() ?: 0.0
                            val c = cb.toDoubleOrNull() ?: 0.0
                            val fat = ft.toDoubleOrNull() ?: 0.0
                            val id = createManual(mn.trim(), k, p, c, fat, serving.ifBlank { null }, unit, gramsPerUnit)
                            onAdd(BuildItem(id, mn.trim(), k, p, c, fat, unit, if (isCountUnit(unit)) (gramsPerUnit ?: 1.0) else 1.0, defaultQtyFor(unit)))
                            onDone()
                        }
                    }.padding(vertical = 14.dp)) {
                    Text("Add to meal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (ok) OnAccent else MutedLight)
                }
            }
            AddMode.NONE -> {}
        }
    }
}

/** Local Food -> a builder item with its natural unit + default quantity. */
private fun Food.toBuildItem() = BuildItem(
    id, name, caloriesPer100, proteinPer100, carbsPer100, fatPer100,
    unit, gramsPerUnit(), defaultQtyFor(unit),
)

/** Online result -> a builder item (identity from the just-saved local id). */
private fun com.mealplanplus.data.generated.model.FoodDto.toBuildItem(id: String): BuildItem {
    val u = (unit ?: com.mealplanplus.data.generated.model.FoodUnit.GRAM).value
    val gpu = when (u) {
        "PIECE" -> gramsPerPiece ?: 1.0
        "CUP"   -> gramsPerCup ?: 1.0
        "TBSP"  -> gramsPerTbsp ?: 1.0
        "TSP"   -> gramsPerTsp ?: 1.0
        else    -> 1.0
    }
    return BuildItem(id, name, caloriesPer100, proteinPer100, carbsPer100, fatPer100, u, gpu, defaultQtyFor(u))
}

@Composable private fun Label(text: String) =
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(bottom = 5.dp))

@Composable private fun NumField(value: String, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

@Composable private fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(SurfaceMuted).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Icon(Icons.Default.Search, null, tint = MutedLight, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 13.sp, color = MutedLight)
            androidx.compose.foundation.text.BasicTextField(value, onChange,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink), singleLine = true,
                modifier = Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable private fun PickRow(name: String, meta: String, added: BuildItem?, onAdd: () -> Unit, onQty: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(meta, fontSize = 10.5.sp, color = Muted)
        }
        if (added == null) {
            Text("+ Add", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(9.dp)).clickable(onClick = onAdd).padding(horizontal = 12.dp, vertical = 7.dp))
        } else {
            QtyField(added.foodId, added.quantity, added.unit, onQty)
        }
    }
    Divider(color = SurfaceMuted)
}

/** Numeric quantity input with the food's unit label; user types any value. */
@Composable private fun QtyField(id: String, quantity: Double, unit: String, onChange: (Double) -> Unit) {
    var text by remember(id) { mutableStateOf(fmtQty(quantity)) }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, BorderCool, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)) {
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { s ->
                val filtered = s.filter { it.isDigit() || it == '.' }.let { v ->
                    if (v.count { it == '.' } > 1) text else v   // keep at most one dot
                }
                text = filtered
                filtered.toDoubleOrNull()?.let(onChange)
            },
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = DmMono, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = Ink, textAlign = androidx.compose.ui.text.style.TextAlign.End),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.widthIn(min = 34.dp).width(46.dp),
        )
        Spacer(Modifier.width(5.dp))
        Text(unitLabel(unit), fontFamily = DmMono, fontSize = 11.sp, color = MutedFaint)
    }
}

private fun fmtQty(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

@Composable private fun ModeIcon(glyph: String, onClick: () -> Unit) =
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(SurfaceMuted).clickable(onClick = onClick)) {
        Text(glyph, fontSize = 16.sp, color = Teal)
    }

@Composable private fun DoneButton(count: Int, onDone: () -> Unit) =
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onDone).padding(vertical = 13.dp)) {
        Text("Done · $count added", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
    }

@Composable private fun DashedBox(text: String) =
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).dashedBorder(DashedStroke).padding(18.dp)) {
        Text(text, fontSize = 11.5.sp, color = MutedLight, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
    drawRoundRect(color = color, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
}

private fun Double.r1(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)


private val MealSort.label: String
    get() = when (this) {
        MealSort.RECENT -> "Recent"; MealSort.NAME -> "Name"
        MealSort.CALORIES -> "Calories"; MealSort.PROTEIN -> "Protein"
    }
