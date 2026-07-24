"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { BottomSheet } from "@/components/ui/BottomSheet";
import { useProfile } from "@/hooks/useProfile";
import type { UserResponse, UserUpdateRequest } from "@/lib/api/user";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", danger: "#b23b3b",
};
const mono = "'DM Mono', monospace";
const rnd = (n: number) => Math.round(n);

const GENDERS: [string, string][] = [["MALE", "Male"], ["FEMALE", "Female"], ["OTHER", "Other"]];
const ACTIVITIES: [string, string][] = [["SEDENTARY", "Sedentary"], ["LIGHT", "Lightly active"], ["MODERATE", "Moderately active"], ["VERY_ACTIVE", "Very active"], ["EXTRA_ACTIVE", "Extra active"]];
const GOALS: [string, string][] = [["LOSE", "Lose weight"], ["MAINTAIN", "Maintain"], ["GAIN", "Gain"]];
const labelOf = (opts: [string, string][], v: string | null | undefined) => opts.find((o) => o[0] === v)?.[1] ?? "—";
const trim = (n: number) => (n % 1 === 0 ? String(n) : n.toFixed(1));

function activityFactor(a: string | null | undefined): number {
  return { SEDENTARY: 1.2, LIGHT: 1.375, MODERATE: 1.55, VERY_ACTIVE: 1.725, EXTRA_ACTIVE: 1.9 }[a ?? ""] ?? 1.55;
}
function bmr(u: UserResponse): number | null {
  if (u.weightKg == null || u.heightCm == null || u.age == null) return null;
  const base = 10 * u.weightKg + 6.25 * u.heightCm - 5 * u.age;
  return u.gender === "MALE" ? base + 5 : u.gender === "FEMALE" ? base - 161 : base - 78;
}
function weightDisplay(kg: number | null | undefined, imp: boolean): string {
  if (kg == null) return "—";
  return imp ? `${(kg * 2.20462).toFixed(1)} lb` : `${trim(kg)} kg`;
}
function heightDisplay(cm: number | null | undefined, imp: boolean): string {
  if (cm == null) return "—";
  if (!imp) return `${trim(cm)} cm`;
  const totalIn = cm / 2.54, ft = Math.floor(totalIn / 12), inch = Math.round(totalIn % 12);
  return `${ft}'${inch}"`;
}

type Editor =
  | { kind: "num"; label: string; value: string; suffix: string; onSave: (v: string) => void }
  | { kind: "opt"; label: string; current: string; options: [string, string][]; onSave: (v: string) => void }
  | { kind: "weight"; kg: number | null | undefined; imperial: boolean; onSave: (kg: number | null) => void }
  | { kind: "height"; cm: number | null | undefined; imperial: boolean; onSave: (cm: number | null) => void };

function Row({ label, value, onClick }: { label: string; value: string; onClick: () => void }) {
  return (
    <div onClick={onClick} style={{ cursor: "pointer", display: "flex", alignItems: "center", padding: "11px 12px", borderTop: `1px solid ${C.bgAlt}` }}>
      <span style={{ flex: 1, font: "400 12.5px system-ui", color: C.muted3 }}>{label}</span>
      <span style={{ font: "600 12.5px system-ui", color: C.ink }}>{value}</span>
      <span style={{ color: C.muted2, marginLeft: 4 }}>›</span>
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <>
      <div style={{ font: "600 10px system-ui", color: C.muted2, letterSpacing: ".04em", margin: "16px 0 7px 4px" }}>{title.toUpperCase()}</div>
      <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, overflow: "hidden" }}>{children}</div>
    </>
  );
}

function NumInput({ value, onChange, suffix }: { value: string; onChange: (v: string) => void; suffix: string }) {
  return (
    <div style={{ display: "flex", alignItems: "center", border: `1.5px solid #dfe6e8`, borderRadius: 11, padding: "12px" }}>
      <input value={value} inputMode="decimal" onChange={(e) => onChange(e.target.value.replace(/[^0-9.]/g, ""))} placeholder="0"
        style={{ flex: 1, border: "none", outline: "none", background: "transparent", fontSize: 14, color: C.ink }} />
      <span style={{ font: "400 12px system-ui", color: C.muted2 }}>{suffix}</span>
    </div>
  );
}
function SaveBtn({ onClick }: { onClick: () => void }) {
  return <button onClick={onClick} style={{ width: "100%", borderRadius: 12, padding: "13px", marginTop: 14, background: C.teal, color: "#fff", font: "600 13px system-ui", border: "none" }}>Save</button>;
}

