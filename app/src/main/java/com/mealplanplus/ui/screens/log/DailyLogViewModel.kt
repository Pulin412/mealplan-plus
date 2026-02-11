package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
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
    val logWithMeals: DailyLogWithMeals? = null,
    val plannedDiet: DietWithMeals? = null,
    val planForDate: Plan? = null,  // To check isCompleted status
    val comparison: MacroComparison = MacroComparison(),
    val isLoading: Boolean = true,
    val showMealPicker: Boolean = false,
    val showDietPicker: Boolean = false,
    val selectedSlot: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val logRepository: DailyLogRepository,
    private val mealRepository: MealRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    val availableMeals: StateFlow<List<Meal>> = mealRepository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val availableDiets: StateFlow<List<Diet>> = dietRepository.getAllDiets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Use flatMapLatest to properly handle date changes and Flow updates
        viewModelScope.launch {
            _date.flatMapLatest { date ->
                _uiState.value = _uiState.value.copy(date = date, isLoading = true)
                logRepository.getLogWithMeals(date)
            }.collect { logWithMeals ->
                val date = _date.value
                // Load plan and planned diet for comparison
                val plan = planRepository.getPlanForDate(date.toString())
                val plannedDiet = plan?.dietId?.let {
                    dietRepository.getDietWithMeals(it)
                }
                val comparison = buildComparison(plannedDiet, logWithMeals)
                _uiState.value = _uiState.value.copy(
                    logWithMeals = logWithMeals,
                    planForDate = plan,
                    plannedDiet = plannedDiet,
                    comparison = comparison,
                    isLoading = false
                )
            }
        }
    }

    private fun buildComparison(planned: DietWithMeals?, actual: DailyLogWithMeals?): MacroComparison {
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

    fun setDate(date: LocalDate) {
        _date.value = date
    }

    fun setDateFromString(dateStr: String?) {
        dateStr?.let {
            try {
                _date.value = logRepository.parseDate(it)
            } catch (e: Exception) {
                // Invalid date, keep current
            }
        }
    }

    fun goToPreviousDay() {
        _date.value = _date.value.minusDays(1)
    }

    fun goToNextDay() {
        _date.value = _date.value.plusDays(1)
    }

    fun goToToday() {
        _date.value = LocalDate.now()
    }

    fun showMealPicker(slotType: String) {
        _uiState.value = _uiState.value.copy(showMealPicker = true, selectedSlot = slotType)
    }

    fun hideMealPicker() {
        _uiState.value = _uiState.value.copy(showMealPicker = false, selectedSlot = null)
    }

    fun logMeal(meal: Meal, quantity: Double = 1.0) {
        val slot = _uiState.value.selectedSlot ?: return
        viewModelScope.launch {
            logRepository.logMeal(
                date = _date.value,
                mealId = meal.id,
                slotType = slot,
                quantity = quantity,
                timestamp = System.currentTimeMillis()
            )
            hideMealPicker()
        }
    }

    // Overload for navigation-based meal picker
    fun logMeal(mealId: Long, slotType: String, quantity: Double = 1.0) {
        viewModelScope.launch {
            logRepository.logMeal(
                date = _date.value,
                mealId = mealId,
                slotType = slotType,
                quantity = quantity,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun deleteLoggedMeal(id: Long) {
        viewModelScope.launch {
            logRepository.deleteLoggedMeal(id)
        }
    }

    fun updateMealQuantity(loggedMeal: LoggedMeal, newQuantity: Double) {
        if (newQuantity <= 0) {
            deleteLoggedMeal(loggedMeal.id)
        } else {
            viewModelScope.launch {
                logRepository.updateLoggedMeal(loggedMeal.copy(quantity = newQuantity))
            }
        }
    }

    fun showDietPicker() {
        _uiState.value = _uiState.value.copy(showDietPicker = true)
    }

    fun hideDietPicker() {
        _uiState.value = _uiState.value.copy(showDietPicker = false)
    }

    fun applyDiet(diet: Diet) {
        viewModelScope.launch {
            val dietWithMeals = dietRepository.getDietWithMeals(diet.id)
            if (dietWithMeals != null) {
                logRepository.applyDiet(_date.value, dietWithMeals)
            }
            hideDietPicker()
        }
    }

    fun applyDietById(dietId: Long, dateStr: String? = null) {
        viewModelScope.launch {
            val date = dateStr?.let {
                try { LocalDate.parse(it) } catch (e: Exception) { null }
            } ?: _date.value
            val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            if (dietWithMeals != null) {
                // Create/update plan for the date (shows color on calendar)
                planRepository.setPlanForDate(date.toString(), dietId)
                // Log the meals
                logRepository.applyDiet(date, dietWithMeals)
                // Reload plan for current date
                loadPlanForDate(date)
            }
        }
    }

    private suspend fun loadPlanForDate(date: LocalDate) {
        val plan = planRepository.getPlanForDate(date.toString())
        _uiState.update { it.copy(planForDate = plan) }
    }

    /**
     * Mark the planned diet for current date as completed
     */
    fun finishPlan() {
        viewModelScope.launch {
            planRepository.completePlan(_date.value.toString())
            // Reload to update UI - trigger by updating date flow
            val currentDate = _date.value
            _date.value = currentDate
        }
    }
}
