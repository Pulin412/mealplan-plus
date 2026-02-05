package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDietUiState(
    val name: String = "",
    val description: String = "",
    val slotMeals: Map<DefaultMealSlot, Meal?> = DefaultMealSlot.entries.associateWith { null },
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddDietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDietUiState())
    val uiState: StateFlow<AddDietUiState> = _uiState.asStateFlow()

    val availableMeals: StateFlow<List<Meal>> = mealRepository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
