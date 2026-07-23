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
import com.mealplanplus.data.local.SavedGroceryItem
import com.mealplanplus.data.local.SavedGroceryList
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

/** A combined shopping-list line: one ingredient with its summed quantity. */
data class GroceryItem(
    val key: String,
    val name: String,
    val unit: String,
    val total: Double,
    val count: Int,
    val category: GroceryCat,
)

enum class GroceryView { ALL, TO_BUY, BOUGHT }

data class GroceryUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val today: LocalDate = LocalDate.now(),
    val selectedDates: Set<LocalDate> = emptySet(),
    val plannedDates: Set<LocalDate> = emptySet(),
    val calOpen: Boolean = false,
    val view: GroceryView = GroceryView.ALL,
    /** Generated (live) or snapshot (saved) shopping-list items. */
    val items: List<GroceryItem> = emptyList(),
    /** key -> bought. For a live list this is [liveChecked]; for a saved list, its own map. */
    val checked: Map<String, Boolean> = emptyMap(),
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
    val boughtCount: Int get() = items.count { checked[it.key] == true }
    val total: Int get() = items.size
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
    private var liveChecked: Map<String, Boolean> = emptyMap()

    init {
        _state.update { it.copy(savedLists = store.load()) }
        preset(7)          // next 7 days auto-selected
        loadData()
        loadPlans()
    }

    // ── Loading ──────────────────────────────────────────────────────────────────
    private fun loadData() {
        viewModelScope.launch {
            runCatching {
                val diets = dietsApi.listDiets(false).body().orEmpty()
                foodsById = foodsApi.listFoods(false).body().orEmpty().associateBy { it.id }
                mealsById = mealsApi.listMeals(false).body().orEmpty().associateBy { it.id }
                dietsById = diets.mapNotNull { d -> d.id?.let { it to d } }.toMap()
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            regenerate()
        }
    }

    private fun loadPlans() {
        viewModelScope.launch {
            val m = _state.value.month
            val today = _state.value.today
            val from = minOf(m.atDay(1), today)
            val to = maxOf(m.atEndOfMonth(), today.plusDays(13))
            runCatching { plansApi.listPlans(from, to).body().orEmpty() }
                .onSuccess { plans ->
                    plansByDate = plans.associateBy { it.date }
                    _state.update {
                        it.copy(
                            loading = false,
                            plannedDates = plans.filter { p -> p.dietId != null }.map { p -> p.date }.toSet(),
                        )
                    }
                    regenerate()
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    // ── Generation: selected days -> combined, categorised ingredients ─────────────
    private fun combine(dates: List<LocalDate>): List<GroceryItem> {
        data class Acc(val name: String, val unit: String, var total: Double, var count: Int)
        val map = LinkedHashMap<String, Acc>()
        fun add(name: String?, qty: Double, unit: String) {
            val nm = (name ?: "Food").trim()
            val key = nm.lowercase() + "|" + unit
            val acc = map.getOrPut(key) { Acc(nm, unit, 0.0, 0) }
            acc.total += qty; acc.count += 1
        }
        dates.forEach { date ->
            val dietId = plansByDate[date]?.dietId ?: return@forEach
            val diet = dietsById[dietId] ?: return@forEach
            (diet.meals ?: emptyList()).forEach { dm ->
                val meal = mealsById[dm.mealId]
                (meal?.items ?: emptyList()).forEach { it ->
                    add(foodsById[it.foodId]?.name, it.quantity, it.unit.value)
                }
            }
            (diet.foodItems ?: emptyList()).forEach { fi ->
                add(foodsById[fi.foodId]?.name, fi.quantity, fi.unit.value)
            }
        }
        return map.values.map {
            GroceryItem(
                key = it.name.lowercase() + "|" + it.unit,
                name = it.name, unit = it.unit, total = it.total, count = it.count,
                category = GroceryCat.of(it.name),
            )
        }.sortedBy { it.name.lowercase() }
    }

    /** Rebuild [GroceryUiState.items] + [checked] from the active saved list or the live selection. */
    private fun regenerate() {
        val s = _state.value
        val active = s.savedLists.firstOrNull { it.id == s.activeId }
        if (active != null) {
            val items = active.items.map {
                GroceryItem(it.key, it.name, it.unit, it.total, it.count, GroceryCat.of(it.name))
            }.sortedBy { it.name.lowercase() }
            _state.update { it.copy(items = items, checked = active.checked) }
        } else {
            val items = combine(s.selectedDates.toList().sorted())
            _state.update { it.copy(items = items, checked = liveChecked) }
        }
    }

    // ── Date picking ───────────────────────────────────────────────────────────────
    fun toggleCal() { _state.update { it.copy(calOpen = !it.calOpen) } }
    fun prevMonth() { _state.update { it.copy(month = it.month.minusMonths(1)) }; loadPlans() }
    fun nextMonth() { _state.update { it.copy(month = it.month.plusMonths(1)) }; loadPlans() }

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

    fun toggleItem(key: String) {
        val s = _state.value
        if (s.activeId != null) {
            val lists = s.savedLists.map { l ->
                if (l.id != s.activeId) l
                else l.copy(checked = l.checked.toMutableMap().apply { this[key] = !(this[key] ?: false) })
            }
            persist(lists)
            _state.update { it.copy(savedLists = lists) }
        } else {
            liveChecked = liveChecked.toMutableMap().apply { this[key] = !(this[key] ?: false) }
            _state.update { it.copy(checked = liveChecked) }
        }
    }

    fun uncheckAll() {
        val s = _state.value
        if (s.activeId != null) {
            val lists = s.savedLists.map { l -> if (l.id == s.activeId) l.copy(checked = emptyMap()) else l }
            persist(lists)
            _state.update { it.copy(savedLists = lists) }
        } else {
            liveChecked = emptyMap()
            _state.update { it.copy(checked = emptyMap()) }
        }
    }

    // ── Saved lists ────────────────────────────────────────────────────────────────
    fun openSaved() { _state.update { it.copy(sheetSaved = true) } }
    fun closeSheet() { _state.update { it.copy(sheetSaved = false) } }

    fun saveList() {
        val s = _state.value
        val keys = s.selectedDates.map { it.toString() }.sorted()
        if (keys.isEmpty() || s.items.isEmpty()) return
        val label = rangeLabel(s.selectedDates.toList().sorted())
        val items = s.items.map { SavedGroceryItem(it.key, it.name, it.unit, it.total, it.count) }
        val checked = s.items.filter { liveChecked[it.key] == true }.associate { it.key to true }
        val id = "gl" + System.currentTimeMillis()
        val saved = SavedGroceryList(
            id = id, name = "Groceries · $label", dateKeys = keys,
            items = items, checked = checked, days = keys.size,
        )
        val lists = listOf(saved) + s.savedLists
        persist(lists)
        _state.update { it.copy(savedLists = lists, activeId = id) }
        regenerate()
    }

    fun loadSaved(id: String) {
        _state.update { it.copy(activeId = id, sheetSaved = false, view = GroceryView.ALL, calOpen = false) }
        regenerate()
    }

    fun deleteSaved(id: String) {
        val lists = _state.value.savedLists.filterNot { it.id == id }
        persist(lists)
        _state.update { it.copy(savedLists = lists, activeId = if (it.activeId == id) null else it.activeId) }
        regenerate()
    }

    fun newList() {
        _state.update { it.copy(activeId = null, view = GroceryView.ALL) }
        regenerate()
    }

    private fun persist(lists: List<SavedGroceryList>) = store.save(lists)

    private fun rangeLabel(dates: List<LocalDate>): String {
        if (dates.isEmpty()) return ""
        fun lbl(d: LocalDate) = "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.dayOfMonth}"
        return if (dates.size == 1) lbl(dates.first()) else "${lbl(dates.first())} – ${lbl(dates.last())}"
    }
}
