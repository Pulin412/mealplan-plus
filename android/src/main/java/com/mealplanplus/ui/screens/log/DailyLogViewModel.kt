package com.mealplanplus.ui.screens.log

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.local.CustomMealSlotDao
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import androidx.glance.appwidget.updateAll
import com.mealplanplus.widget.DietSummaryWidget
import com.mealplanplus.widget.TodayPlanWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.mealplanplus.util.toEpochMs
import java.time.LocalDate
import javax.inject.Inject

data class MacroComparison(
    val plannedCalories: Int = 0,
    val plannedProtein: Int = 0,
    val plannedCarbs: Int = 0,
    val plannedFat: Int = 0,
    val actualCalories: Int = 0,
    val actualProtein: Int = 0,
    val actualCarbs: Int = 0,
    val actualFat: Int = 0
) {
    val hasPlan: Boolean get() = plannedCalories > 0
    val calorieDiff: Int get() = actualCalories - plannedCalories
    val proteinDiff: Int get() = actualProtein - plannedProtein
    val carbsDiff: Int get() = actualCarbs - plannedCarbs
    val fatDiff: Int get() = actualFat - plannedFat
}

data class DailyLogUiState(
    val date: LocalDate = LocalDate.now(),
    val logWithFoods: DailyLogWithFoods? = null,
    val plannedDiet: DietWithMeals? = null,
    val planForDate: Plan? = null,
    val comparison: MacroComparison = MacroComparison(),
    val isLoading: Boolean = true,
    val selectedTab: Int = 0, // 0=Daily Log, 1=Plan vs Actual
    val showFoodPicker: Boolean = false,
    val selectedSlot: DefaultMealSlot? = null,
    val selectedCustomSlot: CustomMealSlot? = null, // for food logging into custom slots
    val finishCompleted: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DailyLogViewModel @Inject constructor(
    private val logRepository: DailyLogRepository,
    private val mealRepository: MealRepository,
    private val planRepository: PlanRepository,
    private val dietRepository: DietRepository,
    private val foodRepository: FoodRepository,
    private val customMealSlotDao: CustomMealSlotDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _date = MutableStateFlow(LocalDate.now())

    private val _uiState = MutableStateFlow(DailyLogUiState())
    val uiState: StateFlow<DailyLogUiState> = _uiState.asStateFlow()

    private val _customSlots = MutableStateFlow<List<CustomMealSlot>>(emptyList())
    val customSlots: StateFlow<List<CustomMealSlot>> = _customSlots.asStateFlow()

    // Unified slot order (default + custom keys) saved per date in SharedPreferences
    private val _savedSlotOrder = MutableStateFlow<List<String>>(emptyList())
    val savedSlotOrder: StateFlow<List<String>> = _savedSlotOrder.asStateFlow()

    val allFoods: StateFlow<List<FoodItem>> = foodRepository.getAllFoods()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch {
            _date.flatMapLatest { date ->
                _uiState.update { it.copy(date = date, isLoading = true) }
                logRepository.getLogWithFoods(date)
            }.collect { logWithFoods ->
                val date = _date.value
                val plan = planRepository.getPlanForDate(date)
                val plannedDiet = plan?.dietId?.let { dietRepository.getDietWithMeals(it) }
                val comparison = buildComparison(plannedDiet, logWithFoods)
                _uiState.update {
                    it.copy(
                        logWithFoods = logWithFoods,
                        planForDate = plan,
                        plannedDiet = plannedDiet,
                        comparison = comparison,
                        isLoading = false
                    )
                }
            }
        }
        // Reactively load custom slots + saved order whenever the date changes
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            _date.flatMapLatest { date ->
                _savedSlotOrder.value = loadSlotOrder(userId, date)
                customMealSlotDao.getSlotsForDate(userId, date.toEpochMs())
            }.collect { slots ->
                _customSlots.value = slots
            }
        }
    }

    // ── Slot order persistence ────────────────────────────────────────────────

    private fun slotOrderPrefs() =
        context.getSharedPreferences("slot_order_prefs", Context.MODE_PRIVATE)

    private fun saveSlotOrder(userId: Long, date: LocalDate, keys: List<String>) {
        slotOrderPrefs().edit().putString("${userId}_${date}", keys.joinToString(",")).apply()
    }

    private fun loadSlotOrder(userId: Long, date: LocalDate): List<String> =
        slotOrderPrefs().getString("${userId}_${date}", null)
            ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    /** Called on drag-end: saves full unified order + updates custom slot DB order. */
    fun reorderSlots(allKeys: List<String>, reorderedCustom: List<CustomMealSlot>) {
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            saveSlotOrder(userId, _date.value, allKeys)
            _savedSlotOrder.value = allKeys
            reorderedCustom.forEachIndexed { index, slot ->
                if (slot.slotOrder != index) customMealSlotDao.update(slot.copy(slotOrder = index))
            }
        }
    }

    // ── Date navigation ───────────────────────────────────────────────────────

    fun setDateFromString(dateStr: String?) {
        _date.value = dateStr?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { LocalDate.now() }
        } ?: LocalDate.now()
    }

    fun goToPreviousDay() { _date.value = _date.value.minusDays(1) }
    fun goToNextDay() { _date.value = _date.value.plusDays(1) }
    fun goToToday() { _date.value = LocalDate.now() }

    // ── Custom slot CRUD ──────────────────────────────────────────────────────

    fun addCustomSlot(name: String) {
        viewModelScope.launch {
            val userId = AuthPreferences.getUserId(context).first() ?: return@launch
            val date = _uiState.value.date
            customMealSlotDao.insert(
                CustomMealSlot(
                    userId = userId,
                    date = date.toEpochMs(),
                    name = name.trim(),
                    slotOrder = 99 + _customSlots.value.size
                )
            )
        }
    }

    fun deleteCustomSlot(id: Long) {
        viewModelScope.launch { customMealSlotDao.deleteById(id) }
    }

    // ── Food picker ───────────────────────────────────────────────────────────

    fun setSelectedTab(tab: Int) { _uiState.update { it.copy(selectedTab = tab) } }

    fun showFoodPickerFor(slot: DefaultMealSlot) {
        _uiState.update { it.copy(showFoodPicker = true, selectedSlot = slot, selectedCustomSlot = null) }
    }

    fun showFoodPickerForCustomSlot(slot: CustomMealSlot) {
        _uiState.update { it.copy(showFoodPicker = true, selectedSlot = null, selectedCustomSlot = slot) }
    }

    /** Set pending custom slot before nav-based food picker (no bottom sheet). */
    fun setPendingCustomSlot(slot: CustomMealSlot) {
        _uiState.update { it.copy(selectedCustomSlot = slot) }
    }

    fun hideFoodPicker() {
        _uiState.update { it.copy(showFoodPicker = false, selectedSlot = null, selectedCustomSlot = null) }
    }

    /** Log food to whichever slot is currently selected (default or custom). */
    fun logFood(foodId: Long, quantity: Double = 100.0) {
        viewModelScope.launch {
            val slotType = _uiState.value.selectedSlot?.name
                ?: _uiState.value.selectedCustomSlot?.let { "CUSTOM_${it.id}" }
                ?: return@launch
            logRepository.logFood(
                date = _date.value,
                foodId = foodId,
                quantity = quantity,
                slotType = slotType,
                timestamp = System.currentTimeMillis()
            )
            hideFoodPicker()
            updateWidgetsIfToday()
        }
    }

    fun deleteLoggedFood(id: Long) {
        viewModelScope.launch {
            logRepository.deleteLoggedFood(id)
            updateWidgetsIfToday()
        }
    }

    // ── Plan management ───────────────────────────────────────────────────────

    fun applyDietById(dietId: Long, dateStr: String? = null) {
        viewModelScope.launch {
            val date = dateStr?.let {
                try { LocalDate.parse(it) } catch (_: Exception) { null }
            } ?: _date.value
            val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            if (dietWithMeals != null) {
                planRepository.setPlanForDate(date, dietId)
                logRepository.applyDiet(date, dietWithMeals)
                loadPlanForDate(date)
            }
            updateWidgetsIfToday()
        }
    }

    private suspend fun loadPlanForDate(date: LocalDate) {
        val plan = planRepository.getPlanForDate(date)
        _uiState.update { it.copy(planForDate = plan) }
    }

    fun finishPlan() {
        viewModelScope.launch {
            planRepository.completePlan(_date.value)
            loadPlanForDate(_date.value)
            _uiState.update { it.copy(finishCompleted = true) }
        }
    }

    fun clearFinishCompleted() { _uiState.update { it.copy(finishCompleted = false) } }

    /** Toggle a default slot: log all planned foods if not logged, clear if logged. */
    fun toggleSlotLogged(slot: DefaultMealSlot) {
        viewModelScope.launch {
            val date = _date.value
            // Query the DB directly so we never act on stale UI state (prevents double-logging)
            val isCurrentlyLogged = logRepository.isSlotLogged(date, slot.name)
            if (isCurrentlyLogged) {
                logRepository.clearSlot(date, slot.name)
            } else {
                val plannedItems = _uiState.value.plannedDiet?.meals?.get(slot.name)?.items ?: emptyList()
                // Always clear first — makes this operation idempotent even if called concurrently
                // or if stale duplicate rows exist from a previous bug. No-op when already empty.
                logRepository.clearSlot(date, slot.name)
                val timestamp = System.currentTimeMillis()
                plannedItems.forEach { foodItem ->
                    logRepository.logFood(
                        date = date,
                        foodId = foodItem.mealFoodItem.foodId,
                        quantity = foodItem.mealFoodItem.quantity,
                        slotType = slot.name,
                        timestamp = timestamp
                    )
                }
            }
            updateWidgetsIfToday()
        }
    }

    fun clearPlan() {
        viewModelScope.launch {
            logRepository.clearAllFoodsForDate(_date.value)
            planRepository.removePlan(_date.value)
            loadPlanForDate(_date.value)
            updateWidgetsIfToday()
        }
    }

    fun reopenPlan() {
        viewModelScope.launch {
            planRepository.uncompletePlan(_date.value)
            loadPlanForDate(_date.value)
        }
    }

    /**
     * Push widget refresh only when the user is editing today's log.
     * Called after every DB mutation that changes logged-food data.
     */
    private fun updateWidgetsIfToday() {
        if (_date.value == LocalDate.now()) {
            viewModelScope.launch {
                TodayPlanWidget().updateAll(context)
                DietSummaryWidget().updateAll(context)
            }
        }
    }

    private fun buildComparison(planned: DietWithMeals?, actual: DailyLogWithFoods?): MacroComparison {
        val loggedCal = actual?.totalCalories ?: 0.0
        val loggedPro = actual?.totalProtein ?: 0.0
        val loggedCarb = actual?.totalCarbs ?: 0.0
        val loggedFat = actual?.totalFat ?: 0.0
        return MacroComparison(
            plannedCalories = planned?.totalCalories?.toInt() ?: 0,
            plannedProtein = planned?.totalProtein?.toInt() ?: 0,
            plannedCarbs = planned?.totalCarbs?.toInt() ?: 0,
            plannedFat = planned?.totalFat?.toInt() ?: 0,
            actualCalories = loggedCal.toInt(),
            actualProtein = loggedPro.toInt(),
            actualCarbs = loggedCarb.toInt(),
            actualFat = loggedFat.toInt()
        )
    }
}
