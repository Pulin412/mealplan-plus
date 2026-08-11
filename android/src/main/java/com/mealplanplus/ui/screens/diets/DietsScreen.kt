package com.mealplanplus.ui.screens.diets

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import com.mealplanplus.data.model.DietEntry
import com.mealplanplus.data.model.DietEntryKind
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.repository.DietSlotUi
import com.mealplanplus.data.repository.DietUi
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.repository.defaultQtyFor
import com.mealplanplus.data.repository.gramsFor
import com.mealplanplus.data.repository.gramsPerUnit
import com.mealplanplus.data.repository.unitLabel
import com.mealplanplus.ui.components.AppCard
import com.mealplanplus.ui.components.CalorieValue
import com.mealplanplus.ui.components.FavoriteStar
import com.mealplanplus.ui.components.ShareToggle
import com.mealplanplus.ui.components.MacroText
import com.mealplanplus.ui.components.SegmentedControl
import com.mealplanplus.ui.components.UnsavedChangesDialog
import com.mealplanplus.ui.navigation.LocalUnsavedChangesController
import com.mealplanplus.ui.navigation.UnsavedChangesController
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.AppText
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DashedStroke
import com.mealplanplus.ui.theme.DeleteColor
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MealItemName
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import kotlin.math.roundToInt
import com.mealplanplus.data.model.MEAL_SLOTS

private val DIET_SLOTS = MEAL_SLOTS

/** Diet slots in canonical [DIET_SLOTS] order; unknown slot names fall to the end. */
private fun List<DietSlotUi>.inSlotOrder(): List<DietSlotUi> =
    sortedBy { DIET_SLOTS.indexOf(it.slot).let { i -> if (i < 0) Int.MAX_VALUE else i } }

