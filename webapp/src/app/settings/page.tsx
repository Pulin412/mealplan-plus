"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useOnboarding } from "@/hooks/useOnboarding";
import { startTour } from "@/lib/tour";
import { submitFeedback } from "@/lib/api/feedback";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { collectExportData, downloadCsv } from "@/lib/export/collectExport";
import { buildCsv } from "@/lib/export/csvExporter";
import { pushSupported, isIos, isStandalone, remindersEnabled, enableReminders, disableReminders } from "@/lib/push";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", faint: "#a2abb1",
  border: "#eaeef0", borderMuted: "#e4e8eb", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", success: "#4da876",
};

function Toggle({ on, onToggle }: { on: boolean; onToggle: () => void }) {
  return (
    <div onClick={onToggle} style={{ width: 44, height: 26, borderRadius: 13, background: on ? C.teal : C.borderMuted, position: "relative", cursor: "pointer", flexShrink: 0, transition: "background .15s" }}>
      <div style={{ position: "absolute", top: 3, left: on ? 21 : 3, width: 20, height: 20, borderRadius: "50%", background: "#fff", boxShadow: "0 1px 2px rgba(0,0,0,.2)", transition: "left .15s" }} />
    </div>
  );
}

function SectionLabel({ text }: { text: string }) {
  return <div style={{ font: "700 10.5px system-ui", letterSpacing: 0.6, textTransform: "uppercase", color: C.faint, margin: "22px 0 8px 4px" }}>{text}</div>;
}
const cardStyle: React.CSSProperties = { background: C.surface, border: `1px solid ${C.border}`, borderRadius: 16 };
// Webapp version (separate track from the Android release). Single source of truth = package.json,
// injected via next.config → NEXT_PUBLIC_APP_VERSION; bump it there per release.
const APP_VERSION = process.env.NEXT_PUBLIC_APP_VERSION ?? "0.0.0";
function Divider() { return <div style={{ height: 1, background: C.border, margin: "0 14px" }} />; }
function ValueRow({ label, value, labelColor = C.ink }: { label: string; value: string; labelColor?: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", padding: "13px 14px", cursor: "pointer" }}>
      <span style={{ font: "500 13.5px system-ui", color: labelColor }}>{label}</span>
      <span style={{ marginLeft: "auto", font: "600 13.5px system-ui", color: C.ink }}>{value}</span>
      <span style={{ marginLeft: 4, color: "#c4ccd1", fontSize: 15 }}>›</span>
    </div>
  );
}

