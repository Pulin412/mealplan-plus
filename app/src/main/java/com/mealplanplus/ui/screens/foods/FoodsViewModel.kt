package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoodTab { ALL, FAVORITES, RECENT }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodsViewModel @Inject constructor(
    private val repository: FoodRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(FoodTab.ALL)
    val selectedTab: StateFlow<FoodTab> = _selectedTab.asStateFlow()

    val foods: StateFlow<List<FoodItem>> = combine(
        _searchQuery,
        _selectedTab
    ) { query, tab ->
        Pair(query, tab)
    }.flatMapLatest { (query, tab) ->
        when {
            query.isNotBlank() -> repository.searchFoods(query)
            tab == FoodTab.FAVORITES -> repository.getFavorites()
            tab == FoodTab.RECENT -> repository.getRecentFoods()
            else -> repository.getAllFoods()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectTab(tab: FoodTab) {
        _selectedTab.value = tab
    }

    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch {
            repository.setFavorite(food.id, !food.isFavorite)
        }
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }
}
