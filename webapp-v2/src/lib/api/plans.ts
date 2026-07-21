import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type DayPlanDto = components["schemas"]["DayPlanDto"];

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

export function upsertPlan(date: string, dietId: number | null, plannedWorkouts: DayPlanDto["plannedWorkouts"] = []): Promise<DayPlanDto> {
  return apiFetch<DayPlanDto>(`/api/v1/plans/${date}`, { method: "PUT", body: JSON.stringify({ date, dietId, plannedWorkouts }) });
}

export function deletePlan(date: string): Promise<void> {
  return apiFetch<void>(`/api/v1/plans/${date}`, { method: "DELETE" });
}
