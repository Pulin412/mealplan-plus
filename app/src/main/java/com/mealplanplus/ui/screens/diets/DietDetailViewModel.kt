package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
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

data class DietDetailUiState(
    val diet: Diet? = null,
    val dietWithMeals: DietWithMeals? = null,
    val availableMeals: List<Meal> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val dietTagIds: Set<Long> = emptySet(),
    val selectedTagIds: Set<Long> = emptySet(),
    val currentPickingSlot: DefaultMealSlot? = null,
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DietDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository,
    private val tagRepository: TagRepository,
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val dietId: Long = savedStateHandle.get<Long>("dietId") ?: 0L

    private val _uiState = MutableStateFlow(DietDetailUiState())
    val uiState: StateFlow<DietDetailUiState> = _uiState.asStateFlow()

    init {
        loadDiet()
        loadTags()
    }

    private fun loadDiet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dietWithMeals = dietRepository.getDietWithMeals(dietId)
                val dietTags = dietRepository.getTagsForDiet(dietId)
                val tagIds = dietTags.map { it.id }.toSet()
                _uiState.update {
                    it.copy(
                        diet = dietWithMeals?.diet,
                        dietWithMeals = dietWithMeals,
                        editName = dietWithMeals?.diet?.name ?: "",
                        editDescription = dietWithMeals?.diet?.description ?: "",
                        dietTagIds = tagIds,
                        selectedTagIds = tagIds,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    fun setPickingSlot(slot: DefaultMealSlot) {
        _uiState.update { it.copy(currentPickingSlot = slot) }
    }

    /** Called when food picker returns with a local food id */
    fun addFoodById(foodId: Long, quantity: Double) {
        val slot = _uiState.value.currentPickingSlot ?: return
        viewModelScope.launch {
            try {
                val mealId = getOrCreateMealForSlot(slot)
                val existing = mealRepository.getMealFoodItems(mealId).find { it.foodId == foodId }
                if (existing != null) {
                    mealRepository.updateMealFoodItem(MealFoodItem(mealId, foodId, quantity, existing.unit))
                } else {
                    mealRepository.addFoodToMeal(mealId, foodId, quantity)
                }
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    /** Called when food picker returns with a USDA food */
    fun addUsdaFood(usdaFood: UsdaFoodResult, quantity: Double) {
        val slot = _uiState.value.currentPickingSlot ?: return
        viewModelScope.launch {
            try {
                val foodItem = usdaFood.toFoodItem()
                val id = foodRepository.insertFood(foodItem)
                val mealId = getOrCreateMealForSlot(slot)
                mealRepository.addFoodToMeal(mealId, id, quantity)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun getOrCreateMealForSlot(slot: DefaultMealSlot): Long {
        val existingMeal = _uiState.value.dietWithMeals?.meals?.get(slot.name)
        return if (existingMeal != null) {
            existingMeal.meal.id
        } else {
            val meal = Meal(userId = 0, name = slot.displayName, slotType = slot.name)
            val mealId = mealRepository.insertMeal(meal)
            dietRepository.setMealForSlot(dietId, slot.name, mealId)
            mealId
        }
    }

    fun removeFood(slot: DefaultMealSlot, item: MealFoodItemWithDetails) {
        viewModelScope.launch {
            try {
                val mealId = _uiState.value.dietWithMeals?.meals?.get(slot.name)?.meal?.id ?: return@launch
                mealRepository.removeFoodFromMeal(mealId, item.mealFoodItem.foodId)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun incrementQty(slot: DefaultMealSlot, item: MealFoodItemWithDetails) {
        updateFoodQty(slot, item, item.mealFoodItem.quantity + 1.0)
    }

    fun decrementQty(slot: DefaultMealSlot, item: MealFoodItemWithDetails) {
        val newQty = (item.mealFoodItem.quantity - 1.0).coerceAtLeast(1.0)
        updateFoodQty(slot, item, newQty)
    }

    private fun updateFoodQty(slot: DefaultMealSlot, item: MealFoodItemWithDetails, newQty: Double) {
        viewModelScope.launch {
            try {
                val updated = item.mealFoodItem.copy(quantity = newQty)
                mealRepository.updateMealFoodItem(updated)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                selectedTagIds = it.dietTagIds
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                selectedTagIds = it.dietTagIds
            )
        }
    }

    fun updateName(name: String) { _uiState.update { it.copy(editName = name) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(editDescription = desc) } }

    fun toggleTag(tagId: Long) {
        _uiState.update { state ->
            val tags = state.selectedTagIds.toMutableSet()
            if (tags.contains(tagId)) tags.remove(tagId) else tags.add(tagId)
            state.copy(selectedTagIds = tags)
        }
    }

    fun saveDiet() {
        val currentDiet = _uiState.value.diet ?: return
        val name = _uiState.value.editName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }
        viewModelScope.launch {
            try {
                val updatedDiet = currentDiet.copy(
                    name = name,
                    description = _uiState.value.editDescription.trim().takeIf { it.isNotBlank() }
                )
                dietRepository.updateDiet(updatedDiet)
                dietRepository.setDietTags(dietId, _uiState.value.selectedTagIds.toList())
                _uiState.update {
                    it.copy(
                        diet = updatedDiet,
                        dietTagIds = it.selectedTagIds,
                        isEditing = false
                    )
                }
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }

    fun deleteDiet(onDeleted: () -> Unit) {
        val diet = _uiState.value.diet ?: return
        viewModelScope.launch {
            try {
                dietRepository.deleteDiet(diet)
                onDeleted()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
