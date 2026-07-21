package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.DashboardApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.model.AddLoggedFoodRequest
import com.mealplanplus.data.generated.model.DashboardDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.ui.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val loading: Boolean = true,
    val dashboard: DashboardDto? = null,
    val foods: List<FoodDto> = emptyList(),  // for the "Add to today" picker
    val error: String? = null,
    val togglingSlot: String? = null,
)

/**
 * Today / Home screen. Online read of the aggregated dashboard (calorie ring, macros, planned
 * slots + logged state, streak) with optimistic slot logging via [LoggingApi].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val loggingApi: LoggingApi,
    private val foodsApi: FoodsApi,
    private val themeStore: ThemeStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { load(); loadFoods() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.dashboard == null, error = null)
            runCatching { dashboardApi.getDashboard(null) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) _state.value = _state.value.copy(loading = false, dashboard = body)
                    else _state.value = _state.value.copy(loading = false, error = "Couldn't load today (${resp.code()})")
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    private fun loadFoods() {
        viewModelScope.launch {
            runCatching { foodsApi.listFoods(false).body() }.getOrNull()
                ?.let { _state.value = _state.value.copy(foods = it) }
        }
    }

    private fun today(): LocalDate = _state.value.dashboard?.date ?: LocalDate.now()

    /** Toggle a planned slot's logged state; the dashboard totals recompute on reload. */
    fun toggleSlot(slot: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(togglingSlot = slot)
            runCatching { loggingApi.toggleMealSlot(today(), slot) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(togglingSlot = null)
            load()
        }
    }

    /** Add an unplanned food to today under a chosen slot. */
    fun addFood(foodId: Long, slot: String, quantity: Double, unit: FoodUnit) {
        viewModelScope.launch {
            runCatching { loggingApi.addLoggedFood(AddLoggedFoodRequest(today(), foodId, slot, quantity, unit)) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            load()
        }
    }

    /** Remove a previously-added unplanned food. */
    fun removeFood(id: Long) {
        viewModelScope.launch {
            runCatching { loggingApi.removeLoggedFood(id) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            load()
        }
    }

    fun toggleTheme(currentlyDark: Boolean) = themeStore.toggle(currentlyDark)
}
