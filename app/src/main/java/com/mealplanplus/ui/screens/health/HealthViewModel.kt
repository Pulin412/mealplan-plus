package com.mealplanplus.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

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
    val selectedPeriodDays: Int = 14,
    // metrics sorted DESC (most recent first)
    val metrics: List<HealthMetric> = emptyList(),
    val stats: HealthStats? = null,
    // Blood Glucose specific
    val estimatedA1c: Double? = null,
    val timeInRangePercent: Int? = null,
    val bgDistribution: BgDistribution? = null,
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
    private val healthRepository: HealthRepository
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
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(state.selectedPeriodDays.toLong())

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
        _uiState.update { it.copy(selectedMetricType = type, selectedCustomTypeId = null, isLoading = true) }
        loadMetrics()
    }

    fun selectCustomType(typeId: Long) {
        _uiState.update { it.copy(selectedCustomTypeId = typeId, selectedMetricType = null, isLoading = true) }
        loadMetrics()
    }

    fun selectPeriod(days: Int) {
        _uiState.update { it.copy(selectedPeriodDays = days, isLoading = true) }
        loadMetrics()
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
