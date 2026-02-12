package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietSummary
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.util.naturalSortKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DietPickerFilter(val label: String) {
    ALL("All"),
    REMISSION("Remission"),
    MAINTENANCE("Maintenance"),
    SOS("SOS")
}

data class DietPickerItem(
    val diet: Diet,
    val totalCalories: Int = 0,
    val mealCount: Int = 0,
    val tags: List<DietTag> = emptyList()
)

data class DietPickerUiState(
    val diets: List<DietPickerItem> = emptyList(),
    val searchQuery: String = "",
    val filter: DietPickerFilter = DietPickerFilter.ALL,
    val isLoading: Boolean = true
)

@HiltViewModel
class DietPickerViewModel @Inject constructor(
    private val dietRepository: DietRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filter = MutableStateFlow(DietPickerFilter.ALL)
    private val _isLoading = MutableStateFlow(true)
    private val _allDiets = MutableStateFlow<List<DietPickerItem>>(emptyList())

    val uiState: StateFlow<DietPickerUiState> = combine(
        _allDiets,
        _searchQuery,
        _filter,
        _isLoading
    ) { diets, query, filter, loading ->
        val filtered = diets
            .filter { item ->
                (query.isBlank() || item.diet.name.contains(query, ignoreCase = true) ||
                        item.diet.description?.contains(query, ignoreCase = true) == true) &&
                        when (filter) {
                            DietPickerFilter.ALL -> true
                            DietPickerFilter.REMISSION -> item.tags.contains(DietTag.REMISSION)
                            DietPickerFilter.MAINTENANCE -> item.tags.contains(DietTag.MAINTENANCE)
                            DietPickerFilter.SOS -> item.tags.contains(DietTag.SOS)
                        }
            }
            .sortedWith(compareBy(
                { naturalSortKey(it.diet.name).first },
                { naturalSortKey(it.diet.name).second }
            ))

        DietPickerUiState(
            diets = filtered,
            searchQuery = query,
            filter = filter,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietPickerUiState())

    init {
        loadDiets()
    }

    private fun loadDiets() {
        viewModelScope.launch {
            // Single JOIN query - no N+1 problem
            dietRepository.getAllDietsWithSummary().collect { summaries ->
                _isLoading.value = true
                val items = summaries.map { summary ->
                    DietPickerItem(
                        diet = summary.toDiet(),
                        totalCalories = summary.totalCalories,
                        mealCount = summary.mealCount,
                        tags = summary.getTagList().ifEmpty { listOf(extractDietType(summary.name)) }
                    )
                }
                _allDiets.value = items
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

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: DietPickerFilter) {
        _filter.value = filter
    }
}
