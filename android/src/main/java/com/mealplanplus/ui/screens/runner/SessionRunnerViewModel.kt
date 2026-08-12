package com.mealplanplus.ui.screens.runner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.ExerciseNoteDto
import com.mealplanplus.data.generated.model.WorkoutSessionDto
import com.mealplanplus.data.generated.model.WorkoutSetDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.data.local.SessionProgressStore
import com.mealplanplus.data.repository.ExerciseRepository
import com.mealplanplus.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class RunPhase { LOADING, READY, ACTIVE, DONE }

/** One target/logged set: reps + optional weight (kg). */
data class RunSet(val reps: Int?, val weightKg: Double?)

/** A library exercise offered by the "Add exercise" picker during logging. */
data class LibExercise(val id: Long, val name: String, val description: String?)

/** One exercise in the runner with its editable sets, template targets, and last-session sets. */
data class RunExercise(
    val exerciseId: Long,
    val name: String,
    val sets: List<RunSet>,
    val description: String? = null,
    val templateSets: List<RunSet> = emptyList(),
    val lastTime: List<RunSet> = emptyList(),
    val note: String = "",            // this session's per-exercise note-to-self
    val lastNote: String? = null,     // the note from last time (shown in the Copy-last preview)
    val fromTemplate: Boolean = true, // true = part of the planned workout; false = added ad-hoc (removable)
)

data class RunnerUiState(
    val phase: RunPhase = RunPhase.LOADING,
    val workoutName: String = "",
    val workoutNote: String = "",     // whole-workout note for this session
    val sessionId: Long? = null,
    val exercises: List<RunExercise> = emptyList(),
    val library: List<LibExercise> = emptyList(),   // for the "Add exercise" picker
    val doneExerciseIds: Set<Long> = emptySet(),     // exercises checked off this session (persisted locally)
    val canAddExercise: Boolean = false,             // only a planned workout can add exercises; a standalone single-exercise log cannot
    val error: String? = null,
    val busy: Boolean = false,
)

/**
 * Runs a planned workout: Ready (template + "Last time") → Active (log set-by-set, auto-saved so it
 * resumes after navigating away) → Done (read-only log). Backed by [WorkoutSessionRepository] —
 * start pre-populates from the template, each edit PUTs the session, finish marks it complete.
 */