@Composable
fun DietsScreen(
    onBack: () -> Unit = {},
    viewModel: DietViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.newDietOpen) {
        NewDietSheet(viewModel)
        return
    }

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
                }
                Text("Diets", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.weight(1f))
                Text("${state.diets.size} saved", fontSize = 13.sp, color = MutedLight)
            }

            SearchBar(state.searchQuery, viewModel::setSearch, "Search your diets…")

            var sortOpen by remember { mutableStateOf(false) }
            DietsToolbar(
                sortMode = state.sortMode, sortOpen = sortOpen,
                onSortClick = { sortOpen = true }, onSortDismiss = { sortOpen = false },
                onSortPick = { viewModel.setSort(it); sortOpen = false },
                favOnly = state.favOnly, favCount = state.favCount, onFavToggle = viewModel::toggleFavOnly,
                importedOnly = state.importedOnly, onImportedToggle = viewModel::toggleImportedOnly,
                viewMode = state.viewMode, onViewToggle = viewModel::setViewMode,
            )

            TagFilterRow(state.allTagNames, state.tagFilter, viewModel::setTagFilter)

            val diets = state.filteredDiets
            when {
                diets.isEmpty() && state.favOnly ->
                    EmptyState("★", "No favourite diets yet", "Tap the ☆ on any diet to save it here.")
                diets.isEmpty() ->
                    EmptyState("🥗", "No diets yet", "Tap + to build a day-plan from your meals & foods.")
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    items(diets, key = { it.diet.id }) { d ->
                        if (state.viewMode == DietViewMode.LIST) {
                            DietListCard(
                                d = d, expanded = d.diet.id in state.expandedIds,
                                shared = d.diet.id in state.sharedIds,
                                imported = d.diet.id in state.importedIds,
                                onToggleExpand = { viewModel.toggleExpand(d.diet.id) },
                                onToggleFav = { viewModel.toggleFavorite(d.diet) },
                                onToggleShare = { viewModel.toggleShare(d.diet) },
                                onDelete = { viewModel.deleteDiet(d.diet) },
                                onEdit = { viewModel.openEditDiet(d.diet) },
                            )
                            Spacer(Modifier.height(8.dp))
                        } else {
                            DietCompactRow(
                                d = d, expanded = d.diet.id in state.expandedIds,
                                shared = d.diet.id in state.sharedIds,
                                imported = d.diet.id in state.importedIds,
                                onToggleExpand = { viewModel.toggleExpand(d.diet.id) },
                                onToggleFav = { viewModel.toggleFavorite(d.diet) },
                                onToggleShare = { viewModel.toggleShare(d.diet) },
                                onDelete = { viewModel.deleteDiet(d.diet) },
                                onEdit = { viewModel.openEditDiet(d.diet) },
                            )
                            Divider(color = SurfaceMuted)
                        }
                    }
                }
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
                .size(56.dp).clip(CircleShape).background(Teal).clickable(onClick = viewModel::openNewDiet),
        ) {
            Icon(Icons.Default.Add, contentDescription = "New diet", tint = OnAccent, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun SearchBar(query: String, onChange: (String) -> Unit, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)).background(SurfaceMuted).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Icon(Icons.Default.Search, null, tint = MutedLight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(hint, fontSize = 14.sp, color = MutedLight)
            androidx.compose.foundation.text.BasicTextField(query, onChange,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Ink), singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
                modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DietsToolbar(
    sortMode: DietSort, sortOpen: Boolean,
    onSortClick: () -> Unit, onSortDismiss: () -> Unit, onSortPick: (DietSort) -> Unit,
    favOnly: Boolean, favCount: Int, onFavToggle: () -> Unit,
    importedOnly: Boolean, onImportedToggle: () -> Unit,
    viewMode: DietViewMode, onViewToggle: (DietViewMode) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, BorderCool, RoundedCornerShape(8.dp))
                    .clickable(onClick = onSortClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
                Text("↕ ${sortLabel(sortMode)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Icon(Icons.Default.KeyboardArrowDown, null, tint = MutedDark, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(expanded = sortOpen, onDismissRequest = onSortDismiss) {
                DietSort.entries.forEach { s ->
                    DropdownMenuItem(text = { Text(sortLabel(s), fontSize = 13.sp) }, onClick = { onSortPick(s) })
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (favOnly) Teal else BorderCool, RoundedCornerShape(8.dp))
                .clickable(onClick = onFavToggle).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(if (favOnly) "★" else "☆", fontSize = 13.sp, color = if (favOnly) Teal else MutedDark)
            Spacer(Modifier.width(4.dp))
            Text("$favCount", fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                color = if (favOnly) Teal else MutedDark)
        }
        Spacer(Modifier.width(8.dp))
        Text("Imported", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = if (importedOnly) Teal else MutedDark,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (importedOnly) Teal else BorderCool, RoundedCornerShape(8.dp))
                .clickable(onClick = onImportedToggle).padding(horizontal = 10.dp, vertical = 6.dp))
        Spacer(Modifier.weight(1f))
        val modes = listOf(DietViewMode.LIST to "☰", DietViewMode.COMPACT to "≣")
        SegmentedControl(
            optionCount = modes.size,
            selectedIndex = modes.indexOfFirst { it.first == viewMode },
            onSelect = { onViewToggle(modes[it].first) },
        ) { i, selected ->
            Text(modes[i].second, fontSize = 14.sp, color = if (selected) Surface else MutedLight)
        }
    }
}

private fun sortLabel(s: DietSort) = when (s) {
    DietSort.RECENT -> "Recent"; DietSort.NAME -> "Name"
    DietSort.CALORIES -> "Calories"; DietSort.PROTEIN -> "Protein"
}

@Composable
private fun TagFilterRow(tags: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    if (tags.isEmpty()) return
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item { TagChip("All", selected == null) { onSelect(null) } }
        items(tags) { t -> TagChip(t, selected == t) { onSelect(if (selected == t) null else t) } }
    }
}

@Composable
private fun TagChip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
        color = if (on) Surface else MutedDark,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
            .background(if (on) Ink else SurfaceMuted)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp))
}

