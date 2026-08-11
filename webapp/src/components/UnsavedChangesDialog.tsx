"use client";

const C = { ink: "#14181b", muted: "#8a949b", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b", bgAlt: "#eef2f3" };

/**
 * Confirmation shown when leaving a create/edit screen with unsaved changes.
 * Save runs the caller's normal save (disabled until valid); Discard leaves; Keep editing stays.
 * Lives under components/ so Tailwind's content globs generate its utility classes.
 */
export function UnsavedChangesDialog({ canSave, onSave, onDiscard, onKeepEditing }: {
  canSave: boolean; onSave: () => void; onDiscard: () => void; onKeepEditing: () => void;
}) {
  return (
    <div className="fixed inset-0 z-[70] flex items-center justify-center p-6" style={{ background: "rgba(0,0,0,0.4)" }}
      onClick={onKeepEditing}>
      <div className="w-full max-w-[320px] rounded-[14px] p-5" style={{ background: "#fff" }} onClick={(e) => e.stopPropagation()}>
        <div className="text-[15px] font-semibold mb-1" style={{ color: C.ink }}>Save changes?</div>
        <div className="text-[13px] mb-4" style={{ color: C.muted }}>
          {canSave ? "You have unsaved changes. Save them before leaving?" : "You have unsaved changes, but they can't be saved yet."}
        </div>
        <div className="flex flex-col gap-2">
          <button onClick={onSave} disabled={!canSave}
            className="w-full rounded-[11px] py-[11px] text-[13px] font-semibold"
            style={{ background: canSave ? C.teal : C.bgAlt, color: canSave ? "#fff" : C.muted }}>Save</button>
          <button onClick={onDiscard}
            className="w-full rounded-[11px] py-[11px] text-[13px] font-semibold"
            style={{ border: `1.5px solid ${C.danger}`, color: C.danger }}>Discard</button>
          <button onClick={onKeepEditing}
            className="w-full rounded-[11px] py-[11px] text-[13px] font-semibold"
            style={{ color: C.ink }}>Keep editing</button>
        </div>
      </div>
    </div>
  );
}
