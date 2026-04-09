package com.mealplanplus.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.healthconnect.ActivityDaySummary
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.HealthConnectRepository
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToInt

/** Granularity selector shared by all metric tabs and the Activity tab. */
enum class PeriodViewType(val label: String) {
    DAYS("Days"),
    WEEK("Week"),
    MONTH("Month")
}

data class HealthStats(val avg: Double, val min: Double, val max: Double)

data class BgDistribution(
    val lowPercent: Int,
    val inRangePercent: Int,
    val elevatedPercent: Int,
    val highPercent: Int
)

data class HealthUiState(
    val selectedMetricType: MetricType? = MetricType.BLOOD_GLUCOSE,
    val selectedCustomTypeId: Long? = null,
    // metrics sorted DESC (most recent first)
    val metrics: List<HealthMetric> = emptyList(),
    val stats: HealthStats? = null,
    // Blood Glucose specific
    val estimatedA1c: Double? = null,
    val timeInRangePercent: Int? = null,
    val bgDistribution: BgDistribution? = null,
    // Period navigator — shared by regular metric tabs
    val metricViewType: PeriodViewType = PeriodViewType.WEEK,
    val metricPeriodOffset: Int = 0,
    val metricRangeLabel: String = "",
    // Activity (Health Connect) tab
    val isActivityTabSelected: Boolean = false,
    val activityHistory: List<ActivityDaySummary> = emptyList(),
    val isHcAvailable: Boolean = false,
    val isHcConnected: Boolean = false,
    val activityViewType: PeriodViewType = PeriodViewType.WEEK,
    /** 0 = current period, -1 = one period back, etc. Never positive (no future). */
    val activityPeriodOffset: Int = 0,
    /** Human-readable label for the currently shown period, e.g. "06/04 – 12/04". */
    val activityRangeLabel: String = "",
    // Log sheet
    val showLogSheet: Boolean = false,
    val logBgValue: String = "",
    val logBgSubType: String = GlucoseSubType.FASTING.name,
    val logWeightValue: String = "",
    val logBpSystolic: String = "",
    val logBpDiastolic: String = "",
    val logCustomValue: String = "",
    val logDate: LocalDate = LocalDate.now(),
    val logNotes: String = "",
    val isSaving: Boolean = false,
    // Custom type dialog
    val showAddCustomTypeDialog: Boolean = false,
    val newCustomTypeName: String = "",
    val newCustomTypeUnit: String = "",
    val newCustomTypeMin: String = "",
    val newCustomTypeMax: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val healthConnectRepository: HealthConnectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    val customTypes: StateFlow<List<CustomMetricType>> = healthRepository.getActiveCustomTypes()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var metricsJob: Job? = null

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        metricsJob?.cancel()
        metricsJob = viewModelScope.launch {
            val state = _uiState.value
            val (startDate, endDate) = periodDateRange(state.metricViewType, state.metricPeriodOffset)
            val label = periodRangeLabel(startDate, endDate, state.metricViewType)
            _uiState.update { it.copy(metricRangeLabel = label) }

            val flow: Flow<List<HealthMetric>> = when {
                state.selectedCustomTypeId != null -> {
                    healthRepository.getMetricsByCustomType(state.selectedCustomTypeId)
                        .map { list ->
                            list.filter {
                                it.date >= startDate.toString() && it.date <= endDate.toString()
                            }
                        }
                }
                state.selectedMetricType != null -> {
                    // getMetricsByTypeInRange returns ASC; reverse → DESC
                    healthRepository.getMetricsByTypeInRange(
                        state.selectedMetricType, startDate.toString(), endDate.toString()
                    ).map { it.reversed() }
                }
                else -> flowOf(emptyList())
            }

            flow.collect { metrics ->
                val stats = computeStats(metrics)
                val (a1c, timeInRange, dist) = computeBgStats(metrics, state.selectedMetricType)
                _uiState.update {
                    it.copy(
                        metrics = metrics,
                        stats = stats,
                        estimatedA1c = a1c,
                        timeInRangePercent = timeInRange,
                        bgDistribution = dist,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun computeStats(metrics: List<HealthMetric>): HealthStats? {
        if (metrics.isEmpty()) return null
        val values = metrics.map { it.value }
        return HealthStats(avg = values.average(), min = values.min(), max = values.max())
    }

    private fun computeBgStats(
        metrics: List<HealthMetric>,
        type: MetricType?
    ): Triple<Double?, Int?, BgDistribution?> {
        if (type != MetricType.BLOOD_GLUCOSE || metrics.isEmpty()) return Triple(null, null, null)
        val values = metrics.map { it.value }
        val avg = values.average()
        val estimatedA1c = (avg + 46.7) / 28.7
        val inRangeCount = values.count { it in 80.0..180.0 }
        val timeInRangePercent = (inRangeCount * 100.0 / values.size).roundToInt()
        val total = values.size
        val distribution = BgDistribution(
            lowPercent = (values.count { it < 70 } * 100 / total).coerceIn(0, 100),
            inRangePercent = (values.count { it in 70.0..140.0 } * 100 / total).coerceIn(0, 100),
            elevatedPercent = (values.count { it in 140.0..200.0 } * 100 / total).coerceIn(0, 100),
            highPercent = (values.count { it > 200 } * 100 / total).coerceIn(0, 100)
        )
        return Triple(estimatedA1c, timeInRangePercent, distribution)
    }

    fun selectMetricType(type: MetricType) {
        _uiState.update {
            it.copy(selectedMetricType = type, selectedCustomTypeId = null,
                isActivityTabSelected = false, metricPeriodOffset = 0, isLoading = true)
        }
        loadMetrics()
    }

    fun selectCustomType(typeId: Long) {
        _uiState.update {
            it.copy(selectedCustomTypeId = typeId, selectedMetricType = null,
                isActivityTabSelected = false, metricPeriodOffset = 0, isLoading = true)
        }
        loadMetrics()
    }

    /** Switch Days/Week/Month granularity for regular metric tabs and reset to current period. */
    fun selectMetricViewType(type: PeriodViewType) {
        _uiState.update { it.copy(metricViewType = type, metricPeriodOffset = 0, isLoading = true) }
        loadMetrics()
    }

    /** Navigate the metric period backwards (delta = -1) or forwards (delta = +1, capped at 0). */
    fun shiftMetricPeriod(delta: Int) {
        val newOffset = (_uiState.value.metricPeriodOffset + delta).coerceAtMost(0)
        _uiState.update { it.copy(metricPeriodOffset = newOffset, isLoading = true) }
        loadMetrics()
    }

    fun selectActivityTab() {
        _uiState.update {
            it.copy(isActivityTabSelected = true, selectedMetricType = null,
                selectedCustomTypeId = null, isLoading = true)
        }
        loadActivityHistory()
    }

    /** Switch Days/Week/Month granularity for the Activity tab and reset to current period. */
    fun selectActivityViewType(type: PeriodViewType) {
        _uiState.update { it.copy(activityViewType = type, activityPeriodOffset = 0, isLoading = true) }
        loadActivityHistory()
    }

    /** Navigate the activity period backwards (delta = -1) or forwards (delta = +1, capped at 0). */
    fun shiftActivityPeriod(delta: Int) {
        val newOffset = (_uiState.value.activityPeriodOffset + delta).coerceAtMost(0)
        _uiState.update { it.copy(activityPeriodOffset = newOffset, isLoading = true) }
        loadActivityHistory()
    }

    // ── Shared period helpers ──────────────────────────────────────────────────

    private fun periodDateRange(viewType: PeriodViewType, offset: Int): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (viewType) {
            PeriodViewType.DAYS -> {
                val end = today.plusDays((offset * 5).toLong())
                val start = end.minusDays(4)
                start to minOf(end, today)
            }
            PeriodViewType.WEEK -> {
                val monday = today.with(DayOfWeek.MONDAY).plusWeeks(offset.toLong())
                val sunday = monday.plusDays(6)
                monday to minOf(sunday, today)
            }
            PeriodViewType.MONTH -> {
                val firstDay = today.withDayOfMonth(1).plusMonths(offset.toLong())
                val lastDay = firstDay.plusMonths(1).minusDays(1)
                firstDay to minOf(lastDay, today)
            }
        }
    }

    private fun periodRangeLabel(start: LocalDate, end: LocalDate, viewType: PeriodViewType): String {
        return when (viewType) {
            PeriodViewType.MONTH -> start.format(DateTimeFormatter.ofPattern("MM/yyyy"))
            else -> {
                val fmt = DateTimeFormatter.ofPattern("dd/MM")
                "${start.format(fmt)} – ${end.format(fmt)}"
            }
        }
    }

    private fun loadActivityHistory() {
        viewModelScope.launch {
            val state = _uiState.value
            val (startDate, endDate) = periodDateRange(state.activityViewType, state.activityPeriodOffset)
            val label = periodRangeLabel(startDate, endDate, state.activityViewType)
            val isAvailable = healthConnectRepository.isAvailable
            val isConnected = if (isAvailable) healthConnectRepository.hasPermissions() else false
            val history = if (isConnected) {
                healthConnectRepository.getActivityHistory(startDate, endDate)
            } else emptyList()
            _uiState.update {
                it.copy(
                    activityHistory = history,
                    activityRangeLabel = label,
                    isHcAvailable = isAvailable,
                    isHcConnected = isConnected,
                    isLoading = false
                )
            }
        }
    }

    fun showLogSheet() {
        _uiState.update {
            it.copy(
                showLogSheet = true,
                logBgValue = "", logBgSubType = GlucoseSubType.FASTING.name,
                logWeightValue = "", logBpSystolic = "", logBpDiastolic = "",
                logCustomValue = "", logDate = LocalDate.now(), logNotes = "", error = null
            )
        }
    }

    fun hideLogSheet() { _uiState.update { it.copy(showLogSheet = false, error = null) } }

    fun updateLogBgValue(v: String) { _uiState.update { it.copy(logBgValue = v) } }
    fun updateLogBgSubType(s: String) { _uiState.update { it.copy(logBgSubType = s) } }
    fun updateLogWeightValue(v: String) { _uiState.update { it.copy(logWeightValue = v) } }
    fun updateLogBpSystolic(v: String) { _uiState.update { it.copy(logBpSystolic = v) } }
    fun updateLogBpDiastolic(v: String) { _uiState.update { it.copy(logBpDiastolic = v) } }
    fun updateLogCustomValue(v: String) { _uiState.update { it.copy(logCustomValue = v) } }
    fun updateLogDate(d: LocalDate) { _uiState.update { it.copy(logDate = d) } }
    fun updateLogNotes(n: String) { _uiState.update { it.copy(logNotes = n) } }

    fun saveAllMetrics() {
        val state = _uiState.value
        val dateStr = state.logDate.toString()
        val notes = state.logNotes.ifBlank { null }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                state.logBgValue.toDoubleOrNull()?.let { v ->
                    healthRepository.logMetric(
                        MetricType.BLOOD_GLUCOSE, v, dateStr,
                        subType = state.logBgSubType, notes = notes
                    )
                }
                state.logWeightValue.toDoubleOrNull()?.let { v ->
                    healthRepository.logMetric(MetricType.WEIGHT, v, dateStr, notes = notes)
                }
                val sys = state.logBpSystolic.toDoubleOrNull()
                val dia = state.logBpDiastolic.toDoubleOrNull()
                if (sys != null && dia != null) {
                    healthRepository.logMetric(
                        MetricType.BLOOD_PRESSURE, sys, dateStr,
                        secondaryValue = dia, notes = notes
                    )
                }
                val customId = state.selectedCustomTypeId
                state.logCustomValue.toDoubleOrNull()?.let { v ->
                    if (customId != null) healthRepository.logCustomMetric(customId, v, dateStr, notes)
                }
                _uiState.update { it.copy(showLogSheet = false, isSaving = false) }
                loadMetrics()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }

    fun deleteMetric(metric: HealthMetric) {
        viewModelScope.launch {
            healthRepository.deleteMetric(metric)
            loadMetrics()
        }
    }

    fun showAddCustomTypeDialog() {
        _uiState.update {
            it.copy(
                showAddCustomTypeDialog = true,
                newCustomTypeName = "", newCustomTypeUnit = "",
                newCustomTypeMin = "", newCustomTypeMax = ""
            )
        }
    }

    fun hideAddCustomTypeDialog() { _uiState.update { it.copy(showAddCustomTypeDialog = false) } }
    fun updateNewCustomTypeName(v: String) { _uiState.update { it.copy(newCustomTypeName = v) } }
    fun updateNewCustomTypeUnit(v: String) { _uiState.update { it.copy(newCustomTypeUnit = v) } }
    fun updateNewCustomTypeMin(v: String) { _uiState.update { it.copy(newCustomTypeMin = v) } }
    fun updateNewCustomTypeMax(v: String) { _uiState.update { it.copy(newCustomTypeMax = v) } }

    fun addCustomType() {
        val state = _uiState.value
        val name = state.newCustomTypeName.trim()
        val unit = state.newCustomTypeUnit.trim()
        if (name.isBlank() || unit.isBlank()) {
            _uiState.update { it.copy(error = "Name and unit required") }
            return
        }
        viewModelScope.launch {
            try {
                val newId = healthRepository.addCustomType(
                    name = name, unit = unit,
                    minValue = state.newCustomTypeMin.toDoubleOrNull(),
                    maxValue = state.newCustomTypeMax.toDoubleOrNull()
                )
                _uiState.update { it.copy(showAddCustomTypeDialog = false) }
                selectCustomType(newId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteCustomType(type: CustomMetricType) {
        viewModelScope.launch {
            // Soft delete (keep data, just hide the type)
            healthRepository.updateCustomType(type.copy(isActive = false))
            if (_uiState.value.selectedCustomTypeId == type.id) {
                _uiState.update {
                    it.copy(selectedCustomTypeId = null, selectedMetricType = MetricType.BLOOD_GLUCOSE)
                }
                loadMetrics()
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