// ── List card ────────────────────────────────────────────────────────────────
@Composable
private fun DietListCard(
    d: DietUi, expanded: Boolean, shared: Boolean, imported: Boolean,
    onToggleExpand: () -> Unit, onToggleFav: () -> Unit, onToggleShare: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit,
) {
    AppCard(modifier = Modifier.clickable(onClick = onToggleExpand)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(d.diet.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(d.summary, fontSize = 10.5.sp, color = MutedLight,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                if (d.diet.tags.isNotEmpty()) {
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        d.diet.tags.take(3).forEach { tag ->
                            Text(tag.name, fontSize = 8.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                        if (d.diet.tags.size > 3)
                            Text("+${d.diet.tags.size - 3}", fontSize = 8.5.sp, color = MutedFaint)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Fixed-width action columns + right-aligned calories so the globe/star/kcal line up
            // across rows regardless of calorie length. Imported rows reserve the globe's slot.
            if (!imported) {
                ShareToggle(shared = shared, onClick = onToggleShare, size = 26.dp)
            } else {
                Spacer(Modifier.width(26.dp))   // reserve the globe column (imported can't re-share)
            }
            Spacer(Modifier.width(4.dp))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onToggleFav)) {
                FavoriteStar(active = d.diet.isFavorite, size = 17.dp)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(66.dp), contentAlignment = Alignment.CenterEnd) {
                CalorieValue(kcal = d.totalKcal)
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 9.dp)) {
                d.slots.inSlotOrder().forEach { SlotGroup(it) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    MacroText(d.totalProtein, d.totalCarbs, d.totalFat, fontSize = 10.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✎ Edit", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal, modifier = Modifier.clickable(onClick = onEdit).padding(end = 14.dp))
                    Text("✕ Remove", fontSize = 12.sp, color = DeleteColor, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
        }
    }
}

/** Renders one slot of a diet (label + its meal/food entries, meals expandable to show ingredients).
 *  Public so the shared-diet detail screen reuses the exact same rendering. */
@Composable
fun SlotGroup(slot: DietSlotUi) {
    Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 2.dp)) {
            // Per-slot kcal intentionally omitted — each ingredient row already shows its calories.
            Text(slot.slot.uppercase(), fontSize = AppText.slotLabel, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Teal.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp))
        }
        slot.entries.forEach { e ->
            val expandable = e.kind == DietEntryKind.MEAL && e.mealFoods.isNotEmpty()
            var open by remember { mutableStateOf(false) }
            Column(Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                        .then(if (expandable) Modifier.clickable { open = !open } else Modifier)
                        .padding(vertical = 2.dp),
                ) {
                    Text((if (e.kind == DietEntryKind.MEAL) "🍲 " else "") + e.name,
                        fontSize = AppText.itemName, fontWeight = FontWeight.Medium, color = MealItemName,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (expandable) Text(if (open) " ▲" else " ▼", fontSize = 9.sp, color = MutedFaint)
                    Spacer(Modifier.width(8.dp))
                    Text(e.meta, fontFamily = DmMono, fontSize = AppText.meta, color = MutedFaint)
                }
                if (expandable && open) {
                    Column(Modifier.fillMaxWidth().padding(start = 18.dp, top = 1.dp, bottom = 3.dp)) {
                        e.mealFoods.forEach { f ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("• ${f.name}", fontSize = AppText.subItem, color = MutedLight,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(6.dp))
                                Text(f.meta, fontFamily = DmMono, fontSize = AppText.meta, color = MutedFaint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DietCompactRow(
    d: DietUi, expanded: Boolean, shared: Boolean, imported: Boolean,
    onToggleExpand: () -> Unit, onToggleFav: () -> Unit, onToggleShare: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onToggleExpand).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.diet.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${d.entryCount} items · ${d.slots.size} slots", fontSize = 9.5.sp, color = MutedFaint)
                if (d.diet.tags.isNotEmpty()) {
                    Text(d.diet.tags.joinToString(" · ") { it.name },
                        fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                }
            }
            if (!imported) {
                ShareToggle(shared = shared, onClick = onToggleShare, size = 22.dp)
            } else {
                Spacer(Modifier.width(22.dp))   // reserve the globe column
            }
            Spacer(Modifier.width(4.dp))
            Box(contentAlignment = Alignment.Center,
                modifier = Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onToggleFav)) {
                FavoriteStar(active = d.diet.isFavorite, size = 13.dp)
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.width(60.dp), contentAlignment = Alignment.CenterEnd) {
                CalorieValue(kcal = d.totalKcal, fontSize = 12.sp)
            }
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                d.slots.inSlotOrder().forEach { SlotGroup(it) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    MacroText(d.totalProtein, d.totalCarbs, d.totalFat, fontSize = 9.5.sp)
                    Spacer(Modifier.weight(1f))
                    Text("✎ Edit", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Teal, modifier = Modifier.clickable(onClick = onEdit).padding(end = 12.dp))
                    Text("✕ Remove", fontSize = 11.sp, color = DeleteColor, modifier = Modifier.clickable(onClick = onDelete))
                }
            }
        }
    }
}

@Composable
private fun EmptyState(glyph: String, title: String, sub: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(glyph, fontSize = 40.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
        Text(sub, fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(top = 4.dp))
    }
}

// ── New Diet builder ──────────────────────────────────────────────────────────
private data class BuildEntry(
    val kind: DietEntryKind,
    val refServerId: String,
    val name: String,
    val slot: String,
    val kcalPer: Double,     // MEAL: total kcal · FOOD: kcal/100g
    val proteinPer: Double,
    val carbsPer: Double,
    val fatPer: Double,
    val unit: String = "GRAM",      // FOOD only
    val gramsPerUnit: Double = 1.0, // FOOD only
    val quantity: Double = 1.0,
) {
    val grams: Double get() = quantity * gramsPerUnit
    val kcal: Int get() = if (kind == DietEntryKind.MEAL) kcalPer.roundToInt() else (kcalPer * grams / 100.0).roundToInt()
    fun macro(per: Double): Double = if (kind == DietEntryKind.MEAL) per * quantity else per * grams / 100.0
}

private enum class AddTab { MEALS, FOODS }

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun NewDietSheet(viewModel: DietViewModel) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val editing = state.editingDiet
    var name by remember(editing?.id) { mutableStateOf(editing?.name ?: "") }
    val entries = remember(editing?.id) {
        mutableStateListOf<BuildEntry>().apply {
            editing?.entries?.forEach { e ->
                when (e.kind) {
                    DietEntryKind.MEAL -> state.meals.find { it.meal.id == e.refServerId }?.let { m ->
                        add(BuildEntry(DietEntryKind.MEAL, m.meal.id, m.meal.name, e.slot,
                            m.totalKcal.toDouble(), m.totalProtein, m.totalCarbs, m.totalFat))
                    }
                    DietEntryKind.FOOD -> state.foods.find { it.id == e.refServerId }?.let { food ->
                        add(BuildEntry(DietEntryKind.FOOD, food.id, food.name, e.slot,
                            food.caloriesPer100, food.proteinPer100, food.carbsPer100, food.fatPer100,
                            e.unit, food.gramsFor(1.0, e.unit), e.quantity))
                    }
                }
            }
        }
    }
    val selectedTags = remember(editing?.id) { mutableStateListOf<DietTag>().apply { editing?.tags?.let { addAll(it) } } }
    var addOpen by remember { mutableStateOf(false) }
    var dirty by remember(editing?.id) { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank() && entries.isNotEmpty()
    fun save() {
        viewModel.createDiet(name.trim(), entries.map {
            DietEntry(it.kind, it.refServerId, it.slot, it.quantity, it.unit)
        }, selectedTags.toList())
    }
    fun attemptClose() { if (dirty) showConfirm = true else viewModel.closeNewDiet() }
    BackHandler { if (addOpen) addOpen = false else attemptClose() }

    // Register with the app-level guard so bottom-nav taps also prompt while dirty.
    val unsaved = LocalUnsavedChangesController.current
    DisposableEffect(dirty, canSave) {
        unsaved.guard = if (dirty) UnsavedChangesController.Guard(canSave, { save() }, { viewModel.closeNewDiet() }) else null
        onDispose { unsaved.guard = null }
    }
    if (showConfirm) {
        UnsavedChangesDialog(
            canSave = canSave,
            onSave = { showConfirm = false; save() },
            onDiscard = { showConfirm = false; viewModel.closeNewDiet() },
            onDismiss = { showConfirm = false },
        )
    }

    val totalKcal = entries.sumOf { it.kcal }
    val p = entries.sumOf { it.macro(it.proteinPer) }
    val c = entries.sumOf { it.macro(it.carbsPer) }
    val f = entries.sumOf { it.macro(it.fatPer) }

    fun setQty(index: Int, q: Double) { if (index in entries.indices) { entries[index] = entries[index].copy(quantity = q); dirty = true } }

    Column(Modifier.fillMaxSize().background(AppBg)) {
        if (!addOpen) {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
                IconButton(onClick = { attemptClose() }) { Icon(Icons.Default.Close, "Close", tint = Ink) }
                Text(if (editing != null) "Edit diet" else "New diet", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                if (editing != null) {
                    Spacer(Modifier.weight(1f))
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                        modifier = Modifier.padding(end = 8.dp).clickable { viewModel.deleteDiet(editing); viewModel.closeNewDiet() })
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                Label("Diet name")
                OutlinedTextField(value = name, onValueChange = { name = it; dirty = true }, singleLine = true,
                    placeholder = { Text("e.g. High-protein day", fontSize = 13.sp, color = MutedLight) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp))

                TagPicker(
                    available = state.availableTags,
                    selected = selectedTags,
                    onToggle = { t -> dirty = true; if (selectedTags.any { it.id == t.id }) selectedTags.removeAll { it.id == t.id } else selectedTags.add(t) },
                    onCreate = { nm -> dirty = true; scope.launch { viewModel.createTag(nm)?.let { if (selectedTags.none { s -> s.id == it.id }) selectedTags.add(it) } } },
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text("Plan", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("$totalKcal kcal", fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                }

                if (entries.isEmpty()) {
                    DashedBox("Nothing planned yet — add meals or foods into slots.")
                } else {
                    // grouped by slot in canonical order
                    DIET_SLOTS.filter { s -> entries.any { it.slot == s } }
                        .plus(entries.map { it.slot }.filter { it !in DIET_SLOTS }.distinct())
                        .forEach { slot ->
                            Text(slot.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                            entries.withIndex().filter { it.value.slot == slot }.forEach { (idx, be) ->
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (be.kind == DietEntryKind.MEAL) "🍲 ${be.name}" else be.name,
                                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                                        Text("${be.kcal} kcal", fontFamily = DmMono, fontSize = 10.sp, color = MutedFaint)
                                    }
                                    if (be.kind == DietEntryKind.FOOD) {
                                        QtyField(be.refServerId + be.slot, be.quantity, be.unit) { setQty(idx, it) }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("✕", fontSize = 13.sp, color = DeleteColor,
                                        modifier = Modifier.clickable { entries.removeAt(idx); dirty = true })
                                }
                                Divider(color = SurfaceMuted)
                            }
                        }
                }

                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp)).dashedBorder(DashedStroke).clickable { addOpen = true }
                    .padding(vertical = 12.dp)) {
                    Text("＋ Add meal or food", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Teal)
                }
                if (entries.isNotEmpty()) {
                    Text("P${p.r1()} · C${c.r1()} · F${f.r1()}", fontFamily = DmMono, fontSize = 10.5.sp, color = MutedFaint,
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(16.dp)
                .clip(RoundedCornerShape(12.dp)).background(if (canSave) Teal else BorderCool)
                .clickable(enabled = canSave) { save() }.padding(vertical = 14.dp)) {
                Text(if (editing != null) "Save changes" else "Save diet", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (canSave) OnAccent else MutedLight)
            }
        } else {
            AddToDietPanel(
                meals = state.meals, foods = state.foods, entries = entries,
                onClose = { addOpen = false },
                onAdd = { be -> if (entries.none { it.refServerId == be.refServerId && it.slot == be.slot }) { entries.add(be); dirty = true } },
                onRemove = { ref, slot -> entries.removeAll { it.refServerId == ref && it.slot == slot }; dirty = true },
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddToDietPanel(
    meals: List<MealUi>,
    foods: List<com.mealplanplus.data.model.Food>,
    entries: List<BuildEntry>,
    onClose: () -> Unit,
    onAdd: (BuildEntry) -> Unit,
    onRemove: (String, String) -> Unit,
) {
    var slot by remember { mutableStateOf(DIET_SLOTS.first()) }
    var tab by remember { mutableStateOf(AddTab.MEALS) }
    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("‹", fontSize = 22.sp, color = Ink, modifier = Modifier.clickable(onClick = onClose))
            Spacer(Modifier.width(8.dp))
            Text("Add to diet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        Label("Slot")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            DIET_SLOTS.forEach { s ->
                val on = s == slot
                Text(s, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) OnAccent else MutedDark,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (on) Teal else Color.Transparent)
                        .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                        .clickable { slot = s }.padding(horizontal = 11.dp, vertical = 6.dp))
            }
        }

        val tabs = listOf(AddTab.MEALS to "Meals", AddTab.FOODS to "Foods")
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
            tabs.forEach { (t, lbl) ->
                val on = t == tab
                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)
                    .clip(RoundedCornerShape(9.dp)).background(if (on) Ink else SurfaceMuted)
                    .clickable { tab = t }.padding(vertical = 9.dp)) {
                    Text(lbl, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (on) Surface else MutedDark)
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        SearchField(query, { query = it }, if (tab == AddTab.MEALS) "Search your meals…" else "Search your foods…")

        LazyColumn(Modifier.weight(1f)) {
            if (tab == AddTab.MEALS) {
                val list = meals.filter { query.isBlank() || it.meal.name.contains(query, ignoreCase = true) }
                items(list, key = { it.meal.id }) { m ->
                    val added = entries.any { it.refServerId == m.meal.id && it.slot == slot }
                    PickRow(m.meal.name, "${m.totalKcal} kcal · ${m.items.size} items", added,
                        onAdd = { onAdd(BuildEntry(DietEntryKind.MEAL, m.meal.id, m.meal.name, slot,
                            m.totalKcal.toDouble(), m.totalProtein, m.totalCarbs, m.totalFat)) },
                        onRemove = { onRemove(m.meal.id, slot) })
                }
            } else {
                val list = foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
                items(list, key = { it.id }) { food ->
                    val added = entries.any { it.refServerId == food.id && it.slot == slot }
                    PickRow(food.name, "${food.caloriesPer100.toInt()} kcal / 100g", added,
                        onAdd = { onAdd(BuildEntry(DietEntryKind.FOOD, food.id, food.name, slot,
                            food.caloriesPer100, food.proteinPer100, food.carbsPer100, food.fatPer100,
                            food.unit, food.gramsPerUnit(), defaultQtyFor(food.unit))) },
                        onRemove = { onRemove(food.id, slot) })
                }
            }
        }

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onClose).padding(vertical = 13.dp)) {
            Text("Done · ${entries.size} added", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TagPicker(
    available: List<DietTag>,
    selected: List<DietTag>,
    onToggle: (DietTag) -> Unit,
    onCreate: (String) -> Unit,
) {
    var newTag by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.Bottom) {
        Label("Tags")
        Text("  · optional", fontSize = 11.sp, color = MutedFaint, modifier = Modifier.padding(bottom = 7.dp))
    }
    val shown = (available + selected).distinctBy { it.id }
    if (shown.isNotEmpty()) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            shown.forEach { tag ->
                val on = selected.any { it.id == tag.id }
                Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = if (on) OnAccent else MutedDark,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (on) Teal else Color.Transparent)
                        .border(1.5.dp, if (on) Teal else BorderCool, RoundedCornerShape(20.dp))
                        .clickable { onToggle(tag) }.padding(horizontal = 11.dp, vertical = 6.dp))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(SurfaceMuted).padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (newTag.isEmpty()) Text("New tag e.g. chicken-based", fontSize = 12.sp, color = MutedLight)
            androidx.compose.foundation.text.BasicTextField(newTag, { newTag = it }, singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = Ink),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.width(8.dp))
        val canAdd = newTag.isNotBlank()
        Text("Add", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (canAdd) Teal else MutedLight,
            modifier = Modifier.clip(RoundedCornerShape(9.dp))
                .border(1.5.dp, if (canAdd) Teal else BorderCool, RoundedCornerShape(9.dp))
                .clickable(enabled = canAdd) { onCreate(newTag.trim()); newTag = "" }
                .padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

// ── small shared helpers ──────────────────────────────────────────────────────
@Composable private fun Label(text: String) =
    Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MutedDark, modifier = Modifier.padding(bottom = 5.dp))

@Composable private fun SearchField(value: String, onChange: (String) -> Unit, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(SurfaceMuted).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Icon(Icons.Default.Search, null, tint = MutedLight, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, fontSize = 13.sp, color = MutedLight)
            androidx.compose.foundation.text.BasicTextField(value, onChange,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, color = Ink), singleLine = true,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
                modifier = Modifier.fillMaxWidth())
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable private fun PickRow(name: String, meta: String, added: Boolean, onAdd: () -> Unit, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(meta, fontSize = 10.5.sp, color = Muted)
        }
        if (added) {
            Text("✓ Added", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onRemove).padding(horizontal = 12.dp, vertical = 7.dp))
        } else {
            Text("+ Add", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Teal,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(9.dp)).clickable(onClick = onAdd).padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
    Divider(color = SurfaceMuted)
}

@Composable private fun QtyField(id: String, quantity: Double, unit: String, onChange: (Double) -> Unit) {
    val step = if (unit in setOf("PIECE", "CUP", "TBSP", "TSP")) 0.5 else 10.0
    val canDec = quantity - step >= 0.0
    // Editable value in the middle; +/- adjust it. Local text state keeps partial input (e.g. "1.") stable.
    var text by remember(id) { mutableStateOf(fmtQty(quantity)) }
    LaunchedEffect(quantity) {
        val f = fmtQty(quantity)
        if (f != text && text.toDoubleOrNull() != quantity) text = f
    }
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, BorderCool, RoundedCornerShape(8.dp))) {
        Box(Modifier.size(30.dp).clickable(enabled = canDec) { onChange((quantity - step).coerceAtLeast(0.0)) }, contentAlignment = Alignment.Center) {
            Text("−", color = if (canDec) Teal else BorderCool, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        androidx.compose.foundation.text.BasicTextField(
            value = text,
            onValueChange = { s ->
                val f = s.filter { it.isDigit() || it == '.' }.let { v -> if (v.count { it == '.' } > 1) text else v }
                text = f
                f.toDoubleOrNull()?.let(onChange)
            },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = DmMono, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Teal),
            modifier = Modifier.widthIn(min = 30.dp).width(44.dp),
        )
        Box(Modifier.size(30.dp).clickable { onChange(quantity + step) }, contentAlignment = Alignment.Center) {
            Text("+", color = Teal, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(unitLabel(unit), fontFamily = DmMono, fontSize = 11.sp, color = MutedFaint, modifier = Modifier.padding(end = 8.dp))
    }
}

@Composable private fun DashedBox(text: String) =
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).dashedBorder(DashedStroke).padding(18.dp)) {
        Text(text, fontSize = 11.5.sp, color = MutedLight, textAlign = TextAlign.Center)
    }

private fun Modifier.dashedBorder(color: Color): Modifier = drawBehind {
    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx(),
        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
    drawRoundRect(color = color, style = stroke, cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
}

private fun fmtQty(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
private fun Double.r1(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
