package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.UsdaFoodRepository
import com.mealplanplus.data.repository.UsdaFoodResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

data class QuickLogEntry(
    val loggedFoodId: Long,
    val food: FoodItem,
    val quantity: Double,
    val unit: FoodUnit
) {
    val calories: Int
        get() = (food.caloriesPer100 * food.toGrams(quantity, unit) / 100).toInt()
}

data class QuickLogUiState(
    val selectedSlot: DefaultMealSlot = DefaultMealSlot.BREAKFAST,
    val searchQuery: String = "",
    val localResults: List<FoodItem> = emptyList(),
    val usdaResults: List<UsdaFoodResult> = emptyList(),
    val isSearchingUsda: Boolean = false,
    val usdaError: String? = null,
    val selectedFood: FoodItem? = null,
    val pendingUsdaFood: UsdaFoodResult? = null,
    val quantityInput: String = "100",
    val selectedUnit: FoodUnit = FoodUnit.GRAM,
    /** GI value typed by the user (empty = not set). Pre-filled from food.glycemicIndex when a food is selected. */
    val giInput: String = "",
    val addedItems: List<QuickLogEntry> = emptyList(),
    val isAdding: Boolean = false,
    val error: String? = null
) {
    val hasResults: Boolean get() = localResults.isNotEmpty() || usdaResults.isNotEmpty() || isSearchingUsda
    val selectedFoodPreview: FoodItem? get() = selectedFood ?: pendingUsdaFood?.toFoodItem()

    /** Live GL contribution for the current food + quantity + GI. */
    val glContribution: Double?
        get() {
            val food = selectedFoodPreview ?: return null
            val gi = giInput.toIntOrNull() ?: food.glycemicIndex ?: return null
            val qty = quantityInput.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
            val grams = food.toGrams(qty, selectedUnit)
            val carbs = food.carbsPer100 * grams / 100.0
            return gi * carbs / 100.0
        }
}

@HiltViewModel
class QuickLogViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val foodRepository: FoodRepository,
    private val usdaRepository: UsdaFoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickLogUiState())
    val uiState: StateFlow<QuickLogUiState> = _uiState.asStateFlow()

    private val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun initForToday() {
        val slot = when (LocalTime.now().hour) {
            in 0..10 -> DefaultMealSlot.BREAKFAST
            in 11..15 -> DefaultMealSlot.LUNCH
            in 16..20 -> DefaultMealSlot.DINNER
            else -> DefaultMealSlot.EVENING_SNACK
        }
        _uiState.update {
            QuickLogUiState(
                selectedSlot = slot,
                addedItems = it.addedItems
            )
        }
    }

    fun updateSlot(slot: DefaultMealSlot) {
        _uiState.update { it.copy(selectedSlot = slot) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                selectedFood = null,
                pendingUsdaFood = null,
                localResults = emptyList(),
                usdaResults = emptyList(),
                usdaError = null
            )
        }
    }

    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.length < 2) return

        // Local — immediate in-memory filter
        val localMatches = allFoods.value
            .filter { it.name.contains(query, ignoreCase = true) }
            .take(8)
        _uiState.update { it.copy(localResults = localMatches, isSearchingUsda = true, usdaError = null) }

        // Online — USDA API
        viewModelScope.launch {
            val result = usdaRepository.searchFoods(query)
            result.fold(
                onSuccess = { foods ->
                    val localNames = _uiState.value.localResults.map { it.name.lowercase() }.toSet()
                    val unique = foods.filter { it.name.lowercase() !in localNames }
                    _uiState.update { it.copy(usdaResults = unique, isSearchingUsda = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSearchingUsda = false, usdaError = e.message ?: "Online search failed") }
                }
            )
        }
    }

    fun selectLocalFood(food: FoodItem) {
        _uiState.update {
            it.copy(
                selectedFood = food,
                pendingUsdaFood = null,
                quantityInput = "100",
                selectedUnit = FoodUnit.GRAM,
                giInput = food.glycemicIndex?.toString() ?: "",
                localResults = emptyList(),
                usdaResults = emptyList()
            )
        }
    }

    fun selectUsdaFood(usda: UsdaFoodResult) {
        _uiState.update {
            it.copy(
                pendingUsdaFood = usda,
                selectedFood = null,
                quantityInput = "100",
                selectedUnit = FoodUnit.GRAM,
                giInput = "",
                localResults = emptyList(),
                usdaResults = emptyList()
            )
        }
    }

    fun dismissSelectedFood() {
        _uiState.update { it.copy(selectedFood = null, pendingUsdaFood = null, giInput = "") }
    }

    fun updateQuantity(qty: String) {
        _uiState.update { it.copy(quantityInput = qty) }
    }

    fun updateUnit(unit: FoodUnit) {
        _uiState.update { it.copy(selectedUnit = unit) }
    }

    fun updateGiInput(gi: String) {
        // Only accept digits, max 3 chars (0-100)
        val filtered = gi.filter { it.isDigit() }.take(3)
        _uiState.update { it.copy(giInput = filtered) }
    }

    fun addFood() {
        val state = _uiState.value
        val qty = state.quantityInput.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        if (state.selectedFood == null && state.pendingUsdaFood == null) return
        _uiState.update { it.copy(isAdding = true) }
        viewModelScope.launch {
            try {
                val gi = state.giInput.toIntOrNull()?.coerceIn(0, 100)
                val foodId: Long
                val displayFood: FoodItem

                if (state.pendingUsdaFood != null) {
                    // Save USDA food with user-supplied GI (if any)
                    val saved = state.pendingUsdaFood.toFoodItem().copy(glycemicIndex = gi)
                    foodId = foodRepository.insertFood(saved)
                    displayFood = saved.copy(id = foodId)
                } else {
                    var food = state.selectedFood!!
                    foodId = food.id
                    // Persist GI change when the user entered or modified it
                    if (gi != food.glycemicIndex) {
                        val updated = food.copy(glycemicIndex = gi)
                        foodRepository.updateFood(updated)
                        food = updated
                    }
                    displayFood = food
                }

                val loggedFoodId = dailyLogRepository.logFood(
                    date = LocalDate.now(),
                    foodId = foodId,
                    quantity = qty,
                    slotType = state.selectedSlot.name,
                    unit = state.selectedUnit
                )
                _uiState.update {
                    it.copy(
                        addedItems = it.addedItems + QuickLogEntry(loggedFoodId, displayFood, qty, state.selectedUnit),
                        selectedFood = null,
                        pendingUsdaFood = null,
                        searchQuery = "",
                        localResults = emptyList(),
                        usdaResults = emptyList(),
                        giInput = "",
                        isAdding = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isAdding = false) }
            }
        }
    }

    fun removeItem(entry: QuickLogEntry) {
        viewModelScope.launch {
            try {
                dailyLogRepository.deleteLoggedFood(entry.loggedFoodId)
                _uiState.update { it.copy(addedItems = it.addedItems - entry) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetOnClose() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                localResults = emptyList(),
                usdaResults = emptyList(),
                selectedFood = null,
                pendingUsdaFood = null,
                giInput = "",
                addedItems = emptyList(),
                isSearchingUsda = false,
                usdaError = null,
                error = null
            )
        }
    }
}
