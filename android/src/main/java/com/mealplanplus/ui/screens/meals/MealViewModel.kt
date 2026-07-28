package com.mealplanplus.ui.screens.meals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealItem
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MealSort { RECENT, NAME, CALORIES, PROTEIN }
enum class MealViewMode { LIST, COMPACT }

data class MealsUiState(
    val meals: List<MealUi> = emptyList(),
    val foods: List<Food> = emptyList(),          // for the New Meal picker
    val searchQuery: String = "",
    val sortMode: MealSort = MealSort.RECENT,
    val viewMode: MealViewMode = MealViewMode.LIST,
    val favOnly: Boolean = false,
    val slotFilter: String? = null,
    val expandedIds: Set<String> = emptySet(),
    val newMealOpen: Boolean = false,
    val editingMeal: Meal? = null,   // non-null = the New Meal sheet is editing this meal
    val error: String? = null,
) {
    /** Distinct slots across all meals, for the filter row. */
    val allSlots: List<String> get() = meals.flatMap { it.meal.slots }.distinct()

    val filteredMeals: List<MealUi>
        get() {
            var list = meals
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter { it.meal.name.lowercase().contains(q) }
            }
            if (favOnly) list = list.filter { it.meal.isFavorite }
            slotFilter?.let { s -> list = list.filter { s in it.meal.slots } }
            return when (sortMode) {
                MealSort.RECENT   -> list.sortedByDescending { it.meal.updatedAt }
                MealSort.NAME     -> list.sortedBy { it.meal.name.lowercase() }
                MealSort.CALORIES -> list.sortedByDescending { it.totalKcal }
                MealSort.PROTEIN  -> list.sortedByDescending { it.totalProtein }
            }
        }

    val favCount: Int get() = meals.count { it.meal.isFavorite }
}

@HiltViewModel
class MealViewModel @Inject constructor(
    private val repository: MealRepository,
    private val foodRepository: FoodRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(MealsUiState())
    val state: StateFlow<MealsUiState> = _state

    init {
        viewModelScope.launch {
            repository.getMeals().collect { meals -> _state.value = _state.value.copy(meals = meals) }
        }
        viewModelScope.launch {
            foodRepository.getFoods().collect { foods -> _state.value = _state.value.copy(foods = foods) }
        }
        sync()
    }

    private fun sync() {
        viewModelScope.launch {
            syncManager.sync().onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun setSearch(q: String)          { _state.value = _state.value.copy(searchQuery = q) }
    fun setSort(s: MealSort)          { _state.value = _state.value.copy(sortMode = s) }
    fun setViewMode(m: MealViewMode)  { _state.value = _state.value.copy(viewMode = m) }
    fun toggleFavOnly()               { _state.value = _state.value.copy(favOnly = !_state.value.favOnly) }
    fun setSlotFilter(slot: String?)  { _state.value = _state.value.copy(slotFilter = slot) }

    fun toggleExpand(id: String) {
        val cur = _state.value.expandedIds
        _state.value = _state.value.copy(expandedIds = if (id in cur) cur - id else cur + id)
    }

    fun toggleFavorite(meal: Meal) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(meal) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            runCatching { repository.delete(meal) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun openNewMeal()  { _state.value = _state.value.copy(newMealOpen = true, editingMeal = null) }
    fun openEditMeal(meal: Meal) { _state.value = _state.value.copy(newMealOpen = true, editingMeal = meal) }
    fun closeNewMeal() { _state.value = _state.value.copy(newMealOpen = false, editingMeal = null) }

    // ── Add-food panel helpers (return the local food id to add into the meal) ───
    suspend fun searchOnline(q: String) =
        runCatching { foodRepository.searchOnline(q) }.getOrDefault(emptyList())

    suspend fun createManualFood(
        name: String, kcal: Double, protein: Double, carbs: Double, fat: Double, serving: String?,
        unit: String = "GRAM", gramsPerUnit: Double? = null,
    ): String = foodRepository.createManual(name, kcal, protein, carbs, fat, serving, unit, gramsPerUnit)

    suspend fun saveOnlineFood(dto: com.mealplanplus.data.generated.model.FoodDto): String =
        foodRepository.addOnline(dto)

    fun createMeal(name: String, slots: List<String>, items: List<MealItem>) {
        if (name.isBlank() || items.isEmpty()) return
        val editing = _state.value.editingMeal
        viewModelScope.launch {
            runCatching {
                if (editing != null) repository.update(editing, name, slots, items)
                else repository.create(name, slots, items)
            }.onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            closeNewMeal()
            sync()
        }
    }
}
