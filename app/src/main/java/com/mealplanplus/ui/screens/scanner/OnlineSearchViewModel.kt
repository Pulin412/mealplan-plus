package com.mealplanplus.ui.screens.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.OpenFoodFactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnlineSearchUiState(
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val results: List<FoodItem> = emptyList(),
    val error: String? = null,
    val savedFoodId: Long? = null
)

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val openFoodFactsRepository: OpenFoodFactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnlineSearchUiState())
    val uiState: StateFlow<OnlineSearchUiState> = _uiState.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < 2) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val result = openFoodFactsRepository.searchProducts(query)
            result.fold(
                onSuccess = { foods ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            results = foods,
                            error = if (foods.isEmpty()) "No products found" else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isLoading = false, error = e.message ?: "Search failed")
                    }
                }
            )
        }
    }

    fun saveFood(food: FoodItem) {
        viewModelScope.launch {
            val id = foodRepository.insertFood(food)
            _uiState.update { it.copy(savedFoodId = id) }
        }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(savedFoodId = null) }
    }
}
