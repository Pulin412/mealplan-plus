"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { collectExportData, downloadCsv } from "@/lib/export/collectExport";
import { buildCsv } from "@/lib/export/csvExporter";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", faint: "#a2abb1",
  border: "#eaeef0", borderMuted: "#e4e8eb", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", success: "#4da876",
};

const NOTIF_DEFS = [
  { key: "meals", label: "Meal reminders", hint: "At each planned slot", icon: "🍽️", bg: "#f6ebe0" },
  { key: "water", label: "Water", hint: "Every 2 hours, 8am–8pm", icon: "💧", bg: "#dfeaf6" },
  { key: "workout", label: "Workout", hint: "On scheduled days", icon: "🏋️", bg: "#dff2e7" },
  { key: "weighin", label: "Weigh-in", hint: "Weekly, Sunday 8am", icon: "⚖️", bg: "#ebe7f3" },
  { key: "glucose", label: "Glucose check", hint: "Before & after meals", icon: "🩸", bg: "#f6e4e2" },
];

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

function SettingsInner() {
  const router = useRouter();
  const [autoBackup, setAutoBackup] = useState(true);
  const [connected, setConnected] = useState(false);
  const [notifOpen, setNotifOpen] = useState(true);
  const [notif, setNotif] = useState<Record<string, boolean>>({ meals: true, water: true, workout: true, weighin: false, glucose: true });
  const onCount = Object.values(notif).filter(Boolean).length;
  const [exporting, setExporting] = useState(false);

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
        {/* Backup & restore */}
        <SectionLabel text="Backup & restore" />
        <div style={cardStyle}>
          <div style={{ display: "flex", alignItems: "center", padding: 14 }}>
            <div style={{ flex: 1 }}>
              <div style={{ font: "600 14.5px system-ui", color: C.ink }}>Auto-backup</div>
              <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>Encrypted, to your account</div>
            </div>
            <Toggle on={autoBackup} onToggle={() => setAutoBackup((v) => !v)} />
          </div>
          <Divider />
          <ValueRow label="Frequency" value="Daily" />
          <Divider />
          <div style={{ display: "flex", alignItems: "center", padding: "11px 14px" }}>
            <span style={{ color: C.success, fontSize: 14 }}>✓</span>
            <span style={{ marginLeft: 8, font: "400 12px system-ui", color: C.muted3 }}>Last backup: Today, 8:04 AM</span>
          </div>
          <div style={{ display: "flex", gap: 10, padding: "2px 14px 14px" }}>
            <button style={{ flex: 1, border: "none", borderRadius: 11, padding: "12px 0", background: C.teal, color: "#fff", font: "600 13px system-ui", cursor: "pointer" }}>Back up now</button>
            <button style={{ flex: 1, borderRadius: 11, padding: "12px 0", background: "none", border: `1.5px solid ${C.borderMuted}`, color: C.teal, font: "600 13px system-ui", cursor: "pointer" }}>Restore</button>
          </div>
        </div>

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

        {/* Notifications */}
        <div onClick={() => setNotifOpen((v) => !v)} style={{ display: "flex", alignItems: "center", cursor: "pointer", margin: "22px 4px 8px" }}>
          <span style={{ font: "700 10.5px system-ui", letterSpacing: 0.6, textTransform: "uppercase", color: C.faint }}>Notifications</span>
          <span style={{ marginLeft: 8, font: "400 10px system-ui", color: C.faint }}>{onCount} of {NOTIF_DEFS.length} on</span>
          <span style={{ marginLeft: "auto", color: C.faint, fontSize: 14 }}>{notifOpen ? "⌄" : "›"}</span>
        </div>
        {notifOpen && (
          <div style={cardStyle}>
            {NOTIF_DEFS.map((n, i) => (
              <div key={n.key}>
                <div style={{ display: "flex", alignItems: "center", padding: "10px 15px" }}>
                  <div style={{ width: 34, height: 34, borderRadius: "50%", background: n.bg, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>{n.icon}</div>
                  <div style={{ marginLeft: 11, flex: 1 }}>
                    <div style={{ font: "600 14px system-ui", color: C.ink }}>{n.label}</div>
                    <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>{n.hint}</div>
                  </div>
                  <Toggle on={!!notif[n.key]} onToggle={() => setNotif((p) => ({ ...p, [n.key]: !p[n.key] }))} />
                </div>
                {i < NOTIF_DEFS.length - 1 && <Divider />}
              </div>
            ))}
            <Divider />
            <ValueRow label="Quiet hours" value="10 PM – 7 AM" labelColor={C.muted3} />
          </div>
        )}
      </div>
    </div>
  );
}

export default function SettingsPage() {
  return <AuthGuard><SettingsInner /></AuthGuard>;
}
