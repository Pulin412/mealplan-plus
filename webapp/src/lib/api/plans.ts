import { apiFetch } from "./client";
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

export type LoggedMealSlotDto = components["schemas"]["LoggedMealSlotDto"];

/** Dates marked complete in [from, to] — for the Plan calendar dots. */
export function getCompletedDays(from: string, to: string): Promise<string[]> {
  return apiFetch<string[]>(`/api/v1/logging/days?from=${from}&to=${to}`);
}

/** Which meal slots were logged on a date — for the past-day recap. */
export function getLoggedSlots(date: string): Promise<LoggedMealSlotDto[]> {
  return apiFetch<LoggedMealSlotDto[]>(`/api/v1/logging/slots?date=${date}`);
}
