package com.mealplanplus.ui.screens.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class DateRange(val label: String, val days: Int) {
    WEEK("7D", 7),
    MONTH("30D", 30),
    QUARTER("90D", 90),
    ALL("All", -1)
}

data class ChartsUiState(
    val selectedMetricType: MetricType = MetricType.WEIGHT,
    val selectedRange: DateRange = DateRange.MONTH,
    val metrics: List<HealthMetric> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        val type = _uiState.value.selectedMetricType
        val range = _uiState.value.selectedRange

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val flow = if (range.days == -1) {
                healthRepository.getMetricsByType(type)
            } else {
                val endDate = LocalDate.now().toString()
                val startDate = LocalDate.now().minusDays(range.days.toLong()).toString()
                healthRepository.getMetricsByTypeInRange(type, startDate, endDate)
            }

            flow.collect { metrics ->
                _uiState.update {
                    it.copy(
                        metrics = metrics.sortedBy { m -> m.date },
                        isLoading = false
                    )
                }
            }
        }
    }

    fun selectMetricType(type: MetricType) {
        _uiState.update { it.copy(selectedMetricType = type) }
        loadMetrics()
    }

    fun selectRange(range: DateRange) {
        _uiState.update { it.copy(selectedRange = range) }
        loadMetrics()
    }
}
