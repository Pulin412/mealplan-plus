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
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthConnectRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.notification.NotificationAlarmBootstrapper
import com.mealplanplus.util.AlarmScheduler
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.CsvExporter
import com.mealplanplus.util.NotificationAlarmType
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.ThemePreferences
import com.mealplanplus.util.toEpochMs
import com.mealplanplus.util.toLocalDate
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
    val importResult: ImportResult? = null,
    // Health Connect state
    val isHealthConnectAvailable: Boolean = false,
    val isHealthConnectConnected: Boolean = false,
    val healthConnectLastSyncWeight: Double? = null
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
    val streakAlertHour: Int = NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR,
    val breakfastMinute: Int = NotificationPreferences.DEFAULT_BREAKFAST_MINUTE,
    val lunchMinute: Int = NotificationPreferences.DEFAULT_LUNCH_MINUTE,
    val dinnerMinute: Int = NotificationPreferences.DEFAULT_DINNER_MINUTE,
    val streakAlertMinute: Int = NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val healthConnectRepository: HealthConnectRepository,
    private val jsonDataImporter: JsonDataImporter,
    private val csvDataImporter: CsvDataImporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** Exposed to the UI so the permission launcher can be initialised with the exact set. */
    val healthConnectPermissions: Set<String> get() = healthConnectRepository.requiredPermissions

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
        checkHealthConnectStatus()
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
        viewModelScope.launch {
            NotificationPreferences.getBreakfastMinute(context).collect { v ->
                _notificationState.update { it.copy(breakfastMinute = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getLunchMinute(context).collect { v ->
                _notificationState.update { it.copy(lunchMinute = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getDinnerMinute(context).collect { v ->
                _notificationState.update { it.copy(dinnerMinute = v) }
            }
        }
        viewModelScope.launch {
            NotificationPreferences.getStreakAlertMinute(context).collect { v ->
                _notificationState.update { it.copy(streakAlertMinute = v) }
            }
        }
    }

    private fun loadDataForExport() {
        viewModelScope.launch {
            dailyLogRepository.getLogsByUser().collect { logs ->
                val logsWithFoods = logs.mapNotNull { log ->
                    dailyLogRepository.getLogWithFoods(log.date.toLocalDate())
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

    /**
     * Called when the user returns from the SCHEDULE_EXACT_ALARM system settings screen.
     * Re-schedules all alarms so any previously-inexact alarms are upgraded to exact.
     */
    fun onExactAlarmPermissionResult() {
        viewModelScope.launch {
            NotificationAlarmBootstrapper.scheduleAll(context)
        }
    }

    // Notification preference setters
    fun setMasterEnabled(enabled: Boolean) {
        viewModelScope.launch {
            NotificationPreferences.setMasterEnabled(context, enabled)
            NotificationAlarmBootstrapper.scheduleAll(context)
        }
    }

    fun setMealRemindersEnabled(enabled: Boolean) {
        viewModelScope.launch {
            NotificationPreferences.setMealRemindersEnabled(context, enabled)
            NotificationAlarmBootstrapper.scheduleAll(context)
        }
    }

    fun setStreakProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            NotificationPreferences.setStreakProtectionEnabled(context, enabled)
            NotificationAlarmBootstrapper.scheduleAll(context)
        }
    }

    fun setWeeklyPlanEnabled(enabled: Boolean) {
        viewModelScope.launch {
            NotificationPreferences.setWeeklyPlanEnabled(context, enabled)
            NotificationAlarmBootstrapper.scheduleAll(context)
        }
    }

    fun setBreakfastHour(hour: Int) {
        viewModelScope.launch {
            NotificationPreferences.setBreakfastHour(context, hour)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.BREAKFAST)
        }
    }

    fun setLunchHour(hour: Int) {
        viewModelScope.launch {
            NotificationPreferences.setLunchHour(context, hour)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.LUNCH)
        }
    }

    fun setDinnerHour(hour: Int) {
        viewModelScope.launch {
            NotificationPreferences.setDinnerHour(context, hour)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.DINNER)
        }
    }

    fun setStreakAlertHour(hour: Int) {
        viewModelScope.launch {
            NotificationPreferences.setStreakAlertHour(context, hour)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.STREAK)
        }
    }

    fun setBreakfastMinute(minute: Int) {
        viewModelScope.launch {
            NotificationPreferences.setBreakfastMinute(context, minute)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.BREAKFAST)
        }
    }

    fun setLunchMinute(minute: Int) {
        viewModelScope.launch {
            NotificationPreferences.setLunchMinute(context, minute)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.LUNCH)
        }
    }

    fun setDinnerMinute(minute: Int) {
        viewModelScope.launch {
            NotificationPreferences.setDinnerMinute(context, minute)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.DINNER)
        }
    }

    fun setStreakAlertMinute(minute: Int) {
        viewModelScope.launch {
            NotificationPreferences.setStreakAlertMinute(context, minute)
            NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.STREAK)
        }
    }

    // ── Health Connect ────────────────────────────────────────────────────────

    /**
     * Checks HC availability and permission status, and optionally syncs the latest
     * weight entry into the Health screen's Room database.
     * Safe to call on every Settings screen resume.
     */
    fun checkHealthConnectStatus() {
        viewModelScope.launch {
            val available = healthConnectRepository.isAvailable
            val connected = available && healthConnectRepository.hasPermissions()
            var latestWeight: Double? = null

            if (connected) {
                latestWeight = healthConnectRepository.getLatestWeightKg()
                if (latestWeight != null) {
                    syncWeightFromHealthConnect(latestWeight)
                }
            }

            _uiState.update {
                it.copy(
                    isHealthConnectAvailable = available,
                    isHealthConnectConnected = connected,
                    healthConnectLastSyncWeight = latestWeight
                )
            }
        }
    }

    /** Called from the UI after the Health Connect permission dialog returns. */
    fun onHealthConnectPermissionsResult() {
        checkHealthConnectStatus()
    }

    /**
     * Launches the Health Connect companion app (not a specific permissions page).
     * Used by the setup guide so the user can navigate to About → enable developer mode.
     */
    fun openHealthConnectApp(ctx: Context) {
        val hcLaunchIntent = ctx.packageManager
            .getLaunchIntentForPackage("com.google.android.apps.healthdata")
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        // On Android 14+ there is no separate HC app; open the system health settings instead
        val systemHealthIntent = android.content.Intent("android.health.ACTION_HEALTH_HOME_SETTINGS")
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(hcLaunchIntent ?: systemHealthIntent)
        } catch (_: Exception) {
            try {
                ctx.startActivity(systemHealthIntent)
            } catch (_: Exception) { /* nothing to open */ }
        }
    }

    /**
     * Opens Health Connect's per-app permissions screen directly.
     *
     * Primary path: the system ACTION_MANAGE_HEALTH_PERMISSIONS deep-link (works on Android 14+
     * and on 9–13 with the HC companion app installed).
     * Fallback: opens the HC companion app launcher, then the generic app-info screen.
     *
     * Use this when the permission dialog closes immediately (common on sideloaded / debug builds
     * because HC's access policy restricts non-Play-Store apps from the standard dialog flow).
     */
    fun openHealthConnectSettings(ctx: Context) {
        val hcPermIntent = android.content.Intent("android.health.ACTION_MANAGE_HEALTH_PERMISSIONS")
            .putExtra(android.content.Intent.EXTRA_PACKAGE_NAME, ctx.packageName)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val hcLaunchIntent = ctx.packageManager
            .getLaunchIntentForPackage("com.google.android.apps.healthdata")
            ?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val appInfoIntent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:${ctx.packageName}"))
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(hcPermIntent)
        } catch (_: Exception) {
            try {
                ctx.startActivity(hcLaunchIntent ?: appInfoIntent)
            } catch (_: Exception) {
                ctx.startActivity(appInfoIntent)
            }
        }
    }

    /**
     * Writes the most recent weight from Health Connect as a WEIGHT metric entry in
     * the local Room database (today's date), if no entry exists for today already.
     */
    private suspend fun syncWeightFromHealthConnect(weightKg: Double) {
        val todayMs = java.time.LocalDate.now().toEpochMs()
        val existing = healthRepository.getMetricsForDate(todayMs)
            .first()
            .any { it.metricType == MetricType.WEIGHT.name }
        if (!existing) {
            healthRepository.logMetric(
                type = MetricType.WEIGHT,
                value = weightKg,
                date = todayMs,
                notes = "Synced from Health Connect"
            )
        }
    }
}
