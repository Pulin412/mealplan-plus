package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogMealPickerUiState(
    val slotType: String = "",
    val searchQuery: String = "",
    val filterSlot: String? = null,
    val allMeals: List<MealWithFoods> = emptyList(),
    val filteredMeals: List<MealWithFoods> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class LogMealPickerViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogMealPickerUiState())
    val uiState: StateFlow<LogMealPickerUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
    }

    private fun loadMeals() {
        viewModelScope.launch {
            mealRepository.getAllMeals().collect { meals ->
                val mealsWithFoods = meals.map { meal ->
                    mealRepository.getMealWithFoods(meal.id) ?: MealWithFoods(meal, emptyList())
                }
                _uiState.update { state ->
                    state.copy(
                        allMeals = mealsWithFoods,
                        isLoading = false
                    )
                }
                applyFilters()
            }
        }
    }

    fun setSlotType(slotType: String) {
        _uiState.update { it.copy(slotType = slotType, filterSlot = slotType) }
        applyFilters()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setFilterSlot(slot: String?) {
        _uiState.update { it.copy(filterSlot = slot) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.allMeals.filter { mealWithFoods ->
            val matchesSearch = state.searchQuery.isBlank() ||
                    mealWithFoods.meal.name.contains(state.searchQuery, ignoreCase = true) ||
                    mealWithFoods.items.any { it.food.name.contains(state.searchQuery, ignoreCase = true) }

            val matchesSlot = state.filterSlot == null ||
                    mealWithFoods.meal.slotType == state.filterSlot ||
                    mealWithFoods.meal.slotType == "CUSTOM"

            matchesSearch && matchesSlot
        }.sortedBy { it.meal.name.lowercase() }

        _uiState.update { it.copy(filteredMeals = filtered) }
    }
}
