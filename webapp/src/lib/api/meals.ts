import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type MealDto = components["schemas"]["MealDto"];
export type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];

/** A food item as built in the UI (references a food by numeric server id). */
export interface MealItemInput {
  foodId: number;
  quantity: number;
  unit: components["schemas"]["FoodUnit"];
}

export interface MealInput {
  name: string;
  slots: string[];
  items: MealItemInput[];
}

export function listMeals(): Promise<MealDto[]> {
  return apiFetch<MealDto[]>("/api/v1/meals");
}

export function createMeal(input: MealInput): Promise<MealDto> {
  return apiFetch<MealDto>("/api/v1/meals", {
    method: "POST",
    body: JSON.stringify(toDto(input)),
  });
}

export function updateMeal(id: number, input: MealInput): Promise<MealDto> {
  return apiFetch<MealDto>(`/api/v1/meals/${id}`, {
    method: "PUT",
    body: JSON.stringify(toDto(input)),
  });
}

export function deleteMeal(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/meals/${id}`, { method: "DELETE" });
}

export function toggleMealFavorite(id: number): Promise<MealDto> {
  return apiFetch<MealDto>(`/api/v1/meals/${id}/favorite`, { method: "PATCH" });
}

function toDto(input: MealInput) {
  return {
    name: input.name,
    slots: input.slots,
    items: input.items.map((it) => ({
      foodId: it.foodId,
      quantity: it.quantity,
      unit: it.unit,
    })),
    isFavorite: false,
  };
}
