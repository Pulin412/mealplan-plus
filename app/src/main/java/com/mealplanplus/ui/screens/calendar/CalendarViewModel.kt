package com.mealplanplus.ui.screens.calendar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.extractShortDietName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val TAG = "CalendarVM"

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val plans: Map<String, Plan> = emptyMap(),
    val dietNames: Map<String, String> = emptyMap(),
    val selectedDiet: Diet? = null,
    val showDietPicker: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val diets: StateFlow<List<Diet>> = dietRepository.getAllDiets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadPlansForMonth()
        // Load selected diet for today on init
        selectDate(LocalDate.now())
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()
        Log.d(TAG, "loadPlansForMonth: $startDate to $endDate")

        viewModelScope.launch {
            planRepository.getPlansInRange(startDate, endDate).collect { plans ->
                Log.d(TAG, "Got ${plans.size} plans from DB")
                plans.forEach { p -> Log.d(TAG, "  Plan: ${p.date}, dietId=${p.dietId}, completed=${p.isCompleted}") }
                // Only include plans with valid dietId
                val validPlans = plans.filter { it.dietId != null }
                Log.d(TAG, "Valid plans (with dietId): ${validPlans.size}")
                val plansMap = validPlans.associateBy { p -> p.date }
                // Load diet names for each valid plan
                val dietNames = mutableMapOf<String, String>()
                validPlans.forEach { plan ->
                    plan.dietId?.let { dietId ->
                        val diet = dietRepository.getDietById(dietId)
                        Log.d(TAG, "  Diet for ${plan.date}: ${diet?.name ?: "NOT FOUND"}")
                        diet?.let { dietNames[plan.date] = extractShortDietName(it.name) }
                    }
                }
                _uiState.update { it.copy(plans = plansMap, dietNames = dietNames) }
                Log.d(TAG, "State updated with ${plansMap.size} plans")
                // Refresh selected diet after plans are loaded
                refreshSelectedDiet()
            }
        }
    }

    private suspend fun refreshSelectedDiet() {
        val dateStr = _uiState.value.selectedDate.toString()
        // Always query DB for the diet - this is the source of truth
        val diet = planRepository.getDietForDate(dateStr)
        _uiState.update { it.copy(selectedDiet = diet) }
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.toString()
            // First update the selected date
            _uiState.update { it.copy(selectedDate = date) }
            // Always query DB for the diet - this is the source of truth
            val diet = planRepository.getDietForDate(dateStr)
            _uiState.update { it.copy(selectedDiet = diet) }
        }
    }

    fun goToPreviousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        loadPlansForMonth()
    }

    fun goToNextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        loadPlansForMonth()
    }

    fun goToToday() {
        val today = LocalDate.now()
        _uiState.update {
            it.copy(
                currentMonth = YearMonth.from(today),
                selectedDate = today
            )
        }
        loadPlansForMonth()
        selectDate(today)
    }

    fun showDietPicker() {
        _uiState.update { it.copy(showDietPicker = true) }
    }

    fun hideDietPicker() {
        _uiState.update { it.copy(showDietPicker = false) }
    }

    fun assignDiet(diet: Diet?) {
        Log.d(TAG, "assignDiet called with diet: ${diet?.name}, id: ${diet?.id}")
        viewModelScope.launch {
            val date = _uiState.value.selectedDate.toString()
            Log.d(TAG, "Saving plan for date: $date with dietId: ${diet?.id}")
            planRepository.setPlanForDate(date, diet?.id)

            // Optimistic update - add to local state immediately
            if (diet != null) {
                val newPlan = Plan(date = date, dietId = diet.id, isCompleted = false)
                val updatedPlans = _uiState.value.plans + (date to newPlan)
                val updatedDietNames = _uiState.value.dietNames + (date to extractShortDietName(diet.name))
                Log.d(TAG, "Updated plans map: ${updatedPlans.keys}")
                Log.d(TAG, "Updated dietNames map: $updatedDietNames")
                _uiState.update {
                    it.copy(
                        plans = updatedPlans,
                        dietNames = updatedDietNames,
                        selectedDiet = diet,
                        showDietPicker = false
                    )
                }
                Log.d(TAG, "State updated. selectedDiet: ${_uiState.value.selectedDiet?.name}")
            } else {
                // Clearing the plan
                val updatedPlans = _uiState.value.plans - date
                val updatedDietNames = _uiState.value.dietNames - date
                _uiState.update {
                    it.copy(
                        plans = updatedPlans,
                        dietNames = updatedDietNames,
                        selectedDiet = null,
                        showDietPicker = false
                    )
                }
            }
        }
    }

    fun clearPlan() {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate.toString()
            planRepository.removePlan(date)

            // Optimistic update - remove from local state immediately
            val updatedPlans = _uiState.value.plans - date
            val updatedDietNames = _uiState.value.dietNames - date
            _uiState.update {
                it.copy(
                    plans = updatedPlans,
                    dietNames = updatedDietNames,
                    selectedDiet = null
                )
            }
        }
    }

    fun copyToDate(targetDate: LocalDate) {
        viewModelScope.launch {
            planRepository.copyPlanToDate(
                _uiState.value.selectedDate.toString(),
                targetDate.toString()
            )
            loadPlansForMonth()
        }
    }
}
