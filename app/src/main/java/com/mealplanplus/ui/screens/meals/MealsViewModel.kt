package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.repository.MealRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealsViewModel @Inject constructor(
    private val repository: MealRepository
) : ViewModel() {

    val meals: StateFlow<List<Meal>> = repository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedMeal = MutableStateFlow<MealWithFoods?>(null)
    val selectedMeal: StateFlow<MealWithFoods?> = _selectedMeal.asStateFlow()

    fun loadMealDetails(mealId: Long) {
        viewModelScope.launch {
            _selectedMeal.value = repository.getMealWithFoods(mealId)
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
        }
    }
}
