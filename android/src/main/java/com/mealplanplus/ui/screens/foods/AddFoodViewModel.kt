package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.UsdaFoodRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val error: String? = null,
    // USDA search state
    val showUsdaSearch: Boolean = false,
    val usdaSearchQuery: String = "",
    val usdaSearchResults: List<UsdaFoodResult> = emptyList(),
    val isSearchingUsda: Boolean = false,
    val usdaSearchError: String? = null
)

@HiltViewModel
class AddFoodViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val usdaRepository: UsdaFoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFoodUiState())
    val uiState: StateFlow<AddFoodUiState> = _uiState.asStateFlow()

    fun updateName(value: String) { _uiState.update { it.copy(name = value) } }
    fun updateBrand(value: String) { _uiState.update { it.copy(brand = value) } }
    fun updateServingSize(value: String) { _uiState.update { it.copy(servingSize = value) } }
    fun updateServingUnit(value: String) { _uiState.update { it.copy(servingUnit = value) } }
    fun updateCalories(value: String) { _uiState.update { it.copy(calories = value) } }
    fun updateProtein(value: String) { _uiState.update { it.copy(protein = value) } }
    fun updateCarbs(value: String) { _uiState.update { it.copy(carbs = value) } }
    fun updateFat(value: String) { _uiState.update { it.copy(fat = value) } }
    fun updateGlycemicIndex(value: String) { _uiState.update { it.copy(glycemicIndex = value) } }

    // USDA Search functions
    fun showUsdaSearch() {
        _uiState.update { it.copy(showUsdaSearch = true, usdaSearchQuery = "", usdaSearchResults = emptyList()) }
    }

    fun hideUsdaSearch() {
        _uiState.update { it.copy(showUsdaSearch = false, usdaSearchError = null) }
    }

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

    /** Pre-fill all form fields from a scanned/found FoodItem (e.g. barcode scan result). */
    fun prefillFromFood(food: FoodItem) {
        _uiState.update {
            it.copy(
                name = food.name,
                brand = food.brand ?: "",
                servingSize = "100",
                servingUnit = "g",
                calories = food.caloriesPer100.toInt().toString(),
                protein = food.proteinPer100.toInt().toString(),
                carbs = food.carbsPer100.toInt().toString(),
                fat = food.fatPer100.toInt().toString(),
                glycemicIndex = food.glycemicIndex?.toString() ?: ""
            )
        }
    }

    fun copyFromUsda(food: UsdaFoodResult) {
        _uiState.update {
            it.copy(
                showUsdaSearch = false,
                name = food.name,
                brand = food.brand ?: "",
                servingSize = food.servingSize.toString(),
                servingUnit = food.servingUnit,
                calories = food.calories.toInt().toString(),
                protein = food.protein.toInt().toString(),
                carbs = food.carbs.toInt().toString(),
                fat = food.fat.toInt().toString()
            )
        }
    }

    fun saveFood() {
        val state = _uiState.value

        // Validation
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }
        if (state.servingSize.isBlank() || state.servingSize.toDoubleOrNull() == null) {
            _uiState.update { it.copy(error = "Valid serving size required") }
            return
        }
        if (state.calories.isBlank() || state.calories.toDoubleOrNull() == null) {
            _uiState.update { it.copy(error = "Valid calories required") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // User enters per-serving values, convert to per-100g
                val serving = state.servingSize.toDoubleOrNull() ?: 100.0
                val factor = if (serving > 0) 100.0 / serving else 1.0
                val food = FoodItem(
                    name = state.name.trim(),
                    brand = state.brand.takeIf { it.isNotBlank() }?.trim(),
                    caloriesPer100 = (state.calories.toDoubleOrNull() ?: 0.0) * factor,
                    proteinPer100 = (state.protein.toDoubleOrNull() ?: 0.0) * factor,
                    carbsPer100 = (state.carbs.toDoubleOrNull() ?: 0.0) * factor,
                    fatPer100 = (state.fat.toDoubleOrNull() ?: 0.0) * factor,
                    gramsPerPiece = if (state.servingUnit != "g" && state.servingUnit != "ml") serving else null,
                    glycemicIndex = state.glycemicIndex.toIntOrNull()
                )
                repository.insertFood(food)
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
