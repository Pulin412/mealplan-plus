"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { useJournal, type DayEntry, type JournalWorkout, type Status } from "@/hooks/useJournal";
import { hasJournalPin, setJournalPin, verifyJournalPin } from "@/lib/journalLock";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", tealSoft: "oklch(0.95 0.03 210)",
  green: "oklch(0.60 0.13 150)", danger: "#b23b3b", blue: "oklch(0.60 0.11 255)",
};
const mono = "'DM Mono', monospace";
const WD_FULL = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const MON = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const iso = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

type J = ReturnType<typeof useJournal>;

// Monochrome line calendar icon — matches the app's clean toolbar glyphs (inherits color).
function CalendarIcon({ size = 17, color = C.ink }: { size?: number; color?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4.5" width="18" height="16" rx="2.5" /><path d="M3 9h18M8 2.5v4M16 2.5v4" />
    </svg>
  );
}

// Monochrome padlock icon (open shackle when unlocked).
function LockIcon({ locked, size = 14, color = C.ink }: { locked: boolean; size?: number; color?: string }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="4.5" y="11" width="15" height="10" rx="2" />
      {locked ? <path d="M8 11V7a4 4 0 0 1 8 0v4" /> : <path d="M8 11V7a4 4 0 0 1 7.5-2" />}
    </svg>
  );
}

const statusMark = (s: Status): { mark: string; color: string } | null => {
  switch (s) {
    case "done": return { mark: "✓", color: C.green };
    case "missed": return { mark: "✗", color: C.danger };
    case "pending": return { mark: "•", color: C.blue };
    default: return null;
  }
};

function JournalInner() {
  const j = useJournal();
  const router = useRouter();
  const [editing, setEditing] = useState<string | null>(null);
  const [calOpen, setCalOpen] = useState(false);
  // Opt-in lock: only engages once a PIN exists. If a PIN is set, notes start hidden each visit.
  const [pinSet, setPinSet] = useState(false);
  const [locked, setLocked] = useState(false);
  const [pinIntent, setPinIntent] = useState<null | "enable" | "unlock">(null);
  useEffect(() => { const has = hasJournalPin(); setPinSet(has); setLocked(has); }, []);

  const toggleLock = () => {
    if (locked) setPinIntent("unlock");          // hidden → authenticate to view
    else if (pinSet) setLocked(true);            // visible + PIN set → re-lock now
    else setPinIntent("enable");                 // no PIN yet → set one to turn the lock on
  };
  const onPinSuccess = () => {
    setPinSet(true);
    setLocked(pinIntent === "enable");           // enabling locks; unlocking reveals
    setPinIntent(null);
  };

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div style={{ padding: "12px 16px 10px", background: C.surface, borderBottom: `1px solid #eef1f3` }}>
        <div style={{ display: "flex", alignItems: "center" }}>
          <div>
            <div style={{ font: "700 21px system-ui", color: C.ink }}>Journal</div>
            <div style={{ font: "400 12px system-ui", color: C.muted2, marginTop: 2 }}>Your daily diary — notes, diets &amp; workouts</div>
          </div>
          <span style={{ flex: 1 }} />
          {!j.atToday && (
            <button onClick={j.resetToToday} style={{ border: "none", background: C.bgAlt, borderRadius: 9, padding: "7px 11px", cursor: "pointer", font: "600 12px system-ui", color: C.teal, marginRight: 6 }}>Today</button>
          )}
          {(() => {
            const label = locked ? "Locked" : pinSet ? "Unlocked" : "Lock";
            const accent = !locked && pinSet; // "Unlocked" state uses the teal highlight
            return (
              <button onClick={toggleLock} aria-label={locked ? "Unlock notes" : "Lock notes"}
                title={locked ? "Notes hidden — tap to unlock" : pinSet ? "Notes visible — tap to lock" : "Set a PIN to hide your notes"}
                style={{ display: "flex", alignItems: "center", gap: 5, height: 34, border: `1px solid ${accent ? C.teal : C.border}`, borderRadius: 9, background: accent ? C.tealSoft : C.surface, cursor: "pointer", color: accent ? C.teal : C.muted3, padding: "0 10px", font: "600 12px system-ui", marginRight: 6 }}>
                <LockIcon locked={locked} color={accent ? C.teal : C.muted3} />{label}
              </button>
            );
          })()}
          <button onClick={() => setCalOpen(true)} aria-label="Open calendar" title="Jump to a date"
            style={{ display: "flex", alignItems: "center", justifyContent: "center", width: 36, height: 34, border: `1px solid ${C.border}`, borderRadius: 9, background: C.surface, cursor: "pointer", color: C.muted3 }}>
            <CalendarIcon color={C.muted3} />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: "12px 16px", paddingBottom: 120 }}>
        {j.error && <div style={{ textAlign: "center", padding: 16, font: "400 12px system-ui", color: C.danger }}>{j.error}</div>}
        {j.loading && j.entries.length === 0 && <div style={{ textAlign: "center", padding: 24, font: "400 12px system-ui", color: C.muted }}>Loading…</div>}

        {j.entries.map((e) => (
          <DayCard key={e.date} e={e} todayIso={j.todayIso} locked={locked}
            editing={editing === e.date}
            onEdit={() => setEditing(e.date)} onClose={() => setEditing(null)}
            onSave={(t) => { j.saveNote(e.date, t); setEditing(null); }}
            onDelete={() => { j.deleteNote(e.date); setEditing(null); }}
            onOpenWorkout={(w) => { if (w.templateId != null) router.push(`/session?templateId=${w.templateId}&name=${encodeURIComponent(w.name)}&return=/journal`); }}
          />
        ))}

        <button onClick={j.loadOlder} style={{ width: "100%", marginTop: 6, border: `1.5px solid ${C.border}`, borderRadius: 12, padding: "11px", background: "none", cursor: "pointer", font: "600 12.5px system-ui", color: C.muted3 }}>
          Load older days
        </button>
      </div>

      {calOpen && <CalendarPopover j={j} onClose={() => setCalOpen(false)} />}
      {pinIntent && <PinModal intent={pinIntent} onClose={() => setPinIntent(null)} onSuccess={onPinSuccess} />}
      <NutritionNav />
    </div>
  );
}

