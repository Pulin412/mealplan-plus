/**
 * Fixed exercise-tag colour palette (design_v2 §3): each tag maps to `oklch(0.52 0.13 hue)`.
 * Chips render as the tag colour text on a 12%-alpha fill. Tags outside this map (user-created)
 * fall back to a neutral colour. Mirrors android `ExerciseTags.kt`.
 */
const TAG_COLORS: Record<string, string> = {
  Chest:     "oklch(0.52 0.13 25)",
  Back:      "oklch(0.52 0.13 255)",
  Legs:      "oklch(0.52 0.13 145)",
  Shoulders: "oklch(0.52 0.13 70)",
  Arms:      "oklch(0.52 0.13 305)",
  Core:      "oklch(0.52 0.13 195)",
  Cardio:    "oklch(0.52 0.13 20)",
  Push:      "oklch(0.52 0.13 210)",
  Pull:      "oklch(0.52 0.13 285)",
  Mobility:  "oklch(0.52 0.13 165)",
};

/** Neutral fallback for user-created tags outside the fixed palette. */
const TAG_FALLBACK = "#5b666e";

export function exerciseTagColor(name: string): string {
  return TAG_COLORS[name] ?? TAG_FALLBACK;
}
