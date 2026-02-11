package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.repository.DietRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DietSortOption(val label: String) {
    NAME_ASC("Name (A-Z)"),
    NAME_DESC("Name (Z-A)"),
    CALORIES_ASC("Calories ↑"),
    CALORIES_DESC("Calories ↓"),
    NEWEST("Newest"),
    OLDEST("Oldest")
}

enum class DietFilterOption(val label: String) {
    ALL("All"),
    REMISSION("Remission"),
    MAINTENANCE("Maintenance"),
    SOS("SOS")
}

data class DietDisplayItem(
    val diet: Diet,
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val mealCount: Int = 0,
    val tags: List<DietTag> = emptyList()
)

data class DietsUiState(
    val diets: List<DietDisplayItem> = emptyList(),
    val searchQuery: String = "",
    val sortOption: DietSortOption = DietSortOption.NAME_ASC,
    val filterOption: DietFilterOption = DietFilterOption.ALL,
    val isLoading: Boolean = true
)

@HiltViewModel
class DietsViewModel @Inject constructor(
    private val repository: DietRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(DietSortOption.NAME_ASC)
    private val _filterOption = MutableStateFlow(DietFilterOption.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _dietsWithMeals = MutableStateFlow<List<DietDisplayItem>>(emptyList())

    val uiState: StateFlow<DietsUiState> = combine(
        _dietsWithMeals,
        _searchQuery,
        _sortOption,
        _filterOption,
        _isLoading
    ) { diets, query, sort, filter, loading ->
        val filtered = diets
            .filter { item ->
                (query.isBlank() || item.diet.name.contains(query, ignoreCase = true) ||
                        item.diet.description?.contains(query, ignoreCase = true) == true) &&
                when (filter) {
                    DietFilterOption.ALL -> true
                    DietFilterOption.REMISSION -> item.tags.contains(DietTag.REMISSION)
                    DietFilterOption.MAINTENANCE -> item.tags.contains(DietTag.MAINTENANCE)
                    DietFilterOption.SOS -> item.tags.contains(DietTag.SOS)
                }
            }
            .let { list ->
                when (sort) {
                    DietSortOption.NAME_ASC -> list.sortedWith(compareBy(
                        { naturalSortKey(it.diet.name).first },
                        { naturalSortKey(it.diet.name).second }
                    ))
                    DietSortOption.NAME_DESC -> list.sortedWith(compareByDescending<DietDisplayItem> {
                        naturalSortKey(it.diet.name).first
                    }.thenByDescending { naturalSortKey(it.diet.name).second })
                    DietSortOption.CALORIES_ASC -> list.sortedBy { it.totalCalories }
                    DietSortOption.CALORIES_DESC -> list.sortedByDescending { it.totalCalories }
                    DietSortOption.NEWEST -> list.sortedByDescending { it.diet.createdAt }
                    DietSortOption.OLDEST -> list.sortedBy { it.diet.createdAt }
                }
            }

        DietsUiState(
            diets = filtered,
            searchQuery = query,
            sortOption = sort,
            filterOption = filter,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietsUiState())

    init {
        loadDietsWithMeals()
    }

    private fun loadDietsWithMeals() {
        viewModelScope.launch {
            repository.getAllDiets().collect { diets ->
                _isLoading.value = true
                val displayItems = diets.map { diet ->
                    val withMeals = repository.getDietWithMeals(diet.id)
                    DietDisplayItem(
                        diet = diet,
                        totalCalories = withMeals?.totalCalories?.toInt() ?: 0,
                        totalProtein = withMeals?.totalProtein?.toInt() ?: 0,
                        totalCarbs = withMeals?.totalCarbs?.toInt() ?: 0,
                        totalFat = withMeals?.totalFat?.toInt() ?: 0,
                        mealCount = withMeals?.meals?.count { it.value != null } ?: 0,
                        tags = diet.getTagList().ifEmpty { listOf(extractDietType(diet.name)) }
                    )
                }
                _dietsWithMeals.value = displayItems
                _isLoading.value = false
            }
        }
    }

    private fun extractDietType(name: String): DietTag {
        return when {
            name.contains("SOS", ignoreCase = true) -> DietTag.SOS
            name.startsWith("Diet-M") || name.contains("Maintenance", ignoreCase = true) -> DietTag.MAINTENANCE
            name.startsWith("Diet-") -> DietTag.REMISSION
            else -> DietTag.CUSTOM
        }
    }

    // Natural sort comparator for diet names (Diet-1, Diet-2, ... Diet-10, Diet-11)
    private fun naturalSortKey(name: String): Pair<String, Int> {
        // Extract prefix (Diet- or Diet-M) and number
        val regex = Regex("^(Diet-M?)(\\d+).*$")
        val match = regex.find(name)
        return if (match != null) {
            val prefix = match.groupValues[1]
            val number = match.groupValues[2].toIntOrNull() ?: 0
            Pair(prefix, number)
        } else {
            Pair(name, 0)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOption(option: DietSortOption) {
        _sortOption.value = option
    }

    fun updateFilterOption(option: DietFilterOption) {
        _filterOption.value = option
    }

    fun deleteDiet(diet: Diet) {
        viewModelScope.launch {
            repository.deleteDiet(diet)
        }
    }

    fun duplicateDiet(diet: Diet) {
        viewModelScope.launch {
            repository.duplicateDiet(diet.id, "${diet.name} (copy)")
        }
    }
}
