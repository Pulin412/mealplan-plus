// Shared logic for how an exercise is logged. An exercise's type decides which set fields the
// runner / builder / logs show — reps+weight (STRENGTH), duration+distance (CARDIO), or duration
// only (TIMED). Mirrors the Android ui/components/WorkoutMetrics.kt so both clients render alike.

export const STRENGTH = "STRENGTH";
export const CARDIO = "CARDIO";
export const TIMED = "TIMED";

export const EXERCISE_TYPES = [STRENGTH, CARDIO, TIMED] as const;
export type ExerciseTypeValue = (typeof EXERCISE_TYPES)[number];

export function normalizeType(raw: string | null | undefined): string {
  const u = (raw ?? "").toUpperCase();
  return (EXERCISE_TYPES as readonly string[]).includes(u) ? u : STRENGTH;
}

export function typeLabel(type: string | null | undefined): string {
  switch (normalizeType(type)) {
    case CARDIO: return "Cardio";
    case TIMED: return "Timed";
    default: return "Strength";
  }
}

export const tracksReps = (t: string | null | undefined) => normalizeType(t) === STRENGTH;
export const tracksWeight = (t: string | null | undefined) => normalizeType(t) === STRENGTH;
export const tracksDuration = (t: string | null | undefined) => {
  const n = normalizeType(t);
  return n === CARDIO || n === TIMED;
};
export const tracksDistance = (t: string | null | undefined) => normalizeType(t) === CARDIO;

// ── Conversions: storage units (seconds / metres) ↔ friendlier stepper units (km). ──────────────
export const metresToKm = (m: number | null | undefined) => (m ?? 0) / 1000;
export const kmToMetres = (km: number): number | null => {
  const m = km * 1000;
  return m > 0 ? m : null;
};

// ── Display formatting. ──────────────────────────────────────────────────────────────────────
export function fmtDuration(seconds: number | null | undefined): string {
  if (seconds == null || seconds <= 0) return "–";
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  const pad = (n: number) => String(n).padStart(2, "0");
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

export function fmtDistance(metres: number | null | undefined): string {
  if (metres == null || metres <= 0) return "–";
  if (metres >= 1000) {
    const km = metres / 1000;
    return `${km % 1 === 0 ? km : km.toFixed(2)} km`;
  }
  return `${Math.round(metres)} m`;
}

/** One-line recap of a logged/target set, tailored to the exercise type. */
export function setSummaryFor(
  type: string | null | undefined,
  reps: number | null,
  weightKg: number | null,
  durationSeconds: number | null,
  distanceMeters: number | null,
): string {
  switch (normalizeType(type)) {
    case CARDIO:
      return [
        durationSeconds && durationSeconds > 0 ? fmtDuration(durationSeconds) : null,
        distanceMeters && distanceMeters > 0 ? fmtDistance(distanceMeters) : null,
      ].filter(Boolean).join(" · ") || "–";
    case TIMED:
      return durationSeconds && durationSeconds > 0 ? fmtDuration(durationSeconds) : "–";
    default: {
      const r = reps != null ? String(reps) : "–";
      return weightKg != null && weightKg > 0 ? `${r}×${weightKg}kg` : r;
    }
  }
}
