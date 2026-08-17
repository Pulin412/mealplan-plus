package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietEntry
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.MEAL_SLOTS
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.DietUi
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.repository.SharingRepository
import com.mealplanplus.data.repository.TagRepository
import com.mealplanplus.data.sync.SyncManager
import com.mealplanplus.util.NaturalOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DietSort { RECENT, NAME, CALORIES, PROTEIN }
enum class DietViewMode { LIST, COMPACT }

data class DietsUiState(
    val diets: List<DietUi> = emptyList(),
    val meals: List<MealUi> = emptyList(),   // for the New Diet picker
    val foods: List<Food> = emptyList(),     // for the New Diet picker
    val availableTags: List<DietTag> = emptyList(),  // from the server tag directory
    val searchQuery: String = "",
    val sortMode: DietSort = DietSort.RECENT,
    val viewMode: DietViewMode = DietViewMode.LIST,
    val favOnly: Boolean = false,
    val importedOnly: Boolean = false,
    // Multi-select filters (match ANY); empty = no filter.
    val tagFilters: Set<String> = emptySet(),
    val slotFilters: Set<String> = emptySet(),
    val expandedIds: Set<String> = emptySet(),
    val sharedIds: Set<String> = emptySet(),   // diet serverIds currently shared with followers
    val importedIds: Set<String> = emptySet(), // diet serverIds copied from other users
    val newDietOpen: Boolean = false,
    val editingDiet: Diet? = null,   // non-null = the New Diet sheet is editing this diet
    val error: String? = null,
) {
    /** Tag names for the filter row: server directory ∪ tags actually used on diets. */
    val allTagNames: List<String>
        get() = (availableTags.map { it.name } + diets.flatMap { d -> d.diet.tags.map { it.name } })
            .distinct().sorted()

    /** Slots present across diets, in canonical meal-slot order (unknowns appended). */
    val allSlotNames: List<String>
        get() {
            val present = diets.flatMap { d -> d.slots.map { it.slot } }.toSet()
            return MEAL_SLOTS.filter { it in present } + present.filter { it !in MEAL_SLOTS }.sorted()
        }

    val filteredDiets: List<DietUi>
        get() {
            val q = searchQuery.trim().lowercase()
            val hasSlot = slotFilters.isNotEmpty()
            var list = diets.filter { d ->
                // Slot filter: the diet must contain at least one selected slot.
                val scopeSlots = if (hasSlot) d.slots.filter { it.slot in slotFilters } else d.slots
                if (hasSlot && scopeSlots.isEmpty()) return@filter false
                // When slots are selected, search only within those slots; otherwise the whole diet
                // (name + tags + every meal & ingredient name).
                if (q.isNotEmpty()) {
                    val haystack = (if (hasSlot) {
                        scopeSlots.flatMap { s -> s.entries.flatMap { e -> listOf(e.name) + e.mealFoods.map { it.name } } }
                    } else {
                        listOf(d.diet.name) + d.diet.tags.map { it.name } +
                            d.slots.flatMap { s -> s.entries.flatMap { e -> listOf(e.name) + e.mealFoods.map { it.name } } }
                    }).joinToString(" ").lowercase()
                    if (!haystack.contains(q)) return@filter false
                }
                if (tagFilters.isNotEmpty() && d.diet.tags.none { it.name in tagFilters }) return@filter false
                true
            }
            if (favOnly) list = list.filter { it.diet.isFavorite }
            if (importedOnly) list = list.filter { it.diet.id in importedIds }
            return when (sortMode) {
                DietSort.RECENT   -> list.sortedByDescending { it.diet.updatedAt }
                DietSort.NAME     -> list.sortedWith(compareBy(NaturalOrder) { it.diet.name })
                DietSort.CALORIES -> list.sortedByDescending { it.totalKcal }
                DietSort.PROTEIN  -> list.sortedByDescending { it.totalProtein }
            }
        }

    val favCount: Int get() = diets.count { it.diet.isFavorite }
}

