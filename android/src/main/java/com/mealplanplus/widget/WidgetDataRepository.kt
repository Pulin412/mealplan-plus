package com.mealplanplus.widget

import android.content.Context
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.PlanWithDietName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Convenience wrapper used exclusively by Glance widgets to load data.
 * Accesses Hilt-managed singletons via [WidgetEntryPoint].
 */
class WidgetDataRepository(context: Context) {

    private val ep = widgetEntryPoint(context)
    private val planRepo = ep.planRepository()
    private val logRepo = ep.dailyLogRepository()
    private val dietRepo = ep.dietRepository()

    private val fmt = DateTimeFormatter.ISO_LOCAL_DATE

    // ─── Calendar widget ────────────────────────────────────────────────────

    /**
     * Returns plans for the 7-day window starting today so the calendar
     * widget can show which diet is assigned to each day.
     */
    suspend fun getWeekPlans(): List<PlanWithDietName> {
        val today = LocalDate.now()
        val end = today.plusDays(6)
        return planRepo
            .getPlansWithDietNames(today.format(fmt), end.format(fmt))
            .firstOrNull() ?: emptyList()
    }

    // ─── Today's plan widget ────────────────────────────────────────────────

    data class TodaySlot(
        val slotType: String,
        val displayName: String,
        val isLogged: Boolean,
        val dietId: Long,
        val mealId: Long?
    )

    /**
     * Returns the diet + slot list for today so the "Today's Plan" widget
     * can show checkboxes per meal slot.
     */
    suspend fun getTodaySlots(): Pair<String?, List<TodaySlot>> {
        val today = LocalDate.now()
        val plan = planRepo.getPlanForDate(today.format(fmt)) ?: return null to emptyList()
        val dietId = plan.dietId ?: return null to emptyList()

        val dietWithMeals = dietRepo.getDietWithMeals(dietId) ?: return null to emptyList()
        val logWithFoods = logRepo.getLogWithFoods(today).firstOrNull()
        val loggedSlots = logWithFoods?.foods?.map { it.loggedFood.slotType }?.toSet() ?: emptySet()

        val slots = dietWithMeals.meals.entries
            .sortedBy { (_, mwf) -> mwf?.meal?.slotType?.let { slotOrder(it) } ?: Int.MAX_VALUE }
            .map { (slotType, mwf) ->
                TodaySlot(
                    slotType = slotType,
                    displayName = slotDisplayName(slotType),
                    isLogged = slotType in loggedSlots,
                    dietId = dietId,
                    mealId = mwf?.meal?.id
                )
            }

        return dietWithMeals.diet.name to slots
    }

    // ─── Diet summary widget ─────────────────────────────────────────────────

    data class DietSummaryData(
        val dietName: String,
        // Goal (from the planned diet)
        val goalCalories: Int,
        val goalProtein: Int,
        val goalCarbs: Int,
        val goalFat: Int,
        // Consumed today (from daily log)
        val consumedCalories: Int,
        val consumedProtein: Int,
        val consumedCarbs: Int,
        val consumedFat: Int
    )

    /**
     * Returns today's planned diet goals AND actual consumed macros for the
     * circular-progress ring widget.
     */
    suspend fun getTodayDietSummary(): DietSummaryData? {
        val today = LocalDate.now()
        val plan  = planRepo.getPlanForDate(today.format(fmt)) ?: return null
        val dietId = plan.dietId ?: return null
        val diet  = dietRepo.getDietWithMeals(dietId) ?: return null
        val log   = logRepo.getLogWithFoods(today).firstOrNull()
        return DietSummaryData(
            dietName         = diet.diet.name,
            goalCalories     = diet.totalCalories.toInt(),
            goalProtein      = diet.totalProtein.toInt(),
            goalCarbs        = diet.totalCarbs.toInt(),
            goalFat          = diet.totalFat.toInt(),
            consumedCalories = log?.totalCalories?.toInt() ?: 0,
            consumedProtein  = log?.totalProtein?.toInt()  ?: 0,
            consumedCarbs    = log?.totalCarbs?.toInt()    ?: 0,
            consumedFat      = log?.totalFat?.toInt()      ?: 0
        )
    }

    // ─── Reactive Flow variants ──────────────────────────────────────────────

