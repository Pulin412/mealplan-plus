import { apiFetch } from "./client";
import { getDiet } from "./diets";
import type { components } from "@/lib/api/types.generated";

export type DayPlanDto = components["schemas"]["DayPlanDto"];
export type PlannedWorkoutDto = components["schemas"]["PlannedWorkoutDto"];
export type PlannedMealDto = components["schemas"]["PlannedMealDto"];

/** DayPlan `date` is an ISO "yyyy-mm-dd" string (per the spec). */
export function isoOf(d: unknown): string {
  return d == null ? "" : String(d);
}

export function listPlans(from: string, to: string): Promise<DayPlanDto[]> {
  return apiFetch<DayPlanDto[]>(`/api/v1/plans?from=${from}&to=${to}`);
}

/** The plan for a single date, or null if none exists yet. */
export function getPlan(date: string): Promise<DayPlanDto | null> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}`).catch(() => null);
}

export function upsertPlan(
  date: string,
  dietId: number | null,
  plannedWorkouts: DayPlanDto["plannedWorkouts"] = [],
  plannedMeals: DayPlanDto["plannedMeals"] = [],
): Promise<DayPlanDto> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}`, { method: "PUT", body: JSON.stringify({ date, dietId, plannedWorkouts, plannedMeals }) });
}

export function deletePlan(date: string): Promise<void> {
  return apiFetch<void>(`/api/v1/plans/${date}`, { method: "DELETE" });
}

/** Add a planned workout (template-linked) to a day. */
export function addPlannedWorkout(date: string, workoutTemplateId: number, activityName: string): Promise<DayPlanDto> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}/workouts`, {
    method: "POST",
    body: JSON.stringify({ workoutTemplateId, activityName }),
  });
}

/** Remove a planned workout from a day by its planned-workout id. */
export function removePlannedWorkout(date: string, workoutId: number): Promise<void> {
  return apiFetch<void>(`/api/v1/plans/${date}/workouts/${workoutId}`, { method: "DELETE" });
}

/** Add a planned meal to a day's slot. */
export function addPlannedMeal(date: string, mealId: number, slot: string): Promise<DayPlanDto> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}/meals`, {
    method: "POST",
    body: JSON.stringify({ mealId, slot }),
  });
}

/** Remove a planned meal from a day by its planned-meal id. */
export function removePlannedMeal(date: string, mealPlanId: number): Promise<void> {
  return apiFetch<void>(`/api/v1/plans/${date}/meals/${mealPlanId}`, { method: "DELETE" });
}

/**
 * Remove one meal from a single day — mirrors the Android PlanRepository.removeMealFromDay logic
 * client-side, using existing endpoints (no backend change):
 *  - a loose **planned meal** matching this slot+meal → delete just that row;
 *  - a **diet meal** → detach only this day from its diet: materialise the diet's meals as per-day
 *    planned meals (minus the cancelled one) and clear the diet link. The diet template and every
 *    other planned day are untouched; this day falls back to the profile calorie target.
 */
export async function removeMealFromDay(date: string, slot: string, mealId: number): Promise<void> {
  const plan = await getPlan(date);
  const planned = plan?.plannedMeals ?? [];

  // Case 1 — a loose planned meal matching this slot+meal: delete just that row.
  const loose = planned.find((pm) => pm.slot === slot && pm.mealId === mealId && pm.id != null);
  if (loose?.id != null) {
    await removePlannedMeal(date, loose.id);
    return;
  }

  // Case 2 — a diet meal: detach the day from the diet, keeping all its meals except this one.
  const dietId = plan?.dietId;
  if (dietId == null) return; // nothing else to remove
  const diet = await getDiet(dietId);
  let dropped = false;
  const keptDietMeals = (diet.meals ?? []).filter((dm) => {
    if (!dropped && dm.slot === slot && dm.mealId === mealId) { dropped = true; return false; }
    return true;
  });
  const newPlanned: PlannedMealDto[] = [
    ...planned.map((pm) => ({ mealId: pm.mealId, slot: pm.slot })),
    ...keptDietMeals.map((dm) => ({ mealId: dm.mealId, slot: dm.slot })),
  ];
  await upsertPlan(date, null, plan?.plannedWorkouts ?? [], newPlanned);
}

export type LoggedMealSlotDto = components["schemas"]["LoggedMealSlotDto"];

/** Dates marked complete in [from, to] — for the Plan calendar dots. */
export function getCompletedDays(from: string, to: string): Promise<string[]> {
  return apiFetch<string[]>(`/api/v1/logging/days?from=${from}&to=${to}`);
}

/** Which meal slots were logged on a date — for the past-day recap. */
export function getLoggedSlots(date: string): Promise<LoggedMealSlotDto[]> {
  return apiFetch<LoggedMealSlotDto[]>(`/api/v1/logging/slots?date=${date}`);
}
