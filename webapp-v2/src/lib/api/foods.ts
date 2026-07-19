import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";
import type { ManualFoodForm } from "@/types/food";

// DTOs come from the generated spec types — never hand-written. Re-exported here
// so screens/hooks import them from the API layer.
export type FoodDto = components["schemas"]["FoodDto"];
export type FoodPage = components["schemas"]["FoodPage"];

export async function listFoods(favoritesOnly = false): Promise<FoodDto[]> {
  return apiFetch<FoodDto[]>(`/api/v1/foods?favorites=${favoritesOnly}`);
}

export async function searchFoodsOnline(q: string, page = 0, size = 30): Promise<FoodPage> {
  const params = new URLSearchParams({ q, page: String(page), size: String(size) });
  return apiFetch<FoodPage>(`/api/v1/foods/search?${params}`);
}

export async function createFood(form: ManualFoodForm): Promise<FoodDto> {
  const kcal = parseFloat(form.kcal) || 0;
  return apiFetch<FoodDto>("/api/v1/foods", {
    method: "POST",
    body: JSON.stringify({
      name:          form.name,
      brand:         null,
      caloriesPer100: kcal,
      proteinPer100:  parseFloat(form.protein) || 0,
      carbsPer100:    parseFloat(form.carbs)   || 0,
      fatPer100:      parseFloat(form.fat)     || 0,
    }),
  });
}

export async function deleteFood(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/foods/${id}`, { method: "DELETE" });
}

export async function toggleFavorite(id: number): Promise<FoodDto> {
  return apiFetch<FoodDto>(`/api/v1/foods/${id}/favorite`, { method: "PATCH" });
}
