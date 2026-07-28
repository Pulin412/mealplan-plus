// FoodDto / FoodPage are NOT declared here — they are generated from the API spec.
// Import them from "@/lib/api/foods" (which re-exports the generated types).
// Only UI-only types live in this file.

export type FoodSort = "recent" | "name" | "calories" | "protein";
export type FoodViewMode = "list" | "compact";
export type FoodSheet = "manual" | "online" | "barcode" | null;

export interface ManualFoodForm {
  name: string;
  servingLabel: string;
  kcal: string;
  protein: string;
  carbs: string;
  fat: string;
}
