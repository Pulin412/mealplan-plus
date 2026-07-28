// Gathers a full ExportData snapshot from the REST APIs and applies the export scope: all meals &
// diets, completed workout sessions from the last 7 days, health readings from the last 90 days.
// Mirrors android `data/export/ExportRepository.kt`. Macro math reuses the shared `foodMacros`
// helper so totals match the Meals/Diets screens exactly.

import { listFoods, type FoodDto } from "@/lib/api/foods";
import { listMeals } from "@/lib/api/meals";
import { listDiets, type DietDto } from "@/lib/api/diets";
import { listExercises } from "@/lib/api/exercises";
import { listWorkoutSessions } from "@/lib/api/sessions";
import { listHealthMetrics } from "@/lib/api/health";
import { foodMacros, type Macros } from "@/lib/nutrition";
import type { ExportData, MealRow, DietRow, WorkoutSetRow, HealthRow } from "./exportData";

const HEALTH_TYPES = ["GLUCOSE", "WEIGHT", "BLOOD_PRESSURE"];

export async function collectExportData(now: Date = new Date()): Promise<ExportData> {
  const [foods, meals, diets, exercises, sessions, healthLists] = await Promise.all([
    listFoods(),
    listMeals(),
    listDiets(),
    listExercises(),
    listWorkoutSessions(),
    Promise.all(HEALTH_TYPES.map((t) => listHealthMetrics(t))),
  ]);

  const foodsById = new Map<number, FoodDto>();
  foods.forEach((f) => { if (f.id != null) foodsById.set(f.id, f); });

  // Per-meal macro totals, reused for both the Meals section and diet meal-entries.
  const mealTotals = new Map<number, Macros>();
  const mealRows: MealRow[] = meals.map((meal) => {
    const totals: Macros = { kcal: 0, protein: 0, carbs: 0, fat: 0 };
    const names: string[] = [];
    (meal.items ?? []).forEach((it) => {
      const food = it.foodId != null ? foodsById.get(it.foodId) : undefined;
      const m = foodMacros(food, it.quantity, it.unit);
      totals.kcal += m.kcal; totals.protein += m.protein; totals.carbs += m.carbs; totals.fat += m.fat;
      names.push(food?.name ?? "Unknown food");
    });
    if (meal.id != null) mealTotals.set(meal.id, totals);
    return {
      name: meal.name,
      slots: meal.slots ?? [],
      kcal: Math.round(totals.kcal),
      proteinG: totals.protein,
      carbsG: totals.carbs,
      fatG: totals.fat,
      items: summarise(names),
    };
  });

  const dietRows: DietRow[] = diets.map((diet) => resolveDiet(diet, mealTotals, foodsById));

  // Workouts — completed sessions within the last 7 days, one row per set, 1-based set# per exercise.
  const exerciseNames = new Map<number, string>();
  exercises.forEach((e) => { if (e.id != null) exerciseNames.set(e.id, e.name); });
  const workoutCutoff = isoDate(addDays(now, -7));
  const workoutSets: WorkoutSetRow[] = sessions
    .filter((s) => s.isCompleted === true && s.date != null && dateOnly(s.date) >= workoutCutoff)
    .sort((a, b) => dateOnly(a.date!).localeCompare(dateOnly(b.date!)))
    .flatMap((session) => {
      const date = dateOnly(session.date!);
      const byExercise = new Map<number, typeof session.sets>();
      (session.sets ?? []).forEach((set) => {
        const arr = byExercise.get(set.exerciseId) ?? [];
        arr!.push(set);
        byExercise.set(set.exerciseId, arr);
      });
      const rows: WorkoutSetRow[] = [];
      byExercise.forEach((sets, exerciseId) => {
        (sets ?? [])
          .slice()
          .sort((a, b) => (a.setNumber ?? 0) - (b.setNumber ?? 0))
          .forEach((set, i) => {
            rows.push({
              date,
              workout: session.name,
              exercise: exerciseNames.get(exerciseId) ?? "Exercise",
              setNumber: i + 1,
              reps: set.reps ?? null,
              weightKg: set.weightKg ?? null,
            });
          });
      });
      return rows;
    });

  // Health — all built-in metric types within the last 90 days.
  const healthCutoff = addDays(now, -90).getTime();
  const health: HealthRow[] = healthLists
    .flat()
    .filter((m) => new Date(m.recordedAt).getTime() >= healthCutoff)
    .sort((a, b) => new Date(a.recordedAt).getTime() - new Date(b.recordedAt).getTime())
    .map((m) => ({
      type: m.type,
      recordedAt: m.recordedAt,
      value: m.value,
      secondaryValue: m.secondaryValue ?? null,
      unit: m.unit,
    }));

  return { meals: mealRows, diets: dietRows, workoutSets, health };
}

function resolveDiet(diet: DietDto, mealTotals: Map<number, Macros>, foodsById: Map<number, FoodDto>): DietRow {
  const totals: Macros = { kcal: 0, protein: 0, carbs: 0, fat: 0 };
  let count = 0;
  (diet.meals ?? []).forEach((dm) => {
    const t = dm.mealId != null ? mealTotals.get(dm.mealId) : undefined;
    if (t) { totals.kcal += t.kcal; totals.protein += t.protein; totals.carbs += t.carbs; totals.fat += t.fat; }
    count++;
  });
  (diet.foodItems ?? []).forEach((fi) => {
    const food = fi.foodId != null ? foodsById.get(fi.foodId) : undefined;
    const m = foodMacros(food, fi.quantity ?? 1, fi.unit);
    totals.kcal += m.kcal; totals.protein += m.protein; totals.carbs += m.carbs; totals.fat += m.fat;
    count++;
  });
  return {
    name: diet.name,
    tags: (diet.tags ?? []).map((t) => t.name),
    kcal: Math.round(totals.kcal),
    proteinG: totals.protein,
    carbsG: totals.carbs,
    fatG: totals.fat,
    entryCount: count,
  };
}

function summarise(names: string[]): string {
  return names.length === 0 ? "No items" : `${names.length} item${names.length === 1 ? "" : "s"} · ${names.join(", ")}`;
}

function addDays(d: Date, days: number): Date {
  const r = new Date(d);
  r.setDate(r.getDate() + days);
  return r;
}

function isoDate(d: Date): string {
  return d.toISOString().slice(0, 10);
}

/** Session dates are ISO ("2026-07-18" or a full datetime) — normalise to the date part. */
function dateOnly(s: string): string {
  return s.slice(0, 10);
}

/** Triggers a browser download of the CSV. */
export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob([content], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
