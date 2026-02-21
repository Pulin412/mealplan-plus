package com.mealplanplus.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.extractShortDietName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class TodaySummary(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val foodCount: Int = 0
)

data class MealSlotProgress(
    val slotName: String,
    val emoji: String,
    val slotType: String,
    val logged: Int,
    val total: Int
) {
    val progress: Float get() = if (total > 0) (logged.toFloat() / total).coerceIn(0f, 1f) else 0f
}

data class HomeUiState(
    val userName: String = "",
    val userInitial: String = "?",
    val todaySummary: TodaySummary = TodaySummary(),
    val calorieGoal: Int = 2000,
    val latestWeight: HealthMetric? = null,
    val latestSugar: HealthMetric? = null,
    val latestHba1c: HealthMetric? = null,
    val glucoseHistory: List<HealthMetric> = emptyList(),
    val dayStreak: Int = 0,
    val weeklyLoggedDates: Set<String> = emptySet(),
    val todayPlanSlots: List<MealSlotProgress> = emptyList(),
    val weeklyCalories: List<DailyMacroSummary> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val plansForMonth: Map<String, Boolean> = emptyMap(),
    val dietNamesForMonth: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val planRepository: PlanRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserName()
        loadTodayData()
        loadTodaySlots()
        loadPlansForMonth()
        loadWeeklyCalories()
        loadGlucoseHistory()
    }

    private fun loadUserName() {
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            authRepository.getCurrentUser(userId).collect { user ->
                val name = user?.displayName?.takeIf { it.isNotBlank() }
                    ?: user?.email?.substringBefore("@")
                    ?: ""
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                _uiState.update { it.copy(userName = name, userInitial = initial) }
            }
        }
    }

    private fun loadTodayData() {
        dailyLogRepository.getLogWithMeals(LocalDate.now())
            .onEach { logWithMeals ->
                val summary = logWithMeals?.let {
                    TodaySummary(
                        calories = it.totalCalories.toInt(),
                        protein = it.totalProtein.toInt(),
                        carbs = it.totalCarbs.toInt(),
                        fat = it.totalFat.toInt(),
                        foodCount = it.meals.size
                    )
                } ?: TodaySummary()
                _uiState.update { it.copy(todaySummary = summary) }
            }
            .launchIn(viewModelScope)

        val today = LocalDate.now().toString()
        healthRepository.getMetricsForDate(today)
            .onEach { metrics ->
                val weight = metrics.firstOrNull { it.metricType == MetricType.WEIGHT.name }
                val sugar = metrics.firstOrNull { it.metricType == MetricType.FASTING_SUGAR.name }
                val hba1c = metrics.firstOrNull { it.metricType == MetricType.HBA1C.name }
                _uiState.update {
                    it.copy(
                        latestWeight = weight,
                        latestSugar = sugar,
                        latestHba1c = hba1c,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadGlucoseHistory() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        healthRepository.getMetricsByTypeInRange(
            MetricType.FASTING_SUGAR,
            startDate.toString(),
            endDate.toString()
        ).onEach { history ->
            _uiState.update { it.copy(glucoseHistory = history) }
        }.launchIn(viewModelScope)
    }

    private fun loadTodaySlots() {
        dailyLogRepository.getLogWithMeals(LocalDate.now())
            .onEach { logWithMeals ->
                val loggedBySlot = logWithMeals?.meals
                    ?.groupBy { it.loggedMeal.slotType.uppercase() }
                    ?: emptyMap()

                val slots = listOf(
                    MealSlotProgress("Breakfast", "🍳", "BREAKFAST",
                        loggedBySlot["BREAKFAST"]?.size ?: 0, 3),
                    MealSlotProgress("Lunch", "☀️", "LUNCH",
                        loggedBySlot["LUNCH"]?.size ?: 0, 3),
                    MealSlotProgress("Dinner", "🌙", "DINNER",
                        loggedBySlot["DINNER"]?.size ?: 0, 3),
                    MealSlotProgress("Snacks", "🍎", "SNACK",
                        loggedBySlot["SNACK"]?.size ?: 0, 2)
                )
                _uiState.update { it.copy(todayPlanSlots = slots) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadWeeklyCalories() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)

        dailyLogRepository.getCompletedDaysCalories(startDate, endDate)
            .onEach { calories ->
                val loggedDates = calories.filter { it.calories > 0 }.map { it.date }.toSet()
                val streak = computeStreak(calories)
                _uiState.update {
                    it.copy(
                        weeklyCalories = calories,
                        weeklyLoggedDates = loggedDates,
                        dayStreak = streak
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun computeStreak(weekData: List<DailyMacroSummary>): Int {
        val today = LocalDate.now()
        var streak = 0
        for (i in 0..6) {
            val date = today.minusDays(i.toLong()).toString()
            val hasData = weekData.any { it.date == date && it.calories > 0 }
            if (hasData) streak++ else break
        }
        return streak
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

        viewModelScope.launch {
            planRepository.getPlansWithDietNames(startDate, endDate).collect { plansWithNames ->
                val validPlans = plansWithNames.filter { it.dietId != null }
                val plansMap = validPlans.associate { plan -> plan.date to plan.isCompleted }
                val dietNames = validPlans.mapNotNull { p ->
                    p.dietName?.let { p.date to extractShortDietName(it) }
                }.toMap()
                _uiState.update { it.copy(plansForMonth = plansMap, dietNamesForMonth = dietNames) }
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

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadTodayData()
        loadTodaySlots()
        loadWeeklyCalories()
        loadGlucoseHistory()
        loadPlansForMonth()
    }
}
