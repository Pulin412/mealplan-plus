package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDietUiState(
    val name: String = "",
    val description: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),
    val slotMeals: Map<DefaultMealSlot, Meal?> = DefaultMealSlot.entries.associateWith { null },
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val newTagName: String = ""
)

@HiltViewModel
class AddDietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDietUiState())
    val uiState: StateFlow<AddDietUiState> = _uiState.asStateFlow()

    val availableMeals: StateFlow<List<Meal>> = mealRepository.getMealsByUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun setMealForSlot(slot: DefaultMealSlot, meal: Meal?) {
        val current = _uiState.value.slotMeals.toMutableMap()
        current[slot] = meal
        _uiState.value = _uiState.value.copy(slotMeals = current)
    }

    fun setMealForSlotById(slot: DefaultMealSlot, mealId: Long) {
        viewModelScope.launch {
            val meal = mealRepository.getMealById(mealId)
            if (meal != null) {
                setMealForSlot(slot, meal)
            }
        }
    }

    fun toggleTag(tagId: Long) {
        val currentTags = _uiState.value.selectedTagIds
        _uiState.value = _uiState.value.copy(
            selectedTagIds = if (tagId in currentTags) currentTags - tagId else currentTags + tagId
        )
    }

    fun updateNewTagName(name: String) {
        _uiState.value = _uiState.value.copy(newTagName = name)
    }

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
                // Tag might already exist
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
                    userId = 0, // Will be set by repository
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim()
                )
                val dietId = dietRepository.insertDiet(diet)

                // Add meal assignments
                state.slotMeals.forEach { (slot, meal) ->
                    if (meal != null) {
                        dietRepository.setMealForSlot(dietId, slot.name, meal.id)
                    }
                }

                // Add tag assignments
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
