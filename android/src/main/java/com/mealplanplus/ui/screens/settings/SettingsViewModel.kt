package com.mealplanplus.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.BuildConfig
import com.mealplanplus.data.export.CsvExporter
import com.mealplanplus.data.export.ExportRepository
import com.mealplanplus.data.repository.FeedbackRepository
import com.mealplanplus.data.healthconnect.HealthConnectManager
import com.mealplanplus.data.healthconnect.HealthConnectSummary
import com.mealplanplus.data.notifications.NotificationScheduler
import com.mealplanplus.data.notifications.NotificationSettings
import com.mealplanplus.data.notifications.NotificationStore
import com.mealplanplus.data.notifications.NotificationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One-shot outcomes the UI reacts to (open the share sheet, or surface an error). */
sealed interface ExportEvent {
    data class Share(val fileName: String, val csv: String) : ExportEvent
    data class Error(val message: String) : ExportEvent
}

/** Health Connect card state: whether the SDK is present, whether we're granted, and today's numbers. */
data class HealthConnectUiState(
    val available: Boolean = true,
    val connected: Boolean = false,
    val summary: HealthConnectSummary = HealthConnectSummary(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
    private val feedbackRepository: FeedbackRepository,
    private val notificationStore: NotificationStore,
    private val notificationScheduler: NotificationScheduler,
    private val healthConnect: HealthConnectManager,
    private val socialRepository: com.mealplanplus.data.repository.SocialRepository,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    /** Server-backed master switch for follow/share notifications. */
    private val _socialNotifications = MutableStateFlow(true)
    val socialNotifications: StateFlow<Boolean> = _socialNotifications.asStateFlow()

    init { viewModelScope.launch { _socialNotifications.value = socialRepository.notificationsEnabled() } }

    fun setSocialNotifications(on: Boolean) {
        _socialNotifications.value = on   // optimistic
        viewModelScope.launch {
            if (!socialRepository.setNotificationsEnabled(on)) {
                _socialNotifications.value = socialRepository.notificationsEnabled()   // revert on failure
            }
        }
    }

    /** Live notification preferences for the Settings UI. */
    val notifications: StateFlow<NotificationSettings> = notificationStore.state

    fun setNotification(type: NotificationType, on: Boolean) {
        notificationStore.setEnabled(type, on)
        notificationScheduler.rescheduleAll()
    }

    /** Master notifications switch — when off, everything is cancelled; when on, per-type flags apply. */
    fun setMasterNotifications(on: Boolean) {
        notificationStore.setMaster(on)
        notificationScheduler.rescheduleAll()
    }

    /** Enable/disable a single meal-slot reminder, then reschedule. */
    fun setMealSlot(slot: String, on: Boolean) {
        notificationStore.setMealSlotEnabled(slot, on)
        notificationScheduler.rescheduleAll()
    }

    fun setMealSlotTime(slot: String, minute: Int) {
        notificationStore.setMealSlotTime(slot, minute)
        notificationScheduler.rescheduleAll()
    }

    fun setWorkoutTime(minute: Int) {
        notificationStore.setWorkoutTime(minute)
        notificationScheduler.rescheduleAll()
    }

    fun setWeighinTime(minute: Int) {
        notificationStore.setWeighinTime(minute)
        notificationScheduler.rescheduleAll()
    }

    fun setWeighinDay(dayIso: Int) {
        notificationStore.setWeighinDay(dayIso)
        notificationScheduler.rescheduleAll()
    }

    fun setQuietHours(on: Boolean) {
        notificationStore.setQuietHours(on)
    }

    // ── Health Connect ──────────────────────────────────────────────────────────
    private val _healthConnect = MutableStateFlow(HealthConnectUiState())
    val healthConnectState: StateFlow<HealthConnectUiState> = _healthConnect.asStateFlow()

    /** Permissions to hand the HC permission-request launcher. */
    val healthConnectPermissions: Set<String> get() = healthConnect.requiredPermissions

    /** Re-read availability, grant status, and today's summary (call on entry + after a grant). */
    fun refreshHealthConnect() {
        viewModelScope.launch {
            if (!healthConnect.isAvailable) {
                _healthConnect.value = HealthConnectUiState(available = false)
                return@launch
            }
            val connected = healthConnect.hasAllPermissions()
            _healthConnect.value = HealthConnectUiState(
                available = true,
                connected = connected,
                summary = if (connected) healthConnect.readTodaySummary() else HealthConnectSummary(),
            )
        }
    }

    fun disconnectHealthConnect() {
        viewModelScope.launch {
            healthConnect.revokeAll()
            refreshHealthConnect()
        }
    }

    private val _events = MutableSharedFlow<ExportEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ExportEvent> = _events

    /** Gather the snapshot, build the CSV off the main thread, then hand the file to the UI to share. */
    fun exportCsv() {
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            val result = runCatching {
                val data = exportRepository.collect()
                CsvExporter.build(data)
            }
            _exporting.value = false
            result.fold(
                onSuccess = { csv -> _events.emit(ExportEvent.Share("mealplan-export-${LocalDate.now()}.csv", csv)) },
                onFailure = { _events.emit(ExportEvent.Error(it.message ?: "Export failed")) },
            )
        }
    }

    // ── Feedback ────────────────────────────────────────────────────────────────
    private val _feedbackSubmitting = MutableStateFlow(false)
    val feedbackSubmitting: StateFlow<Boolean> = _feedbackSubmitting.asStateFlow()

    /** One-shot toast text after a submit attempt (success or failure). */
    private val _feedbackResult = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val feedbackResult: SharedFlow<String> = _feedbackResult

    /** POST the message with this build's version so feedback can be triaged by release. */
    fun sendFeedback(message: String) {
        val text = message.trim()
        if (text.isEmpty() || _feedbackSubmitting.value) return
        viewModelScope.launch {
            _feedbackSubmitting.value = true
            val result = feedbackRepository.submit(text, appVersion = BuildConfig.VERSION_NAME)
            _feedbackSubmitting.value = false
            _feedbackResult.emit(
                if (result.isSuccess) "Thanks for your feedback!"
                else "Couldn't send feedback — please try again",
            )
        }
    }
}
