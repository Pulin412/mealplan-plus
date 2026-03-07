package com.mealplanplus.ui.screens.grocery

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.repository.GroceryRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import javax.inject.Inject

enum class SelectionMode {
    RANGE,
    SPECIFIC
}

data class CreateGroceryUiState(
    val name: String = "",
    val selectionMode: SelectionMode = SelectionMode.RANGE,
    val rangeStart: LocalDate = LocalDate.now(),
    val rangeEnd: LocalDate = LocalDate.now().plusDays(6),
    val selectedDates: Set<LocalDate> = emptySet(),
    val plansInRange: List<PlanPreview> = emptyList(),
    val isGenerating: Boolean = false,
    val generatedListId: Long? = null,
    val error: String? = null
) {
    val effectiveDates: List<LocalDate>
        get() = when (selectionMode) {
            SelectionMode.RANGE -> generateSequence(rangeStart) { it.plusDays(1) }
                .takeWhile { !it.isAfter(rangeEnd) }
                .toList()
            SelectionMode.SPECIFIC -> selectedDates.sorted()
        }

    val dateCount: Int
        get() = effectiveDates.size

    val hasPlans: Boolean
        get() = plansInRange.isNotEmpty()

    val defaultName: String
        get() {
            val dates = effectiveDates
            return when {
                dates.isEmpty() -> "Grocery List"
                dates.size == 1 -> "Groceries for ${dates.first().format(DateTimeFormatter.ofPattern("MMM d"))}"
                else -> "Groceries ${dates.first().format(DateTimeFormatter.ofPattern("MMM d"))} - ${dates.last().format(DateTimeFormatter.ofPattern("MMM d"))}"
            }
        }
}

data class PlanPreview(
    val date: LocalDate,
    val dietName: String?
)

@HiltViewModel
class CreateGroceryListViewModel @Inject constructor(
    private val groceryRepository: GroceryRepository,
    private val planRepository: PlanRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroceryUiState())
    val uiState: StateFlow<CreateGroceryUiState> = _uiState.asStateFlow()

    init {
        refreshPlans()
    }

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun setSelectionMode(mode: SelectionMode) {
        _uiState.update { it.copy(selectionMode = mode) }
        refreshPlans()
    }

    fun setRangeStart(date: LocalDate) {
        _uiState.update {
            val newEnd = if (date.isAfter(it.rangeEnd)) date else it.rangeEnd
            it.copy(rangeStart = date, rangeEnd = newEnd)
        }
        refreshPlans()
    }

    fun setRangeEnd(date: LocalDate) {
        _uiState.update {
            val newStart = if (date.isBefore(it.rangeStart)) date else it.rangeStart
            it.copy(rangeStart = newStart, rangeEnd = date)
        }
        refreshPlans()
    }

    fun toggleDate(date: LocalDate) {
        _uiState.update {
            val newDates = if (date in it.selectedDates) {
                it.selectedDates - date
            } else {
                it.selectedDates + date
            }
            it.copy(selectedDates = newDates)
        }
        refreshPlans()
    }

    fun selectThisWeek() {
        val today = LocalDate.now()
        val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
        val endOfWeek = startOfWeek.plusDays(6)
        _uiState.update {
            it.copy(
                selectionMode = SelectionMode.RANGE,
                rangeStart = startOfWeek,
                rangeEnd = endOfWeek
            )
        }
        refreshPlans()
    }

    fun selectNextWeek() {
        val today = LocalDate.now()
        val startOfNextWeek = today.plusDays(8 - today.dayOfWeek.value.toLong())
        val endOfNextWeek = startOfNextWeek.plusDays(6)
        _uiState.update {
            it.copy(
                selectionMode = SelectionMode.RANGE,
                rangeStart = startOfNextWeek,
                rangeEnd = endOfNextWeek
            )
        }
        refreshPlans()
    }

    fun selectNext7Days() {
        _uiState.update {
            it.copy(
                selectionMode = SelectionMode.RANGE,
                rangeStart = LocalDate.now(),
                rangeEnd = LocalDate.now().plusDays(6)
            )
        }
        refreshPlans()
    }

    private fun refreshPlans() {
        viewModelScope.launch {
            val dates = _uiState.value.effectiveDates
            if (dates.isEmpty()) {
                _uiState.update { it.copy(plansInRange = emptyList()) }
                return@launch
            }

            val startDate = dates.minOrNull()!!
            val endDate = dates.maxOrNull()!!

            planRepository.getPlansWithDietNames(
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            ).collect { plans ->
                val previews = plans
                    .filter { LocalDate.parse(it.date) in dates }
                    .map { plan ->
                        PlanPreview(
                            date = LocalDate.parse(plan.date),
                            dietName = plan.dietName
                        )
                    }
                    .filter { it.dietName != null }
                _uiState.update { it.copy(plansInRange = previews) }
            }
        }
    }

    fun generateList() {
        viewModelScope.launch {
            val state = _uiState.value
            val dates = state.effectiveDates

            if (dates.isEmpty()) {
                _uiState.update { it.copy(error = "Please select at least one date") }
                return@launch
            }

            _uiState.update { it.copy(isGenerating = true, error = null) }

            try {
                val name = state.name.ifBlank { state.defaultName }
                val listId = groceryRepository.generateFromDateRange(name, dates)
                _uiState.update { it.copy(isGenerating = false, generatedListId = listId) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isGenerating = false, error = e.message ?: "Failed to generate list")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearGeneratedId() {
        _uiState.update { it.copy(generatedListId = null) }
    }
}