function EditorSheet({ editor, onClose }: { editor: Editor; onClose: () => void }) {
  const title = editor.kind === "weight" ? (editor.imperial ? "Weight (lb)" : "Weight (kg)")
    : editor.kind === "height" ? (editor.imperial ? "Height (ft / in)" : "Height (cm)")
    : editor.label;
  return (
    <BottomSheet open onClose={onClose} title={title}>
      {editor.kind === "num" && <NumEdit init={editor.value} suffix={editor.suffix} onSave={(v) => { editor.onSave(v); onClose(); }} />}
      {editor.kind === "opt" && (
        <div>
          {editor.options.map(([v, lbl]) => {
            const on = v === editor.current;
            return (
              <div key={v} onClick={() => { editor.onSave(v); onClose(); }} style={{ cursor: "pointer", display: "flex", alignItems: "center", padding: "12px 0" }}>
                <span style={{ flex: 1, font: `${on ? 600 : 400} 13.5px system-ui`, color: on ? C.teal : C.ink }}>{lbl}</span>
                {on && <span style={{ color: C.teal }}>✓</span>}
              </div>
            );
          })}
        </div>
      )}
      {editor.kind === "weight" && <WeightEdit kg={editor.kg} imperial={editor.imperial} onSave={(kg) => { editor.onSave(kg); onClose(); }} />}
      {editor.kind === "height" && <HeightEdit cm={editor.cm} imperial={editor.imperial} onSave={(cm) => { editor.onSave(cm); onClose(); }} />}
    </BottomSheet>
  );
}

function NumEdit({ init, suffix, onSave }: { init: string; suffix: string; onSave: (v: string) => void }) {
  const [text, setText] = useState(init);
  return <><NumInput value={text} onChange={setText} suffix={suffix} /><SaveBtn onClick={() => onSave(text.trim())} /></>;
}
function WeightEdit({ kg, imperial, onSave }: { kg: number | null | undefined; imperial: boolean; onSave: (kg: number | null) => void }) {
  const [text, setText] = useState(kg == null ? "" : imperial ? (kg * 2.20462).toFixed(1) : trim(kg));
  return <><NumInput value={text} onChange={setText} suffix={imperial ? "lb" : "kg"} />
    <SaveBtn onClick={() => { const n = parseFloat(text); onSave(isNaN(n) ? null : imperial ? n / 2.20462 : n); }} /></>;
}
function HeightEdit({ cm, imperial, onSave }: { cm: number | null | undefined; imperial: boolean; onSave: (cm: number | null) => void }) {
  const totalIn = (cm ?? 0) / 2.54;
  const [single, setSingle] = useState(cm == null ? "" : trim(cm as number));
  const [ft, setFt] = useState(cm == null ? "" : String(Math.floor(totalIn / 12)));
  const [inch, setInch] = useState(cm == null ? "" : String(Math.round(totalIn % 12)));
  if (!imperial) return <><NumInput value={single} onChange={setSingle} suffix="cm" /><SaveBtn onClick={() => { const n = parseFloat(single); onSave(isNaN(n) ? null : n); }} /></>;
  return (
    <>
      <div style={{ display: "flex", gap: 10 }}>
        <div style={{ flex: 1 }}><NumInput value={ft} onChange={setFt} suffix="ft" /></div>
        <div style={{ flex: 1 }}><NumInput value={inch} onChange={setInch} suffix="in" /></div>
      </div>
      <SaveBtn onClick={() => { const f = parseInt(ft); const i = parseInt(inch) || 0; onSave(isNaN(f) ? null : (f * 12 + i) * 2.54); }} />
    </>
  );
}

