package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.UsdaFoodRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FoodPickerUiState(
    val searchQuery: String = "",
    val localResults: List<FoodItem> = emptyList(),
    val usdaResults: List<UsdaFoodResult> = emptyList(),
    val isSearchingUsda: Boolean = false,
    val hasSearchedUsda: Boolean = false,
    val searchError: String? = null
)

@HiltViewModel
class FoodPickerViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val usdaRepository: UsdaFoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodPickerUiState())
    val uiState: StateFlow<FoodPickerUiState> = _uiState.asStateFlow()

    val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, hasSearchedUsda = false, usdaResults = emptyList()) }
        searchLocal(query)
    }

    private fun searchLocal(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _uiState.update {
                it.copy(localResults = emptyList(), usdaResults = emptyList(), searchError = null)
            }
            return
        }

        // Local search only
        val localMatches = allFoods.value.filter {
            it.name.contains(trimmed, ignoreCase = true)
        }
        _uiState.update { it.copy(localResults = localMatches) }
    }

    // Manual USDA search triggered by button
    fun searchUsda() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < 2) return

        _uiState.update { it.copy(isSearchingUsda = true, searchError = null) }

        viewModelScope.launch {
            val result = usdaRepository.searchFoods(query)
            result.fold(
                onSuccess = { foods ->
                    val localNames = _uiState.value.localResults.map { it.name.lowercase() }.toSet()
                    val uniqueUsda = foods.filter { it.name.lowercase() !in localNames }
                    _uiState.update {
                        it.copy(
                            isSearchingUsda = false,
                            hasSearchedUsda = true,
                            usdaResults = uniqueUsda
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSearchingUsda = false,
                            hasSearchedUsda = true,
                            searchError = e.message ?: "Search failed"
                        )
                    }
                }
            )
        }
    }
}
