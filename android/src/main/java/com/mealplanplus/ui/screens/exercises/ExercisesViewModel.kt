package com.mealplanplus.ui.screens.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.cache.ResponseCache
import com.mealplanplus.data.cache.render
import com.mealplanplus.data.generated.model.ExerciseDto
import com.mealplanplus.data.generated.model.TagDto
import com.mealplanplus.data.generated.model.TemplateExerciseDto
import com.mealplanplus.data.generated.model.TemplateSetDto
import com.mealplanplus.data.generated.model.WorkoutSessionDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.data.repository.ExerciseRepository
import com.mealplanplus.data.repository.WorkoutRepository
import com.mealplanplus.data.repository.WorkoutSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibTab { EXERCISES, WORKOUTS, LOGS }

/** The whole library screen's server data, cached as one payload for instant paint on cold open. */
private data class ExercisesSnapshot(
    val exercises: List<ExerciseDto>,
    val workouts: List<WorkoutTemplateDto>,
    val logs: List<WorkoutSessionDto>,
    val tags: List<TagDto>,
)

/** Open exercise editor: create (id null) or edit (id set). */
data class ExerciseEditor(
    val id: Long? = null,
    val name: String = "",
    val description: String = "",
    val tagIds: Set<Long> = emptySet(),
)

/** One target set inside the builder: reps + optional weight (kg). */
data class BuilderSet(
    val reps: Int? = 10,
    val weightKg: Double? = null,
)

/** One exercise row inside the workout builder, with its ordered per-set targets. */
data class BuilderItem(
    val exerciseId: Long,
    val name: String,
    val sets: List<BuilderSet> = listOf(BuilderSet()),
)

/** Open workout builder: create (id null) or edit (id set). */
data class WorkoutBuilder(
    val id: Long? = null,
    val name: String = "",
    val items: List<BuilderItem> = emptyList(),
    val pickerOpen: Boolean = false,
    val pickerSearch: String = "",
) {
    val canSave: Boolean get() = name.isNotBlank() && items.isNotEmpty()
}

data class ExercisesUiState(
    val tab: LibTab = LibTab.EXERCISES,
    val exercises: List<ExerciseDto> = emptyList(),
    val workouts: List<WorkoutTemplateDto> = emptyList(),
    val logs: List<WorkoutSessionDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val editor: ExerciseEditor? = null,
    val builder: WorkoutBuilder? = null,
    val openLog: WorkoutSessionDto? = null,
    val today: java.time.LocalDate = java.time.LocalDate.now(),
    val logsMonth: java.time.YearMonth = java.time.YearMonth.now(),
) {
    val tagName: Map<Long, String> get() = tags.associate { it.id to it.name }
    val exerciseName: Map<Long, String> get() = exercises.associate { (it.id ?: -1L) to it.name }

    /** Sessions grouped by their logged date, most recent set-count first within a day. */
    val logsByDate: Map<java.time.LocalDate, List<WorkoutSessionDto>>
        get() = logs.filter { it.date != null }.groupBy { it.date!! }
}

