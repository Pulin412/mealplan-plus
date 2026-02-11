package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyLogWithMeals
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
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
    val currentMonth: YearMonth = YearMonth.now(),
    val plansForMonth: Set<LocalDate> = emptySet(),
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

        // Load latest health metrics
        healthRepository.getMetricsByType(MetricType.WEIGHT)
            .onEach { metrics ->
                _uiState.update { it.copy(latestWeight = metrics.firstOrNull(), isLoading = false) }
            }
            .launchIn(viewModelScope)

        healthRepository.getMetricsByType(MetricType.FASTING_SUGAR)
            .onEach { metrics ->
                _uiState.update { it.copy(latestSugar = metrics.firstOrNull()) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

        viewModelScope.launch {
            planRepository.getPlansInRange(startDate, endDate).collect { plans ->
                val datesWithPlans = plans.mapNotNull { plan ->
                    try { LocalDate.parse(plan.date) } catch (e: Exception) { null }
                }.toSet()
                _uiState.update { it.copy(plansForMonth = datesWithPlans) }
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
