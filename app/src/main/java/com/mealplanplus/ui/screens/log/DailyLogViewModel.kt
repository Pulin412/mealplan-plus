package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val log: DailyLogWithFoods? = null,
    val plannedDiet: DietWithMeals? = null,
    val comparison: MacroComparison = MacroComparison(),
    val isLoading: Boolean = true,
    val showFoodPicker: Boolean = false,
    val selectedSlot: String? = null
)

@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val logRepository: DailyLogRepository,
    private val foodRepository: FoodRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    val availableFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            _date.collectLatest { date ->
                _uiState.value = _uiState.value.copy(date = date, isLoading = true)

                // Load planned diet for comparison
                val plannedDiet = planRepository.getDietForDate(date.toString())?.let {
                    dietRepository.getDietWithMeals(it.id)
                }

                logRepository.getLogWithFoods(date).collect { logWithFoods ->
                    val comparison = buildComparison(plannedDiet, logWithFoods)
                    _uiState.value = _uiState.value.copy(
                        log = logWithFoods,
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

    fun showFoodPicker(slotType: String) {
        _uiState.value = _uiState.value.copy(showFoodPicker = true, selectedSlot = slotType)
    }

    fun hideFoodPicker() {
        _uiState.value = _uiState.value.copy(showFoodPicker = false, selectedSlot = null)
    }

    fun logFood(food: FoodItem, quantity: Double = 1.0) {
        val slot = _uiState.value.selectedSlot ?: return
        viewModelScope.launch {
            logRepository.logFood(
                date = _date.value,
                foodId = food.id,
                quantity = quantity,
                slotType = slot,
                timestamp = System.currentTimeMillis()
            )
            hideFoodPicker()
        }
    }

    fun deleteLoggedFood(id: Long) {
        viewModelScope.launch {
            logRepository.deleteLoggedFood(id)
        }
    }

    fun updateQuantity(loggedFood: LoggedFood, newQuantity: Double) {
        if (newQuantity <= 0) {
            deleteLoggedFood(loggedFood.id)
        } else {
            viewModelScope.launch {
                logRepository.updateLoggedFood(loggedFood.copy(quantity = newQuantity))
            }
        }
    }
}
