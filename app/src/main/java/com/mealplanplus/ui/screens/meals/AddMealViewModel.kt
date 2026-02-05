package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.UsdaFoodRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddMealUiState(
    val name: String = "",
    val description: String = "",
    val selectedSlot: DefaultMealSlot = DefaultMealSlot.BREAKFAST,
    val selectedFoods: List<SelectedFood> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    // USDA search state
    val usdaSearchQuery: String = "",
    val usdaSearchResults: List<UsdaFoodResult> = emptyList(),
    val isSearchingUsda: Boolean = false,
    val usdaSearchError: String? = null
)

data class SelectedFood(
    val food: FoodItem,
    val quantity: Double = 1.0
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
    private val usdaRepository: UsdaFoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState.asStateFlow()

    val availableFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateSlot(slot: DefaultMealSlot) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun addFood(food: FoodItem) {
        val current = _uiState.value.selectedFoods
        if (current.none { it.food.id == food.id }) {
            _uiState.update { it.copy(selectedFoods = current + SelectedFood(food)) }
        }
    }

    fun removeFood(foodId: Long) {
        _uiState.update { it.copy(selectedFoods = it.selectedFoods.filter { sf -> sf.food.id != foodId }) }
    }

    fun updateFoodQuantity(foodId: Long, quantity: Double) {
        _uiState.update { state ->
            state.copy(selectedFoods = state.selectedFoods.map {
                if (it.food.id == foodId) it.copy(quantity = quantity) else it
            })
        }
    }

    // USDA Search functions
    fun updateUsdaSearchQuery(query: String) {
        _uiState.update { it.copy(usdaSearchQuery = query) }
    }

    fun searchUsda() {
        val query = _uiState.value.usdaSearchQuery.trim()
        if (query.length < 2) return

        _uiState.update { it.copy(isSearchingUsda = true, usdaSearchError = null) }

        viewModelScope.launch {
            val result = usdaRepository.searchFoods(query)
            result.fold(
                onSuccess = { foods ->
                    _uiState.update {
                        it.copy(
                            isSearchingUsda = false,
                            usdaSearchResults = foods,
                            usdaSearchError = if (foods.isEmpty()) "No foods found" else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSearchingUsda = false, usdaSearchError = e.message ?: "Search failed")
                    }
                }
            )
        }
    }

    fun addUsdaFood(usdaFood: UsdaFoodResult) {
        viewModelScope.launch {
            // Save USDA food to local DB and add to meal
            val foodItem = usdaFood.toFoodItem()
            val id = foodRepository.insertFood(foodItem)
            val savedFood = foodItem.copy(id = id)

            val current = _uiState.value.selectedFoods
            if (current.none { it.food.id == id }) {
                _uiState.update { it.copy(selectedFoods = current + SelectedFood(savedFood)) }
            }
        }
    }

    fun clearUsdaSearch() {
        _uiState.update { it.copy(usdaSearchQuery = "", usdaSearchResults = emptyList(), usdaSearchError = null) }
    }

    fun saveMeal() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val meal = Meal(
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim(),
                    slotType = state.selectedSlot.name
                )
                val mealId = mealRepository.insertMeal(meal)

                // Add food items
                state.selectedFoods.forEach { sf ->
                    mealRepository.addFoodToMeal(mealId, sf.food.id, sf.quantity)
                }

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
