// Shared nutrition helpers (units, slots, macro math) — mirrors android's data.repository helpers.
import type { components } from "@/lib/api/types.generated";

export type FoodDto = components["schemas"]["FoodDto"];
export type FoodUnit = components["schemas"]["FoodUnit"];

export const FOOD_UNITS: FoodUnit[] = ["GRAM", "ML", "PIECE", "CUP", "TBSP", "TSP"];

/** Canonical meal slots, in display order — mirrors backend CANONICAL_SLOTS / android MEAL_SLOTS. */
export const MEAL_SLOTS = [
  "Early Morning", "Breakfast", "Noon", "Lunch", "Evening",
  "Pre-Workout", "Post-Workout", "Dinner", "Post-Dinner",
];

export function unitLabel(unit: string): string {
  switch (unit) {
    case "ML": return "ml";
    case "PIECE": return "pcs";
    case "CUP": return "cup";
    case "TBSP": return "tbsp";
    case "TSP": return "tsp";
    default: return "g";
  }
}

export function isCountUnit(unit: string): boolean {
  return unit === "PIECE" || unit === "CUP" || unit === "TBSP" || unit === "TSP";
}

export function defaultQtyFor(unit: string): number {
  return isCountUnit(unit) ? 1 : 100;
}

/** Grams one unit of this food weighs (1 for GRAM/ML). */
export function gramsPerUnit(food: FoodDto | undefined, unit: string): number {
  if (!food) return 1;
  switch (unit) {
    case "PIECE": return food.gramsPerPiece ?? 1;
    case "CUP": return food.gramsPerCup ?? 1;
    case "TBSP": return food.gramsPerTbsp ?? 1;
    case "TSP": return food.gramsPerTsp ?? 1;
    default: return 1;
  }
}

export interface Macros { kcal: number; protein: number; carbs: number; fat: number }

/** Macros contributed by `quantity` (in `unit`) of `food`, via its per-100g values. */
export function foodMacros(food: FoodDto | undefined, quantity: number, unit: string): Macros {
  if (!food) return { kcal: 0, protein: 0, carbs: 0, fat: 0 };
  const grams = quantity * gramsPerUnit(food, unit);
  const factor = grams / 100;
  return {
    kcal: food.caloriesPer100 * factor,
    protein: food.proteinPer100 * factor,
    carbs: food.carbsPer100 * factor,
    fat: food.fatPer100 * factor,
  };
}

export const num = (d: number): string => (d % 1 === 0 ? String(d) : d.toFixed(1));