@HiltViewModel
class SessionRunnerViewModel @Inject constructor(
    private val workoutsApi: WorkoutTemplatesApi,
    private val sessionRepo: WorkoutSessionRepository,
    private val exerciseRepo: ExerciseRepository,
    private val progressStore: SessionProgressStore,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val templateId: Long = savedState.get<String>("templateId")?.toLongOrNull() ?: 0L
    private val exerciseId: Long = savedState.get<String>("exerciseId")?.toLongOrNull() ?: 0L
    private val activityName: String = savedState.get<String>("name")?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: "Workout"

    private var template: WorkoutTemplateDto? = null
    private var descById: Map<Long, String?> = emptyMap()
    private var libNames: Map<Long, String> = emptyMap()
    private val today = LocalDate.now()

    private val _state = MutableStateFlow(RunnerUiState(workoutName = activityName, canAddExercise = templateId > 0))
    val state: StateFlow<RunnerUiState> = _state

    init { load() }

    private fun exerciseNames(): Map<Long, String> =
        libNames + (template?.exercises ?: emptyList()).associate { it.exerciseId to (it.exerciseName ?: libNames[it.exerciseId] ?: "Exercise") }

    private fun load() {
        viewModelScope.launch {
            template = if (templateId > 0) runCatching { workoutsApi.getWorkoutTemplate(templateId).body() }.getOrNull() else null
            val lib = exerciseRepo.list()
            descById = lib.associate { (it.id ?: -1L) to it.description }
            libNames = lib.associate { (it.id ?: -1L) to it.name }
            _state.update { it.copy(library = lib.mapNotNull { e -> e.id?.let { id -> LibExercise(id, e.name, e.description) } }) }
            val existing = sessionRepo.listForDate(today).firstOrNull { it.name == activityName }
            if (existing != null) {
                val exercises = exercisesFromSession(existing)
                val active = existing.isCompleted != true
                _state.update {
                    it.copy(
                        phase = if (active) RunPhase.ACTIVE else RunPhase.DONE,
                        sessionId = existing.id,
                        workoutNote = existing.notes.orEmpty(),
                        exercises = exercises,
                        doneExerciseIds = if (active) existing.id?.let { id -> progressStore.getDone(id) }.orEmpty() else emptySet(),
                    )
                }
                if (active) loadLastTimes()
            } else {
                val ready = if (template != null) exercisesFromTemplate() else exerciseReadyList()
                _state.update { it.copy(phase = RunPhase.READY, exercises = ready) }
                loadLastTimes()
            }
        }
    }

    /** Build the Ready-phase view from template targets. */
    private fun exercisesFromTemplate(): List<RunExercise> =
        (template?.exercises ?: emptyList()).sortedBy { it.orderIndex }.map { te ->
            val sets = (te.sets ?: emptyList()).sortedBy { it.setNumber }.map { RunSet(it.reps, it.weightKg) }
            RunExercise(te.exerciseId, te.exerciseName ?: "Exercise", sets = sets, description = descById[te.exerciseId], templateSets = sets)
        }

    /** Ready view for an ad-hoc single exercise (no template): default 3 × 10. */
    private fun exerciseReadyList(): List<RunExercise> {
        if (exerciseId <= 0) return emptyList()
        val sets = List(3) { RunSet(10, null) }
        return listOf(RunExercise(exerciseId, libNames[exerciseId] ?: activityName, sets = sets,
            description = descById[exerciseId], templateSets = sets))
    }

    /** Rebuild exercises from a live session's logged sets (grouped by exercise, template order). */
    private fun exercisesFromSession(session: WorkoutSessionDto): List<RunExercise> {
        val names = exerciseNames()
        val noteByEx = (session.exerciseNotes ?: emptyList()).associate { it.exerciseId to it.note.orEmpty() }
        val order = (template?.exercises ?: emptyList()).sortedBy { it.orderIndex }.map { it.exerciseId }
        // Exercises that belong to the plan (template members, or the base exercise of an ad-hoc
        // single-exercise session) are NOT removable; anything else was added on the fly → removable.
        val plannedIds = order.toSet() + (if (exerciseId > 0) setOf(exerciseId) else emptySet())
        val grouped = (session.sets ?: emptyList()).groupBy { it.exerciseId }
        val ids = (order + grouped.keys).distinct()
        return ids.mapNotNull { id ->
            val sets = grouped[id]?.sortedBy { it.setNumber }?.map { RunSet(it.reps, it.weightKg) } ?: return@mapNotNull null
            RunExercise(id, names[id] ?: "Exercise", sets = sets, description = descById[id], note = noteByEx[id].orEmpty(),
                fromTemplate = id in plannedIds)
        }
    }

    /** Fetch each exercise's most-recent prior session sets (for "Last time" + Copy last). */
    private fun loadLastTimes() {
        viewModelScope.launch {
            val ex = _state.value.exercises
            val last = ex.map { e -> async { e.exerciseId to sessionRepo.lastFullForExercise(e.exerciseId, activityName) } }
                .map { it.await() }.toMap()
            _state.update { s ->
                s.copy(exercises = s.exercises.map { e ->
                    val l = last[e.exerciseId]
                    e.copy(
                        lastTime = l?.sets?.map { RunSet(it.reps, it.weightKg) } ?: e.lastTime,
                        lastNote = l?.note ?: e.lastNote,
                    )
                })
            }
        }
    }

    // ── Ready → Active ─────────────────────────────────────────────────────────────
    fun start() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            // Template workouts start server-side (sets from targets); an ad-hoc exercise creates a session from the Ready sets.
            val result = if (template != null) sessionRepo.start(templateId)
                         else sessionRepo.create(activityName, today, currentSets())
            result
                .onSuccess { session ->
                    val exercises = mergeLastTimes(exercisesFromSession(session))
                    _state.update { it.copy(busy = false, phase = RunPhase.ACTIVE, sessionId = session.id, exercises = exercises,
                        doneExerciseIds = session.id?.let { id -> progressStore.getDone(id) }.orEmpty()) }
                }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    /** Preserve any lastTime we already loaded when swapping the exercise list. */
    private fun mergeLastTimes(fresh: List<RunExercise>): List<RunExercise> {
        val prev = _state.value.exercises.associateBy { it.exerciseId }
        return fresh.map { it.copy(lastTime = prev[it.exerciseId]?.lastTime ?: it.lastTime, lastNote = prev[it.exerciseId]?.lastNote ?: it.lastNote) }
    }

    // ── Active editing (each change is persisted) ───────────────────────────────────
    fun setReps(exId: Long, index: Int, reps: Int?) =
        editSet(exId, index) { it.copy(reps = reps?.coerceIn(0, 100)) }
    fun setWeight(exId: Long, index: Int, weightKg: Double?) =
        editSet(exId, index) { it.copy(weightKg = weightKg?.coerceAtLeast(0.0)) }

    fun addSet(exId: Long) = editExercise(exId) {
        val last = it.sets.lastOrNull() ?: RunSet(10, null)
        it.copy(sets = it.sets + last.copy())
    }
    fun removeSet(exId: Long, index: Int) = editExercise(exId) {
        if (it.sets.size <= 1) it else it.copy(sets = it.sets.filterIndexed { i, _ -> i != index })
    }
    /** Check/uncheck an exercise as done for this session — locks its sets read-only; persisted locally. */
    fun toggleExerciseDone(exId: Long) {
        val id = _state.value.sessionId ?: return
        val next = _state.value.doneExerciseIds.let { if (exId in it) it - exId else it + exId }
        progressStore.setDone(id, next)
        _state.update { it.copy(doneExerciseIds = next) }
    }

    /** Fill this exercise's sets from the last logged session — sets/reps only, never the note. */
    fun copyLast(exId: Long) = editExercise(exId) {
        if (it.lastTime.isEmpty()) it else it.copy(sets = it.lastTime.map { s -> s.copy() })
    }

    /** Per-exercise note-to-self for this session (persisted with the session). */
    fun setExerciseNote(exId: Long, note: String) = editExercise(exId) { it.copy(note = note) }

    /** Whole-workout note for this session. */
    fun setWorkoutNote(note: String) {
        _state.update { it.copy(workoutNote = note) }
        persist()
    }

    /**
     * Add a library exercise to THIS session's log on the fly (default 3 × 10). It's persisted to the
     * session only — the workout template is never touched. No-op if already present.
     */
    fun addExercise(exerciseId: Long) {
        if (_state.value.exercises.any { it.exerciseId == exerciseId }) return
        val lib = _state.value.library.firstOrNull { it.id == exerciseId } ?: return
        val sets = List(3) { RunSet(10, null) }
        _state.update { s -> s.copy(exercises = s.exercises + RunExercise(exerciseId, lib.name, sets = sets, description = lib.description, fromTemplate = false)) }
        persist()
        viewModelScope.launch {
            val l = sessionRepo.lastFullForExercise(exerciseId, activityName) ?: return@launch
            val last = l.sets.map { RunSet(it.reps, it.weightKg) }
            _state.update { s -> s.copy(exercises = s.exercises.map { if (it.exerciseId == exerciseId) it.copy(lastTime = last, lastNote = l.note) else it }) }
        }
    }

    /**
     * Remove an on-the-fly-added exercise from THIS session (never a planned/template exercise).
     * Its sets are dropped on the next persist; no-op for template exercises.
     */
    fun removeExercise(exId: Long) {
        val ex = _state.value.exercises.firstOrNull { it.exerciseId == exId } ?: return
        if (ex.fromTemplate) return
        _state.update { s -> s.copy(
            exercises = s.exercises.filterNot { it.exerciseId == exId },
            doneExerciseIds = s.doneExerciseIds - exId,
        ) }
        persist()
    }

    private fun editSet(exId: Long, index: Int, f: (RunSet) -> RunSet) = editExercise(exId) {
        it.copy(sets = it.sets.mapIndexed { i, s -> if (i == index) f(s) else s })
    }
    private fun editExercise(exId: Long, f: (RunExercise) -> RunExercise) {
        _state.update { s -> s.copy(exercises = s.exercises.map { if (it.exerciseId == exId) f(it) else it }) }
        persist()
    }

    private fun currentSets(): List<WorkoutSetDto> =
        _state.value.exercises.flatMap { ex ->
            ex.sets.mapIndexed { i, s -> WorkoutSetDto(exerciseId = ex.exerciseId, setNumber = i, reps = s.reps, weightKg = s.weightKg) }
        }

    private fun currentExerciseNotes(): List<ExerciseNoteDto> =
        _state.value.exercises.filter { it.note.isNotBlank() }
            .map { ExerciseNoteDto(exerciseId = it.exerciseId, note = it.note) }

    private fun sessionPayload(id: Long) = WorkoutSessionDto(
        id = id, name = _state.value.workoutName, date = today, isCompleted = false,
        notes = _state.value.workoutNote.ifBlank { null },
        sets = currentSets(), exerciseNotes = currentExerciseNotes(),
    )

    /** Save current sets + notes to the in-progress session so we can resume after leaving the screen. */
    private fun persist() {
        val id = _state.value.sessionId ?: return
        viewModelScope.launch { sessionRepo.update(sessionPayload(id)) }
    }

    // ── Finish / re-edit ────────────────────────────────────────────────────────────
    fun finish() {
        val id = _state.value.sessionId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            sessionRepo.update(sessionPayload(id))
            sessionRepo.finish(id)
                .onSuccess { _state.update { it.copy(busy = false, phase = RunPhase.DONE) } }
                .onFailure { e -> _state.update { it.copy(busy = false, error = e.message) } }
        }
    }

    /** Re-open a completed log for editing (Done → Active); re-finishing upserts the same day's log. */
    fun edit() { _state.update { it.copy(phase = RunPhase.ACTIVE) } }
}
