package com.mealplanplus.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
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
    val selectedDietWithMeals: DietWithMeals? = null,
    val selectedDietTags: List<Tag> = emptyList(),
    val showDietPicker: Boolean = false,
    val isWeekView: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val diets: StateFlow<List<Diet>> = dietRepository.getDietsByUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadPlansForMonth()
        selectDate(LocalDate.now())
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

        viewModelScope.launch {
            planRepository.getPlansWithDietNames(startDate, endDate).collect { plansWithNames ->
                val validPlans = plansWithNames.filter { it.dietId != null }
                val plansMap = validPlans.associate { p ->
                    p.date to Plan(p.userId, p.date, p.dietId, p.notes, p.isCompleted)
                }
                val dietNames = validPlans.mapNotNull { p ->
                    p.dietName?.let { p.date to extractShortDietName(it) }
                }.toMap()
                _uiState.update { it.copy(plans = plansMap, dietNames = dietNames) }
                refreshSelectedDiet()
            }
        }
    }

    private suspend fun refreshSelectedDiet() {
        val dateStr = _uiState.value.selectedDate.toString()
        val diet = planRepository.getDietForDate(dateStr)
        _uiState.update { it.copy(selectedDiet = diet) }
        if (diet != null) loadDietDetails(diet.id)
    }

    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            val dateStr = date.toString()
            _uiState.update { it.copy(selectedDate = date) }
            val diet = planRepository.getDietForDate(dateStr)
            _uiState.update { it.copy(selectedDiet = diet, selectedDietWithMeals = null, selectedDietTags = emptyList()) }
            if (diet != null) loadDietDetails(diet.id)
        }
    }

    private fun loadDietDetails(dietId: Long) {
        viewModelScope.launch {
            val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            val tags = dietRepository.getTagsForDiet(dietId)
            _uiState.update { it.copy(selectedDietWithMeals = dietWithMeals, selectedDietTags = tags) }
        }
    }

    fun toggleView() {
        _uiState.update { it.copy(isWeekView = !it.isWeekView) }
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

            if (diet != null) {
                val newPlan = Plan(userId = diet.userId, date = date, dietId = diet.id, isCompleted = false)
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
                loadDietDetails(diet.id)
            } else {
                val updatedPlans = _uiState.value.plans - date
                val updatedDietNames = _uiState.value.dietNames - date
                _uiState.update {
                    it.copy(
                        plans = updatedPlans,
                        dietNames = updatedDietNames,
                        selectedDiet = null,
                        selectedDietWithMeals = null,
                        selectedDietTags = emptyList(),
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
            val updatedPlans = _uiState.value.plans - date
            val updatedDietNames = _uiState.value.dietNames - date
            _uiState.update {
                it.copy(
                    plans = updatedPlans,
                    dietNames = updatedDietNames,
                    selectedDiet = null,
                    selectedDietWithMeals = null,
                    selectedDietTags = emptyList()
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
