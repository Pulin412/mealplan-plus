package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditMealUiState(
    val mealId: Long = 0,
    val name: String = "",
    val description: String = "",
    val selectedSlot: DefaultMealSlot = DefaultMealSlot.BREAKFAST,
    val selectedFoods: List<SelectedFood> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class EditMealViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: 0L

    private val _uiState = MutableStateFlow(EditMealUiState(mealId = mealId))
    val uiState: StateFlow<EditMealUiState> = _uiState.asStateFlow()

    init {
        loadMeal()
    }

    private fun loadMeal() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val mealWithFoods = mealRepository.getMealWithFoods(mealId)
                if (mealWithFoods != null) {
                    val selectedFoods = mealWithFoods.items.map { item ->
                        SelectedFood(item.food, item.mealFoodItem.quantity)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            name = mealWithFoods.meal.name,
                            description = mealWithFoods.meal.description ?: "",
                            selectedSlot = mealWithFoods.meal.defaultSlot ?: DefaultMealSlot.BREAKFAST,
                            selectedFoods = selectedFoods
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Meal not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateSlot(slot: DefaultMealSlot) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun addFoodById(foodId: Long, quantity: Double, unit: com.mealplanplus.data.model.FoodUnit = com.mealplanplus.data.model.FoodUnit.GRAM) {
        viewModelScope.launch {
            foodRepository.getFoodById(foodId)?.let { food ->
                val current = _uiState.value.selectedFoods
                val existing = current.find { it.food.id == food.id }
                if (existing != null) {
                    updateFoodQuantity(food.id, quantity)
                } else {
                    _uiState.update { it.copy(selectedFoods = current + SelectedFood(food, quantity, unit)) }
                }
            }
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

    fun saveMeal() {
        val state = _uiState.value

        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Update meal
                val meal = Meal(
                    id = mealId,
                    userId = 0L,  // Will be overwritten by repository
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim(),
                    slotType = state.selectedSlot.name
                )
                mealRepository.updateMeal(meal)

                // Update food items
                val items = state.selectedFoods.map { sf ->
                    MealFoodItem(mealId, sf.food.id, sf.quantity)
                }
                mealRepository.updateMealFoods(mealId, items)

                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    fun addUsdaFood(usdaFood: UsdaFoodResult, quantity: Double, unit: com.mealplanplus.data.model.FoodUnit = com.mealplanplus.data.model.FoodUnit.GRAM) {
        viewModelScope.launch {
            val foodItem = usdaFood.toFoodItem()
            val id = foodRepository.insertFood(foodItem)
            val savedFood = foodItem.copy(id = id)

            val current = _uiState.value.selectedFoods
            val existing = current.find { it.food.id == id }
            if (existing != null) {
                updateFoodQuantity(id, quantity)
            } else {
                _uiState.update { it.copy(selectedFoods = current + SelectedFood(savedFood, quantity, unit)) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
