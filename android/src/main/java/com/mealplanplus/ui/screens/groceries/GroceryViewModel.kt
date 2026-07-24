package com.mealplanplus.ui.screens.groceries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.MealsApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.data.generated.model.DietDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.MealDto
import com.mealplanplus.data.local.GroceryStore
import com.mealplanplus.data.local.GroceryWork
import com.mealplanplus.data.local.SavedGroceryItem
import com.mealplanplus.data.local.SavedGroceryList
import com.mealplanplus.data.local.WorkRow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** Aisle categories — mirrors the design's grocCats (keyword match + colour). */
enum class GroceryCat(val label: String, val colorArgb: Long, val match: List<String>) {
    PRODUCE("Produce", 0xFF2E9E6A, listOf("banana", "blueberr", "broccoli", "apple", "spinach", "tomato", "lemon", "berr", "avocado", "greens", "fruit", "veg", "lettuce", "onion")),
    PROTEIN("Meat & protein", 0xFFC0563F, listOf("chicken", "salmon", "egg", "whey", "protein", "beef", "turkey", "fish", "tuna", "steak")),
    DAIRY("Dairy", 0xFF5B7FC4, listOf("yogurt", "cheese", "milk", "butter", "cream")),
    PANTRY("Pantry & grains", 0xFFA8823F, listOf("rice", "oatmeal", "oat", "almond", "olive oil", "honey", "granola", "quinoa", "farro", "bread", "peanut", "oil", "seed", "bar", "nut")),
    OTHER("Other", 0xFF7B8288, emptyList());

    companion object {
        fun of(name: String): GroceryCat {
            val n = name.lowercase()
            return entries.firstOrNull { c -> c.match.any { n.contains(it) } } ?: OTHER
        }
    }
}

/**
 * One shopping-list line — an independent, individually-checkable row. A single ingredient can
 * appear as two rows: a checked (bought) one and an unchecked (to-buy) one, e.g. after a refresh
 * adds more of something you'd already ticked. [key] = name|unit groups the two.
 */
data class GroceryRow(
    val id: String,
    val key: String,
    val name: String,
    val unit: String,
    val qty: Double,
    val checked: Boolean,
    val category: GroceryCat,
)

enum class GroceryView { ALL, TO_BUY, BOUGHT }

data class GroceryUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedDates: Set<LocalDate> = emptySet(),
    val plannedDates: Set<LocalDate> = emptySet(),
    val calOpen: Boolean = false,
    val view: GroceryView = GroceryView.ALL,
    /** Rows of the current list (live or the active saved snapshot). */
    val rows: List<GroceryRow> = emptyList(),
    val savedLists: List<SavedGroceryList> = emptyList(),
    val activeId: String? = null,
    val sheetSaved: Boolean = false,
    val error: String? = null,
) {
    val isSaved: Boolean get() = activeId != null
    val activeName: String? get() = savedLists.firstOrNull { it.id == activeId }?.name
    /** Sorted date keys backing the current list (live selection or the saved snapshot). */
    val dateKeys: List<String>
        get() = if (isSaved) savedLists.firstOrNull { it.id == activeId }?.dateKeys.orEmpty()
        else selectedDates.map { it.toString() }.sorted()
    val toBuy: List<GroceryRow> get() = rows.filter { !it.checked }
    val bought: List<GroceryRow> get() = rows.filter { it.checked }
    val boughtCount: Int get() = bought.size
    val total: Int get() = rows.size
}

