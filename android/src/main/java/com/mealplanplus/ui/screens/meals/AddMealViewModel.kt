package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.AuthRepository
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
    // Unified search state (hybrid: local + USDA)
    val searchQuery: String = "",
    val localResults: List<FoodItem> = emptyList(),
    val usdaResults: List<UsdaFoodResult> = emptyList(),
    val isSearchingUsda: Boolean = false,
    val searchError: String? = null,
    // Legacy USDA search state (for modal tab)
    val usdaSearchQuery: String = "",
    val usdaSearchResults: List<UsdaFoodResult> = emptyList(),
    val usdaSearchError: String? = null
)

data class SelectedFood(
    val food: FoodItem,
    val quantity: Double = 100.0,  // Default 100g
    val unit: FoodUnit = FoodUnit.GRAM
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
    private val usdaRepository: UsdaFoodRepository,
    private val authRepository: AuthRepository
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

    fun addFoodWithQuantity(food: FoodItem, quantity: Double, unit: FoodUnit = FoodUnit.GRAM) {
        val current = _uiState.value.selectedFoods
        val existing = current.find { it.food.id == food.id }
        if (existing != null) {
            // Update quantity if already added
            updateFoodQuantity(food.id, quantity, unit)
        } else {
            _uiState.update { it.copy(selectedFoods = current + SelectedFood(food, quantity, unit)) }
        }
    }

    fun addFoodById(foodId: Long, quantity: Double, unit: FoodUnit = FoodUnit.GRAM) {
        viewModelScope.launch {
            foodRepository.getFoodById(foodId)?.let { food ->
                addFoodWithQuantity(food, quantity, unit)
            }
        }
    }

    fun removeFood(foodId: Long) {
        _uiState.update { it.copy(selectedFoods = it.selectedFoods.filter { sf -> sf.food.id != foodId }) }
    }

    fun updateFoodQuantity(foodId: Long, quantity: Double, unit: FoodUnit? = null) {
        _uiState.update { state ->
            state.copy(selectedFoods = state.selectedFoods.map {
                if (it.food.id == foodId) {
                    if (unit != null) it.copy(quantity = quantity, unit = unit)
                    else it.copy(quantity = quantity)
                } else it
            })
        }
    }

    // Hybrid Search: local first, USDA in background
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        hybridSearch(query)
    }

    private fun hybridSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update { it.copy(localResults = emptyList(), usdaResults = emptyList(), isSearchingUsda = false) }
            return
        }

        // Instant local search
        val allFoods = availableFoods.value
        val localMatches = allFoods.filter { it.name.contains(trimmed, ignoreCase = true) }
        _uiState.update { it.copy(localResults = localMatches) }

        // Background USDA search
        _uiState.update { it.copy(isSearchingUsda = true, searchError = null) }
        viewModelScope.launch {
            val result = usdaRepository.searchFoods(trimmed)
            result.fold(
                onSuccess = { foods ->
                    // Filter out foods already in local results
                    val localNames = localMatches.map { it.name.lowercase() }.toSet()
                    val uniqueUsda = foods.filter { it.name.lowercase() !in localNames }
                    _uiState.update { it.copy(isSearchingUsda = false, usdaResults = uniqueUsda) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSearchingUsda = false, searchError = e.message) }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "", localResults = emptyList(), usdaResults = emptyList(), isSearchingUsda = false, searchError = null) }
    }

    // USDA Search functions (for modal tab)
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
        addUsdaFoodWithQuantity(usdaFood, 1.0)
    }

    fun addUsdaFoodWithQuantity(usdaFood: UsdaFoodResult, quantity: Double) {
        viewModelScope.launch {
            // Save USDA food to local DB and add to meal
            val foodItem = usdaFood.toFoodItem()
            val id = foodRepository.insertFood(foodItem)
            val savedFood = foodItem.copy(id = id)

            val current = _uiState.value.selectedFoods
            val existing = current.find { it.food.id == id }
            if (existing != null) {
                updateFoodQuantity(id, quantity)
            } else {
                _uiState.update { it.copy(selectedFoods = current + SelectedFood(savedFood, quantity)) }
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
                val userId = authRepository.getCurrentUserId().first() ?: return@launch
                val meal = Meal(
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim(),
                    userId = userId
                )
                val mealId = mealRepository.insertMeal(meal)

                // Add food items with unit
                state.selectedFoods.forEach { sf ->
                    mealRepository.addFoodToMeal(mealId, sf.food.id, sf.quantity, sf.unit)
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
