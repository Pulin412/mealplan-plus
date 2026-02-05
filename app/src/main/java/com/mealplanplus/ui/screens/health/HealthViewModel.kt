package com.mealplanplus.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HealthUiState(
    val selectedMetricType: MetricType = MetricType.WEIGHT,
    val metrics: List<HealthMetric> = emptyList(),
    val showLogDialog: Boolean = false,
    val logValue: String = "",
    val logDate: LocalDate = LocalDate.now(),
    val logNotes: String = "",
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

    init {
        loadMetrics()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            healthRepository.getMetricsByType(_uiState.value.selectedMetricType).collect { metrics ->
                _uiState.update { it.copy(metrics = metrics, isLoading = false) }
            }
        }
    }

    fun selectMetricType(type: MetricType) {
        _uiState.update { it.copy(selectedMetricType = type, isLoading = true) }
        loadMetrics()
    }

    fun showLogDialog() {
        _uiState.update {
            it.copy(
                showLogDialog = true,
                logValue = "",
                logDate = LocalDate.now(),
                logNotes = "",
                error = null
            )
        }
    }

    fun hideLogDialog() {
        _uiState.update { it.copy(showLogDialog = false, error = null) }
    }

    fun updateLogValue(value: String) {
        _uiState.update { it.copy(logValue = value) }
    }

    fun updateLogDate(date: LocalDate) {
        _uiState.update { it.copy(logDate = date) }
    }

    fun updateLogNotes(notes: String) {
        _uiState.update { it.copy(logNotes = notes) }
    }

    fun saveMetric() {
        val value = _uiState.value.logValue.toDoubleOrNull()
        if (value == null) {
            _uiState.update { it.copy(error = "Enter a valid number") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                healthRepository.logMetric(
                    type = _uiState.value.selectedMetricType,
                    value = value,
                    date = _uiState.value.logDate.toString(),
                    notes = _uiState.value.logNotes.ifBlank { null }
                )
                _uiState.update { it.copy(showLogDialog = false, isLoading = false) }
                loadMetrics()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun deleteMetric(metric: HealthMetric) {
        viewModelScope.launch {
            healthRepository.deleteMetric(metric)
            loadMetrics()
        }
    }
}
