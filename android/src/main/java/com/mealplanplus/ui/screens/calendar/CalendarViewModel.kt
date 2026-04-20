package com.mealplanplus.ui.screens.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import java.time.format.DateTimeFormatter
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.data.repository.WorkoutRepository
import com.mealplanplus.util.extractShortDietName
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val plans: Map<Long, Plan> = emptyMap(),          // key = epoch ms
    val dietNames: Map<Long, String> = emptyMap(),    // key = epoch ms
    val selectedDiet: Diet? = null,
    val selectedDietWithMeals: DietWithMeals? = null,
    val selectedDietTags: List<Tag> = emptyList(),
    val showDietPicker: Boolean = false,
    val isWeekView: Boolean = true,
    val isLoading: Boolean = false,
    val todayLoggedSlots: Map<String, Boolean> = emptyMap(),
    val grocerySnapshot: List<GrocerySnapshotItem>? = null,
    val isGeneratingGroceries: Boolean = false,
    val plannedWorkouts: List<PlannedWorkoutWithTemplate> = emptyList(),
    val showWorkoutPicker: Boolean = false
)

/** A single aggregated ingredient line for the grocery snapshot sheet. */
data class GrocerySnapshotItem(
    val foodName: String,
    val quantity: Double,
    val unitLabel: String
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    val diets: StateFlow<List<Diet>> = authRepository.getCurrentUserId()
        .filterNotNull()
        .flatMapLatest { uid -> dietRepository.getDietsForUser(uid) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val firebaseUid get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val workoutTemplates: StateFlow<List<WorkoutTemplateWithExercises>> =
        workoutRepository.getTemplatesForUser(firebaseUid)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allExercises: StateFlow<List<Exercise>> =
        workoutRepository.getAllExercisesForUser(firebaseUid)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        val startDate = savedStateHandle.get<String>("initialDate")
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now()

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
        val startDate = month.atDay(1).minusDays(6)
        val endDate = month.atEndOfMonth().plusDays(6)

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
        val date = _uiState.value.selectedDate
        val diet = planRepository.getDietForDate(date)
        _uiState.update { it.copy(selectedDiet = diet) }
        if (diet != null) loadDietDetails(diet.id)
    }

    fun selectDate(date: LocalDate) {
        val newMonth = YearMonth.from(date)
        if (newMonth != _uiState.value.currentMonth) {
            _uiState.update { it.copy(currentMonth = newMonth) }
            loadPlansForMonth()
        }
        viewModelScope.launch {
            _uiState.update { it.copy(selectedDate = date) }
            val diet = planRepository.getDietForDate(date)
            _uiState.update { it.copy(selectedDiet = diet, selectedDietWithMeals = null, selectedDietTags = emptyList()) }
            if (diet != null) loadDietDetails(diet.id)
        }
        loadPlannedWorkoutsForDate(date)
    }

    private fun loadPlannedWorkoutsForDate(date: LocalDate) {
        val uid = firebaseUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            workoutRepository.getPlannedForDate(uid, date.toEpochMs()).collect { list ->
                _uiState.update { it.copy(plannedWorkouts = list) }
            }
        }
    }

    fun showWorkoutPicker() { _uiState.update { it.copy(showWorkoutPicker = true) } }
    fun hideWorkoutPicker() { _uiState.update { it.copy(showWorkoutPicker = false) } }

    fun planWorkout(templateId: Long) {
        val uid = firebaseUid
        if (uid.isBlank()) return
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            workoutRepository.planWorkout(
                PlannedWorkout(userId = uid, date = date.toEpochMs(), templateId = templateId)
            )
            hideWorkoutPicker()
        }
    }

    fun unplanWorkout(templateId: Long) {
        val uid = firebaseUid
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            workoutRepository.unplanWorkout(uid, date.toEpochMs(), templateId)
        }
    }

    /** Creates an ad-hoc template from chosen exercises and plans it for the selected date. */
    fun planQuickWorkout(exercises: List<Exercise>) {
        if (exercises.isEmpty()) return
        val uid = firebaseUid
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val name = "Quick Workout · ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
            val template = WorkoutTemplate(userId = uid, name = name, category = WorkoutTemplateCategory.MIXED)
            val templateExercises = exercises.mapIndexed { idx, ex ->
                WorkoutTemplateExercise(templateId = 0L, exerciseId = ex.id, orderIndex = idx)
            }
            val templateId = workoutRepository.saveTemplate(template, templateExercises)
            workoutRepository.planWorkout(PlannedWorkout(userId = uid, date = date.toEpochMs(), templateId = templateId))
            hideWorkoutPicker()
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
            it.copy(currentMonth = YearMonth.from(today), selectedDate = today)
        }
        loadPlansForMonth()
        selectDate(today)
    }

    fun showDietPicker() { _uiState.update { it.copy(showDietPicker = true) } }
    fun hideDietPicker() { _uiState.update { it.copy(showDietPicker = false) } }

    fun assignDiet(diet: Diet?) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val dateMs = date.toEpochMs()
            planRepository.setPlanForDate(date, diet?.id)

            if (diet != null) {
                val existingPlan = _uiState.value.plans[dateMs]
                val newPlan = Plan(userId = existingPlan?.userId ?: 0L, date = dateMs, dietId = diet.id, isCompleted = false)
                val updatedPlans = _uiState.value.plans + (dateMs to newPlan)
                val updatedDietNames = _uiState.value.dietNames + (dateMs to extractShortDietName(diet.name))
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
                val updatedPlans = _uiState.value.plans - dateMs
                val updatedDietNames = _uiState.value.dietNames - dateMs
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
            val date = _uiState.value.selectedDate
            val dateMs = date.toEpochMs()
            planRepository.removePlan(date)
            val updatedPlans = _uiState.value.plans - dateMs
            val updatedDietNames = _uiState.value.dietNames - dateMs
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

    fun assignDietById(dietId: Long) {
        viewModelScope.launch {
            val diet = dietRepository.getDietById(dietId) ?: return@launch
            assignDiet(diet)
        }
    }

    fun copyToDate(targetDate: LocalDate) {
        viewModelScope.launch {
            planRepository.copyPlanToDate(_uiState.value.selectedDate, targetDate)
            loadPlansForMonth()
        }
    }

    fun toggleFavourite(diet: Diet) {
        viewModelScope.launch {
            dietRepository.toggleFavourite(diet)
            val refreshed = dietRepository.getDietById(diet.id)
            _uiState.update { it.copy(selectedDiet = refreshed ?: it.selectedDiet) }
        }
    }

    fun generateGroceriesForDiet() {
        val dietWithMeals = _uiState.value.selectedDietWithMeals ?: return
        _uiState.update { it.copy(isGeneratingGroceries = true) }
        viewModelScope.launch {
            val aggregated = mutableMapOf<Pair<Long, String>, GrocerySnapshotItem>()
            for ((_, mealWithFoods) in dietWithMeals.meals) {
                if (mealWithFoods == null) continue
                for (item in mealWithFoods.items) {
                    val key = item.food.id to item.mealFoodItem.unit.name
                    val existing = aggregated[key]
                    if (existing != null) {
                        aggregated[key] = existing.copy(quantity = existing.quantity + item.mealFoodItem.quantity)
                    } else {
                        aggregated[key] = GrocerySnapshotItem(
                            foodName = item.food.name,
                            quantity = item.mealFoodItem.quantity,
                            unitLabel = item.mealFoodItem.unit.shortLabel
                        )
                    }
                }
            }
            val snapshot = aggregated.values.sortedBy { it.foodName.lowercase() }
            _uiState.update { it.copy(grocerySnapshot = snapshot, isGeneratingGroceries = false) }
        }
    }

    fun clearGrocerySnapshot() {
        _uiState.update { it.copy(grocerySnapshot = null) }
    }
}
