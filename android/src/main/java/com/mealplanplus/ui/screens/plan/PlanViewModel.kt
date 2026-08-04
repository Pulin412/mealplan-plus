package com.mealplanplus.ui.screens.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.cache.ResponseCache
import com.mealplanplus.data.cache.render
import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.model.LoggedMealSlotDto
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.MealsApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.data.generated.model.DietDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.MealDto
import com.mealplanplus.data.generated.model.PlannedWorkoutDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.data.repository.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class DietLine(val name: String, val meta: String, val header: Boolean)
data class DietSlotView(val slot: String, val kcal: Int, val lines: List<DietLine>)
data class DietSummary(
    val id: Long,
    val name: String,
    val kcal: Int,
    val slots: List<DietSlotView> = emptyList(),
    val tags: List<String> = emptyList(),
)

private val SLOT_ORDER = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")

data class PlanUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val plansByDate: Map<LocalDate, DayPlanDto> = emptyMap(),
    val diets: List<DietSummary> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate? = null,
    val error: String? = null,
    val pickerOpen: Boolean = false,
    val pickerSearch: String = "",
    val pickerTag: String? = null,
    val workouts: List<WorkoutTemplateDto> = emptyList(),
    val workoutPickerOpen: Boolean = false,
    val openWorkout: WorkoutTemplateDto? = null,
    val completedDays: Set<LocalDate> = emptySet(),
    val selectedDaySlots: List<LoggedMealSlotDto> = emptyList(),
) {
    val allTags: List<String> get() = diets.flatMap { it.tags }.distinct().sorted()
    val filteredDiets: List<DietSummary> get() = diets.filter { d ->
        (pickerSearch.isBlank() || d.name.contains(pickerSearch, ignoreCase = true)) &&
            (pickerTag == null || d.tags.contains(pickerTag))
    }
}

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val plansApi: PlansApi,
    private val dietsApi: DietsApi,
    private val mealsApi: MealsApi,
    private val foodsApi: FoodsApi,
    private val workoutsApi: WorkoutTemplatesApi,
    private val loggingApi: LoggingApi,
    private val responseCache: ResponseCache,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state

    /** Range key currently reflected in [PlanUiState.plansByDate] — lets a month switch paint that
     *  month's cache immediately, while a same-range silent reload keeps what's shown (no flicker). */
    private var shownRange: String? = null

    init {
        loadDiets()
        loadWorkouts()
        loadPlans()
    }

    private fun loadWorkouts() {
        viewModelScope.launch {
            responseCache.stream<List<WorkoutTemplateDto>>("plan.workouts") {
                workoutsApi.listWorkoutTemplates().body().orEmpty()
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.workouts.isNotEmpty())
                    if (r.keep) st else st.copy(workouts = r.value ?: st.workouts)
                }
            }
        }
    }

    private fun loadDiets() {
        viewModelScope.launch {
            responseCache.stream<List<DietSummary>>("plan.diets") {
                val diets = dietsApi.listDiets(false).body().orEmpty()
                val foods = foodsApi.listFoods(false).body().orEmpty().associateBy { it.id }
                val meals = mealsApi.listMeals(false).body().orEmpty().associateBy { it.id }
                diets.mapNotNull { d -> d.id?.let { resolveDiet(it, d, meals, foods) } }
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.diets.isNotEmpty())
                    if (r.keep) st else st.copy(diets = r.value ?: st.diets)
                }
            }
        }
    }

    fun loadPlans() {
        val m = _state.value.month
        val today = _state.value.today
        val from = minOf(m.atDay(1), today)
        val to = maxOf(m.atEndOfMonth(), today.plusDays(6))
        val range = "$from|$to"
        val sameRange = range == shownRange
        viewModelScope.launch {
            responseCache.stream<List<DayPlanDto>>("plan.plans", range) {
                plansApi.listPlans(from, to).body().orEmpty()
            }.collect { res ->
                _state.update { st ->
                    // Only a *same-range* reload keeps the shown plans; a month switch paints the
                    // new range's cache right away.
                    val r = res.render(hasContent = sameRange && st.plansByDate.isNotEmpty())
                    st.copy(
                        plansByDate = if (r.keep) st.plansByDate else r.value?.associateBy { it.date } ?: st.plansByDate,
                        loading = r.loading,
                        error = r.error,
                    )
                }
            }
            shownRange = range
        }
        loadCompletions(from, to, range)
    }

    /** Days marked complete in the visible range — drives the green (done) / red (missed) dots. */
    private fun loadCompletions(from: LocalDate, to: LocalDate, range: String) {
        viewModelScope.launch {
            responseCache.stream<List<LocalDate>>("plan.completions", range) {
                loggingApi.getCompletedDays(from, to).body().orEmpty()
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.completedDays.isNotEmpty())
                    if (r.keep) st else st.copy(completedDays = r.value?.toSet() ?: st.completedDays)
                }
            }
        }
    }

    fun prevMonth() { _state.value = _state.value.copy(month = _state.value.month.minusMonths(1)); loadPlans() }
    fun nextMonth() { _state.value = _state.value.copy(month = _state.value.month.plusMonths(1)); loadPlans() }
    fun selectDay(date: LocalDate?) {
        _state.value = _state.value.copy(selectedDate = date, selectedDaySlots = emptyList(),
            pickerOpen = false, workoutPickerOpen = false, openWorkout = null)
        // Past-day view: fetch which meal slots were logged so the sheet can show done / not-done.
        if (date != null) viewModelScope.launch {
            val slots = runCatching { loggingApi.getLoggedSlots(date).body().orEmpty() }.getOrDefault(emptyList())
            _state.update { if (it.selectedDate == date) it.copy(selectedDaySlots = slots) else it }
        }
    }

    fun openPicker() { _state.value = _state.value.copy(pickerOpen = true, pickerSearch = "", pickerTag = null) }
    fun closePicker() { _state.value = _state.value.copy(pickerOpen = false) }

    // ── Planned workouts ──────────────────────────────────────────────────────────
    fun openWorkoutPicker() { _state.value = _state.value.copy(workoutPickerOpen = true) }
    fun closeWorkoutPicker() { _state.value = _state.value.copy(workoutPickerOpen = false) }

    /** Open the read-only detail of a planned workout by its template id (no-op if not a template). */
    fun openWorkoutDetail(templateId: Long?) {
        val template = _state.value.workouts.firstOrNull { it.id == templateId } ?: return
        _state.value = _state.value.copy(openWorkout = template)
    }
    fun closeWorkoutDetail() { _state.value = _state.value.copy(openWorkout = null) }

    fun addPlannedWorkout(date: LocalDate, template: WorkoutTemplateDto) {
        val id = template.id ?: return
        viewModelScope.launch {
            runCatching { plansApi.addPlannedWorkout(date, PlannedWorkoutDto(workoutTemplateId = id, activityName = template.name)) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            closeWorkoutPicker()
            loadPlans()
        }
    }

    fun removePlannedWorkout(date: LocalDate, workoutId: Long) {
        viewModelScope.launch {
            runCatching { plansApi.removePlannedWorkout(date, workoutId) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            loadPlans()
        }
    }
    fun setPickerSearch(q: String) { _state.value = _state.value.copy(pickerSearch = q) }
    fun setPickerTag(t: String?) { _state.value = _state.value.copy(pickerTag = t) }

    fun chooseDiet(date: LocalDate, dietId: Long) { setDiet(date, dietId); closePicker() }

    fun setDiet(date: LocalDate, dietId: Long?) {
        val existing = _state.value.plansByDate[date]
        viewModelScope.launch {
            runCatching { plansApi.upsertPlan(date, DayPlanDto(date = date, dietId = dietId, plannedWorkouts = existing?.plannedWorkouts ?: emptyList())) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            loadPlans()
        }
    }

    fun clearDay(date: LocalDate) {
        viewModelScope.launch {
            runCatching { plansApi.deletePlan(date) }.onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(selectedDate = null)
            loadPlans()
        }
    }

    // ── resolve a diet into slot-grouped meals/foods (mirrors the Home diet view) ──
    private fun resolveDiet(id: Long, d: DietDto, meals: Map<Long?, MealDto>, foods: Map<Long?, FoodDto>): DietSummary {
        val lines = LinkedHashMap<String, MutableList<DietLine>>()
        val slotKcal = LinkedHashMap<String, Double>()
        var total = 0.0
        fun bump(slot: String, k: Double) { slotKcal[slot] = (slotKcal[slot] ?: 0.0) + k; total += k }
        fun add(slot: String, line: DietLine) { lines.getOrPut(slot) { mutableListOf() }.add(line) }

        (d.meals ?: emptyList()).forEach { dm ->
            val meal = meals[dm.mealId]
            var mkcal = 0.0
            val items = (meal?.items ?: emptyList()).map { it ->
                mkcal += foodKcal(foods[it.foodId], it.quantity, it.unit.value)
                DietLine(foods[it.foodId]?.name ?: "Food", "${trimNum(it.quantity)} ${unitLabel(it.unit.value)}", header = false)
            }
            add(dm.slot, DietLine(meal?.name ?: "Meal", "${mkcal.roundToInt()} kcal", header = true))
            items.forEach { add(dm.slot, it) }
            bump(dm.slot, mkcal)
        }
        (d.foodItems ?: emptyList()).forEach { fi ->
            val k = foodKcal(foods[fi.foodId], fi.quantity, fi.unit.value)
            add(fi.slot, DietLine(foods[fi.foodId]?.name ?: "Food", "${trimNum(fi.quantity)} ${unitLabel(fi.unit.value)}", header = false))
            bump(fi.slot, k)
        }

        val order = SLOT_ORDER.filter { lines.containsKey(it) } + lines.keys.filter { it !in SLOT_ORDER }
        val slots = order.map { slot -> DietSlotView(slot, (slotKcal[slot] ?: 0.0).roundToInt(), lines[slot].orEmpty()) }
        return DietSummary(id, d.name, total.roundToInt(), slots, (d.tags ?: emptyList()).map { it.name })
    }

    private fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)

    private fun foodKcal(food: FoodDto?, quantity: Double, unit: String): Double {
        if (food == null) return 0.0
        val grams = when (unit) {
            "PIECE" -> quantity * (food.gramsPerPiece ?: 1.0)
            "CUP" -> quantity * (food.gramsPerCup ?: 1.0)
            "TBSP" -> quantity * (food.gramsPerTbsp ?: 1.0)
            "TSP" -> quantity * (food.gramsPerTsp ?: 1.0)
            else -> quantity
        }
        return food.caloriesPer100 * grams / 100.0
    }
}
