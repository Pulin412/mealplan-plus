export interface FoodDto {
  id: number;
  serverId: string | null;
  firebaseUid: string;
  name: string;
  brand: string | null;
  barcode: string | null;
  caloriesPer100: number;
  proteinPer100: number;
  carbsPer100: number;
  fatPer100: number;
  gramsPerPiece: number | null;
  gramsPerCup: number | null;
  gramsPerTbsp: number | null;
  gramsPerTsp: number | null;
  glycemicIndex: number | null;
  isFavorite: boolean;
  verified: boolean;
  updatedAt: string | null;
}

export interface FoodPage {
  content: FoodDto[];
  totalElements: number;
  totalPages: number;
  number: number;
}

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
