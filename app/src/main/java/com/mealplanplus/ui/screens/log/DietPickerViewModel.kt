package com.mealplanplus.ui.screens.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.TagRepository
import com.mealplanplus.util.naturalSortKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DietPickerItem(
    val diet: Diet,
    val totalCalories: Int = 0,
    val mealCount: Int = 0,
    val tags: List<Tag> = emptyList()
)

data class DietPickerUiState(
    val diets: List<DietPickerItem> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val selectedTagId: Long? = null,  // null = ALL
    val isLoading: Boolean = true
)

@HiltViewModel
class DietPickerViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedTagId = MutableStateFlow<Long?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _allDiets = MutableStateFlow<List<DietPickerItem>>(emptyList())
    private val _allTags = MutableStateFlow<List<Tag>>(emptyList())

    val uiState: StateFlow<DietPickerUiState> = combine(
        _allDiets,
        _allTags,
        _searchQuery,
        _selectedTagId,
        _isLoading
    ) { diets, tags, query, tagId, loading ->
        val filtered = diets
            .filter { item ->
                val matchesSearch = query.isBlank() ||
                    item.diet.name.contains(query, ignoreCase = true) ||
                    item.diet.description?.contains(query, ignoreCase = true) == true
                val matchesTag = tagId == null || item.tags.any { it.id == tagId }
                matchesSearch && matchesTag
            }
            .sortedWith(compareBy(
                { naturalSortKey(it.diet.name).first },
                { naturalSortKey(it.diet.name).second }
            ))

        DietPickerUiState(
            diets = filtered,
            allTags = tags,
            searchQuery = query,
            selectedTagId = tagId,
            isLoading = loading
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DietPickerUiState())

    init {
        loadDiets()
        loadTags()
    }

    private fun loadDiets() {
        viewModelScope.launch {
            dietRepository.getDietsWithSummary().collect { summaries ->
                _isLoading.value = true
                val items = summaries.map { summary ->
                    val dietTags = dietRepository.getTagsForDiet(summary.id)
                    DietPickerItem(
                        diet = summary.toDiet(),
                        totalCalories = summary.totalCalories,
                        mealCount = summary.mealCount,
                        tags = dietTags
                    )
                }
                _allDiets.value = items
                _isLoading.value = false
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _allTags.value = tags
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tagId: Long?) {
        _selectedTagId.value = tagId
    }
}
