package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddFoodUiState(
    val name: String = "",
    val brand: String = "",
    val servingSize: String = "",
    val servingUnit: String = "g",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val glycemicIndex: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    fun updateName(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun updateBrand(value: String) { _uiState.value = _uiState.value.copy(brand = value) }
    fun updateServingSize(value: String) { _uiState.value = _uiState.value.copy(servingSize = value) }
    fun updateServingUnit(value: String) { _uiState.value = _uiState.value.copy(servingUnit = value) }
    fun updateCalories(value: String) { _uiState.value = _uiState.value.copy(calories = value) }
    fun updateProtein(value: String) { _uiState.value = _uiState.value.copy(protein = value) }
    fun updateCarbs(value: String) { _uiState.value = _uiState.value.copy(carbs = value) }
    fun updateFat(value: String) { _uiState.value = _uiState.value.copy(fat = value) }
    fun updateGlycemicIndex(value: String) { _uiState.value = _uiState.value.copy(glycemicIndex = value) }

    fun saveFood() {
        val state = _uiState.value

        // Validation
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Name is required")
            return
        }
        if (state.servingSize.isBlank() || state.servingSize.toDoubleOrNull() == null) {
            _uiState.value = state.copy(error = "Valid serving size required")
            return
        }
        if (state.calories.isBlank() || state.calories.toDoubleOrNull() == null) {
            _uiState.value = state.copy(error = "Valid calories required")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val food = FoodItem(
                    name = state.name.trim(),
                    brand = state.brand.takeIf { it.isNotBlank() }?.trim(),
                    servingSize = state.servingSize.toDouble(),
                    servingUnit = state.servingUnit,
                    calories = state.calories.toDoubleOrNull() ?: 0.0,
                    protein = state.protein.toDoubleOrNull() ?: 0.0,
                    carbs = state.carbs.toDoubleOrNull() ?: 0.0,
                    fat = state.fat.toDoubleOrNull() ?: 0.0,
                    glycemicIndex = state.glycemicIndex.toIntOrNull()
                )
                repository.insertFood(food)
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