@HiltViewModel
class GroceryViewModel @Inject constructor(
    private val plansApi: PlansApi,
    private val dietsApi: DietsApi,
    private val mealsApi: MealsApi,
    private val foodsApi: FoodsApi,
    private val store: GroceryStore,
) : ViewModel() {

    private val _state = MutableStateFlow(GroceryUiState())
    val state: StateFlow<GroceryUiState> = _state

    // Server caches used to expand a day-plan into ingredients (same source as Plan).
    private var dietsById: Map<Long, DietDto> = emptyMap()
    private var mealsById: Map<Long?, MealDto> = emptyMap()
    private var foodsById: Map<Long?, FoodDto> = emptyMap()
    private var plansByDate: Map<LocalDate, DayPlanDto> = emptyMap()
    /** The live list's rows (checked + unchecked). Survives navigation; only a refresh or a day
     *  change reconciles it against the plan. */
    private var liveRows: List<GroceryRow> = emptyList()

    init {
        val work = store.loadWork()
        if (work != null) {
            liveRows = work.rows.map { GroceryRow(it.id, it.key, it.name, it.unit, it.qty, it.checked, GroceryCat.of(it.name)) }
            _state.update {
                it.copy(
                    savedLists = store.load(),
                    activeId = work.activeId,
                    selectedDates = work.selected.mapNotNull { k -> runCatching { LocalDate.parse(k) }.getOrNull() }.toSet(),
                    rows = if (work.activeId == null) liveRows.sortedBy { r -> r.name.lowercase() } else it.rows,
                )
            }
            if (work.activeId != null) showRows()
            // Reconcile only if the live list is empty; otherwise keep it until refresh / day change.
            fetchAll(forceRegen = liveRows.isEmpty() && work.activeId == null)
        } else {
            _state.update { it.copy(savedLists = store.load()) }
            preset(7)      // next 7 days auto-selected on first ever open
            fetchAll(forceRegen = true)
        }
    }

    /** Save the live working state so rows + selection survive navigating away and back. */
    private fun persistWork() {
        val s = _state.value
        store.saveWork(
            GroceryWork(
                selected = s.selectedDates.map { it.toString() },
                activeId = s.activeId,
                rows = liveRows.map { WorkRow(it.id, it.key, it.name, it.unit, it.qty, it.checked) },
            ),
        )
    }

    // ── Loading ──────────────────────────────────────────────────────────────────
    /** Fetch library + plans. The list reconciles only when [forceRegen] (refresh / first load);
     *  a routine load just refreshes the calendar dots, leaving the list alone so plan edits
     *  elsewhere don't silently reshuffle it. */
    private fun fetchAll(forceRegen: Boolean) {
        viewModelScope.launch {
            runCatching {
                val diets = dietsApi.listDiets(false).body().orEmpty()
                foodsById = foodsApi.listFoods(false).body().orEmpty().associateBy { it.id }
                mealsById = mealsApi.listMeals(false).body().orEmpty().associateBy { it.id }
                dietsById = diets.mapNotNull { d -> d.id?.let { it to d } }.toMap()
                fetchPlanWindow()
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            if (forceRegen) regenerate() else showRows()
            _state.update { it.copy(loading = false, refreshing = false) }
        }
    }

    private suspend fun fetchPlanWindow() {
        val m = _state.value.month
        val today = _state.value.today
        val from = minOf(m.atDay(1), today)
        val to = maxOf(m.atEndOfMonth(), today.plusDays(13))
        val plans = plansApi.listPlans(from, to).body().orEmpty()
        plansByDate = plansByDate + plans.associateBy { it.date }
        _state.update { it.copy(plannedDates = plansByDate.filterValues { p -> p.dietId != null }.keys) }
    }

    /** Re-pull plan + library and reconcile the list: checked (bought) rows are kept, and each
     *  ingredient's to-buy row is set to (new total − already bought). The ONLY thing that
     *  reflects plan edits. */
    fun refresh() {
        if (_state.value.isSaved) return
        _state.update { it.copy(refreshing = true) }
        fetchAll(forceRegen = true)
    }

    // ── Generation: selected days -> combined ingredient totals ────────────────────
    private data class FoodTotal(val name: String, val unit: String, val total: Double)

    private fun combine(dates: List<LocalDate>): Map<String, FoodTotal> {
        val map = LinkedHashMap<String, FoodTotal>()
        fun add(name: String?, qty: Double, unit: String) {
            val nm = (name ?: "Food").trim()
            val key = nm.lowercase() + "|" + unit
            val cur = map[key]
            map[key] = FoodTotal(nm, unit, (cur?.total ?: 0.0) + qty)
        }
        dates.forEach { date ->
            val dietId = plansByDate[date]?.dietId ?: return@forEach
            val diet = dietsById[dietId] ?: return@forEach
            (diet.meals ?: emptyList()).forEach { dm ->
                val meal = mealsById[dm.mealId]
                (meal?.items ?: emptyList()).forEach { add(foodsById[it.foodId]?.name, it.quantity, it.unit.value) }
            }
            (diet.foodItems ?: emptyList()).forEach { fi -> add(foodsById[fi.foodId]?.name, fi.quantity, fi.unit.value) }
        }
        return map
    }

    /**
     * Reconcile [liveRows] against fresh ingredient totals: keep every checked (bought) row, and for
     * each ingredient set a single to-buy row to (total − already-bought). New ingredients get a
     * to-buy row; ingredients dropped from the plan keep only their bought rows.
     */
    private fun reconcile(fresh: Map<String, FoodTotal>) {
        val byKey = liveRows.groupBy { it.key }
        val out = mutableListOf<GroceryRow>()
        fresh.forEach { (key, ft) ->
            val existing = byKey[key].orEmpty()
            // Bought can't exceed what the plan now needs (capped when a day is removed).
            val boughtQty = existing.filter { it.checked }.sumOf { it.qty }.coerceAtMost(ft.total)
            val remaining = (ft.total - boughtQty).coerceAtLeast(0.0)
            val cat = GroceryCat.of(ft.name)
            if (boughtQty > 0.0) out += GroceryRow("$key#b", key, ft.name, ft.unit, boughtQty, true, cat)
            if (remaining > 0.0) out += GroceryRow("$key#t", key, ft.name, ft.unit, remaining, false, cat)
        }
        byKey.forEach { (key, existing) ->
            if (!fresh.containsKey(key)) {
                val boughtQty = existing.filter { it.checked }.sumOf { it.qty }
                val f = existing.first()
                if (boughtQty > 0.0) out += GroceryRow("$key#b", key, f.name, f.unit, boughtQty, true, GroceryCat.of(f.name))
            }
        }
        liveRows = out.sortedBy { it.name.lowercase() }
    }

    /** Rebuild rows: reconcile the live list against the current selection, or show the active saved
     *  snapshot. Called on a day change, refresh, or first load — NOT on routine navigation. */
    private fun regenerate() {
        val s = _state.value
        if (s.activeId == null) reconcile(combine(s.selectedDates.toList().sorted()))
        showRows()
    }

    /** Push the current rows (live or the active saved list) into UI state + persist the live work. */
    private fun showRows() {
        val s = _state.value
        val active = s.savedLists.firstOrNull { it.id == s.activeId }
        val rows = if (active != null) {
            active.items.map { GroceryRow(it.key, it.key, it.name, it.unit, it.total, active.checked[it.key] == true, GroceryCat.of(it.name)) }
        } else {
            liveRows
        }.sortedBy { it.name.lowercase() }
        _state.update { it.copy(rows = rows) }
        persistWork()
    }

    // ── Date picking ───────────────────────────────────────────────────────────────
    fun toggleCal() { _state.update { it.copy(calOpen = !it.calOpen) } }
    fun prevMonth() { _state.update { it.copy(month = it.month.minusMonths(1)) }; loadDotsForMonth() }
    fun nextMonth() { _state.update { it.copy(month = it.month.plusMonths(1)) }; loadDotsForMonth() }

    /** Refresh the calendar's diet-dots for the visible month without rebuilding the list. */
    private fun loadDotsForMonth() {
        viewModelScope.launch { runCatching { fetchPlanWindow() } }
    }

    fun toggleDay(date: LocalDate) {
        _state.update { st ->
            val sel = st.selectedDates.toMutableSet().apply { if (!add(date)) remove(date) }
            st.copy(selectedDates = sel)
        }
        regenerate()
    }

    fun preset(n: Int) {
        val today = _state.value.today
        val sel = (0 until n).map { today.plusDays(it.toLong()) }.toSet()
        _state.update { it.copy(selectedDates = sel, month = YearMonth.from(today)) }
        regenerate()
    }

    fun clearDates() { _state.update { it.copy(selectedDates = emptySet()) }; regenerate() }

    // ── Views + checking ─────────────────────────────────────────────────────────
    fun setView(v: GroceryView) { _state.update { it.copy(view = v) } }

    /** Toggle one row bought / to-buy. For the live list, the ingredient's rows are consolidated
     *  into at most one bought + one to-buy row. */
    fun toggleRow(id: String) {
        val s = _state.value
        val active = s.savedLists.firstOrNull { it.id == s.activeId }
        if (active != null) {
            val row = s.rows.firstOrNull { it.id == id } ?: return
            val lists = s.savedLists.map { l ->
                if (l.id != active.id) l
                else l.copy(checked = l.checked.toMutableMap().apply { this[row.key] = !(this[row.key] ?: false) })
            }
            persist(lists)
            _state.update { it.copy(savedLists = lists) }
            showRows()
        } else {
            val row = liveRows.firstOrNull { it.id == id } ?: return
            val key = row.key
            val food = liveRows.filter { it.key == key }
            var boughtQty = food.filter { it.checked }.sumOf { it.qty }
            var toBuyQty = food.filter { !it.checked }.sumOf { it.qty }
            if (row.checked) { boughtQty -= row.qty; toBuyQty += row.qty } else { boughtQty += row.qty; toBuyQty -= row.qty }
            val rebuilt = buildList {
                if (boughtQty > 0.0) add(GroceryRow("$key#b", key, row.name, row.unit, boughtQty, true, row.category))
                if (toBuyQty > 0.0) add(GroceryRow("$key#t", key, row.name, row.unit, toBuyQty, false, row.category))
            }
            liveRows = (liveRows.filter { it.key != key } + rebuilt).sortedBy { it.name.lowercase() }
            showRows()
        }
    }

    fun uncheckAll() {
        val s = _state.value
        val active = s.savedLists.firstOrNull { it.id == s.activeId }
        if (active != null) {
            val lists = s.savedLists.map { l -> if (l.id == active.id) l.copy(checked = emptyMap()) else l }
            persist(lists)
            _state.update { it.copy(savedLists = lists) }
            showRows()
        } else {
            liveRows = liveRows.groupBy { it.key }.map { (key, rs) ->
                val f = rs.first()
                GroceryRow("$key#t", key, f.name, f.unit, rs.sumOf { it.qty }, false, f.category)
            }.sortedBy { it.name.lowercase() }
            showRows()
        }
    }

    // ── Saved lists ────────────────────────────────────────────────────────────────
    fun openSaved() { _state.update { it.copy(sheetSaved = true) } }
    fun closeSheet() { _state.update { it.copy(sheetSaved = false) } }

    fun saveList() {
        val s = _state.value
        val keys = s.selectedDates.map { it.toString() }.sorted()
        if (keys.isEmpty() || liveRows.isEmpty()) return
        val label = rangeLabel(s.selectedDates.toList().sorted())
        // Flatten the live rows per ingredient into the saved snapshot (total + fully-bought flag).
        val byFood = liveRows.groupBy { it.key }
        val items = byFood.map { (key, rs) -> val f = rs.first(); SavedGroceryItem(key, f.name, f.unit, rs.sumOf { it.qty }, 1) }
        val checked = byFood.filterValues { rs -> rs.all { it.checked } }.keys.associateWith { true }
        val id = "gl" + System.currentTimeMillis()
        val saved = SavedGroceryList(id = id, name = "Groceries · $label", dateKeys = keys, items = items, checked = checked, days = keys.size)
        val lists = listOf(saved) + s.savedLists
        persist(lists)
        _state.update { it.copy(savedLists = lists, activeId = id) }
        showRows()
    }

    fun loadSaved(id: String) {
        _state.update { it.copy(activeId = id, sheetSaved = false, view = GroceryView.ALL, calOpen = false) }
        showRows()
    }

    fun deleteSaved(id: String) {
        val lists = _state.value.savedLists.filterNot { it.id == id }
        persist(lists)
        _state.update { it.copy(savedLists = lists, activeId = if (it.activeId == id) null else it.activeId) }
        showRows()
    }

    fun newList() {
        _state.update { it.copy(activeId = null, view = GroceryView.ALL) }
        showRows()
    }

    private fun persist(lists: List<SavedGroceryList>) = store.save(lists)

    private fun rangeLabel(dates: List<LocalDate>): String {
        if (dates.isEmpty()) return ""
        fun lbl(d: LocalDate) = "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
        return if (dates.size == 1) lbl(dates.first()) else "${lbl(dates.first())} – ${lbl(dates.last())}"
    }
}
