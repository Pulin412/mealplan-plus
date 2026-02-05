package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
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
    val error: String? = null
)

data class SelectedFood(
    val food: FoodItem,
    val quantity: Double = 1.0
)

@HiltViewModel
class AddMealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddMealUiState())
    val uiState: StateFlow<AddMealUiState> = _uiState.asStateFlow()

    val availableFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateSlot(slot: DefaultMealSlot) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    fun addFood(food: FoodItem) {
        val current = _uiState.value.selectedFoods
        if (current.none { it.food.id == food.id }) {
            _uiState.value = _uiState.value.copy(
                selectedFoods = current + SelectedFood(food)
            )
        }
    }

    fun removeFood(foodId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedFoods = _uiState.value.selectedFoods.filter { it.food.id != foodId }
        )
    }

    fun updateFoodQuantity(foodId: Long, quantity: Double) {
        _uiState.value = _uiState.value.copy(
            selectedFoods = _uiState.value.selectedFoods.map {
                if (it.food.id == foodId) it.copy(quantity = quantity) else it
            }
        )
    }

    fun saveMeal() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Name is required")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

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

                _uiState.value = _uiState.value.copy(isLoading = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
