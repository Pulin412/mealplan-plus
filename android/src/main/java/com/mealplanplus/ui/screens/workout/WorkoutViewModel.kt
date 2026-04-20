package com.mealplanplus.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategoryEntity
import com.mealplanplus.data.model.PlannedWorkout
import com.mealplanplus.data.model.PlannedWorkoutWithTemplate
import com.mealplanplus.data.model.WorkoutSession
import com.mealplanplus.data.model.WorkoutSet
import com.mealplanplus.data.model.WorkoutSessionWithSets
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateExercise
import com.mealplanplus.data.model.WorkoutTemplateWithExercises
import com.mealplanplus.data.repository.WorkoutRepository
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WorkoutUiState(
    val sessions: List<WorkoutSessionWithSets> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val templates: List<WorkoutTemplateWithExercises> = emptyList(),
    val categories: List<ExerciseCategoryEntity> = emptyList(),
    val plannedForDate: List<PlannedWorkoutWithTemplate> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val activeSession: WorkoutSession? = null,
    val activeSets: List<WorkoutSet> = emptyList(),
    val pendingExercise: Exercise? = null
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
        loadTemplates()
        loadCategories()
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
            workoutRepository.getAllExercisesForUser(userId).collect { list ->
                _uiState.update { it.copy(exercises = list) }
            }
        }
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            workoutRepository.getTemplatesForUser(userId).collect { list ->
                _uiState.update { it.copy(templates = list) }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            workoutRepository.getAllCategories().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
    }

    fun loadPlannedForDate(date: LocalDate) {
        viewModelScope.launch {
            workoutRepository.getPlannedForDate(userId, date.toEpochMs()).collect { list ->
                _uiState.update { it.copy(plannedForDate = list) }
            }
        }
    }

    // ── Exercise filtering ────────────────────────────────────────────────────

    fun filterByCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun filteredExercises(): List<Exercise> {
        val state = _uiState.value
        return state.exercises.filter { ex ->
            (state.selectedCategory == null || ex.category == state.selectedCategory) &&
            (state.searchQuery.isBlank() || ex.name.contains(state.searchQuery, ignoreCase = true))
        }
    }

    // ── Exercise CRUD ─────────────────────────────────────────────────────────

    fun saveExercise(
        existingId: Long?,
        name: String,
        category: String,
        muscleGroup: String,
        equipment: String,
        description: String,
        videoLink: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val exercise = Exercise(
                id = existingId ?: 0L,
                name = name.trim(),
                category = category,
                muscleGroup = muscleGroup.trim().ifBlank { null },
                equipment = equipment.trim().ifBlank { null },
                description = description.trim().ifBlank { null },
                videoLink = videoLink.trim().ifBlank { null },
                isSystem = false,
                userId = userId
            )
            if (existingId == null) {
                workoutRepository.insertExercise(exercise)
            } else {
                workoutRepository.updateExercise(exercise)
            }
            onDone()
        }
    }

    fun deleteExercise(exercise: Exercise) {
        viewModelScope.launch { workoutRepository.deleteExercise(exercise) }
    }

    // ── Category CRUD ─────────────────────────────────────────────────────────

    fun addCategory(name: String) {
        viewModelScope.launch { workoutRepository.addCategory(name) }
    }

    fun deleteCategory(category: ExerciseCategoryEntity) {
        viewModelScope.launch { workoutRepository.deleteCategory(category) }
    }

    // ── Template CRUD ─────────────────────────────────────────────────────────

    fun saveTemplate(
        existingId: Long?,
        name: String,
        category: String,
        notes: String,
        exercises: List<WorkoutTemplateExercise>,
        onDone: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val template = WorkoutTemplate(
                id = existingId ?: 0L,
                userId = userId,
                name = name.trim(),
                category = category,
                notes = notes.trim().ifBlank { null }
            )
            val id = workoutRepository.saveTemplate(template, exercises)
            onDone(id)
        }
    }

    fun deleteTemplate(template: WorkoutTemplate) {
        viewModelScope.launch { workoutRepository.deleteTemplate(template) }
    }

    suspend fun getTemplateWithExercises(id: Long): WorkoutTemplateWithExercises? =
        workoutRepository.getTemplateWithExercises(id)

    suspend fun getExerciseById(id: Long): Exercise? =
        workoutRepository.getExerciseById(id)

    fun selectExercise(exercise: Exercise) {
        _uiState.update { it.copy(pendingExercise = exercise) }
    }

    fun consumeSelectedExercise() {
        _uiState.update { it.copy(pendingExercise = null) }
    }

    // ── Session logging ───────────────────────────────────────────────────────

    fun startSession(name: String, date: LocalDate, templateId: Long? = null) {
        viewModelScope.launch {
            val session = WorkoutSession(
                userId = userId,
                name = name,
                date = date.toEpochMs(),
                notes = templateId?.toString()
            )
            val id = workoutRepository.createSession(session)
            _uiState.update { it.copy(activeSession = session.copy(id = id), activeSets = emptyList()) }
        }
    }

    fun addSet(exerciseId: Long, reps: Int?, weightKg: Double?, durationSec: Int?, notes: String? = null) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            val setNumber = _uiState.value.activeSets.count { it.exerciseId == exerciseId } + 1
            val set = WorkoutSet(
                sessionId = session.id,
                exerciseId = exerciseId,
                setNumber = setNumber,
                reps = reps,
                weightKg = weightKg,
                durationSeconds = durationSec,
                notes = notes
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
        viewModelScope.launch { workoutRepository.deleteSession(session) }
    }

    // ── Planning ──────────────────────────────────────────────────────────────

    fun planWorkout(date: LocalDate, templateId: Long) {
        viewModelScope.launch {
            workoutRepository.planWorkout(
                PlannedWorkout(userId = userId, date = date.toEpochMs(), templateId = templateId)
            )
        }
    }

    fun unplanWorkout(date: LocalDate, templateId: Long) {
        viewModelScope.launch {
            workoutRepository.unplanWorkout(userId, date.toEpochMs(), templateId)
        }
    }
}
