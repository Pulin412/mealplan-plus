"use client";

import { useEffect, useState } from "react";

const TEAL = "oklch(0.62 0.09 210)";
const BORDER = "#dfe6e8";
const INK = "#14181b";
const MUTED = "#8a949b";
const MONO = "'DM Mono', monospace";

interface StepperProps {
  value: number;
  onChange: (v: number) => void;
  min?: number;
  max?: number;
  step?: number;
  decimals?: number;
  suffix?: string;
  dense?: boolean;
  className?: string;
}

/**
 * A "− value +" stepper. The middle value is editable (type for big jumps, tap +/− for small ones);
 * the buttons nudge by `step` and clamp to [min, max]. Mirrors the Android Stepper so both clients
 * feel the same. The editable field renders at 16px so iOS Safari doesn't zoom the page on focus.
 */
export function Stepper({
  value, onChange, min = 0, max = 1_000_000, step = 1, decimals = 0, suffix, dense = false, className,
}: StepperProps) {
  const fmt = (v: number) => String(decimals <= 0 ? Math.round(v) : parseFloat(v.toFixed(decimals)));
  const [text, setText] = useState(() => fmt(value));

  useEffect(() => {
    // Sync when the value changes from outside, but don't clobber an in-progress edit.
    const f = fmt(value);
    if (f !== text && parseFloat(text) !== value) setText(f);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const clamp = (v: number) => Math.min(max, Math.max(min, v));
  const bump = (delta: number) => { const n = clamp(value + delta); setText(fmt(n)); onChange(n); };
  const btn = dense ? 30 : 38;
  const canDown = value - step >= min - 1e-9;
  const canUp = value + step <= max + 1e-9;

  return (
    <div className={className}
      style={{ display: "inline-flex", alignItems: "center", flex: "none", borderRadius: 10, border: `1px solid ${BORDER}`, background: "#fff" }}>
      <button type="button" aria-label="Decrease" disabled={!canDown} onClick={() => bump(-step)}
        style={{ width: btn, height: btn, flex: "none", color: canDown ? TEAL : BORDER, fontSize: 20, fontWeight: 700, background: "transparent", cursor: canDown ? "pointer" : "default" }}>−</button>
      <input
        value={text}
        inputMode={decimals > 0 ? "decimal" : "numeric"}
        onChange={(e) => {
          const f = e.target.value.replace(decimals > 0 ? /[^0-9.]/g : /[^0-9]/g, "");
          if (decimals > 0 && (f.match(/\./g)?.length ?? 0) > 1) return;
          setText(f);
          const n = parseFloat(f);
          if (!isNaN(n)) onChange(clamp(n));
        }}
        onBlur={() => setText(fmt(value))}
        style={{ width: dense ? 42 : 56, textAlign: "center", border: "none", outline: "none", background: "transparent", fontSize: 16, fontWeight: 600, color: INK, fontFamily: MONO, minWidth: 0 }}
      />
      {suffix ? <span style={{ color: MUTED, fontSize: 11, fontFamily: MONO, paddingRight: 4 }}>{suffix}</span> : null}
      <button type="button" aria-label="Increase" disabled={!canUp} onClick={() => bump(step)}
        style={{ width: btn, height: btn, flex: "none", color: canUp ? TEAL : BORDER, fontSize: 20, fontWeight: 700, background: "transparent", cursor: canUp ? "pointer" : "default" }}>+</button>
    </div>
  );
}
