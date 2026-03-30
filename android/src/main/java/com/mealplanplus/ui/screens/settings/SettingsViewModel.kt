package com.mealplanplus.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.local.CsvDataImporter
import com.mealplanplus.data.local.ImportResult
import com.mealplanplus.data.local.ImportStrategy
import com.mealplanplus.data.local.JsonDataImporter
import com.mealplanplus.data.model.DailyLogWithFoods
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.CsvExporter
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.ThemePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val exportUri: Uri? = null,
    val error: String? = null,
    // Import state
    val isImporting: Boolean = false,
    val importResult: ImportResult? = null
)

data class ThemeState(
    val darkMode: Boolean = false,
    val dynamicColor: Boolean = true,
    val followSystem: Boolean = true
)

data class NotificationState(
    val masterEnabled: Boolean = false,
    val mealRemindersEnabled: Boolean = true,
    val streakProtectionEnabled: Boolean = true,
    val weeklyPlanEnabled: Boolean = true,
    val breakfastHour: Int = NotificationPreferences.DEFAULT_BREAKFAST_HOUR,
    val lunchHour: Int = NotificationPreferences.DEFAULT_LUNCH_HOUR,
    val dinnerHour: Int = NotificationPreferences.DEFAULT_DINNER_HOUR,
    val streakAlertHour: Int = NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val jsonDataImporter: JsonDataImporter,
    private val csvDataImporter: CsvDataImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _themeState = MutableStateFlow(ThemeState())
    val themeState: StateFlow<ThemeState> = _themeState.asStateFlow()

    private val _notificationState = MutableStateFlow(NotificationState())
    val notificationState: StateFlow<NotificationState> = _notificationState.asStateFlow()

    private var allLogs: List<DailyLogWithFoods> = emptyList()
    private var allHealthMetrics: List<HealthMetric> = emptyList()

    init {
        loadThemePreferences()
        loadNotificationPreferences()
        loadDataForExport()
    }

    private fun loadThemePreferences() {
        viewModelScope.launch {
            combine(
                ThemePreferences.isDarkMode(context),
                ThemePreferences.isDynamicColor(context),
                ThemePreferences.isFollowSystem(context)
            ) { darkMode, dynamicColor, followSystem ->
                ThemeState(darkMode, dynamicColor, followSystem)
            }.collect { state ->
                _themeState.value = state
            }
        }
    }

    private fun loadNotificationPreferences() {
        viewModelScope.launch {
            NotificationPreferences.getMasterEnabled(context).collect { v ->
                _notificationState.update { it.copy(masterEnabled = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getMealRemindersEnabled(context).collect { v ->
                _notificationState.update { it.copy(mealRemindersEnabled = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getStreakProtectionEnabled(context).collect { v ->
                _notificationState.update { it.copy(streakProtectionEnabled = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getWeeklyPlanEnabled(context).collect { v ->
                _notificationState.update { it.copy(weeklyPlanEnabled = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getBreakfastHour(context).collect { v ->
                _notificationState.update { it.copy(breakfastHour = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getLunchHour(context).collect { v ->
                _notificationState.update { it.copy(lunchHour = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getDinnerHour(context).collect { v ->
                _notificationState.update { it.copy(dinnerHour = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getStreakAlertHour(context).collect { v ->
                _notificationState.update { it.copy(streakAlertHour = v) }
            }
        }
    }

    private fun loadDataForExport() {
        viewModelScope.launch {
            dailyLogRepository.getLogsByUser().collect { logs ->
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

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreferences.setDarkMode(context, enabled)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreferences.setDynamicColor(context, enabled)
        }
    }

    fun setFollowSystem(enabled: Boolean) {
        viewModelScope.launch {
            ThemePreferences.setFollowSystem(context, enabled)
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

    // Import functions
    fun importDietsFromJson(uri: Uri, strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null, importResult = null) }
            try {
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw IllegalStateException("Not logged in")

                val result = jsonDataImporter.importFromUri(context, uri, userId, strategy)
                _uiState.update { it.copy(isImporting = false, importResult = result) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importResult = ImportResult(false, errorMessage = e.message ?: "Import failed")
                    )
                }
            }
        }
    }

    fun importDietsFromCsv(uri: Uri, strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null, importResult = null) }
            try {
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw IllegalStateException("Not logged in")

                val result = csvDataImporter.importFromUri(context, uri, userId, strategy)
                _uiState.update { it.copy(isImporting = false, importResult = result) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        importResult = ImportResult(false, errorMessage = e.message ?: "Import failed")
                    )
                }
            }
        }
    }

    /**
     * Auto-detect file type and import accordingly
     */
    fun importDietsFromUri(uri: Uri, strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES) {
        val fileName = uri.lastPathSegment?.lowercase() ?: ""
        when {
            fileName.endsWith(".csv") -> importDietsFromCsv(uri, strategy)
            fileName.endsWith(".json") -> importDietsFromJson(uri, strategy)
            else -> {
                // Try to detect from content type or default to CSV
                viewModelScope.launch {
                    val mimeType = context.contentResolver.getType(uri)
                    when {
                        mimeType?.contains("json") == true -> importDietsFromJson(uri, strategy)
                        mimeType?.contains("csv") == true -> importDietsFromCsv(uri, strategy)
                        else -> {
                            // Default: try JSON first, fallback to CSV
                            importDietsFromJson(uri, strategy)
                        }
                    }
                }
            }
        }
    }

    fun clearImportState() {
        _uiState.update { it.copy(importResult = null, error = null) }
    }

    // Notification preference setters
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch { NotificationPreferences.setMasterEnabled(context, enabled) }
    }

    fun setMealRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch { NotificationPreferences.setMealRemindersEnabled(context, enabled) }
    }

    fun setStreakProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch { NotificationPreferences.setStreakProtectionEnabled(context, enabled) }
    }

    fun setWeeklyPlanEnabled(enabled: Boolean) {
        viewModelScope.launch { NotificationPreferences.setWeeklyPlanEnabled(context, enabled) }
    }

    fun setBreakfastHour(hour: Int) {
        viewModelScope.launch { NotificationPreferences.setBreakfastHour(context, hour) }
    }

    fun setLunchHour(hour: Int) {
        viewModelScope.launch { NotificationPreferences.setLunchHour(context, hour) }
    }

    fun setDinnerHour(hour: Int) {
        viewModelScope.launch { NotificationPreferences.setDinnerHour(context, hour) }
    }

    fun setStreakAlertHour(hour: Int) {
        viewModelScope.launch { NotificationPreferences.setStreakAlertHour(context, hour) }
    }
}
