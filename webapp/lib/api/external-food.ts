import { round1 } from "@/lib/utils";

export interface LookupResult {
  name: string;
  brand?: string;
  barcode?: string;
  caloriesPer100: number;
  proteinPer100: number;
  carbsPer100: number;
  fatPer100: number;
  gramsPerPiece?: number;
  source: "USDA" | "OFF";
}

// ── USDA FoodData Central ─────────────────────────────────────────────────────

const USDA_KEY = process.env.NEXT_PUBLIC_USDA_KEY ?? "DEMO_KEY";

interface UsdaNutrient { nutrientId?: number; value?: number; }
interface UsdaItem {
  fdcId: number; description: string; brandOwner?: string; brandName?: string;
  servingSize?: number; servingSizeUnit?: string;
  foodNutrients?: UsdaNutrient[];
}

function getUsda(id: number, nutrients: UsdaNutrient[] = []) {
  return nutrients.find((n) => n.nutrientId === id)?.value ?? 0;
}

export async function searchUsda(query: string): Promise<LookupResult[]> {
  const url =
    `https://api.nal.usda.gov/fdc/v1/foods/search` +
    `?api_key=${USDA_KEY}&query=${encodeURIComponent(query)}&pageSize=12` +
    `&dataType=Foundation,SR%20Legacy,Branded`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("USDA request failed");
  const data: { foods?: UsdaItem[] } = await res.json();
  return (data.foods ?? []).map((item) => {
    const serving = item.servingSize ?? 100;
    const unit = (item.servingSizeUnit ?? "g").toLowerCase();
    const factor = serving > 0 ? 100 / serving : 1;
    return {
      name:           item.description,
      brand:          item.brandOwner ?? item.brandName,
      caloriesPer100: round1(getUsda(1008, item.foodNutrients) * factor),
      proteinPer100:  round1(getUsda(1003, item.foodNutrients) * factor),
      carbsPer100:    round1(getUsda(1005, item.foodNutrients) * factor),
      fatPer100:      round1(getUsda(1004, item.foodNutrients) * factor),
      gramsPerPiece:  unit !== "g" && unit !== "ml" ? serving : undefined,
      source:         "USDA" as const,
    };
  });
}

// ── OpenFoodFacts ─────────────────────────────────────────────────────────────

interface OFFNutriments {
  "energy-kcal_100g"?: number; energy_100g?: number;
  proteins_100g?: number; carbohydrates_100g?: number; fat_100g?: number;
}
interface OFFProduct { code?: string; product_name?: string; brands?: string; nutriments?: OFFNutriments; }

export async function searchOFF(query: string): Promise<LookupResult[]> {
  const url =
    `https://world.openfoodfacts.org/cgi/search.pl` +
    `?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=12`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("OpenFoodFacts request failed");
  const data: { products?: OFFProduct[] } = await res.json();
  return (data.products ?? [])
    .filter((p) => p.product_name && p.nutriments)
    .map((p) => {
      const n = p.nutriments!;
      const kcal = n["energy-kcal_100g"] ?? (n.energy_100g ? n.energy_100g / 4.184 : 0);
      return {
        name:           p.product_name!,
        brand:          p.brands || undefined,
        barcode:        p.code || undefined,
        caloriesPer100: round1(kcal),
        proteinPer100:  round1(n.proteins_100g ?? 0),
        carbsPer100:    round1(n.carbohydrates_100g ?? 0),
        fatPer100:      round1(n.fat_100g ?? 0),
        source:         "OFF" as const,
      };
    });
}

/** Searches both USDA and OFF in parallel, returns combined unique results. */
export async function searchExternalFood(query: string, source: "USDA" | "OFF"): Promise<LookupResult[]> {
  return source === "USDA" ? searchUsda(query) : searchOFF(query);
}
