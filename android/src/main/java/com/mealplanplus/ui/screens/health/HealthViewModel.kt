package com.mealplanplus.ui.screens.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.cache.ResponseCache
import com.mealplanplus.data.cache.render
import com.mealplanplus.data.generated.model.HealthMetricDto
import com.mealplanplus.data.repository.HealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The three built-in vitals, with their backend type string + display metadata. */
enum class HealthTab(val type: String, val label: String, val unit: String, val metricLabel: String) {
    GLUCOSE("GLUCOSE", "Glucose", "mg/dL", "Blood glucose"),
    WEIGHT("WEIGHT", "Weight", "kg", "Body weight"),
    BP("BLOOD_PRESSURE", "BP", "mmHg", "Blood pressure"),
}

/** Trend-graph window. */
enum class RangeWindow(val label: String, val days: Long) { D7("7D", 7), D30("30D", 30), D90("90D", 90) }

/** One combined log sheet — enter any of the metrics; each filled value saves to its own type. */
data class HealthLog(
    val glucose: String = "",
    val weight: String = "",
    val systolic: String = "",
    val diastolic: String = "",
) {
    val glucoseVal: Double? get() = glucose.toDoubleOrNull()
    val weightVal: Double? get() = weight.toDoubleOrNull()
    val bpTouched: Boolean get() = systolic.isNotBlank() || diastolic.isNotBlank()
    val bpValid: Boolean get() = systolic.toDoubleOrNull() != null && diastolic.toDoubleOrNull() != null
    val bpIncomplete: Boolean get() = bpTouched && !bpValid
    val canSave: Boolean get() = !bpIncomplete && (glucoseVal != null || weightVal != null || bpValid)
}

data class HealthUiState(
    val tab: HealthTab = HealthTab.GLUCOSE,
    val range: RangeWindow = RangeWindow.D7,
    val readings: Map<HealthTab, List<HealthMetricDto>> = emptyMap(),
    val loading: Boolean = true,
    val error: String? = null,
    val log: HealthLog? = null,
) {
    /** Readings for the active tab, oldest-first. */
    val current: List<HealthMetricDto> get() = readings[tab].orEmpty()
}

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val repo: HealthRepository,
    private val responseCache: ResponseCache,
) : ViewModel() {

    private val _state = MutableStateFlow(HealthUiState())
    val state: StateFlow<HealthUiState> = _state

    init { load() }

    /**
     * Read-through cached load: paints the last-known readings instantly on a cold open, then
     * refreshes all three metric types from the server in the background. The cache is keyed by
     * type string ([HealthTab.type]) so Gson never has to serialize enum map keys.
     */
    private fun load() {
        viewModelScope.launch {
            responseCache.stream<Map<String, List<HealthMetricDto>>>("health.readings") {
                HealthTab.entries.map { tab -> async { tab.type to repo.list(tab.type) } }
                    .awaitAll().toMap()
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.readings.isNotEmpty())
                    st.copy(
                        readings = if (r.keep) st.readings
                        else r.value?.let { m -> HealthTab.entries.associateWith { m[it.type].orEmpty() } } ?: st.readings,
                        loading = r.loading,
                        error = r.error,
                    )
                }
            }
        }
    }

    fun setTab(tab: HealthTab) = _state.update { it.copy(tab = tab) }
    fun setRange(range: RangeWindow) = _state.update { it.copy(range = range) }

    // ── Combined log sheet ───────────────────────────────────────────────────────
    fun openLog() = _state.update { it.copy(log = HealthLog(), error = null) }
    fun closeLog() = _state.update { it.copy(log = null) }
    /** Glucose + BP are whole numbers; weight allows one decimal. */
    fun setLogField(field: String, v: String) = _state.update { st ->
        val log = st.log ?: return@update st
        val c = v.filterNumeric(field == "weight")
        st.copy(log = when (field) {
            "glucose" -> log.copy(glucose = c)
            "weight" -> log.copy(weight = c)
            "systolic" -> log.copy(systolic = c)
            else -> log.copy(diastolic = c)
        })
    }

    fun saveLog() {
        val log = _state.value.log ?: return
        if (!log.canSave) return
        viewModelScope.launch {
            val results = buildList {
                log.glucoseVal?.let { add(repo.create("GLUCOSE", it, "mg/dL")) }
                log.weightVal?.let { add(repo.create("WEIGHT", it, "kg")) }
                if (log.bpValid) add(repo.create("BLOOD_PRESSURE", log.systolic.toDouble(), "mmHg", secondaryValue = log.diastolic.toDouble()))
            }
            val err = results.firstNotNullOfOrNull { it.exceptionOrNull() }
            if (err == null) { closeLog(); load() } else _state.update { it.copy(error = err.message) }
        }
    }

    /** Keep digits, and a single decimal point when [allowDecimal]. */
    private fun String.filterNumeric(allowDecimal: Boolean): String {
        val cleaned = filter { it.isDigit() || (allowDecimal && it == '.') }
        return if (!allowDecimal) cleaned else {
            val i = cleaned.indexOf('.')
            if (i < 0) cleaned else cleaned.substring(0, i + 1) + cleaned.substring(i + 1).replace(".", "")
        }
    }
}
