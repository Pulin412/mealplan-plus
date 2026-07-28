package com.mealplanplus.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.DashboardApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.AddLoggedFoodRequest
import com.mealplanplus.data.generated.model.DashboardDto
import com.mealplanplus.data.generated.model.ExerciseDto
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.generated.model.PlannedWorkoutDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import com.mealplanplus.data.healthconnect.HealthConnectManager
import com.mealplanplus.data.healthconnect.HealthConnectSummary
import com.mealplanplus.data.repository.ExerciseRepository
import com.mealplanplus.data.repository.WorkoutSessionRepository
import com.mealplanplus.ui.theme.ThemeStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class WorkoutStatus { PLANNED, IN_PROGRESS, DONE }

/**
 * A workout row on Home. [templateId] set = a planned template workout; [exerciseId] set = an ad-hoc
 * single exercise (planned or already logged). Status is resolved against today's sessions.
 */
data class HomeWorkout(val templateId: Long?, val exerciseId: Long?, val name: String, val status: WorkoutStatus)

data class HomeUiState(
    val loading: Boolean = true,
    val dashboard: DashboardDto? = null,
    val foods: List<FoodDto> = emptyList(),  // for the "Add to today" picker
    val workouts: List<HomeWorkout> = emptyList(),
    val workoutTemplates: List<WorkoutTemplateDto> = emptyList(),  // for the "add workout" picker
    val exercises: List<ExerciseDto> = emptyList(),                // for the "add exercise" picker
    val error: String? = null,
    val togglingSlot: String? = null,
    val hcConnected: Boolean = false,               // Health Connect granted
    val hcSummary: HealthConnectSummary = HealthConnectSummary(),
)

/**
 * Today / Home screen. Online read of the aggregated dashboard (calorie ring, macros, planned
 * slots + logged state, streak) with optimistic slot logging via [LoggingApi].
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val loggingApi: LoggingApi,
    private val foodsApi: FoodsApi,
    private val plansApi: PlansApi,
    private val workoutsApi: WorkoutTemplatesApi,
    private val sessionRepo: WorkoutSessionRepository,
    private val exerciseRepo: ExerciseRepository,
    private val healthConnect: HealthConnectManager,
    private val themeStore: ThemeStore,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    init { load(); loadFoods(); loadWorkouts(); loadWorkoutTemplates(); loadExercises(); loadActivity() }

    /** Today's steps + calories burned from Health Connect (shown only when connected). */
    fun loadActivity() {
        viewModelScope.launch {
            val connected = healthConnect.isAvailable && healthConnect.hasAllPermissions()
            _state.update {
                it.copy(
                    hcConnected = connected,
                    hcSummary = if (connected) healthConnect.readTodaySummary() else HealthConnectSummary(),
                )
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = it.dashboard == null, error = null) }
            runCatching { dashboardApi.getDashboard(null) }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) _state.update { it.copy(loading = false, dashboard = body) }
                    else _state.update { it.copy(loading = false, error = "Couldn't load today (${resp.code()})") }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = e.message) } }
        }
    }

    private fun loadFoods() {
        viewModelScope.launch {
            runCatching { foodsApi.listFoods(false).body() }.getOrNull()
                ?.let { foods -> _state.update { it.copy(foods = foods) } }
        }
    }

    /**
     * Today's workout rows: planned workouts (from the day plan) plus any ad-hoc sessions logged
     * today that aren't tied to a planned workout (e.g. a single random exercise), each resolved
     * to its logging status via today's sessions.
     */
    fun loadWorkouts() {
        viewModelScope.launch {
            val date = today()
            val planned = runCatching { plansApi.getPlan(date).body()?.plannedWorkouts }.getOrNull().orEmpty()
            val sessions = sessionRepo.listForDate(date)
            val plannedRows = planned.map { pw ->
                val session = sessions.firstOrNull { it.name == pw.activityName }
                HomeWorkout(pw.workoutTemplateId, session?.sets?.firstOrNull()?.exerciseId, pw.activityName, statusOf(session))
            }
            // Ad-hoc sessions not backed by a planned workout (single exercises started from Home).
            val plannedNames = planned.map { it.activityName }.toSet()
            val adHocRows = sessions.filter { it.name !in plannedNames }.map { s ->
                HomeWorkout(null, s.sets?.firstOrNull()?.exerciseId, s.name, statusOf(s))
            }
            _state.update { it.copy(workouts = plannedRows + adHocRows) }
        }
    }

    private fun statusOf(session: com.mealplanplus.data.generated.model.WorkoutSessionDto?): WorkoutStatus = when {
        session == null -> WorkoutStatus.PLANNED
        session.isCompleted == true -> WorkoutStatus.DONE
        else -> WorkoutStatus.IN_PROGRESS
    }

    private fun loadWorkoutTemplates() {
        viewModelScope.launch {
            runCatching { workoutsApi.listWorkoutTemplates().body() }.getOrNull()
                ?.let { templates -> _state.update { it.copy(workoutTemplates = templates) } }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            val list = exerciseRepo.list()
            _state.update { it.copy(exercises = list) }
        }
    }

    /** Add a workout template to today's plan (empty-state picker on Home). */
    fun addWorkout(template: WorkoutTemplateDto) {
        val id = template.id ?: return
        viewModelScope.launch {
            runCatching { plansApi.addPlannedWorkout(today(), PlannedWorkoutDto(workoutTemplateId = id, activityName = template.name)) }
                .onFailure { e -> _state.update { s -> s.copy(error = e.message) } }
            loadWorkouts()
        }
    }

    private fun today(): LocalDate = _state.value.dashboard?.date ?: LocalDate.now()

    /** Toggle a planned slot's logged state; the dashboard totals recompute on reload. */
    fun toggleSlot(slot: String) {
        viewModelScope.launch {
            _state.update { it.copy(togglingSlot = slot) }
            runCatching { loggingApi.toggleMealSlot(today(), slot) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(togglingSlot = null) }
            load()
        }
    }

    /** Add an unplanned food to today under a chosen slot. */
    fun addFood(foodId: Long, slot: String, quantity: Double, unit: FoodUnit) {
        viewModelScope.launch {
            runCatching { loggingApi.addLoggedFood(AddLoggedFoodRequest(today(), foodId, slot, quantity, unit)) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            load()
        }
    }

    /** Remove a previously-added unplanned food. */
    fun removeFood(id: Long) {
        viewModelScope.launch {
            runCatching { loggingApi.removeLoggedFood(id) }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            load()
        }
    }

    fun toggleTheme(currentlyDark: Boolean) = themeStore.toggle(currentlyDark)
}
