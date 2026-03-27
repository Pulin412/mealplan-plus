package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietMealSlotUiState(
    val dietId: Long = 0,
    val slotType: String = "",
    val slotDisplayName: String = "",
    val dietName: String = "",
    val currentMeal: Meal? = null,
    val mealWithFoods: MealWithFoods? = null,
    val availableMeals: List<Meal> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DietMealSlotViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val dietId: Long = savedStateHandle.get<Long>("dietId") ?: 0L
    private val slotType: String = savedStateHandle.get<String>("slotType") ?: ""

    private val _uiState = MutableStateFlow(DietMealSlotUiState(dietId = dietId, slotType = slotType))
    val uiState: StateFlow<DietMealSlotUiState> = _uiState.asStateFlow()

    init {
        loadSlotData()
        loadAvailableMeals()
    }

    private fun loadSlotData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dietWithMeals = dietRepository.getDietWithMeals(dietId)
                val slot = DefaultMealSlot.entries.find { it.name == slotType }
                val mealWithFoods = dietWithMeals?.meals?.get(slotType)

                _uiState.update {
                    it.copy(
                        dietName = dietWithMeals?.diet?.name ?: "",
                        slotDisplayName = slot?.displayName ?: slotType,
                        currentMeal = mealWithFoods?.meal,
                        mealWithFoods = mealWithFoods,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadAvailableMeals() {
        viewModelScope.launch {
            mealRepository.getMealsByUser().collect { meals ->
                _uiState.update { it.copy(availableMeals = meals) }
            }
        }
    }

    fun changeMeal(meal: Meal?) {
        viewModelScope.launch {
            try {
                if (meal != null) {
                    dietRepository.setMealForSlot(dietId, slotType, meal.id)
                } else {
                    dietRepository.removeMealFromSlot(dietId, slotType)
                }
                loadSlotData() // Reload to get updated data
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun changeMealById(mealId: Long) {
        viewModelScope.launch {
            try {
                dietRepository.setMealForSlot(dietId, slotType, mealId)
                loadSlotData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addFoodById(foodId: Long, quantity: Double, unit: com.mealplanplus.data.model.FoodUnit = com.mealplanplus.data.model.FoodUnit.GRAM) {
        val currentMeal = _uiState.value.currentMeal ?: return
        viewModelScope.launch {
            try {
                // Verify food exists
                foodRepository.getFoodById(foodId) ?: return@launch
                // Check if food already exists in meal
                val existingItems = mealRepository.getMealFoodItems(currentMeal.id)
                val existing = existingItems.find { it.foodId == foodId }

                if (existing != null) {
                    // Update quantity
                    mealRepository.updateMealFoodItem(
                        MealFoodItem(currentMeal.id, foodId, quantity, unit)
                    )
                } else {
                    // Add new
                    mealRepository.addFoodToMeal(currentMeal.id, foodId, quantity, unit)
                }
                loadSlotData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateFoodQuantity(foodId: Long, quantity: Double) {
        val currentMeal = _uiState.value.currentMeal ?: return
        viewModelScope.launch {
            try {
                val existingItems = mealRepository.getMealFoodItems(currentMeal.id)
                val existing = existingItems.find { it.foodId == foodId } ?: return@launch
                mealRepository.updateMealFoodItem(
                    existing.copy(quantity = quantity)
                )
                loadSlotData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeFood(foodId: Long) {
        val currentMeal = _uiState.value.currentMeal ?: return
        viewModelScope.launch {
            try {
                mealRepository.removeFoodFromMeal(currentMeal.id, foodId)
                loadSlotData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addUsdaFood(usdaFood: UsdaFoodResult, quantity: Double, unit: com.mealplanplus.data.model.FoodUnit = com.mealplanplus.data.model.FoodUnit.GRAM) {
        val currentMeal = _uiState.value.currentMeal ?: return
        viewModelScope.launch {
            try {
                val foodItem = usdaFood.toFoodItem()
                val id = foodRepository.insertFood(foodItem)
                mealRepository.addFoodToMeal(currentMeal.id, id, quantity, unit)
                loadSlotData()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
