package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.TagRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDietUiState(
    val name: String = "",
    val description: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),
    val slotFoodItems: Map<DefaultMealSlot, List<MealFoodItemWithDetails>> =
        DefaultMealSlot.entries.associateWith { emptyList() },
    val currentPickingSlot: DefaultMealSlot? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val newTagName: String = ""
) {
    val estimatedCalories: Int get() = slotFoodItems.values.flatten().sumOf { it.calculatedCalories }.toInt()
    val estimatedProtein: Int get() = slotFoodItems.values.flatten().sumOf { it.calculatedProtein }.toInt()
    val estimatedCarbs: Int get() = slotFoodItems.values.flatten().sumOf { it.calculatedCarbs }.toInt()
    val estimatedFat: Int get() = slotFoodItems.values.flatten().sumOf { it.calculatedFat }.toInt()
}

@HiltViewModel
class AddDietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository,
    private val tagRepository: TagRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDietUiState())
    val uiState: StateFlow<AddDietUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _uiState.value = _uiState.value.copy(allTags = tags)
            }
        }
    }

    fun updateName(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun updateDescription(desc: String) { _uiState.value = _uiState.value.copy(description = desc) }

    fun setPickingSlot(slot: DefaultMealSlot) {
        _uiState.value = _uiState.value.copy(currentPickingSlot = slot)
    }

    fun addFoodById(foodId: Long, quantity: Double) {
        val slot = _uiState.value.currentPickingSlot ?: return
        viewModelScope.launch {
            val food = foodRepository.getFoodById(foodId) ?: return@launch
            val mfi = MealFoodItem(mealId = 0L, foodId = foodId, quantity = quantity, unit = FoodUnit.GRAM)
            addToSlot(slot, MealFoodItemWithDetails(mfi, food))
        }
    }

    fun addUsdaFoodToSlot(usdaFood: UsdaFoodResult, quantity: Double) {
        val slot = _uiState.value.currentPickingSlot ?: return
        viewModelScope.launch {
            val foodItem = usdaFood.toFoodItem()
            val id = foodRepository.insertFood(foodItem)
            val savedFood = foodRepository.getFoodById(id) ?: return@launch
            val mfi = MealFoodItem(mealId = 0L, foodId = id, quantity = quantity, unit = FoodUnit.GRAM)
            addToSlot(slot, MealFoodItemWithDetails(mfi, savedFood))
        }
    }

    private fun addToSlot(slot: DefaultMealSlot, item: MealFoodItemWithDetails) {
        val current = _uiState.value.slotFoodItems.toMutableMap()
        current[slot] = (current[slot] ?: emptyList()) + item
        _uiState.value = _uiState.value.copy(slotFoodItems = current)
    }

    fun removeFood(slot: DefaultMealSlot, index: Int) {
        val current = _uiState.value.slotFoodItems.toMutableMap()
        val list = current[slot]?.toMutableList() ?: return
        if (index in list.indices) list.removeAt(index)
        current[slot] = list
        _uiState.value = _uiState.value.copy(slotFoodItems = current)
    }

    fun incrementQty(slot: DefaultMealSlot, index: Int) = updateQty(slot, index, +1.0)
    fun decrementQty(slot: DefaultMealSlot, index: Int) = updateQty(slot, index, -1.0)

    private fun updateQty(slot: DefaultMealSlot, index: Int, delta: Double) {
        val current = _uiState.value.slotFoodItems.toMutableMap()
        val list = current[slot]?.toMutableList() ?: return
        if (index !in list.indices) return
        val item = list[index]
        val newQty = (item.mealFoodItem.quantity + delta).coerceAtLeast(1.0)
        list[index] = item.copy(mealFoodItem = item.mealFoodItem.copy(quantity = newQty))
        current[slot] = list
        _uiState.value = _uiState.value.copy(slotFoodItems = current)
    }

    fun toggleTag(tagId: Long) {
        val currentTags = _uiState.value.selectedTagIds
        _uiState.value = _uiState.value.copy(
            selectedTagIds = if (tagId in currentTags) currentTags - tagId else currentTags + tagId
        )
    }

    fun updateNewTagName(name: String) { _uiState.value = _uiState.value.copy(newTagName = name) }

    fun createAndSelectTag() {
        val name = _uiState.value.newTagName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val tagId = tagRepository.createTag(name)
                _uiState.value = _uiState.value.copy(
                    selectedTagIds = _uiState.value.selectedTagIds + tagId,
                    newTagName = ""
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Tag already exists")
            }
        }
    }

    fun saveDiet() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.value = state.copy(error = "Name is required")
            return
        }
        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val diet = Diet(
                    userId = 0,
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim()
                )
                val dietId = dietRepository.insertDiet(diet)

                // For each slot that has foods, create an implicit meal and wire it up
                state.slotFoodItems.forEach { (slot, foods) ->
                    if (foods.isNotEmpty()) {
                        val meal = Meal(
                            userId = 0,
                            name = slot.displayName,
                            slotType = slot.name
                        )
                        val mealId = mealRepository.insertMeal(meal)
                        foods.forEach { item ->
                            mealRepository.addFoodToMeal(
                                mealId,
                                item.mealFoodItem.foodId,
                                item.mealFoodItem.quantity,
                                item.mealFoodItem.unit
                            )
                        }
                        dietRepository.setMealForSlot(dietId, slot.name, mealId)
                    }
                }

                if (state.selectedTagIds.isNotEmpty()) {
                    dietRepository.setDietTags(dietId, state.selectedTagIds.toList())
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

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
}
