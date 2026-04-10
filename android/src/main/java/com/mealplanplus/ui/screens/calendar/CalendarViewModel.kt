package com.mealplanplus.ui.screens.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.GroceryRepository
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
    val isWeekView: Boolean = true,
    val isLoading: Boolean = false,
    /** Slot types logged today — used for checkbox state when today is selected. */
    val todayLoggedSlots: Map<String, Boolean> = emptyMap(),
    /** Non-null while generating grocery list; navigates to GroceryDetail then clears. */
    val generatedGroceryListId: Long? = null,
    val isGeneratingGroceries: Boolean = false
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val groceryRepository: GroceryRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val diets: StateFlow<List<Diet>> = dietRepository.getDietsByUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // If the screen was opened from a widget tap with a specific date, honour it.
        val startDate = savedStateHandle.get<String>("initialDate")
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

        // Pre-set month so loadPlansForMonth() fetches the correct date range.
        if (startDate != LocalDate.now()) {
            _uiState.update {
                it.copy(
                    currentMonth = YearMonth.from(startDate),
                    selectedDate = startDate
                )
            }
        }
        loadPlansForMonth()
        selectDate(startDate)
        observeTodayLog()
    }

    /** Keep today's logged-slot map in sync so checkboxes stay reactive. */
    private fun observeTodayLog() {
        dailyLogRepository.getLogWithFoods(LocalDate.now())
            .onEach { logWithFoods ->
                val logged = logWithFoods?.foods
                    ?.groupBy { it.loggedFood.slotType.uppercase() }
                    ?.mapValues { true }
                    ?: emptyMap()
                _uiState.update { it.copy(todayLoggedSlots = logged) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Toggle a meal slot logged/unlogged for today.
     * Only callable when the selected date is today.
     */
    fun toggleSlotLogged(slotType: String) {
        val dietWithMeals = _uiState.value.selectedDietWithMeals ?: return
        viewModelScope.launch {
            val today = LocalDate.now()
            val isCurrentlyLogged = dailyLogRepository.isSlotLogged(today, slotType)
            if (isCurrentlyLogged) {
                dailyLogRepository.clearSlot(today, slotType)
            } else {
                val plannedFoods = dietWithMeals.meals[slotType]?.items ?: return@launch
                if (plannedFoods.isEmpty()) return@launch
                dailyLogRepository.clearSlot(today, slotType)
                val timestamp = System.currentTimeMillis()
                plannedFoods.forEach { foodItem ->
                    dailyLogRepository.logFood(
                        date = today,
                        foodId = foodItem.mealFoodItem.foodId,
                        quantity = foodItem.mealFoodItem.quantity,
                        slotType = slotType,
                        timestamp = timestamp
                    )
                }
            }
        }
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        // Extend ±6 days so weeks that straddle a month boundary are fully covered
        val startDate = month.atDay(1).minusDays(6).toString()
        val endDate = month.atEndOfMonth().plusDays(6).toString()

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
        // If week navigation crosses into a new month, reload plans for that month
        val newMonth = YearMonth.from(date)
        if (newMonth != _uiState.value.currentMonth) {
            _uiState.update { it.copy(currentMonth = newMonth) }
            loadPlansForMonth()
        }
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

    /** Assign a diet by id — called when returning from DietPickerScreen via savedStateHandle. */
    fun assignDietById(dietId: Long) {
        viewModelScope.launch {
            val diet = dietRepository.getDietById(dietId) ?: return@launch
            assignDiet(diet)
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

    fun toggleFavourite(diet: Diet) {
        viewModelScope.launch {
            dietRepository.toggleFavourite(diet)
            // Refresh selectedDiet so the star icon updates immediately
            val refreshed = dietRepository.getDietById(diet.id)
            _uiState.update { it.copy(selectedDiet = refreshed ?: it.selectedDiet) }
        }
    }

    fun generateGroceriesForDiet() {
        val diet = _uiState.value.selectedDiet ?: return
        _uiState.update { it.copy(isGeneratingGroceries = true) }
        viewModelScope.launch {
            try {
                val listId = groceryRepository.generateFromDiet(diet.name, diet.id)
                _uiState.update { it.copy(generatedGroceryListId = listId, isGeneratingGroceries = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingGroceries = false) }
            }
        }
    }

    fun clearGeneratedGroceryListId() {
        _uiState.update { it.copy(generatedGroceryListId = null) }
    }
}
