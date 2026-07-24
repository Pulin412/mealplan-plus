import { apiFetch } from "./client";
import type { FoodDto } from "./foods";

/** A product resolved from a scanned barcode (per-100g nutrition). */
export interface ScannedProduct {
  name: string;
  brand: string | null;
  barcode: string;
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
}

/**
 * Resolve a barcode via Open Food Facts (free, no key). Called directly from the browser — OFF sends
 * permissive CORS headers, so no backend proxy is needed. Returns null when the product is unknown
 * or has no usable name; missing nutriments default to 0.
 */
export async function lookupBarcode(barcode: string): Promise<ScannedProduct | null> {
  const url = `https://world.openfoodfacts.org/api/v2/product/${encodeURIComponent(barcode)}.json?fields=code,product_name,brands,nutriments`;
  const res = await fetch(url, { headers: { Accept: "application/json" } });
  if (!res.ok) return null;
  const data = await res.json();
  if (data?.status !== 1 || !data.product) return null;
  const p = data.product;
  const n = p.nutriments ?? {};
  const name: string = (p.product_name ?? "").trim();
  if (!name) return null;
  return {
    name,
    brand: p.brands ? String(p.brands).split(",")[0].trim() || null : null,
    barcode,
    kcal: n["energy-kcal_100g"] ?? 0,
    protein: n["proteins_100g"] ?? 0,
    carbs: n["carbohydrates_100g"] ?? 0,
    fat: n["fat_100g"] ?? 0,
  };
}

/** Persist a scanned product as the user's own food (keeps brand + barcode). */
export async function createScannedFood(p: ScannedProduct): Promise<FoodDto> {
  return apiFetch<FoodDto>("/api/v1/foods", {
    method: "POST",
    body: JSON.stringify({
      name: p.name,
      brand: p.brand,
      barcode: p.barcode,
      caloriesPer100: p.kcal,
      proteinPer100: p.protein,
      carbsPer100: p.carbs,
      fatPer100: p.fat,
    }),
  });
}