function ProfileInner() {
  const p = useProfile();
  const router = useRouter();
  const [editor, setEditor] = useState<Editor | null>(null);
  const [confirmClear, setConfirmClear] = useState(false);
  const u = p.user;
  const imp = u?.units === "IMPERIAL";
  const set = (patch: UserUpdateRequest) => p.patch(patch);

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div style={{ display: "flex", alignItems: "center", padding: "8px 16px 8px 6px" }}>
        <button onClick={() => router.replace("/today")} style={{ fontSize: 22, color: C.ink, padding: "0 8px" }}>‹</button>
        <span style={{ font: "700 19px system-ui", color: C.ink }}>Profile</span>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: "0 16px", paddingBottom: 120 }}>
        {p.loading && <div style={{ textAlign: "center", padding: 48, font: "400 12px system-ui", color: C.muted }}>Loading…</div>}
        {u && (
          <>
            <div style={{ display: "flex", alignItems: "center", gap: 12, background: C.surface, border: `1px solid ${C.border}`, borderRadius: 14, padding: 14 }}>
              <div style={{ width: 52, height: 52, borderRadius: "50%", background: C.teal, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 24 }}>●</div>
              <div>
                <div style={{ font: "700 15px system-ui", color: C.ink }}>{u.displayName ?? "Set your name"}</div>
                <div style={{ font: "400 11px system-ui", color: C.muted2, marginTop: 2 }}>{u.email ?? "—"}</div>
              </div>
            </div>

            <Section title="Body">
              <Row label="Name" value={u.displayName ?? "—"} onClick={() => setEditor({ kind: "num", label: "Name", value: u.displayName ?? "", suffix: "", onSave: (v) => set({ displayName: v.trim() || null }) })} />
              <Row label="Height" value={heightDisplay(u.heightCm, imp)} onClick={() => setEditor({ kind: "height", cm: u.heightCm, imperial: imp, onSave: (cm) => set({ heightCm: cm }) })} />
              <Row label="Weight" value={weightDisplay(u.weightKg, imp)} onClick={() => setEditor({ kind: "weight", kg: u.weightKg, imperial: imp, onSave: (kg) => set({ weightKg: kg }) })} />
              <Row label="Age" value={u.age != null ? String(u.age) : "—"} onClick={() => setEditor({ kind: "num", label: "Age", value: u.age?.toString() ?? "", suffix: "yrs", onSave: (v) => set({ age: parseInt(v) || null }) })} />
              <Row label="Sex" value={labelOf(GENDERS, u.gender)} onClick={() => setEditor({ kind: "opt", label: "Sex", current: u.gender ?? "", options: GENDERS, onSave: (v) => set({ gender: v as UserUpdateRequest["gender"] }) })} />
              <Row label="Activity" value={labelOf(ACTIVITIES, u.activityLevel)} onClick={() => setEditor({ kind: "opt", label: "Activity level", current: u.activityLevel ?? "", options: ACTIVITIES, onSave: (v) => set({ activityLevel: v as UserUpdateRequest["activityLevel"] }) })} />
            </Section>

            <Section title="Goal & targets">
              <Row label="Goal" value={labelOf(GOALS, u.goalType)} onClick={() => setEditor({ kind: "opt", label: "Goal", current: u.goalType ?? "", options: GOALS, onSave: (v) => set({ goalType: v as UserUpdateRequest["goalType"] }) })} />
              <Row label="Calorie target" value={u.targetCalories != null ? `${u.targetCalories} kcal` : "—"} onClick={() => setEditor({ kind: "num", label: "Calorie target (kcal)", value: u.targetCalories?.toString() ?? "", suffix: "kcal", onSave: (v) => set({ targetCalories: parseInt(v) || null }) })} />
              <Row label="Protein" value={u.targetProtein != null ? `${u.targetProtein} g` : "—"} onClick={() => setEditor({ kind: "num", label: "Protein target (g)", value: u.targetProtein?.toString() ?? "", suffix: "g", onSave: (v) => set({ targetProtein: parseInt(v) || null }) })} />
              <Row label="Carbs" value={u.targetCarbs != null ? `${u.targetCarbs} g` : "—"} onClick={() => setEditor({ kind: "num", label: "Carbs target (g)", value: u.targetCarbs?.toString() ?? "", suffix: "g", onSave: (v) => set({ targetCarbs: parseInt(v) || null }) })} />
              <Row label="Fat" value={u.targetFat != null ? `${u.targetFat} g` : "—"} onClick={() => setEditor({ kind: "num", label: "Fat target (g)", value: u.targetFat?.toString() ?? "", suffix: "g", onSave: (v) => set({ targetFat: parseInt(v) || null }) })} />
            </Section>

            <Section title="Energy">
              {(() => {
                const b = bmr(u); const t = b == null ? null : b * activityFactor(u.activityLevel);
                return (
                  <>
                    <div style={{ display: "flex", padding: "11px 12px" }}><span style={{ flex: 1, font: "400 12.5px system-ui", color: C.muted3 }}>BMR</span><span style={{ font: `600 12px ${mono}`, color: C.ink }}>{b == null ? "Add body stats" : `${rnd(b)} kcal/day`}</span></div>
                    <div style={{ display: "flex", padding: "11px 12px", borderTop: `1px solid ${C.bgAlt}` }}><span style={{ flex: 1, font: "400 12.5px system-ui", color: C.muted3 }}>TDEE</span><span style={{ font: `600 12px ${mono}`, color: C.ink }}>{t == null ? "—" : `${rnd(t)} kcal/day`}</span></div>
                  </>
                );
              })()}
            </Section>

            <Section title="Preferences">
              <div style={{ display: "flex", alignItems: "center", padding: "10px 12px" }}>
                <span style={{ flex: 1, font: "400 12.5px system-ui", color: C.muted3 }}>Units</span>
                <div style={{ display: "flex", border: `1px solid #dfe6e8`, borderRadius: 9, overflow: "hidden" }}>
                  {[[false, "Metric"], [true, "Imperial"]].map(([impV, lbl]) => {
                    const on = impV === imp;
                    return <button key={lbl as string} onClick={() => { if (!on) set({ units: impV ? "IMPERIAL" : "METRIC" }); }}
                      style={{ font: "600 11.5px system-ui", padding: "7px 12px", background: on ? C.ink : "#fff", color: on ? "#fff" : C.muted3, border: "none" }}>{lbl as string}</button>;
                  })}
                </div>
              </div>
            </Section>

            <Section title="Account">
              <button onClick={() => p.signOut()} style={{ width: "100%", textAlign: "left", padding: "13px 12px", font: "600 12.5px system-ui", color: C.ink, background: "none", border: "none", cursor: "pointer" }}>Log out</button>
              <button onClick={() => setConfirmClear(true)} style={{ width: "100%", textAlign: "left", padding: "13px 12px", font: "600 12.5px system-ui", color: C.danger, background: "none", border: "none", borderTop: `1px solid ${C.bgAlt}`, cursor: "pointer" }}>Clear all data</button>
            </Section>
            <div style={{ font: "400 10px system-ui", color: C.muted2, margin: "12px 0 0 4px" }}>Signed in as {u.email ?? "—"}</div>
          </>
        )}
      </div>

      {editor && <EditorSheet editor={editor} onClose={() => setEditor(null)} />}
      {confirmClear && (
        <BottomSheet open onClose={() => setConfirmClear(false)} title="Clear all data?">
          <div style={{ font: "400 12px system-ui", color: C.muted2, marginTop: -8, marginBottom: 16 }}>This signs you out of this device. Your data on the server is not deleted.</div>
          <button onClick={() => { setConfirmClear(false); p.signOut(); }} style={{ width: "100%", borderRadius: 12, padding: 13, background: C.danger, color: "#fff", font: "600 13px system-ui", border: "none" }}>Clear &amp; sign out</button>
          <button onClick={() => setConfirmClear(false)} style={{ width: "100%", padding: 12, marginTop: 4, background: "none", color: C.muted3, font: "600 13px system-ui", border: "none" }}>Cancel</button>
        </BottomSheet>
      )}
      <NutritionNav />
    </div>
  );
}

export default function ProfilePage() {
  return <AuthGuard><ProfileInner /></AuthGuard>;
}
