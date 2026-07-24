package com.mealplanplus.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.export.CsvExporter
import com.mealplanplus.data.export.ExportRepository
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val exportRepository: ExportRepository,
) : ViewModel() {

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

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
}