@HiltViewModel
class DietViewModel @Inject constructor(
    private val repository: DietRepository,
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
    private val tagRepository: TagRepository,
    private val sharingRepository: SharingRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(DietsUiState())
    val state: StateFlow<DietsUiState> = _state

    init {
        viewModelScope.launch {
            repository.getDiets().collect { diets -> _state.value = _state.value.copy(diets = diets) }
        }
        loadShared()
        viewModelScope.launch {
            mealRepository.getMeals().collect { meals -> _state.value = _state.value.copy(meals = meals) }
        }
        viewModelScope.launch {
            foodRepository.getFoods().collect { foods -> _state.value = _state.value.copy(foods = foods) }
        }
        sync()
        loadTags()
    }

    private fun sync() {
        viewModelScope.launch {
            syncManager.sync().onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            loadTags()   // refresh the directory after a sync may have created assignments
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            val tags = tagRepository.listDietTags()
            if (tags.isNotEmpty()) _state.value = _state.value.copy(availableTags = tags)
        }
    }

    /** Create (or reuse) a tag on the server; adds it to the directory and returns it. */
    suspend fun createTag(name: String): DietTag? {
        val tag = tagRepository.createDietTag(name) ?: return null
        val cur = _state.value.availableTags
        if (cur.none { it.id == tag.id }) _state.value = _state.value.copy(availableTags = cur + tag)
        return tag
    }

    fun setSearch(q: String)          { _state.value = _state.value.copy(searchQuery = q) }
    fun setSort(s: DietSort)          { _state.value = _state.value.copy(sortMode = s) }
    fun setViewMode(m: DietViewMode)  { _state.value = _state.value.copy(viewMode = m) }
    fun toggleFavOnly()               { _state.value = _state.value.copy(favOnly = !_state.value.favOnly) }
    fun toggleTagFilter(tag: String) {
        val cur = _state.value.tagFilters
        _state.value = _state.value.copy(tagFilters = if (tag in cur) cur - tag else cur + tag)
    }
    fun clearTagFilters() { _state.value = _state.value.copy(tagFilters = emptySet()) }
    fun toggleSlotFilter(slot: String) {
        val cur = _state.value.slotFilters
        _state.value = _state.value.copy(slotFilters = if (slot in cur) cur - slot else cur + slot)
    }
    fun clearSlotFilters() { _state.value = _state.value.copy(slotFilters = emptySet()) }

    fun toggleExpand(id: String) {
        val cur = _state.value.expandedIds
        _state.value = _state.value.copy(expandedIds = if (id in cur) cur - id else cur + id)
    }

    private fun loadShared() {
        viewModelScope.launch {
            val flags = sharingRepository.dietFlags()
            _state.value = _state.value.copy(
                sharedIds = flags.shared.map { it.toString() }.toSet(),
                importedIds = flags.imported.map { it.toString() }.toSet(),
            )
        }
    }

    fun toggleImportedOnly() { _state.value = _state.value.copy(importedOnly = !_state.value.importedOnly) }

    /** Flip whether this diet is shared with followers (direct REST by serverId; not part of sync). */
    fun toggleShare(diet: Diet) {
        viewModelScope.launch {
            val newState = runCatching { sharingRepository.toggleDiet(java.util.UUID.fromString(diet.id)) }.getOrNull()
            if (newState != null) {
                val cur = _state.value.sharedIds
                _state.value = _state.value.copy(sharedIds = if (newState) cur + diet.id else cur - diet.id)
            } else {
                _state.value = _state.value.copy(error = "Couldn't update sharing")
            }
        }
    }

    fun toggleFavorite(diet: Diet) {
        viewModelScope.launch {
            runCatching { repository.toggleFavorite(diet) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun deleteDiet(diet: Diet) {
        viewModelScope.launch {
            runCatching { repository.delete(diet) }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            sync()
        }
    }

    fun openNewDiet()  { _state.value = _state.value.copy(newDietOpen = true, editingDiet = null) }
    fun openEditDiet(diet: Diet) { _state.value = _state.value.copy(newDietOpen = true, editingDiet = diet) }
    fun closeNewDiet() { _state.value = _state.value.copy(newDietOpen = false, editingDiet = null) }

    fun createDiet(name: String, entries: List<DietEntry>, tags: List<DietTag> = emptyList(), description: String? = null) {
        if (name.isBlank() || entries.isEmpty()) return
        val editing = _state.value.editingDiet
        viewModelScope.launch {
            runCatching {
                if (editing != null) repository.update(editing, name, entries, tags, description)
                else repository.create(name, entries, tags, description = description)
            }.onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            closeNewDiet()
            sync()
        }
    }
}
