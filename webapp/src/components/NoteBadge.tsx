"use client";

import { useState } from "react";

/**
 * Compact note affordance for list/summary rows (meals, diets, the choose-a-diet picker, Home's
 * planned slots). Renders nothing when [note] is blank; otherwise a small note icon that opens a
 * lightweight modal with the full text. Mirrors the Android NoteBadge.
 */
export function NoteBadge({ note, label = "details" }: { note?: string | null; label?: string }) {
  const [open, setOpen] = useState(false);
  const text = note?.trim();
  if (!text) return null;

  return (
    <>
      <button
        type="button"
        aria-label="View note"
        title="View note"
        onClick={(e) => { e.stopPropagation(); setOpen(true); }}
        className="inline flex-none underline align-baseline text-[10.5px]"
        style={{ color: "oklch(0.62 0.09 210)" }}
      >
        {label}
      </button>

      {open && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center p-6"
          style={{ background: "rgba(0,0,0,0.35)" }}
          onClick={(e) => { e.stopPropagation(); setOpen(false); }}
        >
          <div
            className="w-full max-w-sm rounded-xl p-4"
            style={{ background: "var(--color-surface, #fff)", border: "1px solid var(--color-border, #eaeef0)" }}
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center mb-2">
              <span className="text-[13px] font-semibold" style={{ color: "var(--color-ink, #14181b)" }}>Note</span>
              <span className="flex-1" />
              <button type="button" onClick={() => setOpen(false)} className="text-[16px] leading-none" style={{ color: "#8a949b" }} aria-label="Close">✕</button>
            </div>
            <p className="text-[13px] whitespace-pre-wrap break-words" style={{ color: "var(--color-ink, #14181b)" }}>{text}</p>
          </div>
        </div>
      )}
    </>
  );
}
