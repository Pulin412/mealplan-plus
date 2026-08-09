"use client";

import { useState } from "react";
import { reportContent, type ReportRequest } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", muted3: "#5b666e", border: "#eaeef0", surface: "#fff", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b", bgAlt: "#f2f4f5" };

const REASONS = ["Spam", "Inappropriate", "Harassment", "Impersonation", "Other"];

type Props = {
  entityType: ReportRequest["entityType"];
  entityServerId?: string;
  reportedHandle?: string;
  /** What is being reported, e.g. "@alex" or "this diet" — shown in the title. */
  subject: string;
  onClose: () => void;
};

/** Report a user or a shared item. The backend records it for later moderation; there is no
 *  immediate visible effect, so we just confirm receipt. */
export function ReportDialog({ entityType, entityServerId, reportedHandle, subject, onClose }: Props) {
  const [reason, setReason] = useState<string | null>(null);
  const [detail, setDetail] = useState("");
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  async function submit() {
    if (!reason || busy) return;
    setBusy(true);
    try {
      await reportContent({ entityType, entityServerId, reportedHandle, reason, detail: detail.trim() || undefined });
      setDone(true);
    } catch {
      setDetail((d) => d); // keep input; surface below
      alert("Couldn't send report — please try again.");
    } finally { setBusy(false); }
  }

  return (
    <div onClick={() => !busy && onClose()}
      style={{ position: "fixed", inset: 0, zIndex: 60, background: "rgba(0,0,0,.4)", display: "flex", alignItems: "center", justifyContent: "center", padding: 20 }}>
      <div onClick={(e) => e.stopPropagation()} style={{ width: "100%", maxWidth: 380, background: C.surface, borderRadius: 14, padding: 18 }}>
        {done ? (
          <>
            <div style={{ font: "700 17px system-ui", color: C.ink }}>Report received</div>
            <div style={{ font: "400 12.5px system-ui", color: C.muted3, marginTop: 6, lineHeight: 1.45 }}>
              Thanks — our team will review {subject}. You can also block them to stop seeing their content.
            </div>
            <div style={{ display: "flex", justifyContent: "flex-end", marginTop: 14 }}>
              <button onClick={onClose} style={{ border: "none", background: C.teal, color: "#fff", font: "600 13px system-ui", borderRadius: 8, padding: "8px 18px", cursor: "pointer" }}>Done</button>
            </div>
          </>
        ) : (
          <>
            <div style={{ font: "700 17px system-ui", color: C.ink }}>Report {subject}</div>
            <div style={{ font: "400 12.5px system-ui", color: C.muted, marginTop: 6 }}>Why are you reporting this?</div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginTop: 12 }}>
              {REASONS.map((r) => (
                <button key={r} onClick={() => setReason(r)}
                  style={{ border: `1px solid ${reason === r ? C.teal : C.border}`, background: reason === r ? C.teal : C.bgAlt, color: reason === r ? "#fff" : C.ink, font: "600 12.5px system-ui", borderRadius: 999, padding: "7px 13px", cursor: "pointer" }}>{r}</button>
              ))}
            </div>
            <textarea value={detail} onChange={(e) => setDetail(e.target.value)} disabled={busy}
              placeholder="Add details (optional)" rows={3} maxLength={1000}
              style={{ width: "100%", marginTop: 12, padding: 10, border: `1px solid ${C.border}`, borderRadius: 10, font: "400 14px system-ui", color: C.ink, resize: "vertical", outline: "none" }} />
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}>
              <button onClick={onClose} disabled={busy} style={{ border: "none", background: "transparent", color: C.muted3, font: "600 13px system-ui", padding: "8px 12px", cursor: "pointer" }}>Cancel</button>
              <button onClick={submit} disabled={busy || !reason}
                style={{ border: "none", background: C.danger, color: "#fff", font: "600 13px system-ui", borderRadius: 8, padding: "8px 18px", cursor: busy || !reason ? "default" : "pointer", opacity: busy || !reason ? 0.6 : 1 }}>{busy ? "Sending…" : "Report"}</button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
