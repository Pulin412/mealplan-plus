package com.mealplanplus.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyLogWithFoods
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportUri: Uri? = null,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var allLogs: List<DailyLogWithFoods> = emptyList()
    private var allHealthMetrics: List<HealthMetric> = emptyList()

    init {
        // Load data for export
        viewModelScope.launch {
            dailyLogRepository.getAllLogs().collect { logs ->
                // Convert to logs with foods
                val logsWithFoods = logs.mapNotNull { log ->
                    dailyLogRepository.getLogWithFoods(dailyLogRepository.parseDate(log.date))
                        .firstOrNull()
                }
                allLogs = logsWithFoods
            }
        }
        viewModelScope.launch {
            healthRepository.getRecentMetrics(1000).collect { metrics ->
                allHealthMetrics = metrics
            }
        }
    }

    fun exportFoodLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val uri = CsvExporter.exportFoodLog(context, allLogs)
                if (uri != null) {
                    _uiState.update { it.copy(isExporting = false, exportSuccess = true, exportUri = uri) }
                } else {
                    _uiState.update { it.copy(isExporting = false, error = "Export failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    fun exportHealthMetrics() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val uri = CsvExporter.exportHealthMetrics(context, allHealthMetrics)
                if (uri != null) {
                    _uiState.update { it.copy(isExporting = false, exportSuccess = true, exportUri = uri) }
                } else {
                    _uiState.update { it.copy(isExporting = false, error = "Export failed") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = e.message) }
            }
        }
    }

    fun shareExport() {
        _uiState.value.exportUri?.let { uri ->
            CsvExporter.shareFile(context, uri, "Share Export")
        }
    }

    fun clearExportState() {
        _uiState.update { it.copy(exportSuccess = false, exportUri = null, error = null) }
    }
}
