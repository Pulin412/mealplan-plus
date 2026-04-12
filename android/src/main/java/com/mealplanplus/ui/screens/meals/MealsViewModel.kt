package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.util.naturalSortKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MealsUiState(
    val searchQuery: String = "",
    val filterSlot: String? = null,
    val showDietBrowser: Boolean = false,
    val allMeals: List<MealWithFoods> = emptyList(),
    val filteredMeals: List<MealWithFoods> = emptyList(),
    val diets: List<DietWithMeals> = emptyList(),
    /** mealId → set of slotTypes this meal appears in across all diets */
    val mealSlotTypes: Map<Long, Set<String>> = emptyMap(),
    val expandedDietId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MealsUiState())
    val uiState: StateFlow<MealsUiState> = _uiState.asStateFlow()

    val meals: StateFlow<List<Meal>> = mealRepository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedMeal = MutableStateFlow<MealWithFoods?>(null)
    val selectedMeal: StateFlow<MealWithFoods?> = _selectedMeal.asStateFlow()

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
                // Build mealId → set of slotTypes it appears in across all diet templates.
                // Used by applyFilters() to honour the BREAKFAST / LUNCH / DINNER chips.
                val mealSlotTypes = mutableMapOf<Long, MutableSet<String>>()
                for (dwm in dietsWithMeals) {
                    for ((slotType, mealWithFoods) in dwm.meals) {
                        val mealId = mealWithFoods?.meal?.id ?: continue
                        mealSlotTypes.getOrPut(mealId) { mutableSetOf() }.add(slotType)
                    }
                }
                _uiState.update { it.copy(diets = dietsWithMeals, mealSlotTypes = mealSlotTypes) }
                applyFilters()
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
            val matchesSearch = state.searchQuery.isBlank() ||
                mealWithFoods.meal.name.contains(state.searchQuery, ignoreCase = true) ||
                mealWithFoods.items.any { it.food.name.contains(state.searchQuery, ignoreCase = true) }

            val matchesSlot = state.filterSlot == null ||
                state.mealSlotTypes[mealWithFoods.meal.id]?.contains(state.filterSlot) == true

            matchesSearch && matchesSlot
        }.sortedBy { it.meal.name.lowercase() }

        _uiState.update { it.copy(filteredMeals = filtered) }
    }

    // Filter diets based on search query
    fun getFilteredDiets(): List<DietWithMeals> {
        val query = _uiState.value.searchQuery
        val diets = if (query.isBlank()) {
            _uiState.value.diets
        } else {
            _uiState.value.diets.filter { dietWithMeals ->
                dietWithMeals.diet.name.contains(query, ignoreCase = true) ||
                        dietWithMeals.meals.values.any { mealWithFoods ->
                            mealWithFoods?.meal?.name?.contains(query, ignoreCase = true) == true
                        }
            }
        }
        return diets.sortedWith(compareBy(
            { naturalSortKey(it.diet.name).first },
            { naturalSortKey(it.diet.name).second }
        ))
    }

    fun loadMealDetails(mealId: Long) {
        viewModelScope.launch {
            _selectedMeal.value = mealRepository.getMealWithFoods(mealId)
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            mealRepository.deleteMeal(meal)
        }
    }
}
