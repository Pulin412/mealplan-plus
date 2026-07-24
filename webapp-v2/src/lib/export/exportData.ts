// Flat row models for CSV export — mirrors android-v2 `data/export/ExportData.kt`.
// The collector maps DTOs down to these so `buildCsv` stays a pure string function.

export interface MealRow {
  name: string;
  slots: string[];
  kcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  items: string;
}

export interface DietRow {
  name: string;
  tags: string[];
  kcal: number;
  proteinG: number;
  carbsG: number;
  fatG: number;
  entryCount: number;
}

/** One row per logged set (a session fans out into many rows). */
export interface WorkoutSetRow {
  date: string; // ISO-8601 date, e.g. 2026-07-22
  workout: string;
  exercise: string;
  setNumber: number;
  reps: number | null;
  weightKg: number | null;
}

export interface HealthRow {
  type: string;
  recordedAt: string; // ISO-8601 instant
  value: number;
  secondaryValue: number | null; // diastolic for blood pressure
  unit: string;
}

export interface ExportData {
  meals: MealRow[];
  diets: DietRow[];
  workoutSets: WorkoutSetRow[];
  health: HealthRow[];
}
