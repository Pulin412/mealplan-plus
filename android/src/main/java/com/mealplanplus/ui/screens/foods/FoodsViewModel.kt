package com.mealplanplus.ui.screens.foods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FoodTab { ALL, FAVORITES, RECENT }

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FoodsViewModel @Inject constructor(
    private val repository: FoodRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(FoodTab.ALL)
    val selectedTab: StateFlow<FoodTab> = _selectedTab.asStateFlow()

    // Tags
    val tags: StateFlow<List<Tag>> = tagRepository.getTagsByUser()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    // Base food list (query + tab)
    private val _baseFoods: Flow<List<FoodItem>> = combine(
        _searchQuery,
        _selectedTab
    ) { query, tab ->
        Pair(query, tab)
    }.flatMapLatest { (query, tab) ->
        when {
            query.isNotBlank() -> repository.searchFoods(query)
            tab == FoodTab.FAVORITES -> repository.getFavorites()
            tab == FoodTab.RECENT -> repository.getRecentFoods()
            else -> repository.getAllFoods()
        }
    }

    // Tag-filtered food ids: for each selected tag, a Set<Long> of food ids
    // Intersection: food must appear in ALL selected tag id sets
    val foods: StateFlow<List<FoodItem>> = combine(
        _baseFoods,
        _selectedTagIds.flatMapLatest { tagIds ->
            if (tagIds.isEmpty()) {
                flowOf(emptyMap())
            } else {
                val tagFlows = tagIds.map { tagId ->
                    repository.getFoodIdsForTag(tagId).map { ids -> tagId to ids.toSet() }
                }
                combine(tagFlows) { pairs -> pairs.toMap() }
            }
        }
    ) { base, tagFoodMap ->
        if (tagFoodMap.isEmpty()) {
            base
        } else {
            base.filter { food ->
                tagFoodMap.values.all { foodIds -> food.id in foodIds }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectTab(tab: FoodTab) {
        _selectedTab.value = tab
    }

    fun toggleTag(tagId: Long) {
        _selectedTagIds.value = if (tagId in _selectedTagIds.value) {
            _selectedTagIds.value - tagId
        } else {
            _selectedTagIds.value + tagId
        }
    }

    fun clearTagFilter() {
        _selectedTagIds.value = emptySet()
    }

    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch {
            repository.setFavorite(food.id, !food.isFavorite)
        }
    }

    fun deleteFood(food: FoodItem) {
        viewModelScope.launch {
            repository.deleteFood(food)
        }
    }

    fun addTagToFood(foodId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.addTagToFood(foodId, tagId)
        }
    }

    fun removeTagFromFood(foodId: Long, tagId: Long) {
        viewModelScope.launch {
            repository.removeTagFromFood(foodId, tagId)
        }
    }

    fun getTagsForFood(foodId: Long) = repository.getTagsForFood(foodId)
}
