package com.mealplanplus.ui.screens.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import com.mealplanplus.data.healthconnect.ActivitySummary
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.LoggedFoodWithDetails
import com.mealplanplus.data.model.MealFoodItemWithDetails
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.HealthConnectRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.SyncPreferences
import com.mealplanplus.util.extractShortDietName
import com.mealplanplus.util.toEpochMs
import com.mealplanplus.util.toLocalDate
import androidx.glance.appwidget.updateAll
import com.mealplanplus.widget.DietSummaryWidget
import com.mealplanplus.widget.TodayPlanWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

data class TodaySummary(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val foodCount: Int = 0
)

/** Represents a single slot in Today's Plan */
data class TodayPlanSlot(
    val slotType: String,
    val slotDisplayName: String,
    val emoji: String,
    val plannedMealName: String?,
    val plannedMealId: Long?,
    val plannedFoods: List<MealFoodItemWithDetails> = emptyList(),
    val isLogged: Boolean,
    val dietId: Long? = null,
    val loggedFoods: List<LoggedFoodWithDetails> = emptyList()
)

// Legacy – kept for any screen still referencing it
data class MealSlotProgress(
    val slotName: String,
    val emoji: String,
    val slotType: String,
    val logged: Int,
    val total: Int
) {
    val progress: Float get() = if (total > 0) (logged.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/** State for a day in the week calendar */
enum class WeekDayState { COMPLETED, PLANNED_FUTURE, MISSED, NO_DATA }

data class WeekDayInfo(
    val date: LocalDate,
    val dietLabel: String?,
    val state: WeekDayState
)

data class HomeUiState(
    val userName: String = "",
    val userInitial: String = "?",
    val todaySummary: TodaySummary = TodaySummary(),
    val calorieGoal: Int = 2000,
    val latestWeight: HealthMetric? = null,
    val latestSugar: HealthMetric? = null,
    val latestHba1c: HealthMetric? = null,
    val glucoseHistory: List<HealthMetric> = emptyList(),
    val dayStreak: Int = 0,
    val weeklyLoggedDates: Set<Long> = emptySet(),
    // Rich slot data from today's diet
    val todayPlanSlots: List<TodayPlanSlot> = emptyList(),
    val hasDietToday: Boolean = false,
    // Rich week info (colour + diet label)
    val weekDays: List<WeekDayInfo> = emptyList(),
    val weeklyCalories: List<DailyMacroSummary> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val plansForMonth: Map<Long, Boolean> = emptyMap(),
    val dietNamesForMonth: Map<Long, String> = emptyMap(),
    val isLoading: Boolean = true,
    val activitySummary: ActivitySummary = ActivitySummary(),
    val isTodayCompleted: Boolean = false,
    val finishCompleted: Boolean = false,
    val todayDietName: String? = null,
    /** Epoch ms of the last successful sync, or null if never synced. */
    val lastSyncedAt: Long? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val authRepository: AuthRepository,
    private val healthConnectRepository: HealthConnectRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    private var weekDataJob: Job? = null
    private var streakJob: Job? = null

    init {
        loadUserName()
        loadTodayData()
        loadTodayPlanSlots()
        loadGlucoseHistory()
        loadStreakData()
        loadLastSyncedAt()
        loadActivityData()
        observeDateChange()
        viewModelScope.launch {
            _weekOffset.collect { loadWeekData() }
        }
    }

    /**
     * Observes up to 365 days of logged-food dates and computes an unbounded streak.
     * Cancels any previous subscription to avoid duplicate observers accumulating over time.
     */
    private fun loadStreakData() {
        streakJob?.cancel()
        val today = LocalDate.now()
        val startDate = today.minusDays(365)
        streakJob = dailyLogRepository.getLoggedDatesForStreak(startDate, today)
            .onEach { entries ->
                val loggedDates = entries.map { it.date }.toSet()
                _uiState.update { it.copy(dayStreak = computeStreak(loggedDates)) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadActivityData() {
        viewModelScope.launch {
            val summary = healthConnectRepository.getTodayActivity()
            _uiState.update { it.copy(activitySummary = summary) }
        }
    }

    fun previousWeek() { _weekOffset.update { it - 1 } }
    fun nextWeek() { if (_weekOffset.value < 0) _weekOffset.update { it + 1 } }

    private fun loadUserName() {
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            authRepository.getCurrentUser(userId).collect { user ->
                val name = user?.displayName?.takeIf { it.isNotBlank() }
                    ?: user?.email?.substringBefore("@")
                    ?: ""
                val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                val calorieGoal = user?.targetCalories ?: 2000
                _uiState.update { it.copy(userName = name, userInitial = initial, calorieGoal = calorieGoal) }
            }
        }
    }

    private fun loadTodayData() {
        dailyLogRepository.getLogWithFoods(LocalDate.now())
            .onEach { logWithFoods ->
                val summary = logWithFoods?.let {
                    TodaySummary(
                        calories = it.totalCalories.toInt(),
                        protein = it.totalProtein.toInt(),
                        carbs = it.totalCarbs.toInt(),
                        fat = it.totalFat.toInt(),
                        foodCount = it.foods.size
                    )
                } ?: TodaySummary()
                _uiState.update { it.copy(todaySummary = summary) }
            }
            .launchIn(viewModelScope)

        val todayMs = LocalDate.now().toEpochMs()
        healthRepository.getMetricsForDate(todayMs)
            .onEach { metrics ->
                val weight = metrics.firstOrNull { it.metricType == MetricType.WEIGHT.name }
                val sugar = metrics.firstOrNull { it.metricType == MetricType.BLOOD_GLUCOSE.name }
                _uiState.update {
                    it.copy(
                        latestWeight = weight,
                        latestSugar = sugar,
                        latestHba1c = null,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadGlucoseHistory() {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6)
        healthRepository.getMetricsByTypeInRange(
            MetricType.BLOOD_GLUCOSE,
            startDate.toEpochMs(),
            endDate.toEpochMs()
        ).onEach { history ->
            _uiState.update { it.copy(glucoseHistory = history) }
        }.launchIn(viewModelScope)
    }

    private fun loadTodayPlanSlots() {
        val todayMs = LocalDate.now().toEpochMs()
        val today = LocalDate.now()
        viewModelScope.launch {
            combine(
                dailyLogRepository.getLogWithFoods(today),
                planRepository.getPlansWithDietNames(today, today)
            ) { logWithFoods, plans ->
                Pair(logWithFoods, plans)
            }
                .onEach { (logWithFoods, plans) ->
                    val dietId = plans.firstOrNull { it.dietId != null }?.dietId

                    val dietSlots: List<TodayPlanSlot> = if (dietId != null) {
                        val dietWithMeals = dietRepository.getDietWithMeals(dietId)
                        dietWithMeals?.meals
                            ?.entries
                            ?.sortedBy { (slotType, _) ->
                                DefaultMealSlot.fromString(slotType)?.order ?: Int.MAX_VALUE
                            }
                            ?.map { (slotType, mealWithFoods) ->
                                val slotFoods = logWithFoods?.foodsForSlot(slotType) ?: emptyList()
                                TodayPlanSlot(
                                    slotType = slotType,
                                    slotDisplayName = DefaultMealSlot.fromString(slotType)?.displayName
                                        ?: if (slotType.startsWith("CUSTOM:")) slotType.removePrefix("CUSTOM:")
                                        else slotType.replace("_", " ").lowercase().replaceFirstChar { it.uppercaseChar() },
                                    emoji = slotEmoji(slotType),
                                    plannedMealName = mealWithFoods?.meal?.name,
                                    plannedMealId = mealWithFoods?.meal?.id,
                                    plannedFoods = mealWithFoods?.items ?: emptyList(),
                                    isLogged = slotFoods.isNotEmpty(),
                                    dietId = dietId
                                )
                            } ?: emptyList()
                    } else {
                        (logWithFoods?.foods ?: emptyList())
                            .filter { !it.loggedFood.slotType.startsWith("CUSTOM_") }
                            .groupBy { it.loggedFood.slotType.uppercase() }
                            .entries
                            .sortedBy { (slotType, _) ->
                                DefaultMealSlot.fromString(slotType)?.order ?: Int.MAX_VALUE
                            }
                            .map { (slotType, _) ->
                                TodayPlanSlot(
                                    slotType = slotType,
                                    slotDisplayName = DefaultMealSlot.fromString(slotType)?.displayName
                                        ?: slotType.replace("_", " ").lowercase()
                                            .replaceFirstChar { it.uppercaseChar() },
                                    emoji = slotEmoji(slotType),
                                    plannedMealName = null,
                                    plannedMealId = null,
                                    plannedFoods = emptyList(),
                                    isLogged = true
                                )
                            }
                    }

                    val isTodayCompleted = plans.firstOrNull()?.isCompleted == true
                    val todayDietName = plans.firstOrNull { it.dietId != null }?.dietName

                    _uiState.update {
                        it.copy(
                            todayPlanSlots = dietSlots,
                            hasDietToday = dietId != null,
                            isTodayCompleted = isTodayCompleted,
                            todayDietName = todayDietName
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    fun planDietForToday(dietId: Long) {
        viewModelScope.launch {
            planRepository.setPlanForDate(LocalDate.now(), dietId)
        }
    }

    fun toggleSlotLogged(slot: TodayPlanSlot) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val isCurrentlyLogged = dailyLogRepository.isSlotLogged(today, slot.slotType)
            if (isCurrentlyLogged) {
                dailyLogRepository.clearSlot(today, slot.slotType)
            } else if (slot.plannedFoods.isNotEmpty()) {
                dailyLogRepository.clearSlot(today, slot.slotType)
                val timestamp = System.currentTimeMillis()
                slot.plannedFoods.forEach { foodItem ->
                    dailyLogRepository.logFood(
                        date = today,
                        foodId = foodItem.mealFoodItem.foodId,
                        quantity = foodItem.mealFoodItem.quantity,
                        slotType = slot.slotType,
                        timestamp = timestamp
                    )
                }
            }
            TodayPlanWidget().updateAll(context)
            DietSummaryWidget().updateAll(context)
        }
    }

    fun finishTodayPlan() {
        viewModelScope.launch {
            planRepository.completePlan(LocalDate.now())
            _uiState.update { it.copy(finishCompleted = true) }
        }
    }

    fun reopenTodayPlan() {
        viewModelScope.launch {
            planRepository.uncompletePlan(LocalDate.now())
        }
    }

    fun clearFinishCompleted() { _uiState.update { it.copy(finishCompleted = false) } }

    private fun loadWeekData() {
        weekDataJob?.cancel()

        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStart = monday.plusWeeks(_weekOffset.value.toLong())
        val weekEnd = weekStart.plusDays(6)

        weekDataJob = combine(
            dailyLogRepository.getCompletedDaysCalories(weekStart, weekEnd),
            planRepository.getPlansWithDietNames(weekStart, weekEnd)
        ) { calories, plans ->
            val loggedDates = calories.filter { it.calories > 0 }.map { it.date }.toSet()

            val weekDietLabels = plans
                .filter { it.dietId != null }
                .mapNotNull { p -> p.dietName?.let { p.date to extractShortDietName(it) } }
                .toMap()
            val weekPlansMap = plans.filter { it.dietId != null }
                .associate { it.date to it.isCompleted }

            val weekDays = (0..6).map { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                val dateMs = date.toEpochMs()
                val isFuture = date.isAfter(today)
                val isCompleted = weekPlansMap[dateMs] == true
                val hasCalories = dateMs in loggedDates
                val hasplan = weekPlansMap.containsKey(dateMs)

                val state = when {
                    !isFuture && (isCompleted || hasCalories) -> WeekDayState.COMPLETED
                    isFuture && hasplan -> WeekDayState.PLANNED_FUTURE
                    !isFuture && hasplan && !hasCalories -> WeekDayState.MISSED
                    !isFuture && date.isBefore(today) && !hasCalories -> WeekDayState.MISSED
                    else -> WeekDayState.NO_DATA
                }

                WeekDayInfo(
                    date = date,
                    dietLabel = weekDietLabels[dateMs],
                    state = state
                )
            }

            Pair(loggedDates, weekDays)
        }
            .onEach { (loggedDates, weekDays) ->
                _uiState.update {
                    it.copy(
                        weeklyLoggedDates = loggedDates,
                        weekDays = weekDays
                    )
                }
            }
            .launchIn(viewModelScope)

        loadPlansForMonth()
    }

    /**
     * Walks backwards from today until it finds a day with no logged food.
     *
     * Key rule: if today has no food logged yet (i.e. it's morning / user hasn't eaten yet),
     * we don't count it but also don't break on it — we start counting from yesterday.
     * This prevents the streak showing 0 for the entire morning every single day.
     *
     * [loggedDates] is the set of epoch-ms values where food was logged.
     */
    private fun computeStreak(loggedDates: Set<Long>): Int {
        val today = LocalDate.now()
        val todayMs = today.toEpochMs()
        // Start from today if the user has already logged something today; otherwise
        // start from yesterday so the streak doesn't reset to 0 mid-morning.
        var i = if (todayMs in loggedDates) 0 else 1
        var streak = 0
        while (true) {
            val dateMs = today.minusDays(i.toLong()).toEpochMs()
            if (dateMs in loggedDates) { streak++; i++ } else break
        }
        return streak
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1)
        val endDate = month.atEndOfMonth()

        viewModelScope.launch {
            planRepository.getPlansWithDietNames(startDate, endDate).collect { plansWithNames ->
                val validPlans = plansWithNames.filter { it.dietId != null }
                val plansMap = validPlans.associate { plan -> plan.date to plan.isCompleted }
                val dietNames = validPlans.mapNotNull { p ->
                    p.dietName?.let { p.date to extractShortDietName(it) }
                }.toMap()
                _uiState.update { it.copy(plansForMonth = plansMap, dietNamesForMonth = dietNames) }
            }
        }
    }

    fun goToPreviousMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.minusMonths(1)) }
        loadPlansForMonth()
    }

    fun goToNextMonth() {
        _uiState.update { it.copy(currentMonth = it.currentMonth.plusMonths(1)) }
        loadPlansForMonth()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadTodayData()
        loadTodayPlanSlots()
        loadWeekData()
        loadGlucoseHistory()
        loadStreakData()
        loadActivityData()
    }

    fun onHealthConnectPermissionsGranted() {
        loadActivityData()
    }

    companion object {
        fun slotEmoji(slotType: String): String = when (slotType.uppercase()) {
            "BREAKFAST" -> "🍳"
            "LUNCH" -> "☀️"
            "DINNER" -> "🌙"
            "SNACK", "EVENING_SNACK" -> "🍎"
            "PRE_WORKOUT" -> "💪"
            "POST_WORKOUT" -> "🥤"
            "EARLY_MORNING" -> "🌅"
            "NOON" -> "🌞"
            "MID_MORNING" -> "☕"
            "EVENING" -> "🌆"
            "POST_DINNER" -> "🍵"
            else -> "🍽️"
        }
    }

    /**
     * Registers a BroadcastReceiver for ACTION_DATE_CHANGED (fired at local midnight by the OS,
     * even in Doze mode) and ACTION_TIME_CHANGED (user manually changes clock/timezone).
     * Falls back to a coroutine delay as a belt-and-suspenders backup.
     */
    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            loadTodayData()
            loadTodayPlanSlots()
            loadStreakData()
            loadGlucoseHistory()
        }
    }

    private fun observeDateChange() {
        // Primary: OS broadcast — fires reliably at midnight even in Doze
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, dateChangeReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Secondary: coroutine-delay fallback for when the app is actively foregrounded
        viewModelScope.launch {
            while (true) {
                val now = LocalDateTime.now()
                val millisUntilMidnight = Duration.between(
                    now,
                    now.toLocalDate().plusDays(1).atStartOfDay()
                ).toMillis()
                delay(millisUntilMidnight)
                loadTodayData()
                loadTodayPlanSlots()
                loadStreakData()
                loadGlucoseHistory()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { context.unregisterReceiver(dateChangeReceiver) } catch (_: Exception) {}
    }

    private fun loadLastSyncedAt() {
        viewModelScope.launch {
            SyncPreferences.getLastSyncTimestamp(context)
                .collect { ts -> _uiState.update { it.copy(lastSyncedAt = if (ts == 0L) null else ts) } }
        }
    }
}
