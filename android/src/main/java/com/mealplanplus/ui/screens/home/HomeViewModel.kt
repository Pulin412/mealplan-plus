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
import com.mealplanplus.data.cache.Resource
import com.mealplanplus.data.cache.ResponseCache
import com.mealplanplus.data.cache.render
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

/**
 * Yesterday's cached dashboard reshaped as today's empty shell (see `DashboardShellTest`): keeps the
 * layout, targets, streak and diet context so a new day's first open paints instantly, but resets
 * every logged value so we never flash yesterday's checkmarks / full ring before the refresh lands.
 */
fun DashboardDto.shellFor(today: LocalDate): DashboardDto =
    if (date == today) this
    else copy(
        date = today,
        calorieRing = calorieRing.copy(consumed = 0.0, remaining = calorieRing.target.toDouble(), isOver = false),
        macros = macros.copy(consumedProtein = 0.0, consumedCarbs = 0.0, consumedFat = 0.0),
        slots = slots.map { it.copy(isLogged = false) },
        additionalFoods = emptyList(),
    )

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
    val onlineResults: List<FoodDto> = emptyList(),  // Open Food Facts search results in the add sheet
    val onlineSearching: Boolean = false,
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
    private val responseCache: ResponseCache,
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

    /**
     * Read-through cached load: paints the cached dashboard instantly on a cold open (no spinner,
     * no waiting on a Cloud Run cold start), then refreshes from the server in the background. The
     * cache only *fills* an empty screen — once a dashboard is shown (including an optimistic slot
     * flip) [Resource.Loading] leaves it untouched, so a silent reload never flickers stale data.
     *
     * One stable key (not per-day) so the first open of a *new* day still has yesterday's snapshot to
     * paint from — reshaped via [shellFor] into today's empty layout so nothing stale (checkmarks, a
     * full ring) flashes before the fresh dashboard lands. Server [Resource.Success] is today's real
     * data, so it's shown as-is.
     */
    fun load() {
        viewModelScope.launch {
            responseCache.stream("dashboard") {
                dashboardApi.getDashboard(null).let { resp ->
                    resp.body().takeIf { resp.isSuccessful }
                        ?: throw IllegalStateException("Couldn't load today (${resp.code()})")
                }
            }.collect { res ->
                val today = LocalDate.now()
                fun cached(d: DashboardDto?): DashboardDto? = d?.shellFor(today)
                when (res) {
                    is Resource.Loading -> _state.update {
                        val shown = it.dashboard ?: cached(res.data)
                        it.copy(dashboard = shown, loading = shown == null, error = null)
                    }
                    is Resource.Success -> _state.update {
                        it.copy(loading = false, dashboard = res.data, error = null)
                    }
                    is Resource.Error -> _state.update {
                        val shown = it.dashboard ?: cached(res.data)
                        it.copy(
                            loading = false,
                            dashboard = shown,
                            error = if (shown == null) (res.error.message ?: "Couldn't load today") else null,
                        )
                    }
                }
            }
        }
    }

    private fun loadFoods() {
        viewModelScope.launch {
            responseCache.stream<List<FoodDto>>("home.foods") {
                foodsApi.listFoods(false).let { r ->
                    r.body().takeIf { r.isSuccessful } ?: throw IllegalStateException("foods ${r.code()}")
                }
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.foods.isNotEmpty())
                    st.copy(foods = if (r.keep) st.foods else r.value ?: st.foods)
                }
            }
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
            // Cache the planned list so the workout rows paint instantly on a cold open; the local
            // sessions (already on-device) are merged live on every emission to resolve status.
            responseCache.stream<List<PlannedWorkoutDto>>("home.plan") {
                plansApi.getPlan(date).let { r ->
                    (r.body()?.plannedWorkouts.orEmpty()).takeIf { r.isSuccessful }
                        ?: throw IllegalStateException("plan ${r.code()}")
                }
            }.collect { res ->
                val planned = res.data ?: return@collect     // nothing known yet → keep current rows
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
    }

    private fun statusOf(session: com.mealplanplus.data.generated.model.WorkoutSessionDto?): WorkoutStatus = when {
        session == null -> WorkoutStatus.PLANNED
        session.isCompleted == true -> WorkoutStatus.DONE
        else -> WorkoutStatus.IN_PROGRESS
    }

    private fun loadWorkoutTemplates() {
        viewModelScope.launch {
            responseCache.stream<List<WorkoutTemplateDto>>("home.templates") {
                workoutsApi.listWorkoutTemplates().let { r ->
                    r.body().takeIf { r.isSuccessful } ?: throw IllegalStateException("templates ${r.code()}")
                }
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.workoutTemplates.isNotEmpty())
                    st.copy(workoutTemplates = if (r.keep) st.workoutTemplates else r.value ?: st.workoutTemplates)
                }
            }
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

    /**
     * Toggle a planned slot's logged state. The checkmark flips **optimistically** so the tap feels
     * instant (previously it waited for a network write + a full dashboard reload). We fire the
     * toggle in the background, then silently reload to reconcile the ring/macros; on failure we
     * revert the flip.
     */
    fun toggleSlot(slot: String) {
        val current = _state.value.dashboard ?: return
        fun flip(d: DashboardDto) = d.copy(
            slots = d.slots.map { if (it.slot == slot) it.copy(isLogged = !it.isLogged) else it },
        )
        _state.update { it.copy(dashboard = flip(current), togglingSlot = slot) }
        viewModelScope.launch {
            val ok = runCatching { loggingApi.toggleMealSlot(today(), slot) }
                .map { it.isSuccessful }.getOrDefault(false)
            if (ok) {
                load()   // dashboard != null → silent reload (no spinner); refreshes ring + macros
            } else {
                _state.update { st ->
                    st.copy(dashboard = st.dashboard?.let(::flip), error = "Couldn't update — check your connection")
                }
            }
            _state.update { it.copy(togglingSlot = null) }
        }
    }

    /**
     * Mark today complete / not-complete. A day can be marked done even if not every meal is logged;
     * only completed days count toward the streak. Optimistic flip, then reload to reconcile the
     * streak (and authoritative completion state) from the server.
     */
    fun toggleDayComplete() {
        val current = _state.value.dashboard ?: return
        val was = current.dayCompleted ?: false
        _state.update { it.copy(dashboard = it.dashboard?.copy(dayCompleted = !was)) }
        viewModelScope.launch {
            val ok = runCatching { loggingApi.toggleDayComplete(today()) }.map { it.isSuccessful }.getOrDefault(false)
            if (ok) load()   // refresh streak + authoritative dayCompleted (dashboard != null → silent reload)
            else _state.update { it.copy(dashboard = it.dashboard?.copy(dayCompleted = was), error = "Couldn't update — check your connection") }
        }
    }

    /** Search Open Food Facts (public API) for the add-to-today sheet. */
    fun searchOnlineFoods(query: String) {
        val q = query.trim()
        if (q.isBlank()) { _state.update { it.copy(onlineResults = emptyList(), onlineSearching = false) }; return }
        viewModelScope.launch {
            _state.update { it.copy(onlineSearching = true) }
            val results = runCatching { foodsApi.searchFoodsOnline(q).body() }.getOrNull().orEmpty()
            _state.update { it.copy(onlineResults = results, onlineSearching = false) }
        }
    }

    fun clearOnlineResults() = _state.update { it.copy(onlineResults = emptyList(), onlineSearching = false) }

    /** Persist an Open Food Facts result as a food (server assigns an id), then log it to today. */
    fun addOnlineFood(dto: FoodDto, slot: String, quantity: Double, unit: FoodUnit) {
        viewModelScope.launch {
            val id = runCatching { foodsApi.createFood(dto).body()?.id }.getOrNull()
            if (id != null) {
                loadFoods()   // pull the new food into state.foods so "Added today" can show its calories
                addFood(id, slot, quantity, unit)
            } else _state.update { it.copy(error = "Couldn't add that food") }
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
