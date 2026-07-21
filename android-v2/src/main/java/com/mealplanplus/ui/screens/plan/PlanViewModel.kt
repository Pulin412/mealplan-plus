package com.mealplanplus.ui.screens.plan

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
import com.mealplanplus.data.repository.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlin.math.roundToInt

data class DietLine(val name: String, val meta: String, val header: Boolean)
data class DietSlotView(val slot: String, val kcal: Int, val lines: List<DietLine>)
data class DietSummary(val id: Long, val name: String, val kcal: Int, val slots: List<DietSlotView> = emptyList())

private val SLOT_ORDER = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")

data class PlanUiState(
    val loading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    val plansByDate: Map<LocalDate, DayPlanDto> = emptyMap(),
    val diets: List<DietSummary> = emptyList(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate? = null,
    val error: String? = null,
)

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val plansApi: PlansApi,
    private val dietsApi: DietsApi,
    private val mealsApi: MealsApi,
    private val foodsApi: FoodsApi,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanUiState())
    val state: StateFlow<PlanUiState> = _state

    init {
        loadDiets()
        loadPlans()
    }

    private fun loadDiets() {
        viewModelScope.launch {
            runCatching {
                val diets = dietsApi.listDiets(false).body().orEmpty()
                val foods = foodsApi.listFoods(false).body().orEmpty().associateBy { it.id }
                val meals = mealsApi.listMeals(false).body().orEmpty().associateBy { it.id }
                diets.mapNotNull { d -> d.id?.let { resolveDiet(it, d, meals, foods) } }
            }.onSuccess { _state.value = _state.value.copy(diets = it) }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.plansByDate.isEmpty(), error = null)
            val m = _state.value.month
            val today = _state.value.today
            val from = minOf(m.atDay(1), today)
            val to = maxOf(m.atEndOfMonth(), today.plusDays(6))
            runCatching { plansApi.listPlans(from, to).body().orEmpty() }
                .onSuccess { plans -> _state.value = _state.value.copy(loading = false, plansByDate = plans.associateBy { it.date }) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    fun prevMonth() { _state.value = _state.value.copy(month = _state.value.month.minusMonths(1)); loadPlans() }
    fun nextMonth() { _state.value = _state.value.copy(month = _state.value.month.plusMonths(1)); loadPlans() }
    fun selectDay(date: LocalDate?) { _state.value = _state.value.copy(selectedDate = date) }

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
        return DietSummary(id, d.name, total.roundToInt(), slots)
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
