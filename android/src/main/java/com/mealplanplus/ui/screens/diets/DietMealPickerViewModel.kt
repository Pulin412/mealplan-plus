package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietMealPickerUiState(
    val slotType: String = "",
    val searchQuery: String = "",
    val filterSlot: String? = null,
    val filterTagIds: Set<Long> = emptySet(),
    val showDietBrowser: Boolean = false,
    val allMeals: List<MealWithFoods> = emptyList(),
    val filteredMeals: List<MealWithFoods> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val dietTagMap: Map<Long, List<Tag>> = emptyMap(),
    val diets: List<DietWithMeals> = emptyList(),
    val expandedDietId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class DietMealPickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val dietRepository: DietRepository,
    private val tagRepository: TagRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val slotType: String = savedStateHandle.get<String>("slotType") ?: ""

    private val _uiState = MutableStateFlow(DietMealPickerUiState(slotType = slotType, filterSlot = slotType))
    val uiState: StateFlow<DietMealPickerUiState> = _uiState.asStateFlow()

    init {
        loadMeals()
        loadDiets()
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    private fun loadMeals() {
        viewModelScope.launch {
            authRepository.getCurrentUserId()
                .filterNotNull()
                .flatMapLatest { uid -> mealRepository.getMealsForUser(uid) }
                .collect { meals ->
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
            authRepository.getCurrentUserId()
                .filterNotNull()
                .flatMapLatest { uid -> dietRepository.getDietsForUser(uid) }
                .collect { diets ->
                    val dietsWithMeals = diets.mapNotNull { diet ->
                        dietRepository.getDietWithMeals(diet.id)
                    }
                    val dietIds = diets.map { it.id }
                    val tagRows = if (dietIds.isNotEmpty()) tagRepository.getTagsForDiets(dietIds) else emptyList()
                    val dietTagMap = tagRows.groupBy({ it.dietId }, { it.toTag() })
                    _uiState.update { it.copy(diets = dietsWithMeals, dietTagMap = dietTagMap) }
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

    fun toggleTagFilter(tagId: Long) {
        _uiState.update { state ->
            val ids = state.filterTagIds.toMutableSet()
            if (ids.contains(tagId)) ids.remove(tagId) else ids.add(tagId)
            state.copy(filterTagIds = ids)
        }
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

    fun getFilteredDiets(): List<DietWithMeals> {
        val state = _uiState.value
        return state.diets.filter { dietWithMeals ->
            val matchesSearch = state.searchQuery.isBlank() ||
                dietWithMeals.diet.name.contains(state.searchQuery, ignoreCase = true) ||
                dietWithMeals.meals.values.any { mwf ->
                    mwf?.meal?.name?.contains(state.searchQuery, ignoreCase = true) == true
                }
            val matchesTags = state.filterTagIds.isEmpty() ||
                (state.dietTagMap[dietWithMeals.diet.id]?.any { it.id in state.filterTagIds } == true)
            matchesSearch && matchesTags
        }
    }
}
