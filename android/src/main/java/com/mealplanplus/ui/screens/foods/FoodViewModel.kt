package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.repository.BarcodeRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.sync.SyncManager
import com.mealplanplus.util.NaturalOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoodSort { RECENT, NAME, CALORIES, PROTEIN }
enum class FoodViewMode { LIST, COMPACT }
enum class FoodSheet { MANUAL, ONLINE, BARCODE }

/** Barcode sheet phases: live camera → looking up → show product → not found. */
enum class BarcodePhase { SCANNING, LOOKING_UP, RESULT, NOT_FOUND }

data class FoodsUiState(
    val foods: List<Food> = emptyList(),
    val searchQuery: String = "",
    val sortMode: FoodSort = FoodSort.RECENT,
    val viewMode: FoodViewMode = FoodViewMode.LIST,
    val favOnly: Boolean = false,
    val categoryFilter: String? = null,   // null = all categories
    val expandedIds: Set<String> = emptySet(),
    val activeSheet: FoodSheet? = null,
    val fanOpen: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val manualName: String = "",
    val manualServing: String = "",
    val manualKcal: String = "",
    val manualProtein: String = "",
    val manualCarbs: String = "",
    val manualFat: String = "",
    val manualUnit: String = "GRAM",
    val manualGramsPerUnit: String = "",
    val manualCategory: String = "",
    val editingFoodId: String? = null,   // null = creating; non-null = editing that food
    val onlineQuery: String = "",
    val onlineResults: List<FoodDto> = emptyList(),
    val onlineLoading: Boolean = false,
    val barcodePhase: BarcodePhase = BarcodePhase.SCANNING,
    val barcodeResult: FoodDto? = null,
    val barcodeMessage: String? = null,
) {
    val filteredFoods: List<Food>
        get() {
            var list = foods
            if (searchQuery.isNotBlank()) {
                val q = searchQuery.trim().lowercase()
                list = list.filter {
                    it.name.lowercase().contains(q) ||
                    it.brand?.lowercase()?.contains(q) == true
                }
            }
            if (favOnly) {
                list = list.filter { it.isFavorite }
            }
            categoryFilter?.let { cat -> list = list.filter { it.category == cat } }
            list = when (sortMode) {
                FoodSort.RECENT   -> list.sortedByDescending { it.updatedAt }
                FoodSort.NAME     -> list.sortedWith(compareBy(NaturalOrder) { it.name })
                FoodSort.CALORIES -> list.sortedByDescending { it.caloriesPer100 }
                FoodSort.PROTEIN  -> list.sortedByDescending { it.proteinPer100 }
            }
            return list
        }

    val favCount: Int get() = foods.count { it.isFavorite }

    /** Distinct categories actually present in the user's foods — drives the filter chips. */
    val usedCategories: List<String>
        get() = foods.mapNotNull { it.category?.takeIf(String::isNotBlank) }.distinct().sorted()

    val isSaveManualEnabled: Boolean
        get() = manualName.isNotBlank() && manualKcal.isNotBlank() &&
            (!com.mealplanplus.data.repository.isCountUnit(manualUnit) ||
                (manualGramsPerUnit.toDoubleOrNull() ?: 0.0) > 0.0)
}

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val barcodeRepository: BarcodeRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FoodsUiState())
    val state: StateFlow<FoodsUiState> = _state

    init {
        // UI reads the local cache reactively; sync runs in the background.
        viewModelScope.launch {
            repository.getFoods().collect { foods ->
                _state.value = _state.value.copy(foods = foods)
            }
        }
        sync()   // pull server changes on open
    }

    /** Push local (dirty) changes and pull server changes. Fire-and-forget. */
    private fun sync() {
        viewModelScope.launch {
            syncManager.sync().onFailure { e -> _state.value = _state.value.copy(error = e.message) }
        }
    }

    fun setSearch(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun setSort(sort: FoodSort) {
        _state.value = _state.value.copy(sortMode = sort)
    }

    fun setViewMode(mode: FoodViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun toggleFavOnly() {
        _state.value = _state.value.copy(favOnly = !_state.value.favOnly)
    }

    /** Toggle a category filter chip (tapping the active one clears it). */
    fun setCategoryFilter(category: String?) {
        val current = _state.value.categoryFilter
        _state.value = _state.value.copy(categoryFilter = if (current == category) null else category)
    }

    fun toggleExpand(id: String) {
        val current = _state.value.expandedIds
        _state.value = _state.value.copy(
            expandedIds = if (id in current) current - id else current + id
        )
    }

    fun toggleFavorite(food: Food) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(food) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun deleteFood(food: Food) {
        viewModelScope.launch {
            runCatching { repository.delete(food) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun openFan() {
        _state.value = _state.value.copy(fanOpen = true)
    }

    fun closeFan() {
        _state.value = _state.value.copy(fanOpen = false)
    }

    fun openSheet(sheet: FoodSheet) {
        _state.value = _state.value.copy(
            activeSheet = sheet, fanOpen = false,
            barcodePhase = BarcodePhase.SCANNING, barcodeResult = null, barcodeMessage = null,
        )
    }

    fun closeSheet() {
        _state.value = _state.value.copy(
            activeSheet = null,
            manualName = "",
            manualServing = "",
            manualKcal = "",
            manualProtein = "",
            manualCarbs = "",
            manualFat = "",
            manualUnit = "GRAM",
            manualGramsPerUnit = "",
            manualCategory = "",
            editingFoodId = null,
            onlineQuery = "",
            onlineResults = emptyList(),
            barcodePhase = BarcodePhase.SCANNING,
            barcodeResult = null,
            barcodeMessage = null,
        )
    }

    // ── Barcode scanning ────────────────────────────────────────────────────────
    /** A barcode was detected (or entered) — look the product up on Open Food Facts. */
    fun onBarcodeScanned(code: String) {
        if (_state.value.barcodePhase != BarcodePhase.SCANNING) return  // ignore extra hits after the first
        _state.value = _state.value.copy(barcodePhase = BarcodePhase.LOOKING_UP)
        viewModelScope.launch {
            val dto = runCatching { barcodeRepository.lookup(code) }.getOrNull()
            _state.value = if (dto != null)
                _state.value.copy(barcodePhase = BarcodePhase.RESULT, barcodeResult = dto)
            else
                _state.value.copy(barcodePhase = BarcodePhase.NOT_FOUND, barcodeMessage = "No product found for $code")
        }
    }

    /** Back to the live camera to try again. */
    fun rescanBarcode() {
        _state.value = _state.value.copy(barcodePhase = BarcodePhase.SCANNING, barcodeResult = null, barcodeMessage = null)
    }

    /** Save the scanned product into the user's foods (same path as an online result). */
    fun addScannedFood() {
        val dto = _state.value.barcodeResult ?: return
        viewModelScope.launch {
            runCatching { repository.addOnline(dto) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            closeSheet()
            sync()
        }
    }

    /** Open the manual sheet pre-filled to edit an existing food. */
    fun openEditFood(food: Food) {
        val gpu = when (food.unit) {
            "PIECE" -> food.gramsPerPiece; "CUP" -> food.gramsPerCup
            "TBSP"  -> food.gramsPerTbsp;  "TSP" -> food.gramsPerTsp; else -> null
        }
        _state.value = _state.value.copy(
            activeSheet = FoodSheet.MANUAL,
            fanOpen = false,
            editingFoodId = food.id,
            manualName = food.name,
            manualServing = food.servingLabel ?: "",
            manualKcal = food.caloriesPer100.numStr(),
            manualProtein = food.proteinPer100.numStr(),
            manualCarbs = food.carbsPer100.numStr(),
            manualFat = food.fatPer100.numStr(),
            manualUnit = food.unit,
            manualGramsPerUnit = gpu?.numStr() ?: "",
            manualCategory = food.category ?: "",
        )
    }

    fun setManualName(v: String)    { _state.value = _state.value.copy(manualName = v) }
    fun setManualServing(v: String) { _state.value = _state.value.copy(manualServing = v) }
    fun setManualKcal(v: String)    { _state.value = _state.value.copy(manualKcal = v) }
    fun setManualProtein(v: String) { _state.value = _state.value.copy(manualProtein = v) }
    fun setManualCarbs(v: String)   { _state.value = _state.value.copy(manualCarbs = v) }
    fun setManualFat(v: String)     { _state.value = _state.value.copy(manualFat = v) }
    fun setManualUnit(v: String)    { _state.value = _state.value.copy(manualUnit = v) }
    fun setManualGramsPerUnit(v: String) { _state.value = _state.value.copy(manualGramsPerUnit = v) }
    fun setManualCategory(v: String) { _state.value = _state.value.copy(manualCategory = v) }

    fun saveManual() {
        val s = _state.value
        if (!s.isSaveManualEnabled) return
        val gpu = if (com.mealplanplus.data.repository.isCountUnit(s.manualUnit))
            s.manualGramsPerUnit.toDoubleOrNull() else null
        val kcal = s.manualKcal.toDoubleOrNull() ?: 0.0
        val protein = s.manualProtein.toDoubleOrNull() ?: 0.0
        val carbs = s.manualCarbs.toDoubleOrNull() ?: 0.0
        val fat = s.manualFat.toDoubleOrNull() ?: 0.0
        val editing = s.editingFoodId?.let { id -> s.foods.find { it.id == id } }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            runCatching {
                val category = s.manualCategory.trim().ifBlank { null }
                if (editing != null) {
                    repository.update(editing.copy(
                        name           = s.manualName.trim(),
                        servingLabel   = s.manualServing.ifBlank { null },
                        category       = category,
                        caloriesPer100 = kcal, proteinPer100 = protein, carbsPer100 = carbs, fatPer100 = fat,
                        unit           = s.manualUnit,
                        gramsPerPiece  = if (s.manualUnit == "PIECE") gpu else null,
                        gramsPerCup    = if (s.manualUnit == "CUP")   gpu else null,
                        gramsPerTbsp   = if (s.manualUnit == "TBSP")  gpu else null,
                        gramsPerTsp    = if (s.manualUnit == "TSP")   gpu else null,
                    ))
                } else {
                    repository.createManual(
                        name = s.manualName.trim(), caloriesPer100 = kcal, proteinPer100 = protein,
                        carbsPer100 = carbs, fatPer100 = fat, servingLabel = s.manualServing.ifBlank { null },
                        unit = s.manualUnit, gramsPerUnit = gpu, category = category,
                    )
                }
            }.onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(isLoading = false)
            closeSheet()
            sync()
        }
    }

    private fun Double.numStr(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

    fun setOnlineQuery(q: String) {
        _state.value = _state.value.copy(onlineQuery = q)
    }

    fun searchOnline() {
        val q = _state.value.onlineQuery.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(onlineLoading = true, onlineResults = emptyList())
            // Foods "Search online" uses Open Food Facts (the Meal builder keeps the backend search).
            runCatching { barcodeRepository.search(q) }
                .onSuccess { results -> _state.value = _state.value.copy(onlineResults = results) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(onlineLoading = false)
        }
    }

    fun addOnlineFood(dto: FoodDto) {
        viewModelScope.launch {
            runCatching { repository.addOnline(dto) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }
}
