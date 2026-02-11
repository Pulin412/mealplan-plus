package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietDetailUiState(
    val diet: Diet? = null,
    val dietWithMeals: DietWithMeals? = null,
    val availableMeals: List<Meal> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val editTags: List<DietTag> = emptyList(),
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DietDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val dietId: Long = savedStateHandle.get<Long>("dietId") ?: 0L

    private val _uiState = MutableStateFlow(DietDetailUiState())
    val uiState: StateFlow<DietDetailUiState> = _uiState.asStateFlow()

    init {
        loadDiet()
        loadAvailableMeals()
    }

    private fun loadDiet() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val dietWithMeals = dietRepository.getDietWithMeals(dietId)
                _uiState.update {
                    it.copy(
                        diet = dietWithMeals?.diet,
                        dietWithMeals = dietWithMeals,
                        editName = dietWithMeals?.diet?.name ?: "",
                        editDescription = dietWithMeals?.diet?.description ?: "",
                        editTags = dietWithMeals?.diet?.getTagList() ?: emptyList(),
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
            mealRepository.getAllMeals().collect { meals ->
                _uiState.update { it.copy(availableMeals = meals) }
            }
        }
    }

    fun startEditing() {
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                editTags = it.diet?.getTagList() ?: emptyList()
            )
        }
    }

    fun cancelEditing() {
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = it.diet?.name ?: "",
                editDescription = it.diet?.description ?: "",
                editTags = it.diet?.getTagList() ?: emptyList()
            )
        }
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(editDescription = description) }
    }

    fun toggleTag(tag: DietTag) {
        _uiState.update { state ->
            val currentTags = state.editTags.toMutableList()
            if (currentTags.contains(tag)) {
                currentTags.remove(tag)
            } else {
                currentTags.add(tag)
            }
            state.copy(editTags = currentTags)
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
                val tagsString = _uiState.value.editTags.joinToString(",") { it.name }
                val updatedDiet = currentDiet.copy(
                    name = name,
                    description = _uiState.value.editDescription.trim().takeIf { it.isNotBlank() },
                    tags = tagsString
                )
                dietRepository.updateDiet(updatedDiet)
                _uiState.update {
                    it.copy(
                        diet = updatedDiet,
                        isEditing = false,
                        isSaved = true
                    )
                }
                loadDiet() // Reload to get fresh data
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setMealForSlot(slotType: String, meal: Meal?) {
        viewModelScope.launch {
            try {
                if (meal != null) {
                    dietRepository.setMealForSlot(dietId, slotType, meal.id)
                } else {
                    dietRepository.removeMealFromSlot(dietId, slotType)
                }
                loadDiet() // Reload to get updated data
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
