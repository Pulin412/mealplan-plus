package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoodsViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val foods: StateFlow<List<FoodItem>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.getAllFoods()
            } else {
                repository.searchFoods(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }
}
