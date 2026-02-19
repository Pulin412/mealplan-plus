package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.TagRepository
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

enum class TagFilterMode(val label: String) {
    ANY("Any"),
    ALL("All")
}

data class DietDisplayItem(
    val diet: Diet,
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFat: Int = 0,
    val mealCount: Int = 0,
    val tags: List<Tag> = emptyList()
)

data class DietsUiState(
    val diets: List<DietDisplayItem> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val tagFilterMode: TagFilterMode = TagFilterMode.ANY,
    val searchQuery: String = "",
    val sortOption: DietSortOption = DietSortOption.NAME_ASC,
    val isLoading: Boolean = true,
    val showTagsDialog: Boolean = false
)

@HiltViewModel
class DietsViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _sortOption = MutableStateFlow(DietSortOption.NAME_ASC)
    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _tagFilterMode = MutableStateFlow(TagFilterMode.ANY)
    private val _isLoading = MutableStateFlow(true)
    private val _dietsWithMeals = MutableStateFlow<List<DietDisplayItem>>(emptyList())
    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())
    private val _showTagsDialog = MutableStateFlow(false)

    val uiState: StateFlow<DietsUiState> = combine(
        _dietsWithMeals,
        _allTags,
        _selectedTagIds,
        _tagFilterMode,
        combine(_searchQuery, _sortOption, _isLoading, _showTagsDialog) { q, s, l, d ->
            Triple(q, s, l to d)
        }
    ) { diets, tags, selectedTags, filterMode, (query, sort, loadingAndDialog) ->
        val (loading, showDialog) = loadingAndDialog
        val filtered = diets
            .filter { item ->
                val matchesSearch = query.isBlank() ||
                    item.diet.name.contains(query, ignoreCase = true) ||
                    item.diet.description?.contains(query, ignoreCase = true) == true

                val matchesTags = if (selectedTags.isEmpty()) {
                    true
                } else {
                    val itemTagIds = item.tags.map { it.id }.toSet()
                    when (filterMode) {
                        TagFilterMode.ANY -> itemTagIds.any { it in selectedTags }
                        TagFilterMode.ALL -> selectedTags.all { it in itemTagIds }
                    }
                }

                matchesSearch && matchesTags
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
            allTags = tags,
            selectedTagIds = selectedTags,
            tagFilterMode = filterMode,
            searchQuery = query,
            sortOption = sort,
            isLoading = loading,
            showTagsDialog = showDialog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietsUiState())

    init {
        loadTags()
        loadDietsWithMeals()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    private fun loadDietsWithMeals() {
        viewModelScope.launch {
            dietRepository.getDietsWithFullSummary().collect { summaries ->
                _isLoading.value = true
                val displayItems = summaries.map { summary ->
                    val tags = dietRepository.getTagsForDiet(summary.id)
                    DietDisplayItem(
                        diet = summary.toDiet(),
                        totalCalories = summary.totalCalories,
                        totalProtein = summary.totalProtein,
                        totalCarbs = summary.totalCarbs,
                        totalFat = summary.totalFat,
                        mealCount = summary.mealCount,
                        tags = tags
                    )
                }
                _dietsWithMeals.value = displayItems
                _isLoading.value = false
            }
        }
    }

    // Natural sort comparator for diet names (Diet-1, Diet-2, ... Diet-10, Diet-11)
    private fun naturalSortKey(name: String): Pair<String, Int> {
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

    fun toggleTagFilter(tagId: Long) {
        _selectedTagIds.value = if (tagId in _selectedTagIds.value) {
            _selectedTagIds.value - tagId
        } else {
            _selectedTagIds.value + tagId
        }
    }

    fun clearTagFilters() {
        _selectedTagIds.value = emptySet()
    }

    fun toggleTagFilterMode() {
        _tagFilterMode.value = when (_tagFilterMode.value) {
            TagFilterMode.ANY -> TagFilterMode.ALL
            TagFilterMode.ALL -> TagFilterMode.ANY
        }
    }

    fun showTagsManagement() {
        _showTagsDialog.value = true
    }

    fun hideTagsManagement() {
        _showTagsDialog.value = false
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            tagRepository.createTag(name)
        }
    }

    fun createTagWithColor(name: String, color: String) {
        viewModelScope.launch {
            tagRepository.createTagWithColor(name, color)
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagRepository.deleteTag(tag)
            _selectedTagIds.value = _selectedTagIds.value - tag.id
        }
    }

    fun deleteDiet(diet: Diet) {
        viewModelScope.launch {
            dietRepository.deleteDiet(diet)
        }
    }

    fun duplicateDiet(diet: Diet) {
        viewModelScope.launch {
            dietRepository.duplicateDiet(diet.id, "${diet.name} (copy)")
        }
    }
}
