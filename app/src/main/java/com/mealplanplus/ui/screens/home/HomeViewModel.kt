package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyLogWithMeals
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.extractShortDietName
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class HomeUiState(
    val todaySummary: TodaySummary = TodaySummary(),
    val latestWeight: HealthMetric? = null,
    val latestSugar: HealthMetric? = null,
    val weeklyCalories: List<DailyMacroSummary> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val plansForMonth: Map<String, Boolean> = emptyMap(),  // date string → isCompleted
    val dietNamesForMonth: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayData()
        loadPlansForMonth()
        loadWeeklyCalories()
    }

    private fun loadTodayData() {
        // Load today's meal log
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

        // Load today's health metrics only (not historical values)
        val today = LocalDate.now().toString()
        healthRepository.getMetricsForDate(today)
            .onEach { metrics ->
                val todayWeight = metrics.firstOrNull { it.metricType == MetricType.WEIGHT.name }
                val todaySugar = metrics.firstOrNull { it.metricType == MetricType.FASTING_SUGAR.name }
                _uiState.update {
                    it.copy(
                        latestWeight = todayWeight,
                        latestSugar = todaySugar,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadWeeklyCalories() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6) // Last 7 days

        dailyLogRepository.getCompletedDaysCalories(startDate, endDate)
            .onEach { calories ->
                _uiState.update { it.copy(weeklyCalories = calories) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

        viewModelScope.launch {
            // Single JOIN query - no N+1 problem
            planRepository.getPlansWithDietNames(startDate, endDate).collect { plansWithNames ->
                // Only include plans with valid dietId for color coding
                val validPlans = plansWithNames.filter { it.dietId != null }
                val plansMap = validPlans.associate { plan ->
                    plan.date to plan.isCompleted
                }
                // Extract diet names directly from query result
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
        loadPlansForMonth()
    }
}
