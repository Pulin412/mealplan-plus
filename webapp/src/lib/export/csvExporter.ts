// Pure CSV serialiser — mirrors android `data/export/CsvExporter.kt`. Produces one file with four
// labelled sections (Meals, Diets, Workouts, Health). RFC-4180 escaping; numbers use a fixed decimal
// point. Filtering (last-7-day workouts, last-90-day health) happens upstream in the collector.

import type { ExportData } from "./exportData";

export function buildCsv(data: ExportData): string {
  let out = "";

  out += "# MEALS\n";
  out += "name,slots,kcal,protein_g,carbs_g,fat_g,items\n";
  for (const m of data.meals) {
    out += row([m.name, m.slots.join(";"), String(m.kcal), num(m.proteinG), num(m.carbsG), num(m.fatG), m.items]);
  }

  out += "\n# DIETS\n";
  out += "name,tags,kcal,protein_g,carbs_g,fat_g,entries\n";
  for (const d of data.diets) {
    out += row([d.name, d.tags.join(";"), String(d.kcal), num(d.proteinG), num(d.carbsG), num(d.fatG), String(d.entryCount)]);
  }

  out += "\n# WORKOUTS (last 7 days)\n";
  out += "date,workout,exercise,set,reps,weight_kg\n";
  for (const w of data.workoutSets) {
    out += row([w.date, w.workout, w.exercise, String(w.setNumber), w.reps != null ? String(w.reps) : "", w.weightKg != null ? num(w.weightKg) : ""]);
  }

  out += "\n# HEALTH (last 90 days)\n";
  out += "type,recorded_at,value,secondary,unit\n";
  for (const h of data.health) {
    out += row([h.type, h.recordedAt, num(h.value), h.secondaryValue != null ? num(h.secondaryValue) : "", h.unit]);
  }

  return out;
}

function row(fields: string[]): string {
  return fields.map(esc).join(",") + "\n";
}

function esc(v: string): string {
  return /[",\n\r]/.test(v) ? `"${v.replace(/"/g, '""')}"` : v;
}

/** Round to one decimal; drop a trailing ".0" so whole numbers stay clean. */
function num(d: number): string {
  const r = Math.round(d * 10) / 10;
  return Number.isInteger(r) ? String(r) : r.toFixed(1);
}