    /**
     * Emits a fresh slot list whenever the logged_foods table changes for today.
     * Use inside [provideContent] with [collectAsState] so the widget self-updates.
     */
    fun getTodaySlotsFlow(): Flow<Pair<String?, List<TodaySlot>>> =
        logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods ->
                val today = LocalDate.now()
                val plan = planRepo.getPlanForDate(today.format(fmt))
                    ?: return@map (null to emptyList())
                val dietId = plan.dietId ?: return@map (null to emptyList())
                val dietWithMeals = dietRepo.getDietWithMeals(dietId)
                    ?: return@map (null to emptyList())
                val loggedSlots = logWithFoods?.foods
                    ?.map { it.loggedFood.slotType }?.toSet() ?: emptySet()
                val slots = dietWithMeals.meals.entries
                    .sortedBy { (_, mwf) ->
                        mwf?.meal?.slotType?.let { slotOrder(it) } ?: Int.MAX_VALUE
                    }
                    .map { (slotType, mwf) ->
                        TodaySlot(
                            slotType    = slotType,
                            displayName = slotDisplayName(slotType),
                            isLogged    = slotType in loggedSlots,
                            dietId      = dietId,
                            mealId      = mwf?.meal?.id
                        )
                    }
                dietWithMeals.diet.name to slots
            }
            .catch { emit(null to emptyList()) }

    /**
     * Emits fresh diet-summary data whenever the logged_foods table changes for today.
     * Use inside [provideContent] with [collectAsState] so the widget self-updates.
     */
    fun getTodayDietSummaryFlow(): Flow<DietSummaryData?> =
        logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods ->
                val today = LocalDate.now()
                val plan  = planRepo.getPlanForDate(today.format(fmt))
                    ?: return@map null
                val dietId = plan.dietId ?: return@map null
                val diet  = dietRepo.getDietWithMeals(dietId) ?: return@map null
                DietSummaryData(
                    dietName         = diet.diet.name,
                    goalCalories     = diet.totalCalories.toInt(),
                    goalProtein      = diet.totalProtein.toInt(),
                    goalCarbs        = diet.totalCarbs.toInt(),
                    goalFat          = diet.totalFat.toInt(),
                    consumedCalories = logWithFoods?.totalCalories?.toInt() ?: 0,
                    consumedProtein  = logWithFoods?.totalProtein?.toInt()  ?: 0,
                    consumedCarbs    = logWithFoods?.totalCarbs?.toInt()    ?: 0,
                    consumedFat      = logWithFoods?.totalFat?.toInt()      ?: 0
                )
            }
            .catch { emit(null) }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Log all foods in a slot to today's daily log (mirrors what the Home screen does).
     * Clears the slot first so this operation is idempotent — stale widget action parameters
     * that have isLogged=false when the slot is actually already logged will not create duplicates.
     */
    suspend fun logSlot(slotType: String) {
        val today = LocalDate.now()
        val plan = planRepo.getPlanForDate(today.format(fmt)) ?: return
        val dietId = plan.dietId ?: return
        val dietWithMeals = dietRepo.getDietWithMeals(dietId) ?: return
        val mwf = dietWithMeals.meals[slotType] ?: return

        // Clear first to prevent duplicate rows if the widget's baked-in isLogged was stale
        logRepo.clearSlot(today, slotType)

        mwf.items.forEach { item ->
            logRepo.logFood(
                date = today,
                foodId = item.mealFoodItem.foodId,
                quantity = item.mealFoodItem.quantity,
                slotType = slotType,
                unit = item.mealFoodItem.unit
            )
        }
    }

    /**
     * Clear all logged foods for a slot today (un-check).
     */
    suspend fun clearSlot(slotType: String) {
        logRepo.clearSlot(LocalDate.now(), slotType)
    }

    /**
     * Toggle a slot: queries the DB directly (never relies on baked-in widget parameters)
     * so the log/clear direction is always correct even if the pending intent is stale.
     */
    suspend fun toggleSlot(slotType: String) {
        if (logRepo.isSlotLogged(LocalDate.now(), slotType)) {
            clearSlot(slotType)
        } else {
            logSlot(slotType)
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun slotDisplayName(slotType: String): String {
        if (slotType.startsWith("CUSTOM:")) {
            return slotType.removePrefix("CUSTOM:")
                .split("_", " ")
                .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }
        }
        return com.mealplanplus.data.model.DefaultMealSlot.fromString(slotType)?.displayName
            ?: slotType.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }

    private fun slotOrder(slotType: String): Int =
        com.mealplanplus.data.model.DefaultMealSlot.fromString(slotType)?.order ?: 99
}
