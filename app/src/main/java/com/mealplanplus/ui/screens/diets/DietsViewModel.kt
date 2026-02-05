package com.mealplanplus.ui.screens.diets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.repository.DietRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DietsViewModel @Inject constructor(
    private val repository: DietRepository
) : ViewModel() {

    val diets: StateFlow<List<Diet>> = repository.getAllDiets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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
