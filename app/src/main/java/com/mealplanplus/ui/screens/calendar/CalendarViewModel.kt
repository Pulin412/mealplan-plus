package com.mealplanplus.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.PlanRepository
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
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

        viewModelScope.launch {
            planRepository.getPlansInRange(startDate, endDate).collect { plans ->
                _uiState.update { it.copy(plans = plans.associateBy { p -> p.date }) }
            }
        }
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            val diet = planRepository.getDietForDate(date.toString())
            _uiState.update {
                it.copy(selectedDate = date, selectedDiet = diet)
            }
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
            _uiState.update { it.copy(selectedDiet = diet, showDietPicker = false) }
            loadPlansForMonth()
        }
    }

    fun clearPlan() {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate.toString()
            planRepository.removePlan(date)
            _uiState.update { it.copy(selectedDiet = null) }
            loadPlansForMonth()
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
