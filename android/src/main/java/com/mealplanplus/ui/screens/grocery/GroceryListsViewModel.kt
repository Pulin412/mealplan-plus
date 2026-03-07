package com.mealplanplus.ui.screens.grocery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.data.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryListsUiState(
    val lists: List<GroceryListItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class GroceryListItem(
    val list: GroceryList,
    val itemCount: Int = 0,
    val checkedCount: Int = 0
) {
    val progressPercent: Float
        get() = if (itemCount > 0) checkedCount.toFloat() / itemCount else 0f
}

@HiltViewModel
class GroceryListsViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroceryListsUiState())
    val uiState: StateFlow<GroceryListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    private fun loadLists() {
        viewModelScope.launch {
            groceryRepository.getListsByUser()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { lists ->
                    // For each list, get item counts
                    val listItems = lists.map { list ->
                        val withItems = groceryRepository.getListWithItemsOnce(list.id)
                        GroceryListItem(
                            list = list,
                            itemCount = withItems?.totalCount ?: 0,
                            checkedCount = withItems?.checkedCount ?: 0
                        )
                    }
                    _uiState.update {
                        it.copy(lists = listItems, isLoading = false)
                    }
                }
        }
    }

    fun deleteList(list: GroceryList) {
        viewModelScope.launch {
            groceryRepository.deleteList(list)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
