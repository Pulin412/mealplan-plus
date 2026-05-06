package com.mealplanplus.ui.screens.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import com.mealplanplus.util.toEpochMs
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
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
    // ── Streak tab ────────────────────────────────────────────────────────────
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val loggedDatesThisMonth: Set<Long> = emptySet(),
    val totalDaysLogged: Int = 0,
    val avgCaloriesPerDay: Int = 0,
    val avgProteinPerDay: Int = 0,

    // ── Health tab ────────────────────────────────────────────────────────────
    val selectedMetricType: MetricType = MetricType.BLOOD_GLUCOSE,
    val healthRange: DateRange = DateRange.MONTH,
    val healthMetrics: List<HealthMetric> = emptyList(),

    // Normal ranges (user-configurable, used for threshold lines)
    val glucoseNormalMin: Float = 70f,
    val glucoseNormalMax: Float = 100f,
    val glucoseHighThreshold: Float = 180f,
    val bpSystolicNormal: Float = 120f,
    val bpDiastolicNormal: Float = 80f,

    // ── Nutrition tab ─────────────────────────────────────────────────────────
    val nutritionRange: DateRange = DateRange.WEEK,
    val macroTotals: List<DailyMacroSummary> = emptyList(),

    // ── Insights tab ─────────────────────────────────────────────────────────
    val insightsRange: DateRange = DateRange.MONTH,
    val totalPlans: Int = 0,
    val completedPlans: Int = 0,

    val isLoading: Boolean = false
)

@HiltViewModel
class ChartsViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()

    private var healthJob: Job? = null
    private var nutritionJob: Job? = null
    private var insightsJob: Job? = null

    init {
        loadStreakData()
        loadHealthMetrics()
        loadNutritionData()
        loadInsightsData()
    }

    // ── Streak tab ────────────────────────────────────────────────────────────

    private fun loadStreakData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val yearAgo = today.minusDays(365)
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = today.withDayOfMonth(today.lengthOfMonth())

            // All-year dates → streak + best streak
            dailyLogRepository.getLoggedDatesForStreak(yearAgo, today).collect { summaries ->
                val loggedMs = summaries.map { it.date }.toSet()

                val current = computeCurrentStreak(loggedMs, today)
                val best = computeBestStreak(loggedMs, today)

                // This-month dates for calendar
                val monthDates = dailyLogRepository
                    .getLoggedDatesForStreak(monthStart, monthEnd)
                    .first()
                    .map { it.date }.toSet()

                // All-time 30-day average macros
                val macros30 = dailyLogRepository
                    .getDailyMacroTotals(today.minusDays(30), today)
                    .first()

                val avgCal = if (macros30.isNotEmpty()) macros30.map { it.calories }.average().toInt() else 0
                val avgProt = if (macros30.isNotEmpty()) macros30.map { it.protein }.average().toInt() else 0

                _uiState.update {
                    it.copy(
                        currentStreak = current,
                        bestStreak = best,
                        loggedDatesThisMonth = monthDates,
                        totalDaysLogged = loggedMs.size,
                        avgCaloriesPerDay = avgCal,
                        avgProteinPerDay = avgProt
                    )
                }
            }
        }
    }

    private fun computeCurrentStreak(loggedMs: Set<Long>, today: LocalDate): Int {
        // If today has nothing logged yet, start from yesterday so the streak
        // doesn't drop to 0 every morning before the user's first meal.
        var i = if (today.toEpochMs() in loggedMs) 0L else 1L
        var streak = 0
        while (true) {
            val ms = today.minusDays(i).toEpochMs()
            if (ms in loggedMs) { streak++; i++ } else break
        }
        return streak
    }

    private fun computeBestStreak(loggedMs: Set<Long>, today: LocalDate): Int {
        if (loggedMs.isEmpty()) return 0
        var best = 0
        var current = 0
        var day = today.minusDays(365)
        val endDay = today
        while (!day.isAfter(endDay)) {
            if (day.toEpochMs() in loggedMs) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
            day = day.plusDays(1)
        }
        return best
    }

    // Health tab
    private fun loadHealthMetrics() {
        healthJob?.cancel()
        val type = _uiState.value.selectedMetricType
        val range = _uiState.value.healthRange

        healthJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val flow = if (range.days == -1) {
                    healthRepository.getMetricsByType(type)
                } else {
                    val endDate = LocalDate.now().toEpochMs()
                    val startDate = LocalDate.now().minusDays(range.days.toLong()).toEpochMs()
                    healthRepository.getMetricsByTypeInRange(type, startDate, endDate)
                }
                flow.collect { metrics ->
                    _uiState.update {
                        it.copy(
                            healthMetrics = metrics.sortedBy { m -> m.date },
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(healthMetrics = emptyList(), isLoading = false) }
            }
        }
    }

    fun selectMetricType(type: MetricType) {
        _uiState.update { it.copy(selectedMetricType = type) }
        loadHealthMetrics()
    }

    fun selectHealthRange(range: DateRange) {
        _uiState.update { it.copy(healthRange = range) }
        loadHealthMetrics()
    }

    // Nutrition tab
    private fun loadNutritionData() {
        nutritionJob?.cancel()
        val range = _uiState.value.nutritionRange
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(range.days.toLong())

        nutritionJob = viewModelScope.launch {
            try {
                dailyLogRepository.getDailyMacroTotals(startDate, endDate).collect { macros ->
                    _uiState.update { it.copy(macroTotals = macros) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(macroTotals = emptyList()) }
            }
        }
    }

    fun updateGlucoseRange(min: Float, max: Float, high: Float) {
        _uiState.update { it.copy(glucoseNormalMin = min, glucoseNormalMax = max, glucoseHighThreshold = high) }
    }

    fun updateBpRange(systolic: Float, diastolic: Float) {
        _uiState.update { it.copy(bpSystolicNormal = systolic, bpDiastolicNormal = diastolic) }
    }

    fun selectNutritionRange(range: DateRange) {
        _uiState.update { it.copy(nutritionRange = range) }
        loadNutritionData()
    }

    // Insights tab
    private fun loadInsightsData() {
        insightsJob?.cancel()
        val range = _uiState.value.insightsRange
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(range.days.toLong())

        insightsJob = viewModelScope.launch {
            try {
                planRepository.getPlansInRange(startDate, endDate).collect { plans ->
                    val total = plans.size
                    val completed = plans.count { it.isCompleted }
                    _uiState.update {
                        it.copy(totalPlans = total, completedPlans = completed)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(totalPlans = 0, completedPlans = 0) }
            }
        }
    }

    fun selectInsightsRange(range: DateRange) {
        _uiState.update { it.copy(insightsRange = range) }
        loadInsightsData()
    }
}
