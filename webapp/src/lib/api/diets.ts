import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";
import type { FoodUnit } from "@/lib/nutrition";

export type DietDto = components["schemas"]["DietDto"];

export type DietEntryKind = "meal" | "food";

/** One entry as built in the UI — references a meal or food by numeric server id. */
export interface DietEntryInput {
  kind: DietEntryKind;
  refId: number;
  slot: string;
  quantity: number;
  unit: FoodUnit;
}

export interface DietInput {
  name: string;
  entries: DietEntryInput[];
  tagIds: number[];
  notes?: string | null;
}

export function listDiets(): Promise<DietDto[]> {
  return apiFetch<DietDto[]>("/api/v1/diets");
}

export function getDiet(id: number): Promise<DietDto> {
  return apiFetch<DietDto>(`/api/v1/diets/${id}`);
}

export type DietUsageDto = components["schemas"]["DietUsageDto"];

/** How frequently a diet is assigned to days in the plan (this user). */
export function getDietUsage(id: number): Promise<DietUsageDto> {
  return apiFetch<DietUsageDto>(`/api/v1/diets/${id}/usage`);
}

export function createDiet(input: DietInput): Promise<DietDto> {
  return apiFetch<DietDto>("/api/v1/diets", { method: "POST", body: JSON.stringify(toDto(input)) });
}

export function updateDiet(id: number, input: DietInput): Promise<DietDto> {
  return apiFetch<DietDto>(`/api/v1/diets/${id}`, { method: "PUT", body: JSON.stringify(toDto(input)) });
}

export function deleteDiet(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/diets/${id}`, { method: "DELETE" });
}

export function toggleDietFavorite(id: number): Promise<DietDto> {
  return apiFetch<DietDto>(`/api/v1/diets/${id}/favorite`, { method: "PATCH" });
}

function toDto(input: DietInput) {
  return {
    name: input.name,
    // Diets store their note in the existing `description` column (no separate notes field).
    description: input.notes ?? null,
    meals: input.entries.filter((e) => e.kind === "meal").map((e) => ({ mealId: e.refId, dayOfWeek: 0, slot: e.slot })),
    foodItems: input.entries.filter((e) => e.kind === "food").map((e) => ({ foodId: e.refId, slot: e.slot, quantity: e.quantity, unit: e.unit })),
    tagIds: input.tagIds,
    isFavorite: false,
  };
}