function DayCard({ e, todayIso, locked, editing, onEdit, onClose, onSave, onDelete, onOpenWorkout }: {
  e: DayEntry; todayIso: string; locked: boolean; editing: boolean;
  onEdit: () => void; onClose: () => void; onSave: (t: string) => void; onDelete: () => void;
  onOpenWorkout: (w: JournalWorkout) => void;
}) {
  const [showMeals, setShowMeals] = useState(false);
  const [showWorkout, setShowWorkout] = useState<string | null>(null);
  const [y, m, d] = e.date.split("-").map(Number);
  const dt = new Date(y, m - 1, d);
  const isToday = e.date === todayIso;
  const isPast = e.date < todayIso;
  const diet = statusMark(e.dietStatus);
  const openW = e.workouts.find((w) => w.key === showWorkout);

  return (
    <div style={{ background: C.surface, border: `1px solid ${isToday ? C.teal : C.border}`, borderRadius: 14, padding: 14, marginBottom: 10 }}>
      {/* Date */}
      <div style={{ display: "flex", alignItems: "baseline", gap: 6 }}>
        <span style={{ font: "700 14px system-ui", color: C.ink }}>{MON[m - 1]} {d}</span>
        <span style={{ font: "500 11px system-ui", color: C.muted2 }}>{WD_FULL[dt.getDay()]}{isToday ? " · Today" : ""}</span>
      </div>

      {/* Diet (tap to expand the day's planned meals) + workout pills — light info, no meals inline. */}
      {(e.dietName || e.workouts.length > 0) && (
        <div style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: 6, marginTop: 9 }}>
          {e.dietName && (
            <button onClick={() => setShowMeals((v) => !v)}
              style={{ display: "inline-flex", alignItems: "center", gap: 5, border: `1px solid ${C.border}`, background: C.bgAlt, borderRadius: 20, padding: "5px 10px", cursor: "pointer", font: "600 11.5px system-ui", color: C.teal }}>
              {e.dietName}
              {diet && <span style={{ color: diet.color, fontWeight: 700 }}>{diet.mark}</span>}
              <span style={{ color: C.muted2, fontSize: 9 }}>{showMeals ? "▲" : "▼"}</span>
            </button>
          )}
          {e.workouts.map((w) => {
            const c = w.status === "done" ? C.green : w.status === "active" ? C.blue : C.muted3;
            // Done → expand its read-only log. Not-done today/upcoming → open the logger.
            // Past & not done (missed) → not tappable.
            const hasLog = w.status === "done" && w.log.length > 0;
            const canRun = w.status !== "done" && w.templateId != null && !isPast;
            const onClick = hasLog ? () => setShowWorkout(showWorkout === w.key ? null : w.key) : canRun ? () => onOpenWorkout(w) : undefined;
            return (
              <button key={w.key} onClick={onClick} disabled={!onClick}
                style={{ display: "inline-flex", alignItems: "center", gap: 5, border: `1px solid ${C.border}`, background: C.bgAlt, borderRadius: 20, padding: "5px 10px", cursor: onClick ? "pointer" : "default", font: "600 11.5px system-ui", color: C.ink }}>
                {w.name}
                <span style={{ color: c, fontWeight: 700 }}>{w.status === "done" ? "✓" : w.status === "active" ? "•" : "✗"}</span>
                {hasLog ? <span style={{ color: C.muted2, fontSize: 9 }}>{showWorkout === w.key ? "▲" : "▼"}</span>
                  : canRun ? <span style={{ color: C.muted2, fontSize: 12 }}>›</span> : null}
              </button>
            );
          })}
        </div>
      )}

      {/* Read-only workout log (completed session), expanded under its pill. */}
      {openW && openW.log.length > 0 && (
        <div style={{ marginTop: 8, borderTop: `1px solid ${C.bgAlt}`, paddingTop: 8 }}>
          {openW.log.map((ex, i) => (
            <div key={i} style={{ padding: "3px 0" }}>
              <div style={{ display: "flex", alignItems: "baseline" }}>
                <span style={{ flex: 1, font: "600 12.5px system-ui", color: C.ink }}>{ex.name}</span>
                <span style={{ font: `400 10.5px ${mono}`, color: C.muted2 }}>{ex.sets.length} set{ex.sets.length === 1 ? "" : "s"}</span>
              </div>
              {ex.sets.map((label, k) => (
                <div key={k} style={{ display: "flex", alignItems: "baseline", padding: "1px 0 1px 10px" }}>
                  <span style={{ width: 52, font: "400 11px system-ui", color: C.muted3 }}>Set {k + 1}</span>
                  <span style={{ font: `400 11px ${mono}`, color: C.ink }}>{label}</span>
                </div>
              ))}
            </div>
          ))}
        </div>
      )}

      {/* Day's planned meals (names/slots/kcal only — full detail lives in the diet). */}
      {showMeals && e.meals.length > 0 && (
        <div style={{ marginTop: 8, borderTop: `1px solid ${C.bgAlt}`, paddingTop: 8 }}>
          {e.meals.map((meal, i) => (
            <div key={i} style={{ padding: "3px 0" }}>
              <div style={{ display: "flex", alignItems: "baseline" }}>
                <span style={{ font: `600 9px ${mono}`, letterSpacing: 0.4, color: C.teal, background: C.bgAlt, borderRadius: 6, padding: "2px 6px", textTransform: "uppercase", marginRight: 8 }}>{meal.slot}</span>
                <span style={{ flex: 1, font: "600 12.5px system-ui", color: C.ink }}>{meal.name}</span>
                <span style={{ font: `400 10.5px ${mono}`, color: C.muted2 }}>{meal.kcal} kcal</span>
              </div>
              {meal.items.length > 0 && (
                <div style={{ paddingLeft: 10, marginTop: 1 }}>
                  {meal.items.map((f, k) => (
                    <div key={k} style={{ display: "flex", alignItems: "baseline", padding: "1px 0" }}>
                      <span style={{ flex: 1, font: "400 11.5px system-ui", color: C.muted3 }}>• {f.name}</span>
                      <span style={{ font: `400 10px ${mono}`, color: C.muted2 }}>{f.meta}</span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Note — the focus of the day (hidden while locked) */}
      <div style={{ marginTop: 10 }}>
        {locked ? (
          e.note ? (
            <div style={{ display: "flex", alignItems: "center", gap: 8, font: "500 12.5px system-ui", color: C.muted2, background: C.bgAlt, borderRadius: 10, padding: "10px 12px" }}>
              <LockIcon locked color={C.muted2} /> Note hidden — unlock to view
            </div>
          ) : null
        ) : editing ? (
          <NoteEditor initial={e.note} onSave={onSave} onCancel={onClose} onDelete={e.note ? onDelete : undefined} />
        ) : e.note ? (
          <div onClick={onEdit} style={{ cursor: "pointer", font: "400 13.5px system-ui", color: C.ink, lineHeight: 1.5, whiteSpace: "pre-wrap", background: C.tealSoft, borderRadius: 10, padding: "10px 12px" }}>
            {e.note}
          </div>
        ) : (
          <button onClick={onEdit} style={{ border: `1.5px dashed ${C.border}`, background: "none", borderRadius: 10, padding: "9px 12px", width: "100%", textAlign: "left", cursor: "pointer", font: "500 12.5px system-ui", color: C.muted2 }}>
            ✎ Add a note…
          </button>
        )}
      </div>
    </div>
  );
}

function NoteEditor({ initial, onSave, onCancel, onDelete }: { initial: string; onSave: (t: string) => void; onCancel: () => void; onDelete?: () => void }) {
  const [draft, setDraft] = useState(initial);
  return (
    <div>
      <textarea value={draft} onChange={(e) => setDraft(e.target.value)} rows={3} autoFocus
        placeholder="e.g. Cheated a bit — pizza night, back on track Monday."
        style={{ width: "100%", border: `1.5px solid ${C.teal}`, borderRadius: 10, padding: "10px 12px", font: "400 13.5px system-ui", color: C.ink, outline: "none", resize: "vertical", background: C.surface, lineHeight: 1.5 }} />
      <div style={{ display: "flex", alignItems: "center", marginTop: 8 }}>
        {onDelete && <button onClick={onDelete} style={{ border: "none", background: "none", cursor: "pointer", font: "600 12px system-ui", color: C.danger, padding: 4 }}>Delete</button>}
        <span style={{ flex: 1 }} />
        <button onClick={onCancel} style={{ border: "none", background: "none", cursor: "pointer", font: "600 12px system-ui", color: C.muted2, padding: "4px 10px" }}>Cancel</button>
        <button onClick={() => onSave(draft)} style={{ border: "none", borderRadius: 9, padding: "8px 16px", cursor: "pointer", font: "600 12px system-ui", background: C.teal, color: "#fff" }}>Save</button>
      </div>
    </div>
  );
}

// ── Calendar popover — jump to any date (incl. previous days) ──────────────────────
function CalendarPopover({ j, onClose }: { j: J; onClose: () => void }) {
  const [ym, setYm] = useState(() => { const [y, m] = j.endDate.split("-").map(Number); return { year: y, month: m }; });
  const { year, month } = ym;
  const firstDow = (new Date(year, month - 1, 1).getDay() + 6) % 7;
  const days = new Date(year, month, 0).getDate();
  const cells: (number | null)[] = [...Array(firstDow).fill(null), ...Array.from({ length: days }, (_, i) => i + 1)];
  const prev = () => setYm(({ year, month }) => (month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 }));
  const next = () => setYm(({ year, month }) => (month === 12 ? { year: year + 1, month: 1 } : { year, month: month + 1 }));

  return (
    <div onClick={onClose} style={{ position: "fixed", inset: 0, zIndex: 50, background: "rgba(20,24,27,.34)", display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
      <div onClick={(e) => e.stopPropagation()} style={{ background: C.surface, borderTopLeftRadius: 22, borderTopRightRadius: 22, padding: "18px 18px 28px" }}>
        <div style={{ width: 36, height: 4, borderRadius: 4, background: "#dfe3e6", margin: "0 auto 14px" }} />
        <div style={{ display: "flex", alignItems: "center", marginBottom: 12 }}>
          <button onClick={prev} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3, cursor: "pointer" }}>‹</button>
          <span style={{ flex: 1, textAlign: "center", font: "700 15px system-ui", color: C.ink }}>{MONTHS[month - 1]} {year}</span>
          <button onClick={next} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3, cursor: "pointer" }}>›</button>
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(7,1fr)" }}>
          {["M", "T", "W", "T", "F", "S", "S"].map((d, i) => <div key={i} style={{ textAlign: "center", font: "600 10px system-ui", color: C.muted2, paddingBottom: 6 }}>{d}</div>)}
          {cells.map((day, i) => {
            if (day == null) return <div key={i} />;
            const dIso = iso(year, month, day);
            const isSel = dIso === j.endDate;
            const isToday = dIso === j.todayIso;
            return (
              <div key={i} style={{ aspectRatio: "1", display: "flex", justifyContent: "center", alignItems: "center" }}>
                <button onClick={() => { j.jumpTo(dIso); onClose(); }} style={{
                  width: 36, height: 36, borderRadius: 10, border: `1px solid ${isToday && !isSel ? C.teal : "transparent"}`,
                  background: isSel ? C.teal : "transparent", cursor: "pointer",
                  font: `${isSel ? 700 : 500} 13px system-ui`, color: isSel ? "#fff" : C.ink,
                }}>{day}</button>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// PIN sheet. intent "enable" = turning the lock on (set a new PIN). intent "unlock" = entering the
// PIN to reveal notes, with a "Forgot PIN?" link that resets to a fresh PIN (simple reset).
function PinModal({ intent, onClose, onSuccess }: { intent: "enable" | "unlock"; onClose: () => void; onSuccess: () => void }) {
  const [resetting, setResetting] = useState(false);
  const setMode = intent === "enable" || resetting; // show new + confirm fields
  const [pin, setPin] = useState("");
  const [confirm, setConfirm] = useState("");
  const [err, setErr] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    setErr(null);
    if (!/^\d{4,8}$/.test(pin)) { setErr("Use a 4–8 digit PIN."); return; }
    setBusy(true);
    try {
      if (setMode) {
        if (pin !== confirm) { setErr("PINs don't match."); return; }
        await setJournalPin(pin); onSuccess();
      } else {
        if (await verifyJournalPin(pin)) onSuccess(); else setErr("Wrong PIN.");
      }
    } finally { setBusy(false); }
  };

  const title = intent === "enable" ? "Set a journal PIN" : resetting ? "Reset your PIN" : "Enter your PIN";
  const sub = intent === "enable" ? "Your notes will be hidden until unlocked with this PIN."
    : resetting ? "Choose a new PIN — this replaces the old one." : "Unlock to view and edit your notes.";

  return (
    <div onClick={onClose} style={{ position: "fixed", inset: 0, zIndex: 60, background: "rgba(20,24,27,.34)", display: "flex", flexDirection: "column", justifyContent: "flex-end" }}>
      <div onClick={(e) => e.stopPropagation()} style={{ background: C.surface, borderTopLeftRadius: 22, borderTopRightRadius: 22, padding: "20px 18px 28px" }}>
        <div style={{ width: 36, height: 4, borderRadius: 4, background: "#dfe3e6", margin: "0 auto 16px" }} />
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 6 }}>
          <LockIcon locked size={18} color={C.ink} />
          <span style={{ font: "700 17px system-ui", color: C.ink }}>{title}</span>
        </div>
        <div style={{ font: "400 12px system-ui", color: C.muted2, marginBottom: 14 }}>{sub}</div>
        <input autoFocus type="password" inputMode="numeric" value={pin} onChange={(e) => setPin(e.target.value.replace(/\D/g, ""))}
          onKeyDown={(e) => { if (e.key === "Enter" && !setMode) void submit(); }} placeholder={setMode ? "New PIN" : "PIN"}
          style={{ width: "100%", border: `1.5px solid ${C.border}`, borderRadius: 11, padding: "12px", font: `600 16px ${mono}`, letterSpacing: 4, textAlign: "center", color: C.ink, outline: "none", marginBottom: 10 }} />
        {setMode && (
          <input type="password" inputMode="numeric" value={confirm} onChange={(e) => setConfirm(e.target.value.replace(/\D/g, ""))}
            onKeyDown={(e) => { if (e.key === "Enter") void submit(); }} placeholder="Confirm PIN"
            style={{ width: "100%", border: `1.5px solid ${C.border}`, borderRadius: 11, padding: "12px", font: `600 16px ${mono}`, letterSpacing: 4, textAlign: "center", color: C.ink, outline: "none", marginBottom: 10 }} />
        )}
        {err && <div style={{ font: "500 12px system-ui", color: C.danger, marginBottom: 10 }}>{err}</div>}
        <button onClick={() => void submit()} disabled={busy}
          style={{ width: "100%", border: "none", borderRadius: 12, padding: "14px", cursor: "pointer", font: "600 13px system-ui", background: C.teal, color: "#fff" }}>
          {setMode ? "Save PIN" : "Unlock"}
        </button>
        {intent === "unlock" && !resetting && (
          <button onClick={() => { setResetting(true); setPin(""); setConfirm(""); setErr(null); }}
            style={{ width: "100%", border: "none", background: "none", cursor: "pointer", font: "500 12px system-ui", color: C.muted2, padding: "12px 0 0" }}>
            Forgot PIN? Reset it
          </button>
        )}
      </div>
    </div>
  );
}

export default function JournalPage() {
  return <AuthGuard><JournalInner /></AuthGuard>;
}