@HiltViewModel
class ExercisesViewModel @Inject constructor(
    private val exerciseRepo: ExerciseRepository,
    private val workoutRepo: WorkoutRepository,
    private val sessionRepo: WorkoutSessionRepository,
    private val responseCache: ResponseCache,
) : ViewModel() {

    private val _state = MutableStateFlow(ExercisesUiState())
    val state: StateFlow<ExercisesUiState> = _state

    init { load() }

    /**
     * Read-through cached load: paints the last-known library instantly on a cold open, then
     * refreshes exercises / workouts / logs / tags from the server in the background.
     */
    private fun load() {
        viewModelScope.launch {
            responseCache.stream<ExercisesSnapshot>("exercises.snapshot") {
                coroutineScope {
                    val exercises = async { exerciseRepo.list() }
                    val workouts = async { workoutRepo.list() }
                    val logs = async { sessionRepo.list() }
                    val tags = async { exerciseRepo.listTags() }
                    ExercisesSnapshot(exercises.await(), workouts.await(), logs.await(), tags.await())
                }
            }.collect { res ->
                _state.update { st ->
                    val has = st.exercises.isNotEmpty() || st.workouts.isNotEmpty() ||
                        st.logs.isNotEmpty() || st.tags.isNotEmpty()
                    val r = res.render(hasContent = has)
                    if (r.keep) st.copy(loading = r.loading, error = r.error)
                    else st.copy(
                        exercises = r.value?.exercises ?: st.exercises,
                        workouts = r.value?.workouts ?: st.workouts,
                        logs = r.value?.logs ?: st.logs,
                        tags = r.value?.tags ?: st.tags,
                        loading = r.loading,
                        error = r.error,
                    )
                }
            }
        }
    }

    fun setTab(tab: LibTab) { _state.value = _state.value.copy(tab = tab) }

    // ── Logs (read-only) ──────────────────────────────────────────────────────────
    fun openLog(session: WorkoutSessionDto) { _state.value = _state.value.copy(openLog = session) }
    fun closeLog() { _state.value = _state.value.copy(openLog = null) }
    fun prevLogsMonth() { _state.value = _state.value.copy(logsMonth = _state.value.logsMonth.minusMonths(1)) }
    fun nextLogsMonth() { _state.value = _state.value.copy(logsMonth = _state.value.logsMonth.plusMonths(1)) }

    // ── Exercise editor ──────────────────────────────────────────────────────────
    fun openNewExercise() { _state.value = _state.value.copy(editor = ExerciseEditor()) }
    fun openEditExercise(e: ExerciseDto) {
        _state.value = _state.value.copy(
            editor = ExerciseEditor(id = e.id, name = e.name, description = e.description ?: "",
                tagIds = (e.tagIds ?: emptyList()).toSet()),
        )
    }
    fun closeEditor() { _state.value = _state.value.copy(editor = null) }
    fun setEditorName(name: String) { _state.value = _state.value.copy(editor = _state.value.editor?.copy(name = name)) }
    fun setEditorDescription(desc: String) { _state.value = _state.value.copy(editor = _state.value.editor?.copy(description = desc)) }
    fun toggleEditorTag(tagId: Long) {
        val ed = _state.value.editor ?: return
        val next = if (tagId in ed.tagIds) ed.tagIds - tagId else ed.tagIds + tagId
        _state.value = _state.value.copy(editor = ed.copy(tagIds = next))
    }

    fun saveExercise() {
        val ed = _state.value.editor ?: return
        if (ed.name.isBlank()) return
        val desc = ed.description.trim().ifBlank { null }
        viewModelScope.launch {
            val result = if (ed.id == null) exerciseRepo.create(ed.name, desc, ed.tagIds.toList())
            else exerciseRepo.update(ed.id, ed.name, desc, ed.tagIds.toList())
            result.onSuccess { closeEditor(); load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun deleteExercise(id: Long) {
        viewModelScope.launch {
            exerciseRepo.delete(id).onSuccess { closeEditor(); load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    // ── Workout builder ──────────────────────────────────────────────────────────
    fun openNewWorkout() { _state.value = _state.value.copy(builder = WorkoutBuilder()) }
    fun openEditWorkout(w: WorkoutTemplateDto) {
        _state.value = _state.value.copy(
            builder = WorkoutBuilder(
                id = w.id, name = w.name,
                items = (w.exercises ?: emptyList()).map { te ->
                    val sets = (te.sets ?: emptyList()).sortedBy { it.setNumber }
                        .map { BuilderSet(reps = it.reps, weightKg = it.weightKg) }
                    BuilderItem(te.exerciseId, te.exerciseName ?: "Exercise", sets.ifEmpty { listOf(BuilderSet()) })
                },
            ),
        )
    }
    fun closeBuilder() { _state.value = _state.value.copy(builder = null) }
    fun setBuilderName(name: String) { _state.value = _state.value.copy(builder = _state.value.builder?.copy(name = name)) }
    fun openPicker() { _state.value = _state.value.copy(builder = _state.value.builder?.copy(pickerOpen = true, pickerSearch = "")) }
    fun closePicker() { _state.value = _state.value.copy(builder = _state.value.builder?.copy(pickerOpen = false)) }
    fun setPickerSearch(q: String) { _state.value = _state.value.copy(builder = _state.value.builder?.copy(pickerSearch = q)) }

    fun addToBuilder(e: ExerciseDto) {
        val b = _state.value.builder ?: return
        val id = e.id ?: return
        if (b.items.any { it.exerciseId == id }) return
        _state.value = _state.value.copy(builder = b.copy(items = b.items + BuilderItem(id, e.name), pickerOpen = false))
    }
    fun removeFromBuilder(exerciseId: Long) {
        val b = _state.value.builder ?: return
        _state.value = _state.value.copy(builder = b.copy(items = b.items.filterNot { it.exerciseId == exerciseId }))
    }

    // ── Per-set editing ──────────────────────────────────────────────────────────
    /** Duplicate a specific set (reps + weight), inserting the copy right after it. */
    fun duplicateSet(exerciseId: Long, index: Int) = updateItem(exerciseId) {
        val s = it.sets.getOrNull(index) ?: return@updateItem it
        it.copy(sets = it.sets.toMutableList().apply { add(index + 1, s) })
    }
    fun removeSet(exerciseId: Long, index: Int) = updateItem(exerciseId) {
        if (it.sets.size <= 1) it else it.copy(sets = it.sets.filterIndexed { i, _ -> i != index })
    }
    fun setReps(exerciseId: Long, index: Int, reps: Int?) =
        updateSet(exerciseId, index) { it.copy(reps = reps?.coerceIn(1, 100)) }
    fun setWeight(exerciseId: Long, index: Int, weightKg: Double?) =
        updateSet(exerciseId, index) { it.copy(weightKg = weightKg?.coerceAtLeast(0.0)) }

    private fun updateSet(exerciseId: Long, index: Int, f: (BuilderSet) -> BuilderSet) = updateItem(exerciseId) {
        it.copy(sets = it.sets.mapIndexed { i, s -> if (i == index) f(s) else s })
    }
    private fun updateItem(exerciseId: Long, f: (BuilderItem) -> BuilderItem) {
        val b = _state.value.builder ?: return
        _state.value = _state.value.copy(builder = b.copy(items = b.items.map { if (it.exerciseId == exerciseId) f(it) else it }))
    }

    /** Library exercises not already in the builder, filtered by the picker search. */
    fun pickerCandidates(): List<ExerciseDto> {
        val b = _state.value.builder ?: return emptyList()
        val chosen = b.items.map { it.exerciseId }.toSet()
        val q = b.pickerSearch.trim()
        val tagName = _state.value.tagName
        return _state.value.exercises.filter { e ->
            e.id !in chosen && (q.isBlank() ||
                e.name.contains(q, true) ||
                (e.tagIds ?: emptyList()).any { tagName[it]?.contains(q, true) == true })
        }
    }

    fun saveWorkout() {
        val b = _state.value.builder ?: return
        if (!b.canSave) return
        val entries = b.items.map { item ->
            TemplateExerciseDto(
                exerciseId = item.exerciseId,
                sets = item.sets.mapIndexed { i, s -> TemplateSetDto(setNumber = i, reps = s.reps, weightKg = s.weightKg) },
            )
        }
        viewModelScope.launch {
            val result = if (b.id == null) workoutRepo.create(b.name, entries)
            else workoutRepo.update(b.id, b.name, entries)
            result.onSuccess { closeBuilder(); load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            workoutRepo.delete(id).onSuccess { closeBuilder(); load() }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }
}
