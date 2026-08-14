package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.model.DayPlanDto
import com.mealplanplus.data.generated.model.PlannedMealDto
import com.mealplanplus.data.remote.ApiErrors
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Day-plan edits that span the diet + planned-meal layers.
 *
 * Removing a meal from a single day has two cases:
 *  - it's a loose **planned meal** (added on top of, or instead of, a diet) → just delete that row;
 *  - it's a **diet meal** (from the day's diet template) → we can't delete the template row (that would
 *    strip it from every day on that diet), so we *detach* this one day: materialise the diet's meals as
 *    per-day planned meals, clear the diet link, and drop the cancelled one. Every other day and the diet
 *    itself are untouched. The day then behaves like a custom day whose meals can be freely swapped.
 */
@Singleton
class PlanRepository @Inject constructor(
    private val plansApi: PlansApi,
    private val dietsApi: DietsApi,
) {
    suspend fun removeMealFromDay(date: LocalDate, slot: String, mealId: Long): Result<Unit> = runCatching {
        val plan = plansApi.getPlan(date).let { r ->
            if (!r.isSuccessful) error(ApiErrors.messageFor(r))
            r.body() ?: error("No plan for $date")
        }
        val planned = plan.plannedMeals.orEmpty()

        // Case 1 — a loose planned meal matching this slot+meal: delete just that row.
        val loose = planned.firstOrNull { it.slot == slot && it.mealId == mealId && it.id != null }
        if (loose != null) {
            val resp = plansApi.removePlannedMeal(date, loose.id!!)
            if (!resp.isSuccessful) error(ApiErrors.messageFor(resp))
            return@runCatching
        }

        // Case 2 — a diet meal: detach the day from the diet, keeping all its meals except this one.
        val dietId = plan.dietId ?: return@runCatching  // nothing else to remove
        val diet = dietsApi.getDiet(dietId).let { r ->
            if (!r.isSuccessful) error(ApiErrors.messageFor(r))
            r.body() ?: error("Diet $dietId not found")
        }
        // Drop exactly one matching diet meal; keep the rest as planned meals alongside existing loose ones.
        var dropped = false
        val keptDietMeals = diet.meals.orEmpty().filter { dm ->
            if (!dropped && dm.slot == slot && dm.mealId == mealId) { dropped = true; false } else true
        }
        val newPlanned = planned.map { PlannedMealDto(mealId = it.mealId, slot = it.slot) } +
            keptDietMeals.map { PlannedMealDto(mealId = it.mealId, slot = it.slot) }

        val resp = plansApi.upsertPlan(
            date,
            DayPlanDto(
                date = date,
                dietId = null,
                plannedWorkouts = plan.plannedWorkouts.orEmpty(),
                plannedMeals = newPlanned,
            ),
        )
        if (!resp.isSuccessful) error(ApiErrors.messageFor(resp))
    }
}
