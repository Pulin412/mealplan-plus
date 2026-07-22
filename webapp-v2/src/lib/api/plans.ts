import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type DayPlanDto = components["schemas"]["DayPlanDto"];
export type PlannedWorkoutDto = components["schemas"]["PlannedWorkoutDto"];

/** DayPlan `date` arrives as a [y,m,d] array (Jackson) — normalise to ISO. */
export function isoOf(d: unknown): string {
  if (Array.isArray(d)) {
    const [y, m, day] = d as number[];
    return `${y}-${String(m).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
  }
  return String(d);
}

export function listPlans(from: string, to: string): Promise<DayPlanDto[]> {
  return apiFetch<DayPlanDto[]>(`/api/v1/plans?from=${from}&to=${to}`);
}

/** The plan for a single date, or null if none exists yet. */
export function getPlan(date: string): Promise<DayPlanDto | null> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}`).catch(() => null);
}

export function upsertPlan(date: string, dietId: number | null, plannedWorkouts: DayPlanDto["plannedWorkouts"] = []): Promise<DayPlanDto> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}`, { method: "PUT", body: JSON.stringify({ date, dietId, plannedWorkouts }) });
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
