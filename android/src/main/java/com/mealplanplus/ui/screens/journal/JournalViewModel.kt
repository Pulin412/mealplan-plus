package com.mealplanplus.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.ExercisesApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.JournalApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.api.MealsApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.data.generated.model.DietDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.JournalEntryDto
import com.mealplanplus.data.generated.model.MealDto
import com.mealplanplus.data.model.MEAL_SLOTS
import com.mealplanplus.data.repository.WorkoutSessionRepository
import com.mealplanplus.data.repository.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

enum class JStatus { DONE, MISSED, PENDING, NONE }
data class JFood(val name: String, val meta: String)
data class JMeal(val slot: String, val name: String, val kcal: Int, val items: List<JFood>)
data class JLogSet(val label: String)
data class JLogExercise(val name: String, val sets: List<JLogSet>)
/** [log] is the read-only logged sets, populated only for a completed ("done") session. */
data class JWorkout(val key: String, val name: String, val templateId: Long?, val status: String, val log: List<JLogExercise> = emptyList())
data class JDay(
    val date: LocalDate,
    val dietName: String?,
    val dietStatus: JStatus,
    val meals: List<JMeal>,
    val workouts: List<JWorkout>,
    val workoutStatus: JStatus,
    val note: String,
)

data class JournalUiState(
    val today: LocalDate = LocalDate.now(),
    val endDate: LocalDate = LocalDate.now(),
    val fromDate: LocalDate = LocalDate.now().with(DayOfWeek.MONDAY),
    val entries: List<JDay> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val atToday: Boolean get() = endDate == today
}

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val plansApi: PlansApi,
    private val loggingApi: LoggingApi,
    private val dietsApi: DietsApi,
    private val mealsApi: MealsApi,
    private val foodsApi: FoodsApi,
    private val exercisesApi: ExercisesApi,
    private val journalApi: JournalApi,
    private val sessionRepo: WorkoutSessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(JournalUiState())
    val state: StateFlow<JournalUiState> = _state

    // Raw caches — entries are rebuilt from these whenever anything loads.
    private var dietsById: Map<Long, DietDto> = emptyMap()
    private var mealsById: Map<Long, MealResolved> = emptyMap()
    private var sessionsByDate: Map<LocalDate, List<com.mealplanplus.data.generated.model.WorkoutSessionDto>> = emptyMap()
    private var plansByDate: Map<LocalDate, DayPlanDto> = emptyMap()
    private var completedDays: Set<LocalDate> = emptySet()
    private var notes: Map<LocalDate, String> = emptyMap()
    private var exerciseNameById: Map<Long, String> = emptyMap()

    private data class MealResolved(val name: String, val kcal: Int, val items: List<JFood>)

    init {
        loadLibrary()
        loadWindow()
    }

    private fun loadLibrary() {
        viewModelScope.launch {
            runCatching {
                val foods = foodsApi.listFoods(false).body().orEmpty().associateBy { it.id }
                val meals = mealsApi.listMeals(false).body().orEmpty()
                dietsById = dietsApi.listDiets(false).body().orEmpty().mapNotNull { d -> d.id?.let { it to d } }.toMap()
                mealsById = meals.mapNotNull { m ->
                    m.id?.let { id ->
                        var kcal = 0.0
                        val items = (m.items ?: emptyList()).map { it ->
                            kcal += foodKcal(foods[it.foodId], it.quantity, it.unit.value)
                            JFood(foods[it.foodId]?.name ?: "Food", "${trimNum(it.quantity)} ${unitLabel(it.unit.value)}")
                        }
                        id to MealResolved(m.name, kcal.roundToInt(), items)
                    }
                }.toMap()
                sessionsByDate = sessionRepo.list().filter { it.date != null }.groupBy { it.date!! }
                exerciseNameById = exercisesApi.listExercises(null, null).body().orEmpty().mapNotNull { ex -> ex.id?.let { it to ex.name } }.toMap()
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            rebuild()
        }
    }

    private fun loadWindow() {
        val from = _state.value.fromDate
        val end = _state.value.endDate
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                plansByDate = plansApi.listPlans(from, end).body().orEmpty().associateBy { it.date }
                completedDays = loggingApi.getCompletedDays(from, end).body().orEmpty().toSet()
                notes = journalApi.listJournal(from, end).body().orEmpty().associate { it.date to it.note }
            }.onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(loading = false) }
            rebuild()
        }
    }

    private fun rebuild() {
        val today = _state.value.today
        val end = _state.value.endDate
        val from = _state.value.fromDate
        val out = ArrayList<JDay>()
        var date = end
        while (!date.isBefore(from)) {
            val plan = plansByDate[date]
            val note = notes[date].orEmpty()
            val workouts = workoutsFor(date, plan)
            val dietStatus = dietStatusFor(date, plan, today)
            val hasContent = dietStatus != JStatus.NONE || workouts.isNotEmpty() || note.isNotBlank()
            if (hasContent || date == today || date == end) {
                out.add(JDay(
                    date = date,
                    dietName = plan?.dietId?.let { dietsById[it]?.name },
                    dietStatus = dietStatus,
                    meals = mealsFor(plan),
                    workouts = workouts,
                    workoutStatus = workoutStatusFor(date, workouts, today),
                    note = note,
                ))
            }
            date = date.minusDays(1)
        }
        _state.update { it.copy(entries = out) }
    }

    private fun dietStatusFor(date: LocalDate, plan: DayPlanDto?, today: LocalDate): JStatus {
        val hasDiet = plan?.dietId != null || !plan?.plannedMeals.isNullOrEmpty()
        if (!hasDiet) return JStatus.NONE
        if (date in completedDays) return JStatus.DONE
        return if (date.isBefore(today)) JStatus.MISSED else JStatus.PENDING
    }

    private fun mealsFor(plan: DayPlanDto?): List<JMeal> {
        if (plan == null) return emptyList()
        val rows = ArrayList<JMeal>()
        plan.dietId?.let { did ->
            (dietsById[did]?.meals ?: emptyList()).forEach { dm ->
                val m = mealsById[dm.mealId]
                rows.add(JMeal(dm.slot, m?.name ?: "Meal", m?.kcal ?: 0, m?.items ?: emptyList()))
            }
        }
        (plan.plannedMeals ?: emptyList()).forEach { pm ->
            val m = mealsById[pm.mealId]
            rows.add(JMeal(pm.slot, m?.name ?: "Meal", m?.kcal ?: 0, m?.items ?: emptyList()))
        }
        return rows.sortedBy { MEAL_SLOTS.indexOf(it.slot).let { i -> if (i < 0) 99 else i } }
    }

    private fun workoutsFor(date: LocalDate, plan: DayPlanDto?): List<JWorkout> {
        val planned = plan?.plannedWorkouts ?: emptyList()
        val byName = HashMap<String, com.mealplanplus.data.generated.model.WorkoutSessionDto>()
        (sessionsByDate[date] ?: emptyList()).forEach { s ->
            val cur = byName[s.name]
            if (cur == null || (s.isCompleted == true && cur.isCompleted != true)) byName[s.name] = s
        }
        val out = ArrayList<JWorkout>()
        planned.forEach { w ->
            val s = byName.remove(w.activityName)
            val status = if (s != null) (if (s.isCompleted == true) "done" else "active") else "todo"
            out.add(JWorkout("p${w.id ?: w.activityName}", w.activityName, w.workoutTemplateId, status, logFor(s, status)))
        }
        byName.forEach { (name, s) ->
            val status = if (s.isCompleted == true) "done" else "active"
            out.add(JWorkout("s${s.id ?: name}", name, null, status, logFor(s, status)))
        }
        return out
    }

    /** Build the read-only logged sets for a completed session, grouped by exercise. */
    private fun logFor(s: com.mealplanplus.data.generated.model.WorkoutSessionDto?, status: String): List<JLogExercise> {
        if (s == null || status != "done") return emptyList()
        return (s.sets ?: emptyList()).groupBy { it.exerciseId }.map { (exId, sets) ->
            JLogExercise(
                name = exerciseNameById[exId] ?: "Exercise",
                sets = sets.sortedBy { it.setNumber }.map { JLogSet(setLabel(it)) },
            )
        }
    }

    private fun setLabel(s: com.mealplanplus.data.generated.model.WorkoutSetDto): String {
        val km = s.distanceMeters?.takeIf { it > 0 }?.let { " · ${"%.1f".format(it / 1000.0)} km" } ?: ""
        return when {
            s.durationSeconds != null && s.durationSeconds!! > 0 -> "%d:%02d".format(s.durationSeconds!! / 60, s.durationSeconds!! % 60) + km
            s.reps != null || s.weightKg != null -> {
                val r = s.reps?.toString() ?: "–"
                if (s.weightKg != null && s.weightKg!! > 0) "$r × ${trimNum(s.weightKg!!)} kg" else "$r reps"
            }
            km.isNotEmpty() -> km.removePrefix(" · ")
            else -> "–"
        }
    }

    private fun workoutStatusFor(date: LocalDate, ws: List<JWorkout>, today: LocalDate): JStatus {
        if (ws.isEmpty()) return JStatus.NONE
        if (ws.any { it.status == "done" }) return JStatus.DONE
        return if (date.isBefore(today)) JStatus.MISSED else JStatus.PENDING
    }

    // ── Navigation ──────────────────────────────────────────────────────────────────
    fun jumpTo(date: LocalDate) { _state.update { it.copy(endDate = date, fromDate = date.with(DayOfWeek.MONDAY)) }; loadWindow() }
    fun resetToToday() { val t = _state.value.today; _state.update { it.copy(endDate = t, fromDate = t.with(DayOfWeek.MONDAY)) }; loadWindow() }
    /** First tap expands the week to the month start; after that, a month further back each tap. */
    fun loadOlder() {
        _state.update {
            val monthStart = it.endDate.withDayOfMonth(1)
            val next = if (it.fromDate.isAfter(monthStart)) monthStart else it.fromDate.minusDays(1).withDayOfMonth(1)
            it.copy(fromDate = next)
        }
        loadWindow()
    }

    // ── Notes CRUD (server-persisted) — optimistic with revert ────────────────────────
    fun saveNote(date: LocalDate, text: String) {
        val trimmed = text.trim()
        val prev = notes
        notes = if (trimmed.isEmpty()) notes - date else notes + (date to trimmed)
        rebuild()
        viewModelScope.launch {
            val ok = runCatching {
                val code = if (trimmed.isEmpty()) journalApi.deleteJournal(date).code()
                           else journalApi.upsertJournal(date, JournalEntryDto(date = date, note = trimmed)).code()
                code in 200..299
            }.getOrDefault(false)
            if (!ok) { notes = prev; _state.update { it.copy(error = "Couldn't save note") }; rebuild() }
        }
    }

    fun deleteNote(date: LocalDate) {
        val prev = notes
        notes = notes - date
        rebuild()
        viewModelScope.launch {
            val ok = runCatching {
                val code = journalApi.deleteJournal(date).code()   // 404 = already gone → fine
                code in 200..299 || code == 404
            }.getOrDefault(false)
            if (!ok) { notes = prev; _state.update { it.copy(error = "Couldn't delete note") }; rebuild() }
        }
    }

    private fun trimNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else "%.1f".format(v)
    private fun foodKcal(food: FoodDto?, quantity: Double, unit: String): Double {
        if (food == null) return 0.0
        val grams = when (unit) {
            "PIECE" -> quantity * (food.gramsPerPiece ?: 1.0)
            "CUP" -> quantity * (food.gramsPerCup ?: 1.0)
            "TBSP" -> quantity * (food.gramsPerTbsp ?: 1.0)
            "TSP" -> quantity * (food.gramsPerTsp ?: 1.0)
            else -> quantity
        }
        return food.caloriesPer100 * grams / 100.0
    }
}
