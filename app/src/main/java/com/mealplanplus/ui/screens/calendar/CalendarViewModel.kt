package com.mealplanplus.ui.screens.calendar

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

        viewModelScope.launch {
            // Single JOIN query - no N+1 problem
            planRepository.getPlansWithDietNames(startDate, endDate).collect { plansWithNames ->
                // Only include plans with valid dietId
                val validPlans = plansWithNames.filter { it.dietId != null }
                // Convert to Plan objects for UI state
                val plansMap = validPlans.associate { p ->
                    p.date to Plan(p.date, p.dietId, p.notes, p.isCompleted)
                }
                // Extract diet names directly from query result
                val dietNames = validPlans.mapNotNull { p ->
                    p.dietName?.let { p.date to extractShortDietName(it) }
                }.toMap()
                _uiState.update { it.copy(plans = plansMap, dietNames = dietNames) }
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
        viewModelScope.launch {
            val date = _uiState.value.selectedDate.toString()
            planRepository.setPlanForDate(date, diet?.id)

            // Optimistic update - add to local state immediately
            if (diet != null) {
                val newPlan = Plan(date = date, dietId = diet.id, isCompleted = false)
                val updatedPlans = _uiState.value.plans + (date to newPlan)
                val updatedDietNames = _uiState.value.dietNames + (date to extractShortDietName(diet.name))
                _uiState.update {
                    it.copy(
                        plans = updatedPlans,
                        dietNames = updatedDietNames,
                        selectedDiet = diet,
                        showDietPicker = false
                    )
                }
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
