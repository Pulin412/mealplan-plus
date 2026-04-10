package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.MealFoodItemWithDetails
import com.mealplanplus.data.repository.DietRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class IngredientSortOrder { ALPHABETICAL, QUANTITY }

@HiltViewModel
class MealDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dietRepository: DietRepository
) : ViewModel() {

    private val dietId: Long = savedStateHandle.get<Long>("dietId") ?: 0L
    private val slotType: String = savedStateHandle.get<String>("slotType") ?: ""

    data class UiState(
        val slotLabel: String = "",
        val instructions: String = "",
        val allFoods: List<MealFoodItemWithDetails> = emptyList(),
        val sortedFoods: List<MealFoodItemWithDetails> = emptyList(),
        val checkedFoodIds: Set<Long> = emptySet(),
        val sortOrder: IngredientSortOrder = IngredientSortOrder.QUANTITY,
        val sortAscending: Boolean = true,
        val totalCalories: Double = 0.0,
        val totalProtein: Double = 0.0,
        val totalCarbs: Double = 0.0,
        val totalFat: Double = 0.0,
        val isLoading: Boolean = true,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun toggleChecked(foodId: Long) {
        _uiState.update { state ->
            val updated = if (foodId in state.checkedFoodIds)
                state.checkedFoodIds - foodId
            else
                state.checkedFoodIds + foodId
            state.copy(checkedFoodIds = updated)
        }
    }

    fun setSortOrder(order: IngredientSortOrder) {
        _uiState.update { state ->
            val newAscending = if (state.sortOrder == order) !state.sortAscending else true
            state.copy(
                sortOrder = order,
                sortAscending = newAscending,
                sortedFoods = state.allFoods.sorted(order, newAscending)
            )
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dietWithMeals = dietRepository.getDietWithMeals(dietId)
                val mealWithFoods = dietWithMeals?.meals?.get(slotType)
                val instructions = dietWithMeals?.instructions?.get(slotType) ?: ""
                val slot = DefaultMealSlot.entries.find { it.name == slotType }
                val foods = mealWithFoods?.items ?: emptyList()
                _uiState.update {
                    it.copy(
                        slotLabel = slot?.displayName ?: slotType,
                        instructions = instructions,
                        allFoods = foods,
                        sortedFoods = foods.sorted(IngredientSortOrder.QUANTITY, ascending = true),
                        totalCalories = foods.sumOf { f -> f.calculatedCalories },
                        totalProtein = foods.sumOf { f -> f.calculatedProtein },
                        totalCarbs = foods.sumOf { f -> f.calculatedCarbs },
                        totalFat = foods.sumOf { f -> f.calculatedFat },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun List<MealFoodItemWithDetails>.sorted(order: IngredientSortOrder, ascending: Boolean) =
        when (order) {
            IngredientSortOrder.ALPHABETICAL ->
                if (ascending) sortedBy { it.food.name.lowercase() }
                else sortedByDescending { it.food.name.lowercase() }
            IngredientSortOrder.QUANTITY ->
                if (ascending) sortedBy { it.mealFoodItem.quantity }
                else sortedByDescending { it.mealFoodItem.quantity }
        }
}
