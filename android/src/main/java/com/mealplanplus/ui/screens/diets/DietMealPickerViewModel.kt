package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietMealPickerUiState(
    val slotType: String = "",
    val searchQuery: String = "",
    val filterSlot: String? = null,
    val showDietBrowser: Boolean = false,
    val allMeals: List<MealWithFoods> = emptyList(),
    val filteredMeals: List<MealWithFoods> = emptyList(),
    val diets: List<DietWithMeals> = emptyList(),
    val expandedDietId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DietMealPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val slotType: String = savedStateHandle.get<String>("slotType") ?: ""

    private val _uiState = MutableStateFlow(DietMealPickerUiState(slotType = slotType, filterSlot = slotType))
    val uiState: StateFlow<DietMealPickerUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
        loadDiets()
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

    private fun loadDiets() {
        viewModelScope.launch {
            dietRepository.getAllDiets().collect { diets ->
                val dietsWithMeals = diets.mapNotNull { diet ->
                    dietRepository.getDietWithMeals(diet.id)
                }
                _uiState.update { it.copy(diets = dietsWithMeals) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setFilterSlot(slot: String?) {
        _uiState.update { it.copy(filterSlot = slot, showDietBrowser = false) }
        applyFilters()
    }

    fun toggleDietBrowser() {
        _uiState.update { it.copy(showDietBrowser = !it.showDietBrowser) }
    }

    fun setShowDietBrowser(show: Boolean) {
        _uiState.update { it.copy(showDietBrowser = show) }
    }

    fun toggleExpandDiet(dietId: Long) {
        _uiState.update { state ->
            state.copy(
                expandedDietId = if (state.expandedDietId == dietId) null else dietId
            )
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.allMeals.filter { mealWithFoods ->
            state.searchQuery.isBlank() ||
                mealWithFoods.meal.name.contains(state.searchQuery, ignoreCase = true) ||
                mealWithFoods.items.any { it.food.name.contains(state.searchQuery, ignoreCase = true) }
        }.sortedBy { it.meal.name.lowercase() }

        _uiState.update { it.copy(filteredMeals = filtered) }
    }

    // Filter diets based on search query
    fun getFilteredDiets(): List<DietWithMeals> {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return _uiState.value.diets

        return _uiState.value.diets.filter { dietWithMeals ->
            dietWithMeals.diet.name.contains(query, ignoreCase = true) ||
                    dietWithMeals.meals.values.any { mealWithFoods ->
                        mealWithFoods?.meal?.name?.contains(query, ignoreCase = true) == true
                    }
        }
    }
}
