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

/** Extra per-100g nutrients (V17). `null` = unknown for that food (not entered / not in OFF). */
export interface ExtraNutrients { fiber: number | null; sugars: number | null; saturatedFat: number | null; sodium: number | null }

/** Extra nutrients contributed by `quantity` (in `unit`) of `food`; each stays null when the food lacks it. */
export function foodExtras(food: FoodDto | undefined, quantity: number, unit: string): ExtraNutrients {
  if (!food) return { fiber: null, sugars: null, saturatedFat: null, sodium: null };
  const factor = (quantity * gramsPerUnit(food, unit)) / 100;
  const scale = (v?: number | null) => (v == null ? null : v * factor);
  return { fiber: scale(food.fiberPer100), sugars: scale(food.sugarsPer100), saturatedFat: scale(food.saturatedFatPer100), sodium: scale(food.sodiumPer100) };
}

/** Sum a list of ExtraNutrients: null unless at least one food reported that nutrient (then sum the known ones). */
export function sumExtras(list: ExtraNutrients[]): ExtraNutrients {
  const add = (key: keyof ExtraNutrients): number | null => {
    const vals = list.map((e) => e[key]).filter((v): v is number => v != null);
    return vals.length ? vals.reduce((a, b) => a + b, 0) : null;
  };
  return { fiber: add("fiber"), sugars: add("sugars"), saturatedFat: add("saturatedFat"), sodium: add("sodium") };
}

export const num = (d: number): string => (d % 1 === 0 ? String(d) : d.toFixed(1));
