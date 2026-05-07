import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import type { components } from "@/lib/api/types.generated";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

type FoodDto       = components["schemas"]["FoodDto"];
type LoggedFoodDto = components["schemas"]["LoggedFoodDto"];

// ── Date helpers ──────────────────────────────────────────────────────────────

export function todayStr(): string {
  return new Date().toISOString().split("T")[0];
}

export function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + days);
  return d.toISOString().split("T")[0];
}

export function formatDateShort(dateStr: string): string {
  const today     = todayStr();
  const yesterday = addDays(today, -1);
  if (dateStr === today)     return "Today";
  if (dateStr === yesterday) return "Yesterday";
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", {
    weekday: "short", day: "numeric", month: "short",
  });
}

export function formatDateLabel(dateStr: string): string {
  const today    = todayStr();
  const tomorrow = addDays(today, 1);
  const yesterday = addDays(today, -1);
  if (dateStr === today)     return "Today";
  if (dateStr === yesterday) return "Yesterday";
  if (dateStr === tomorrow)  return "Tomorrow";
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", {
    weekday: "long", day: "numeric", month: "short",
  });
}

// ── Nutrition helpers ─────────────────────────────────────────────────────────

/** Convert a logged food's quantity to grams (GRAM unit = direct; anything else = qty × 100). */
export function toGrams(lf: Pick<LoggedFoodDto, "quantity" | "unit">): number {
  return lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
}

export function calcNutrient(
  food: Pick<FoodDto, "caloriesPer100" | "proteinPer100" | "carbsPer100" | "fatPer100">,
  lf: Pick<LoggedFoodDto, "quantity" | "unit">,
  key: keyof Pick<FoodDto, "caloriesPer100" | "proteinPer100" | "carbsPer100" | "fatPer100">
): number {
  return (food[key] * toGrams(lf)) / 100;
}

export function calcCalories(
  food: Pick<FoodDto, "caloriesPer100">,
  lf: Pick<LoggedFoodDto, "quantity" | "unit">
): number {
  return (food.caloriesPer100 * toGrams(lf)) / 100;
}

// ── Meal slot constants ───────────────────────────────────────────────────────

export const MEAL_SLOTS = [
  { key: "Breakfast", emoji: "🌅", color: "#F59E0B", bg: "#FFF8E6", text: "#D97706" },
  { key: "Lunch",     emoji: "☀️",  color: "#2E7D52", bg: "#E8F5EE", text: "#2E7D52" },
  { key: "Dinner",    emoji: "🌙", color: "#7C3AED", bg: "#F3EEFF", text: "#7C3AED" },
  { key: "Snack",     emoji: "🍎", color: "#DC2626", bg: "#FFF0F0", text: "#DC2626" },
] as const;

export type MealSlotKey = typeof MEAL_SLOTS[number]["key"];

export function getMealSlot(key: string) {
  return MEAL_SLOTS.find((s) => s.key === key);
}

// ── Misc helpers ──────────────────────────────────────────────────────────────

export function round1(v: number): number {
  return Math.round(v * 10) / 10;
}
