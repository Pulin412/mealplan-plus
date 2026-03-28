package com.mealplanplus.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetBgStyle
import com.mealplanplus.util.WidgetPreferences
import com.mealplanplus.util.WidgetTextWeight
import com.mealplanplus.util.WidgetTextSize
import com.mealplanplus.widget.CalendarWidget
import com.mealplanplus.widget.DietSummaryWidget
import com.mealplanplus.widget.TodayPlanWidget
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(WidgetAppearanceState())
    val state: StateFlow<WidgetAppearanceState> = _state

    /**
     * Single-threaded sequential scope for all DataStore writes.
     * Because it has parallelism = 1, any coroutine submitted here is guaranteed to
     * run *after* every previously submitted coroutine finishes.  This lets us use a
     * trivial no-op as a flush barrier in [forceUpdateWidgets].
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    init {
        // Observe all preference flows and merge into a single state.
        // Use the combine(Iterable) overload since there are more than 5 flows.
        combine(
            listOf(
                WidgetPreferences.bgStyleFlow(context),
                WidgetPreferences.bgAlphaFlow(context),
                WidgetPreferences.accentColorFlow(context),
                WidgetPreferences.useDynamicFlow(context),
                WidgetPreferences.textColorFlow(context),
                WidgetPreferences.textWeightFlow(context),
                WidgetPreferences.textSizeFlow(context)
            )
        ) { values ->
            WidgetAppearanceState(
                bgStyle     = values[0] as String,
                bgAlpha     = values[1] as Float,
                accentColor = values[2] as String,
                useDynamic  = values[3] as Boolean,
                textColor   = values[4] as String,
                textWeight  = runCatching { WidgetTextWeight.valueOf(values[5] as String) }
                    .getOrDefault(WidgetTextWeight.NORMAL),
                textSize    = runCatching { WidgetTextSize.valueOf(values[6] as String) }
                    .getOrDefault(WidgetTextSize.MEDIUM)
            )
        }.onEach { _state.value = it }.launchIn(viewModelScope)
    }

    fun setBgStyle(style: String) {
        writeScope.launch {
            WidgetPreferences.setBgStyle(context, style)
            // When the user picks any manual style, switch off dynamic
            if (style != WidgetBgStyle.DYNAMIC) {
                WidgetPreferences.setUseDynamic(context, false)
            }
        }
    }

    fun setBgAlpha(alpha: Float) {
        writeScope.launch { WidgetPreferences.setBgAlpha(context, alpha) }
    }

    fun setAccentColor(hex: String) {
        writeScope.launch { WidgetPreferences.setAccentColor(context, hex) }
    }

    fun setUseDynamic(enabled: Boolean) {
        writeScope.launch {
            WidgetPreferences.setUseDynamic(context, enabled)
            if (enabled) {
                // Sync style tag so the UI shows "Dynamic" as selected
                WidgetPreferences.setBgStyle(context, WidgetBgStyle.DYNAMIC)
            }
        }
    }

    fun setTextColor(hex: String) {
        writeScope.launch { WidgetPreferences.setTextColor(context, hex) }
    }

    fun setTextWeight(weight: WidgetTextWeight) {
        writeScope.launch { WidgetPreferences.setTextWeight(context, weight) }
    }

    fun setTextSize(size: WidgetTextSize) {
        writeScope.launch { WidgetPreferences.setTextSize(context, size) }
    }

    /**
     * Reliably pushes the latest appearance settings to all three widgets.
     *
     * All set* calls above use [writeScope], which is a single-threaded sequential
     * dispatcher (parallelism = 1).  Submitting a no-op [async] to that same scope
     * and awaiting it is therefore a guaranteed flush barrier: it cannot complete
     * until every previously-submitted write coroutine has finished committing to
     * DataStore.  Only then do we trigger the three widget recompositions.
     */
    suspend fun forceUpdateWidgets() {
        writeScope.async { }.await()          // drain all pending DataStore writes
        DietSummaryWidget().updateAll(context)
        TodayPlanWidget().updateAll(context)
        CalendarWidget().updateAll(context)
    }

    override fun onCleared() {
        super.onCleared()
        writeScope.cancel()
    }
}
