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
        val foods: List<MealFoodItemWithDetails> = emptyList(),
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
                        foods = foods,
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
}
