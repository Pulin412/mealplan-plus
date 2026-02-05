package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyLogWithFoods
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.time.LocalDate
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
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadTodayData()
    }

    private fun loadTodayData() {
        // Load today's food log
        dailyLogRepository.getLogWithFoods(LocalDate.now())
            .onEach { logWithFoods ->
                val summary = logWithFoods?.let {
                    TodaySummary(
                        calories = it.totalCalories.toInt(),
                        protein = it.totalProtein.toInt(),
                        carbs = it.totalCarbs.toInt(),
                        fat = it.totalFat.toInt(),
                        foodCount = it.foods.size
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

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadTodayData()
    }
}
