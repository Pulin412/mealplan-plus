package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDietUiState(
    val name: String = "",
    val description: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<Tag> = emptyList(),
    val newTagName: String = "",
    val isLoading: Boolean = false,
    val savedDietId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class AddDietViewModel @Inject constructor(
    private val dietRepository: DietRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDietUiState())
    val uiState: StateFlow<AddDietUiState> = _uiState.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagRepository.getTagsByUser().collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    fun updateName(name: String) { _uiState.update { it.copy(name = name) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }

    fun toggleTag(tagId: Long) {
        _uiState.update { state ->
            val tags = state.selectedTagIds.toMutableSet()
            if (tags.contains(tagId)) tags.remove(tagId) else tags.add(tagId)
            state.copy(selectedTagIds = tags)
        }
    }

    fun updateNewTagName(name: String) { _uiState.update { it.copy(newTagName = name) } }

    fun createAndSelectTag() {
        val name = _uiState.value.newTagName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val tagId = tagRepository.createTag(name)
                _uiState.update { state ->
                    state.copy(
                        selectedTagIds = state.selectedTagIds + tagId,
                        newTagName = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Tag already exists") }
            }
        }
    }

    fun saveDiet() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val diet = Diet(
                    userId = 0,
                    name = state.name.trim(),
                    description = state.description.takeIf { it.isNotBlank() }?.trim()
                )
                val dietId = dietRepository.insertDiet(diet)
                if (state.selectedTagIds.isNotEmpty()) {
                    dietRepository.setDietTags(dietId, state.selectedTagIds.toList())
                }
                _uiState.update { it.copy(isLoading = false, savedDietId = dietId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to save") }
            }
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
}
