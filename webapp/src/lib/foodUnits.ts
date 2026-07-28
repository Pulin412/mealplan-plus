// Food measurement units — mirrors Android's FOOD_UNITS / unitLabel / isCountUnit.
// Macros are stored per 100g; for count units the matching gramsPer* factor converts a
// quantity (in this unit) to grams.
export const FOOD_UNITS = ["GRAM", "ML", "PIECE", "CUP", "TBSP", "TSP"] as const;

export function unitLabel(u: string): string {
  switch (u) {
    case "ML": return "ml";
    case "PIECE": return "pcs";
    case "CUP": return "cup";
    case "TBSP": return "tbsp";
    case "TSP": return "tsp";
    default: return "g";
  }
}

export const isCountUnit = (u: string): boolean => ["PIECE", "CUP", "TBSP", "TSP"].includes(u);
