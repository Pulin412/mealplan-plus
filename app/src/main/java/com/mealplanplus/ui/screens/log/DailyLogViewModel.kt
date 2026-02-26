package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class MacroComparison(
    val plannedCalories: Int = 0,
    val plannedProtein: Int = 0,
    val plannedCarbs: Int = 0,
    val plannedFat: Int = 0,
    val actualCalories: Int = 0,
    val actualProtein: Int = 0,
    val actualCarbs: Int = 0,
    val actualFat: Int = 0
) {
    val hasPlan: Boolean get() = plannedCalories > 0
    val calorieDiff: Int get() = actualCalories - plannedCalories
    val proteinDiff: Int get() = actualProtein - plannedProtein
    val carbsDiff: Int get() = actualCarbs - plannedCarbs
    val fatDiff: Int get() = actualFat - plannedFat
}

data class DailyLogUiState(
    val date: LocalDate = LocalDate.now(),
    val logWithFoods: DailyLogWithFoods? = null,
    val plannedDiet: DietWithMeals? = null,
    val planForDate: Plan? = null,
    val comparison: MacroComparison = MacroComparison(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0=Daily Log, 1=Plan vs Actual
    val showFoodPicker: Boolean = false,
    val selectedSlot: DefaultMealSlot? = null,
    val finishCompleted: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val logRepository: DailyLogRepository,
    private val mealRepository: MealRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            _date.flatMapLatest { date ->
                _uiState.update { it.copy(date = date, isLoading = true) }
                logRepository.getLogWithFoods(date)
            }.collect { logWithFoods ->
                val date = _date.value
                val plan = planRepository.getPlanForDate(date.toString())
                val plannedDiet = plan?.dietId?.let { dietRepository.getDietWithMeals(it) }
                val comparison = buildComparison(plannedDiet, logWithFoods)
                _uiState.update {
                    it.copy(
                        logWithFoods = logWithFoods,
                        planForDate = plan,
                        plannedDiet = plannedDiet,
                        comparison = comparison,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun buildComparison(planned: DietWithMeals?, actual: DailyLogWithFoods?): MacroComparison {
        return MacroComparison(
            plannedCalories = planned?.totalCalories?.toInt() ?: 0,
            plannedProtein = planned?.totalProtein?.toInt() ?: 0,
            plannedCarbs = planned?.totalCarbs?.toInt() ?: 0,
            plannedFat = planned?.totalFat?.toInt() ?: 0,
            actualCalories = actual?.totalCalories?.toInt() ?: 0,
            actualProtein = actual?.totalProtein?.toInt() ?: 0,
            actualCarbs = actual?.totalCarbs?.toInt() ?: 0,
            actualFat = actual?.totalFat?.toInt() ?: 0
        )
    }

    fun setDateFromString(dateStr: String?) {
        dateStr?.let {
            try { _date.value = logRepository.parseDate(it) } catch (_: Exception) {}
        }
    }

    fun goToPreviousDay() { _date.value = _date.value.minusDays(1) }
    fun goToNextDay() { _date.value = _date.value.plusDays(1) }
    fun goToToday() { _date.value = LocalDate.now() }

    fun setSelectedTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }

    fun showFoodPickerFor(slot: DefaultMealSlot) {
        _uiState.update { it.copy(showFoodPicker = true, selectedSlot = slot) }
    }

    fun hideFoodPicker() {
        _uiState.update { it.copy(showFoodPicker = false, selectedSlot = null) }
    }

    fun logFood(foodId: Long, quantity: Double = 100.0) {
        val slot = _uiState.value.selectedSlot ?: return
        viewModelScope.launch {
            logRepository.logFood(
                date = _date.value,
                foodId = foodId,
                quantity = quantity,
                slotType = slot.name,
                timestamp = System.currentTimeMillis()
            )
            hideFoodPicker()
        }
    }

    fun deleteLoggedFood(id: Long) {
        viewModelScope.launch { logRepository.deleteLoggedFood(id) }
    }

    fun applyDietById(dietId: Long, dateStr: String? = null) {
        viewModelScope.launch {
            val date = dateStr?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            } ?: _date.value
            val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            if (dietWithMeals != null) {
                planRepository.setPlanForDate(date.toString(), dietId)
                logRepository.applyDiet(date, dietWithMeals)
                loadPlanForDate(date)
            }
        }
    }

    private suspend fun loadPlanForDate(date: LocalDate) {
        val plan = planRepository.getPlanForDate(date.toString())
        _uiState.update { it.copy(planForDate = plan) }
    }

    fun finishPlan() {
        viewModelScope.launch {
            planRepository.completePlan(_date.value.toString())
            loadPlanForDate(_date.value)
            _uiState.update { it.copy(finishCompleted = true) }
        }
    }

    fun clearFinishCompleted() { _uiState.update { it.copy(finishCompleted = false) } }

    fun clearPlan() {
        viewModelScope.launch {
            logRepository.clearLoggedMeals(_date.value)
            planRepository.removePlan(_date.value.toString())
            loadPlanForDate(_date.value)
        }
    }

    fun reopenPlan() {
        viewModelScope.launch {
            planRepository.uncompletePlan(_date.value.toString())
            loadPlanForDate(_date.value)
        }
    }
}
