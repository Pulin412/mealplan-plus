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

/**
 * "Search online" → Open Food Facts via our backend proxy (the reliable OFF search host sends no
 * CORS header, so the browser can't call it directly). Returns lightweight FoodDto results with no
 * id; the chosen one is created via createFood/createScannedFood. (The old `/foods/search` — our own
 * DB — is still used by other callers like the meal builder.)
 */
export async function searchFoodsOnline(q: string): Promise<FoodDto[]> {
  return apiFetch<FoodDto[]>(`/api/v1/foods/search-online?q=${encodeURIComponent(q)}`);
}

export async function createFood(form: ManualFoodForm): Promise<FoodDto> {
  const kcal = parseFloat(form.kcal) || 0;
  const gpu = form.gramsPerUnit ? parseFloat(form.gramsPerUnit) : null;
  return apiFetch<FoodDto>("/api/v1/foods", {
    method: "POST",
    body: JSON.stringify({
      name:          form.name,
      brand:         null,
      category:      form.category.trim() || null,
      caloriesPer100: kcal,
      proteinPer100:  parseFloat(form.protein) || 0,
      carbsPer100:    parseFloat(form.carbs)   || 0,
      fatPer100:      parseFloat(form.fat)     || 0,
      unit:          form.unit,
      gramsPerPiece: form.unit === "PIECE" ? gpu : null,
      gramsPerCup:   form.unit === "CUP"   ? gpu : null,
      gramsPerTbsp:  form.unit === "TBSP"  ? gpu : null,
      gramsPerTsp:   form.unit === "TSP"   ? gpu : null,
    }),
  });
}

export async function deleteFood(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/foods/${id}`, { method: "DELETE" });
}

export async function toggleFavorite(id: number): Promise<FoodDto> {
  return apiFetch<FoodDto>(`/api/v1/foods/${id}/favorite`, { method: "PATCH" });
}
