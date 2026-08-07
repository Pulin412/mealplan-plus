/**
 * Natural, case-insensitive name comparison so numbers sort by value, not lexically:
 * "M1, M2, … M9, M10" instead of "M1, M10, M11, … M2".
 */
export function naturalCompare(a: string, b: string): number {
  return a.localeCompare(b, undefined, { numeric: true, sensitivity: "base" });
}
