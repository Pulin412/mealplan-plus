package com.mealplanplus.ui.screens.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.DefaultMealSlot
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.LoggedFoodWithDetails
import com.mealplanplus.data.model.MealFoodItemWithDetails
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.local.CustomMealSlotDao
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.extractShortDietName
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
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
    val slotType: String,          // e.g. "BREAKFAST"
    val slotDisplayName: String,   // e.g. "Breakfast"
    val emoji: String,
    val plannedMealName: String?,  // name of the meal assigned in the diet
    val plannedMealId: Long?,      // meal id from diet
    val plannedFoods: List<MealFoodItemWithDetails> = emptyList(), // foods in planned meal (for logging)
    val isLogged: Boolean,         // true if at least one food was logged for this slot today
    val dietId: Long? = null,      // dietId for navigating to MealDetailScreen
    val loggedFoods: List<LoggedFoodWithDetails> = emptyList() // actual logged foods (for custom slots)
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
    val dietLabel: String?,   // Short name like "M17" or null
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
    val weeklyLoggedDates: Set<String> = emptySet(),
    // Rich slot data from today's diet
    val todayPlanSlots: List<TodayPlanSlot> = emptyList(),
    val hasDietToday: Boolean = false,
    // Rich week info (colour + diet label)
    val weekDays: List<WeekDayInfo> = emptyList(),
    val weeklyCalories: List<DailyMacroSummary> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val plansForMonth: Map<String, Boolean> = emptyMap(),
    val dietNamesForMonth: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dailyLogRepository: DailyLogRepository,
    private val healthRepository: HealthRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val authRepository: AuthRepository,
    private val customMealSlotDao: CustomMealSlotDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    // Tracks the combine job so we cancel it before re-launching on offset change.
    // Without this, old jobs (different offset) keep emitting and overwrite weekDays.
    private var weekDataJob: Job? = null

    init {
        loadUserName()
        loadTodayData()
        loadTodayPlanSlots()
        loadGlucoseHistory()
        viewModelScope.launch {
            _weekOffset.collect { loadWeekData() }
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

        val today = LocalDate.now().toString()
        healthRepository.getMetricsForDate(today)
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
            startDate.toString(),
            endDate.toString()
        ).onEach { history ->
            _uiState.update { it.copy(glucoseHistory = history) }
        }.launchIn(viewModelScope)
    }

    /**
     * Loads Today's Plan slots from today's assigned diet + any custom slots.
     * Combines log, plan, and custom-slot flows so home screen stays in sync
     * with DailyLogScreen.
     */
    private fun loadTodayPlanSlots() {
        val todayStr = LocalDate.now().toString()
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            combine(
                dailyLogRepository.getLogWithFoods(LocalDate.now()),
                planRepository.getPlansWithDietNames(todayStr, todayStr),
                customMealSlotDao.getSlotsForDate(userId, todayStr)
            ) { logWithFoods, plans, customSlots ->
                Triple(logWithFoods, plans, customSlots)
            }
                .onEach { (logWithFoods, plans, customSlots) ->
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
                        // No diet assigned — show default slots that have logged foods
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

                    // Append custom slots (always shown, logged or not)
                    val customTodaySlots = customSlots.map { slot ->
                        val slotType = "CUSTOM_${slot.id}"
                        val isLogged = logWithFoods?.foodsForSlot(slotType)?.isNotEmpty() == true
                        TodayPlanSlot(
                            slotType = slotType,
                            slotDisplayName = slot.name,
                            emoji = "✦",
                            plannedMealName = null,
                            plannedMealId = null,
                            plannedFoods = emptyList(),
                            isLogged = isLogged,
                            dietId = null,
                            loggedFoods = logWithFoods?.foodsForSlot(slotType) ?: emptyList()
                        )
                    }

                    _uiState.update {
                        it.copy(
                            todayPlanSlots = dietSlots + customTodaySlots,
                            hasDietToday = dietId != null
                        )
                    }
                }
                .launchIn(viewModelScope)
        }
    }

    /** Plan a diet for today (called when user picks a diet from DietPickerScreen via HomeScreen). */
    fun planDietForToday(dietId: Long) {
        viewModelScope.launch {
            planRepository.setPlanForDate(LocalDate.now().toString(), dietId)
            // flows auto-refresh via combine in loadTodayPlanSlots
        }
    }

    /** Toggle a slot: log all planned foods if not logged, clear slot if already logged. */
    fun toggleSlotLogged(slot: TodayPlanSlot) {
        viewModelScope.launch {
            val today = LocalDate.now()
            if (slot.isLogged) {
                // Un-log: clear all logged foods for this slot
                dailyLogRepository.clearSlot(today, slot.slotType)
            } else if (slot.plannedFoods.isNotEmpty()) {
                // Log: insert LoggedFood for each food in the planned meal
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
            // getLogWithFoods flow auto-refreshes todayPlanSlots and DailyLogScreen
        }
    }

    /**
     * Loads the 7-day week row with rich colour states and diet labels,
     * plus weekly logged dates, streak, and month plans.
     */
    private fun loadWeekData() {
        // Cancel any previous week data collection (different offset) to avoid
        // stale flows overwriting weekDays when logged_foods changes
        weekDataJob?.cancel()

        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekStart = monday.plusWeeks(_weekOffset.value.toLong())
        val weekEnd = weekStart.plusDays(6)

        // Combine: macro summaries (to know which days have calories logged)
        //          + plans for the week (to know planned/completed state)
        weekDataJob = combine(
            dailyLogRepository.getCompletedDaysCalories(weekStart, weekEnd),
            planRepository.getPlansWithDietNames(weekStart.toString(), weekEnd.toString())
        ) { calories, plans ->
            val loggedDates = calories.filter { it.calories > 0 }.map { it.date }.toSet()
            val streak = computeStreak(calories)

            // Build diet label map for the week
            val weekDietLabels = plans
                .filter { it.dietId != null }
                .mapNotNull { p -> p.dietName?.let { p.date to extractShortDietName(it) } }
                .toMap()
            val weekPlansMap = plans.filter { it.dietId != null }
                .associate { it.date to it.isCompleted }

            val weekDays = (0..6).map { dayIndex ->
                val date = weekStart.plusDays(dayIndex.toLong())
                val dateStr = date.toString()
                val isFuture = date.isAfter(today)
                val isCompleted = weekPlansMap[dateStr] == true
                val hasCalories = dateStr in loggedDates
                val hasplan = weekPlansMap.containsKey(dateStr)

                val state = when {
                    // Today or past: completed or has logged calories → green
                    !isFuture && (isCompleted || hasCalories) -> WeekDayState.COMPLETED
                    // Future with plan → orange
                    isFuture && hasplan -> WeekDayState.PLANNED_FUTURE
                    // Past with a plan but not completed and no calories → red (missed)
                    !isFuture && hasplan && !hasCalories -> WeekDayState.MISSED
                    // Past no plan and no calories → red (missed)
                    !isFuture && date.isBefore(today) && !hasCalories -> WeekDayState.MISSED
                    else -> WeekDayState.NO_DATA
                }

                WeekDayInfo(
                    date = date,
                    dietLabel = weekDietLabels[dateStr],
                    state = state
                )
            }

            Triple(loggedDates, streak, weekDays)
        }
            .onEach { (loggedDates, streak, weekDays) ->
                _uiState.update {
                    it.copy(
                        weeklyLoggedDates = loggedDates,
                        dayStreak = streak,
                        weekDays = weekDays
                    )
                }
            }
            .launchIn(viewModelScope)

        // Also load full month plans for the calendar section
        loadPlansForMonth()
    }

    private fun computeStreak(weekData: List<DailyMacroSummary>): Int {
        val today = LocalDate.now()
        var streak = 0
        for (i in 0..6) {
            val date = today.minusDays(i.toLong()).toString()
            val hasData = weekData.any { it.date == date && it.calories > 0 }
            if (hasData) streak++ else break
        }
        return streak
    }

    private fun loadPlansForMonth() {
        val month = _uiState.value.currentMonth
        val startDate = month.atDay(1).toString()
        val endDate = month.atEndOfMonth().toString()

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
}
