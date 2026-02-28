package com.mealplanplus.ui.screens.grocery

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.GroceryItemWithFood
import com.mealplanplus.data.model.GroceryListWithItems
import com.mealplanplus.data.repository.GroceryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroceryDetailUiState(
    val list: GroceryListWithItems? = null,
    val isLoading: Boolean = true,
    val isRegenerating: Boolean = false,
    val error: String? = null,
    val showAddItemDialog: Boolean = false,
    val showEditItemDialog: GroceryItemWithFood? = null,
    val selectedTab: Int = 0  // 0=All, 1=To Buy (unchecked only)
)

@HiltViewModel
class GroceryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groceryRepository: GroceryRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val listId: Long = savedStateHandle["listId"] ?: 0L

    private val _uiState = MutableStateFlow(GroceryDetailUiState())
    val uiState: StateFlow<GroceryDetailUiState> = _uiState.asStateFlow()

    init {
        loadList()
    }

    private fun loadList() {
        viewModelScope.launch {
            groceryRepository.getListWithItems(listId)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { listWithItems ->
                    _uiState.update {
                        it.copy(list = listWithItems, isLoading = false)
                    }
                }
        }
    }

    fun setSelectedTab(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun toggleItemChecked(itemId: Long, currentChecked: Boolean) {
        viewModelScope.launch {
            groceryRepository.toggleItemChecked(itemId, !currentChecked)
        }
    }

    fun uncheckAllItems() {
        viewModelScope.launch {
            groceryRepository.uncheckAllItems(listId)
        }
    }

    fun deleteItem(itemId: Long) {
        viewModelScope.launch {
            groceryRepository.deleteItem(itemId)
        }
    }

    fun showAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = true) }
    }

    fun hideAddItemDialog() {
        _uiState.update { it.copy(showAddItemDialog = false) }
    }

    fun addCustomItem(name: String, quantity: Double, unit: FoodUnit, category: String? = null) {
        viewModelScope.launch {
            groceryRepository.addCustomItem(listId, name, quantity, unit, category)
            _uiState.update { it.copy(showAddItemDialog = false) }
        }
    }

    fun showEditItemDialog(item: GroceryItemWithFood) {
        _uiState.update { it.copy(showEditItemDialog = item) }
    }

    fun hideEditItemDialog() {
        _uiState.update { it.copy(showEditItemDialog = null) }
    }

    fun updateItemQuantity(itemId: Long, quantity: Double) {
        viewModelScope.launch {
            groceryRepository.updateItemQuantity(itemId, quantity)
            _uiState.update { it.copy(showEditItemDialog = null) }
        }
    }

    fun regenerateList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRegenerating = true) }
            try {
                groceryRepository.regenerateList(listId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isRegenerating = false) }
            }
        }
    }

    fun shareList() {
        val list = _uiState.value.list ?: return
        val text = formatListAsText(list)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, list.list.name)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Share grocery list").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun formatListAsText(list: GroceryListWithItems): String {
        val sb = StringBuilder()
        sb.appendLine("🛒 ${list.list.name}")
        list.list.dateRangeDisplay?.let { sb.appendLine(it) }
        sb.appendLine("---")

        val sortedItems = list.items.sortedWith(
            compareBy({ it.item.isChecked }, { it.displayName.lowercase() })
        )

        for (item in sortedItems) {
            val check = if (item.item.isChecked) "☑" else "☐"
            sb.appendLine("$check ${item.displayName} - ${item.displayQuantity}")
        }

        sb.appendLine()
        sb.appendLine("${list.checkedCount}/${list.totalCount} items checked")

        return sb.toString()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
