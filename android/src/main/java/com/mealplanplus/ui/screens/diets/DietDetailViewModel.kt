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
    val customSlotTypes: List<String> = emptyList(), // "CUSTOM:<name>" entries for this diet
    val availableMeals: List<Meal> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val dietTagIds: Set<Long> = emptySet(),
    val selectedTagIds: Set<Long> = emptySet(),
    val currentPickingSlotType: String? = null, // unified: DefaultMealSlot.name OR "CUSTOM:<name>"
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val editSlotInstructions: Map<String, String> = emptyMap(),
    val isSaved: Boolean = false,
    val error: String? = null,
    val newTagName: String = ""
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
                val customSlotTypes = dietWithMeals?.meals?.keys
                    ?.filter { it.startsWith("CUSTOM:") }
                    ?.sorted() ?: emptyList()
                _uiState.update {
                    it.copy(
                        diet = dietWithMeals?.diet,
                        dietWithMeals = dietWithMeals,
                        customSlotTypes = customSlotTypes,
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

    /** Works for both DefaultMealSlot names and "CUSTOM:<name>" slot types */
    fun setPickingSlot(slot: DefaultMealSlot) = setPickingSlotType(slot.name)
    fun setPickingSlotType(slotType: String) {
        _uiState.update { it.copy(currentPickingSlotType = slotType) }
    }

    fun addFoodById(foodId: Long, quantity: Double, unit: FoodUnit = FoodUnit.GRAM) {
        val slotType = _uiState.value.currentPickingSlotType ?: return
        viewModelScope.launch {
            try {
                val mealId = getOrCreateMealForSlotType(slotType)
                val existing = mealRepository.getMealFoodItems(mealId).find { it.foodId == foodId }
                if (existing != null) {
                    mealRepository.updateMealFoodItem(MealFoodItem(mealId, foodId, quantity, unit))
                } else {
                    mealRepository.addFoodToMeal(mealId, foodId, quantity, unit)
                }
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addUsdaFood(usdaFood: UsdaFoodResult, quantity: Double, unit: FoodUnit = FoodUnit.GRAM) {
        val slotType = _uiState.value.currentPickingSlotType ?: return
        viewModelScope.launch {
            try {
                val foodItem = usdaFood.toFoodItem()
                val id = foodRepository.insertFood(foodItem)
                val mealId = getOrCreateMealForSlotType(slotType)
                mealRepository.addFoodToMeal(mealId, id, quantity, unit)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun getOrCreateMealForSlotType(slotType: String): Long {
        val existingMeal = _uiState.value.dietWithMeals?.meals?.get(slotType)
        return if (existingMeal != null) {
            existingMeal.meal.id
        } else {
            val displayName = DefaultMealSlot.fromString(slotType)?.displayName
                ?: if (slotType.startsWith("CUSTOM:")) slotType.removePrefix("CUSTOM:")
                else slotType
            val meal = Meal(name = displayName)
            val mealId = mealRepository.insertMeal(meal)
            dietRepository.setMealForSlot(dietId, slotType, mealId)
            mealId
        }
    }

    fun removeFood(slot: DefaultMealSlot, item: MealFoodItemWithDetails) =
        removeFoodFromSlot(slot.name, item)

    fun removeFoodFromSlot(slotType: String, item: MealFoodItemWithDetails) {
        viewModelScope.launch {
            try {
                val mealId = _uiState.value.dietWithMeals?.meals?.get(slotType)?.meal?.id ?: return@launch
                mealRepository.removeFoodFromMeal(mealId, item.mealFoodItem.foodId)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun incrementQty(slot: DefaultMealSlot, item: MealFoodItemWithDetails) =
        updateFoodQty(slot.name, item, item.mealFoodItem.quantity + 1.0)

    fun decrementQty(slot: DefaultMealSlot, item: MealFoodItemWithDetails) =
        updateFoodQty(slot.name, item, (item.mealFoodItem.quantity - 1.0).coerceAtLeast(1.0))

    fun incrementQtyInSlot(slotType: String, item: MealFoodItemWithDetails) =
        updateFoodQty(slotType, item, item.mealFoodItem.quantity + 1.0)

    fun decrementQtyInSlot(slotType: String, item: MealFoodItemWithDetails) =
        updateFoodQty(slotType, item, (item.mealFoodItem.quantity - 1.0).coerceAtLeast(1.0))

    private fun updateFoodQty(slotType: String, item: MealFoodItemWithDetails, newQty: Double) {
        viewModelScope.launch {
            try {
                mealRepository.updateMealFoodItem(item.mealFoodItem.copy(quantity = newQty))
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ── Custom slot management ──────────────────────────────────────────────

    fun addCustomSlot(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val slotType = "CUSTOM:$trimmed"
        if (_uiState.value.customSlotTypes.contains(slotType)) return
        viewModelScope.launch {
            try {
                dietRepository.setMealForSlot(dietId, slotType, null)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeCustomSlot(slotType: String) {
        viewModelScope.launch {
            try {
                dietRepository.removeMealFromSlot(dietId, slotType)
                loadDiet()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateSlotInstructionsForType(slotType: String, text: String) {
        _uiState.update { state ->
            state.copy(editSlotInstructions = state.editSlotInstructions + (slotType to text))
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                selectedTagIds = it.dietTagIds,
                editSlotInstructions = it.dietWithMeals?.instructions
                    ?.mapValues { (_, v) -> v ?: "" } ?: emptyMap()
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                selectedTagIds = it.dietTagIds,
                editSlotInstructions = it.dietWithMeals?.instructions
                    ?.mapValues { (_, v) -> v ?: "" } ?: emptyMap()
            )
        }
    }

    fun updateSlotInstructions(slot: DefaultMealSlot, text: String) =
        updateSlotInstructionsForType(slot.name, text)

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
                val instrs = _uiState.value.editSlotInstructions
                for ((slotType, text) in instrs) {
                    dietRepository.updateSlotInstructions(
                        dietId, slotType, text.trim().takeIf { it.isNotBlank() }
                    )
                }
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

    fun updateNewTagName(name: String) { _uiState.update { it.copy(newTagName = name) } }

    fun createNewTagAndSelect() {
        val name = _uiState.value.newTagName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val newId = tagRepository.createTag(name)
                _uiState.update { state ->
                    state.copy(
                        newTagName = "",
                        selectedTagIds = state.selectedTagIds + newId
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

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
