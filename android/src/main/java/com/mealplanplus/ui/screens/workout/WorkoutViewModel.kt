package com.mealplanplus.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSet
import com.mealplanplus.data.model.WorkoutSessionWithSets
import com.mealplanplus.data.repository.WorkoutRepository
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WorkoutUiState(
    val sessions: List<WorkoutSessionWithSets> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val selectedCategory: ExerciseCategory? = null,
    val searchQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    // active session being logged
    val activeSession: WorkoutSession? = null,
    val activeSets: List<WorkoutSet> = emptyList()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState: StateFlow<WorkoutUiState> = _uiState.asStateFlow()

    private val userId get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    init {
        loadHistory()
        loadExercises()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            workoutRepository.getSessions(userId).collect { list ->
                val withSets = list.map { session ->
                    workoutRepository.getSessionWithSets(session.id)
                        ?: WorkoutSessionWithSets(session, emptyList())
                }
                _uiState.update { it.copy(sessions = withSets) }
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            workoutRepository.getAllExercises().collect { list ->
                _uiState.update { it.copy(exercises = list) }
            }
        }
    }

    fun filterByCategory(category: ExerciseCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun filteredExercises(): List<Exercise> {
        val state = _uiState.value
        return state.exercises
            .filter { ex ->
                (state.selectedCategory == null || ex.category == state.selectedCategory) &&
                (state.searchQuery.isBlank() || ex.name.contains(state.searchQuery, ignoreCase = true))
            }
    }

    fun startSession(name: String, date: LocalDate) {
        viewModelScope.launch {
            val session = WorkoutSession(
                userId = userId,
                name = name,
                date = date.toEpochMs()
            )
            val id = workoutRepository.createSession(session)
            _uiState.update { it.copy(activeSession = session.copy(id = id), activeSets = emptyList()) }
        }
    }

    fun addSet(exerciseId: Long, reps: Int?, weightKg: Double?, durationSec: Int?) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            val setNumber = _uiState.value.activeSets.count { it.exerciseId == exerciseId } + 1
            val set = WorkoutSet(
                sessionId = session.id,
                exerciseId = exerciseId,
                setNumber = setNumber,
                reps = reps,
                weightKg = weightKg,
                durationSeconds = durationSec
            )
            workoutRepository.addSet(set)
            _uiState.update { it.copy(activeSets = it.activeSets + set) }
        }
    }

    fun finishSession() {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            workoutRepository.updateSession(session.copy(isCompleted = true, updatedAt = System.currentTimeMillis()))
            _uiState.update { it.copy(activeSession = null, activeSets = emptyList()) }
        }
    }

    fun deleteSession(session: WorkoutSession) {
        viewModelScope.launch {
            workoutRepository.deleteSession(session)
        }
    }
}