// Web Push reminders. On web, a "you haven't logged yet" nudge that fires when the app is closed can
// only come from the server (no on-device scheduler like Android's AlarmManager) — enabling this
// stores a browser push subscription; a scheduled backend job sends the reminder. iOS additionally
// requires the PWA be installed to the Home Screen.
function RemindersSection() {
  const [enabled, setEnabled] = useState(false);
  const [busy, setBusy] = useState(false);
  const [hint, setHint] = useState<string | null>(null);
  const supported = pushSupported();

  useEffect(() => {
    remindersEnabled().then(setEnabled).catch(() => {});
    if (isIos() && !isStandalone()) {
      setHint("On iPhone/iPad, add MealPlan+ to your Home Screen first (Share → Add to Home Screen) to turn on reminders.");
    }
  }, []);

  async function toggle() {
    if (busy || !supported) return;
    setBusy(true);
    setHint(null);
    try {
      if (enabled) {
        await disableReminders();
        setEnabled(false);
      } else {
        const r = await enableReminders();
        setEnabled(r.ok);
        if (!r.ok) {
          setHint(
            r.reason === "ios-not-installed" ? "Add MealPlan+ to your Home Screen first (Share → Add to Home Screen), then turn on reminders."
            : r.reason === "denied" ? "Notifications are blocked — allow them in your browser settings, then try again."
            : r.reason === "unsupported" ? "This browser doesn't support notifications."
            : r.reason === "no-key" ? "Reminders aren't configured on this deployment yet."
            : (r.message || "Couldn't turn on reminders."),
          );
        }
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      <SectionLabel text="Reminders" />
      <div style={{ ...cardStyle, display: "flex", alignItems: "center", padding: 14, opacity: supported ? 1 : 0.6 }}>
        <div style={{ width: 34, height: 34, borderRadius: "50%", background: C.bgAlt, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>🔔</div>
        <div style={{ marginLeft: 11, flex: 1 }}>
          <div style={{ font: "600 14.5px system-ui", color: C.ink }}>Daily log reminder</div>
        </div>
        <Toggle on={enabled} onToggle={toggle} />
      </div>
      {hint && <div style={{ font: "400 11.5px system-ui", color: C.muted3, margin: "8px 4px 0", lineHeight: 1.4 }}>{hint}</div>}
    </>
  );
}

function SettingsInner() {
  const router = useRouter();
  const ob = useOnboarding();
  const [connected, setConnected] = useState(false);
  const [exporting, setExporting] = useState(false);

  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackText, setFeedbackText] = useState("");
  const [sendingFeedback, setSendingFeedback] = useState(false);

  async function handleSendFeedback() {
    const text = feedbackText.trim();
    if (!text || sendingFeedback) return;
    setSendingFeedback(true);
    try {
      await submitFeedback(text, APP_VERSION);
      setFeedbackText("");
      setFeedbackOpen(false);
      alert("Thanks for your feedback!");
    } catch {
      alert("Couldn't send feedback — please try again.");
    } finally {
      setSendingFeedback(false);
    }
  }

  async function handleExport() {
    if (exporting) return;
    setExporting(true);
    try {
      const csv = buildCsv(await collectExportData());
      downloadCsv(`mealplan-export-${new Date().toISOString().slice(0, 10)}.csv`, csv);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Export failed");
    } finally {
      setExporting(false);
    }
  }

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div style={{ display: "flex", alignItems: "center", padding: "10px 16px 8px 6px" }}>
        <button onClick={() => router.back()} style={{ fontSize: 20, color: C.ink, padding: "0 8px", cursor: "pointer" }}>‹</button>
        <span style={{ font: "700 19px system-ui", color: C.ink }}>Settings</span>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: "0 16px 40px" }}>
        {/* Backup & restore intentionally omitted — redundant with backend sync (data lives in
            Postgres keyed to the Firebase UID; a reinstall re-syncs). See docs/FEATURES.md → Dropped. */}

        <RemindersSection />

        {/* Health Connect */}
        <SectionLabel text="Health Connect" />
        <div style={{ ...cardStyle, display: "flex", alignItems: "center", padding: 14 }}>
          <div style={{ width: 34, height: 34, borderRadius: "50%", background: C.bgAlt, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16, color: C.muted3 }}>♡</div>
          <div style={{ marginLeft: 11, flex: 1 }}>
            <div style={{ font: "600 14.5px system-ui", color: C.ink }}>Health Connect</div>
            <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>{connected ? "Connected" : "Not connected"}</div>
          </div>
          <Toggle on={connected} onToggle={() => setConnected((v) => !v)} />
        </div>

        {/* Export data */}
        <SectionLabel text="Export data" />
        <div style={cardStyle}>
          <ValueRow label="Format" value="CSV" />
          <Divider />
          <div style={{ display: "flex", alignItems: "center", padding: "12px 14px" }}>
            <span style={{ font: "400 13.5px system-ui", color: C.ink }}>Includes</span>
            <span style={{ marginLeft: "auto", font: "400 12px system-ui", color: C.muted }}>Meals · workouts · health</span>
          </div>
          <div style={{ padding: 14 }}>
            <button onClick={handleExport} disabled={exporting} style={{ width: "100%", border: "none", borderRadius: 11, padding: "12px 0", background: C.bgAlt, color: C.muted3, font: "600 13px system-ui", cursor: exporting ? "default" : "pointer", opacity: exporting ? 0.6 : 1 }}>{exporting ? "Exporting…" : "⬇ Export  CSV"}</button>
          </div>
        </div>

        {/* Feedback */}
        <SectionLabel text="Feedback" />
        <div style={cardStyle}>
          <div onClick={() => setFeedbackOpen(true)}
            style={{ display: "flex", alignItems: "center", padding: "12px 14px", cursor: "pointer" }}>
            <div style={{ width: 34, height: 34, borderRadius: "50%", background: "#dfeaf6", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>💬</div>
            <div style={{ marginLeft: 11, flex: 1 }}>
              <div style={{ font: "600 14px system-ui", color: C.ink }}>Send feedback</div>
              <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>Report a bug or suggest an improvement</div>
            </div>
            <span style={{ color: "#c4ccd1", fontSize: 15 }}>›</span>
          </div>
        </div>

        {/* Help */}
        <SectionLabel text="Help" />
        <div style={cardStyle}>
          <div onClick={() => { void startTour(router); }}
            style={{ display: "flex", alignItems: "center", padding: "13px 14px", cursor: "pointer", borderBottom: `1px solid ${C.border}` }}>
            <span style={{ font: "500 13.5px system-ui", color: C.ink }}>Replay app tour</span>
            <span style={{ marginLeft: "auto", color: "#c4ccd1", fontSize: 15 }}>›</span>
          </div>
          <div onClick={() => { ob.reset(); router.push("/today"); }}
            style={{ display: "flex", alignItems: "center", padding: "13px 14px", cursor: "pointer" }}>
            <span style={{ font: "500 13.5px system-ui", color: C.ink }}>Replay setup / onboarding</span>
            <span style={{ marginLeft: "auto", color: "#c4ccd1", fontSize: 15 }}>›</span>
          </div>
        </div>

        {/* About */}
        <SectionLabel text="About" />
        <div style={cardStyle}>
          <a href="/privacy" style={{ display: "flex", alignItems: "center", padding: "13px 14px", textDecoration: "none" }}>
            <span style={{ font: "500 13.5px system-ui", color: C.ink }}>Privacy Policy</span>
            <span style={{ marginLeft: "auto", color: "#c4ccd1", fontSize: 15 }}>›</span>
          </a>
        </div>

        <div style={{ textAlign: "center", font: "400 11px system-ui", color: "#9aa4aa", margin: "24px 0" }}>© MealPlan+ · v{APP_VERSION}</div>
      </div>

      {feedbackOpen && (
        <div onClick={() => !sendingFeedback && setFeedbackOpen(false)}
          style={{ position: "fixed", inset: 0, zIndex: 50, background: "rgba(0,0,0,.4)", display: "flex", alignItems: "center", justifyContent: "center", padding: 20 }}>
          <div onClick={(e) => e.stopPropagation()}
            style={{ width: "100%", maxWidth: 380, background: C.surface, borderRadius: 14, padding: 18 }}>
            <div style={{ font: "700 17px system-ui", color: C.ink }}>Send feedback</div>
            <div style={{ font: "400 12.5px system-ui", color: C.muted2, marginTop: 6 }}>Tell us what&apos;s working or what&apos;s not. Your app version is included automatically.</div>
            <textarea
              value={feedbackText}
              onChange={(e) => setFeedbackText(e.target.value)}
              disabled={sendingFeedback}
              placeholder="Your feedback…"
              rows={5}
              style={{ width: "100%", marginTop: 12, padding: 10, border: `1px solid ${C.border}`, borderRadius: 10, font: "400 14px system-ui", color: C.ink, resize: "vertical", outline: "none" }}
            />
            <div style={{ display: "flex", justifyContent: "flex-end", gap: 8, marginTop: 14 }}>
              <button onClick={() => setFeedbackOpen(false)} disabled={sendingFeedback}
                style={{ border: "none", background: "transparent", color: C.muted3, font: "600 13px system-ui", padding: "8px 12px", cursor: "pointer" }}>Cancel</button>
              <button onClick={handleSendFeedback} disabled={sendingFeedback || !feedbackText.trim()}
                style={{ border: "none", background: C.teal, color: "#fff", font: "600 13px system-ui", borderRadius: 8, padding: "8px 18px", cursor: sendingFeedback || !feedbackText.trim() ? "default" : "pointer", opacity: sendingFeedback || !feedbackText.trim() ? 0.6 : 1 }}>{sendingFeedback ? "Sending…" : "Send"}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default function SettingsPage() {
  return <AuthGuard><SettingsInner /></AuthGuard>;
}
