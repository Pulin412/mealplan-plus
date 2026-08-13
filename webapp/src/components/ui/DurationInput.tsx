"use client";

import { Stepper } from "@/components/ui/Stepper";

/**
 * Minutes + seconds input for a duration, stored/returned as total **seconds** (null when zero).
 * Two steppers so what you log — e.g. 5 min 4 sec — reads back exactly as 5:04, with none of the
 * decimal-minute ambiguity. Mirrors the Android ui/components/DurationInput.kt.
 */
export function DurationInput({
  seconds,
  onChange,
}: {
  seconds: number | null;
  onChange: (v: number | null) => void;
}) {
  const total = Math.max(0, seconds ?? 0);
  const mins = Math.floor(total / 60);
  const secs = total % 60;
  const emit = (m: number, s: number) => {
    const v = Math.max(0, m) * 60 + Math.min(59, Math.max(0, s));
    onChange(v > 0 ? v : null);
  };
  return (
    <div className="flex items-center gap-2">
      <Stepper value={mins} onChange={(m) => emit(m, secs)} min={0} max={999} suffix="m" dense />
      <Stepper value={secs} onChange={(s) => emit(mins, s)} min={0} max={59} step={5} suffix="s" dense />
    </div>
  );
}
