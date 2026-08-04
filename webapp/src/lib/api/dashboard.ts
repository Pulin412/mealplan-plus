import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";
import type { FoodUnit } from "@/lib/nutrition";

export type DashboardDto = components["schemas"]["DashboardDto"];
export type SlotStatusDto = components["schemas"]["SlotStatusDto"];
export type LoggedFoodDto = components["schemas"]["LoggedFoodResponseDto"];

/** The dashboard `date` is an ISO "yyyy-mm-dd" string (per the spec). */
export function isoDate(d: unknown): string {
  return d == null ? "" : String(d);
}

export function getDashboard(): Promise<DashboardDto> {
  return apiFetch<DashboardDto>("/api/v1/dashboard");
}

export function toggleMealSlot(date: string, slot: string): Promise<unknown> {
  return apiFetch(`/api/v1/logging/slots/${date}/${encodeURIComponent(slot)}/toggle`, { method: "POST" });
}

export function addLoggedFood(date: string, foodId: number, mealSlot: string, quantity: number, unit: FoodUnit): Promise<unknown> {
  return apiFetch("/api/v1/logging/foods", {
    method: "POST",
    body: JSON.stringify({ date, foodId, mealSlot, quantity, unit }),
  });
}

export function removeLoggedFood(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/logging/foods/${id}`, { method: "DELETE" });
}

export type DayCompletionDto = components["schemas"]["DayCompletionDto"];

/** Toggle whether a day is marked complete (the unit the streak counts). Returns the new state. */
export function toggleDayComplete(date: string): Promise<DayCompletionDto> {
  return apiFetch<DayCompletionDto>(`/api/v1/logging/days/${date}/toggle`, { method: "POST" });
}
