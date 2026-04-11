package com.mealplanplus.widget

import android.content.Context
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.PlanWithDietName
import com.mealplanplus.util.toEpochMs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Convenience wrapper used exclusively by Glance widgets to load data.
 * Accesses Hilt-managed singletons via [WidgetEntryPoint].
 */
class WidgetDataRepository(context: Context) {

    private val ep = widgetEntryPoint(context)
    private val planRepo = ep.planRepository()
    private val logRepo = ep.dailyLogRepository()
    private val dietRepo = ep.dietRepository()

    // ─── Calendar widget ────────────────────────────────────────────────────

    suspend fun getWeekPlans(): List<PlanWithDietName> {
        val today = LocalDate.now()
        val end = today.plusDays(6)
        return planRepo.getPlansWithDietNames(today, end).firstOrNull() ?: emptyList()
    }

    // ─── Today's plan widget ────────────────────────────────────────────────

    data class TodaySlot(
        val slotType: String,
        val displayName: String,
        val isLogged: Boolean,
        val dietId: Long,
        val mealId: Long?
    )

    suspend fun getTodaySlots(): Pair<String?, List<TodaySlot>> {
        val today = LocalDate.now()
        val plan = planRepo.getPlanForDate(today) ?: return null to emptyList()
        val dietId = plan.dietId ?: return null to emptyList()

        val dietWithMeals = dietRepo.getDietWithMeals(dietId) ?: return null to emptyList()
        val logWithFoods = logRepo.getLogWithFoods(today).firstOrNull()
        val loggedSlots = logWithFoods?.foods?.map { it.loggedFood.slotType }?.toSet() ?: emptySet()

        val slots = dietWithMeals.meals.entries
            .sortedBy { (slotType, _) -> slotOrder(slotType) }
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
        val goalCalories: Int,
        val goalProtein: Int,
        val goalCarbs: Int,
        val goalFat: Int,
        val consumedCalories: Int,
        val consumedProtein: Int,
        val consumedCarbs: Int,
        val consumedFat: Int
    )

    suspend fun getTodayDietSummary(): DietSummaryData? {
        val today = LocalDate.now()
        val plan = planRepo.getPlanForDate(today) ?: return null
        val dietId = plan.dietId ?: return null
        val diet = dietRepo.getDietWithMeals(dietId) ?: return null
        val log = logRepo.getLogWithFoods(today).firstOrNull()
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

    fun getTodaySlotsFlow(): Flow<Pair<String?, List<TodaySlot>>> =
        logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods ->
                val today = LocalDate.now()
                val plan = planRepo.getPlanForDate(today)
                    ?: return@map (null to emptyList())
                val dietId = plan.dietId ?: return@map (null to emptyList())
                val dietWithMeals = dietRepo.getDietWithMeals(dietId)
                    ?: return@map (null to emptyList())
                val loggedSlots = logWithFoods?.foods
                    ?.map { it.loggedFood.slotType }?.toSet() ?: emptySet()
                val slots = dietWithMeals.meals.entries
                    .sortedBy { (slotType, _) -> slotOrder(slotType) }
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

    fun getTodayDietSummaryFlow(): Flow<DietSummaryData?> =
        logRepo.getLogWithFoods(LocalDate.now())
            .map { logWithFoods ->
                val today = LocalDate.now()
                val plan = planRepo.getPlanForDate(today) ?: return@map null
                val dietId = plan.dietId ?: return@map null
                val diet = dietRepo.getDietWithMeals(dietId) ?: return@map null
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

    suspend fun logSlot(slotType: String) {
        val today = LocalDate.now()
        val plan = planRepo.getPlanForDate(today) ?: return
        val dietId = plan.dietId ?: return
        val dietWithMeals = dietRepo.getDietWithMeals(dietId) ?: return
        val mwf = dietWithMeals.meals[slotType] ?: return

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

    suspend fun clearSlot(slotType: String) {
        logRepo.clearSlot(LocalDate.now(), slotType)
    }

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
