package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoodSort { RECENT, NAME, CALORIES, PROTEIN }
enum class FoodViewMode { LIST, COMPACT }
enum class FoodSheet { MANUAL, ONLINE, BARCODE }

data class FoodsUiState(
    val foods: List<Food> = emptyList(),
    val searchQuery: String = "",
    val sortMode: FoodSort = FoodSort.RECENT,
    val viewMode: FoodViewMode = FoodViewMode.LIST,
    val favOnly: Boolean = false,
    val expandedIds: Set<String> = emptySet(),
    val activeSheet: FoodSheet? = null,
    val fanOpen: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val manualName: String = "",
    val manualServing: String = "",
    val manualKcal: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = "",
    val onlineQuery: String = "",
    val onlineResults: List<FoodDto> = emptyList(),
    val onlineLoading: Boolean = false,
) {
    val filteredFoods: List<Food>
        get() {
            var list = foods
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                    it.brand?.lowercase()?.contains(q) == true
                }
            }
            if (favOnly) {
                list = list.filter { it.isFavorite }
            }
            list = when (sortMode) {
                FoodSort.RECENT   -> list.sortedByDescending { it.updatedAt }
                FoodSort.NAME     -> list.sortedBy { it.name.lowercase() }
                FoodSort.CALORIES -> list.sortedByDescending { it.caloriesPer100 }
                FoodSort.PROTEIN  -> list.sortedByDescending { it.proteinPer100 }
            }
            return list
        }

    val favCount: Int get() = foods.count { it.isFavorite }

    val isSaveManualEnabled: Boolean
        get() = manualName.isNotBlank() && manualKcal.isNotBlank()
}

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FoodsUiState())
    val state: StateFlow<FoodsUiState> = _state

    init {
        // UI reads the local cache reactively; sync runs in the background.
        viewModelScope.launch {
            repository.getFoods().collect { foods ->
                _state.value = _state.value.copy(foods = foods)
            }
        }
        sync()   // pull server changes on open
    }

    /** Push local (dirty) changes and pull server changes. Fire-and-forget. */
    private fun sync() {
        viewModelScope.launch {
            syncManager.sync().onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun setSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSort(sort: FoodSort) {
        _state.value = _state.value.copy(sortMode = sort)
    }

    fun setViewMode(mode: FoodViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun toggleFavOnly() {
        _state.value = _state.value.copy(favOnly = !_state.value.favOnly)
    }

    fun toggleExpand(id: String) {
        val current = _state.value.expandedIds
        _state.value = _state.value.copy(
            expandedIds = if (id in current) current - id else current + id
        )
    }

    fun toggleFavorite(food: Food) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(food) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch {
            runCatching { repository.delete(food) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun openFan() {
        _state.value = _state.value.copy(fanOpen = true)
    }

    fun closeFan() {
        _state.value = _state.value.copy(fanOpen = false)
    }

    fun openSheet(sheet: FoodSheet) {
        _state.value = _state.value.copy(activeSheet = sheet, fanOpen = false)
    }

    fun closeSheet() {
        _state.value = _state.value.copy(
            activeSheet = null,
            manualName = "",
            manualServing = "",
            manualKcal = "",
            manualProtein = "",
            manualCarbs = "",
            manualFat = "",
            onlineQuery = "",
            onlineResults = emptyList(),
        )
    }

    fun setManualName(v: String)    { _state.value = _state.value.copy(manualName = v) }
    fun setManualServing(v: String) { _state.value = _state.value.copy(manualServing = v) }
    fun setManualKcal(v: String)    { _state.value = _state.value.copy(manualKcal = v) }
    fun setManualProtein(v: String) { _state.value = _state.value.copy(manualProtein = v) }
    fun setManualCarbs(v: String)   { _state.value = _state.value.copy(manualCarbs = v) }
    fun setManualFat(v: String)     { _state.value = _state.value.copy(manualFat = v) }

    fun saveManual() {
        val s = _state.value
        if (!s.isSaveManualEnabled) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                repository.createManual(
                    name           = s.manualName.trim(),
                    caloriesPer100 = s.manualKcal.toDoubleOrNull() ?: 0.0,
                    proteinPer100  = s.manualProtein.toDoubleOrNull() ?: 0.0,
                    carbsPer100    = s.manualCarbs.toDoubleOrNull() ?: 0.0,
                    fatPer100      = s.manualFat.toDoubleOrNull() ?: 0.0,
                    servingLabel   = s.manualServing.ifBlank { null },
                )
            }.onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(isLoading = false)
            closeSheet()
            sync()
        }
    }

    fun setOnlineQuery(q: String) {
        _state.value = _state.value.copy(onlineQuery = q)
    }

    fun searchOnline() {
        val q = _state.value.onlineQuery.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(onlineLoading = true, onlineResults = emptyList())
            runCatching { repository.searchOnline(q) }
                .onSuccess { results -> _state.value = _state.value.copy(onlineResults = results) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(onlineLoading = false)
        }
    }

    fun addOnlineFood(dto: FoodDto) {
        viewModelScope.launch {
            runCatching { repository.addOnline(dto) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }
}
