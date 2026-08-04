"use client";

import { useState } from "react";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { BottomSheet } from "@/components/ui/BottomSheet";
import { usePlan } from "@/hooks/usePlan";
import type { DayPlanDto } from "@/lib/api/plans";
import type { WorkoutTemplateDto } from "@/lib/api/workouts";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", green: "oklch(0.66 0.13 150)", danger: "#b23b3b", blue: "oklch(0.60 0.11 255)",
};
const mono = "'DM Mono', monospace";
const iso = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const WD = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];

function Dot({ color }: { color: string }) { return <span style={{ width: 4, height: 4, borderRadius: "50%", background: color }} />; }

function Calendar({ p }: { p: ReturnType<typeof usePlan> }) {
  const { year, month } = p.ym;
  const firstDow = (new Date(year, month - 1, 1).getDay() + 6) % 7; // Mon=0..Sun=6
  const days = new Date(year, month, 0).getDate();
  const cells: (number | null)[] = [...Array(firstDow).fill(null), ...Array.from({ length: days }, (_, i) => i + 1)];
  return (
    <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 14, padding: 12 }}>
      <div style={{ display: "flex", alignItems: "center", marginBottom: 10 }}>
        <button onClick={p.prevMonth} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3 }}>‹</button>
        <span style={{ flex: 1, textAlign: "center", font: "700 14px system-ui", color: C.ink }}>{MONTHS[month - 1]} {year}</span>
        <button onClick={p.nextMonth} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3 }}>›</button>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(7,1fr)" }}>
        {["M", "T", "W", "T", "F", "S", "S"].map((d, i) => <div key={i} style={{ textAlign: "center", font: "600 9.5px system-ui", color: C.muted2, paddingBottom: 4 }}>{d}</div>)}
        {cells.map((day, i) => {
          if (day == null) return <div key={i} />;
          const dIso = iso(year, month, day);
          const plan = p.plans[dIso];
          const isToday = dIso === p.todayIso;
          // Meal dot: green = marked complete · blue = planned (today/upcoming) · red = planned but
          // the day passed without completing. Workout dot: always blue.
          const completed = p.completedDays.has(dIso);
          const past = dIso < p.todayIso;
          const planned = plan?.dietId != null;
          const mealColor = completed ? C.green : planned && past ? C.danger : planned ? C.blue : null;
          return (
            <div key={i} style={{ aspectRatio: "1", display: "flex", justifyContent: "center", alignItems: "center" }}>
              <div onClick={() => p.setSelected(dIso)} style={{ cursor: "pointer", width: 34, height: 34, borderRadius: "50%", background: isToday ? C.teal : "transparent", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
                <span style={{ font: `${isToday ? 700 : 500} 12px system-ui`, color: isToday ? "#fff" : C.ink }}>{day}</span>
                <div style={{ display: "flex", gap: 2, marginTop: 1 }}>
                  {mealColor && <Dot color={isToday ? "#fff" : mealColor} />}
                  {plan?.plannedWorkouts && plan.plannedWorkouts.length > 0 && <Dot color={isToday ? "#fff" : C.blue} />}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function NextSeven({ p }: { p: ReturnType<typeof usePlan> }) {
  const rows = Array.from({ length: 7 }, (_, off) => {
    const d = new Date(p.today); d.setDate(p.today.getDate() + off);
    const dIso = iso(d.getFullYear(), d.getMonth() + 1, d.getDate());
    const plan = p.plans[dIso];
    const diet = plan?.dietId != null ? p.diets.find((x) => x.id === plan.dietId) : undefined;
    const workouts = plan?.plannedWorkouts?.map((w) => w.activityName).filter(Boolean) ?? [];
    return { off, d, dIso, diet, workouts };
  });
  return (
    <>
      <div style={{ font: "600 12.5px system-ui", color: C.ink, margin: "18px 0 8px 2px" }}>Next 7 days</div>
      <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, overflow: "hidden" }}>
        {rows.map(({ off, d, dIso, diet, workouts }) => (
          <div key={off} onClick={() => p.setSelected(dIso)} style={{ cursor: "pointer", display: "flex", alignItems: "center", padding: "11px 12px", borderBottom: off < 6 ? `1px solid ${C.bgAlt}` : "none" }}>
            <div style={{ width: 52 }}>
              <div style={{ font: "700 11.5px system-ui", color: C.ink }}>{off === 0 ? "Today" : WD[(d.getDay() + 6) % 7]}</div>
              <div style={{ font: "400 9.5px system-ui", color: C.muted2 }}>{d.getDate()} {MONTHS[d.getMonth()].slice(0, 3)}</div>
            </div>
            <div style={{ flex: 1, marginLeft: 10 }}>
              {diet ? <div style={{ font: "600 11.5px system-ui", color: C.teal }}>{diet.name}</div> : <div style={{ font: "400 11px system-ui", color: C.muted2 }}>No diet</div>}
              <div style={{ font: "400 10px system-ui", color: workouts.length ? C.green : C.muted2 }}>{workouts.length ? workouts.join(", ") : "No workout"}</div>
            </div>
            {diet && <span style={{ font: `600 10.5px ${mono}`, color: C.muted2 }}>{diet.kcal}</span>}
          </div>
        ))}
      </div>
    </>
  );
}

function DaySheet({ p, dateIso }: { p: ReturnType<typeof usePlan>; dateIso: string }) {
  const plan: DayPlanDto | undefined = p.plans[dateIso];
  const [y, m, d] = dateIso.split("-").map(Number);
  const dt = new Date(y, m - 1, d);
  const workouts = plan?.plannedWorkouts ?? [];
  const selectedDiet = plan?.dietId != null ? p.diets.find((di) => di.id === plan.dietId) : undefined;
  return (
    <BottomSheet open onClose={() => p.setSelected(null)} title="">
      <div style={{ marginTop: -8 }}>
        <div style={{ display: "flex", alignItems: "flex-start" }}>
          <div style={{ flex: 1 }}>
            <div style={{ font: "700 16px system-ui", color: C.ink }}>{dt.toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "short" })}</div>
          </div>
          {plan && <button onClick={() => p.clearDay(dateIso)} style={{ font: "600 12px system-ui", color: C.danger, background: "none", border: "none", cursor: "pointer" }}>Clear day</button>}
        </div>

        {/* Past-day recap: whether the day was marked complete and which meal slots were logged. */}
        {dateIso < p.todayIso && (
          <div style={{ marginTop: 12 }}>
            <div style={{ display: "flex", alignItems: "center" }}>
              <span style={{ font: "600 11px system-ui", color: C.muted2 }}>This day</span>
              <span style={{ flex: 1 }} />
              <span style={{ font: "600 11.5px system-ui", color: p.completedDays.has(dateIso) ? C.green : C.muted2 }}>{p.completedDays.has(dateIso) ? "Completed" : "Not completed"}</span>
            </div>
            {p.selectedSlots.length > 0 && (
              <div style={{ marginTop: 6 }}>
                {p.selectedSlots.map((s, i) => (
                  <div key={i} style={{ display: "flex", alignItems: "center", padding: "2px 0" }}>
                    <span style={{ width: 20, font: "700 12px system-ui", color: s.isLogged ? C.green : C.danger }}>{s.isLogged ? "✓" : "✗"}</span>
                    <span style={{ font: "400 12px system-ui", color: s.isLogged ? C.ink : C.muted2 }}>{s.slot}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        <div style={{ display: "flex", alignItems: "center", margin: "16px 0 4px" }}>
          <span style={{ font: "600 11px system-ui", color: C.muted2 }}>Diet plan</span>
          <span style={{ flex: 1 }} />
          {selectedDiet && <button onClick={p.openPicker} style={{ font: "600 12px system-ui", color: C.teal, background: "none", border: "none", cursor: "pointer" }}>Change diet</button>}
        </div>
        {selectedDiet ? (
          <>
            <div style={{ font: "700 14px system-ui", color: C.ink, marginTop: 2 }}>{selectedDiet.name}</div>
            <DietDetail diet={selectedDiet} />
          </>
        ) : (
          <button onClick={p.openPicker} style={{ width: "100%", borderRadius: 12, padding: "13px", marginTop: 2, background: C.teal, border: "none", font: "600 13px system-ui", color: "#fff", cursor: "pointer" }}>＋ Pick a diet</button>
        )}

        <div style={{ font: "600 11px system-ui", color: C.muted2, margin: "16px 0 4px" }}>Exercises</div>
        {workouts.length === 0 ? (
          <div style={{ font: "400 11.5px system-ui", color: C.muted2, marginBottom: 8 }}>No workouts planned.</div>
        ) : (
          <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 8 }}>
            {workouts.map((w, i) => (
              <span key={w.id ?? i} style={{ display: "inline-flex", alignItems: "center", font: "600 11px system-ui", color: C.ink, background: C.bgAlt, borderRadius: 20, padding: "6px 6px 6px 11px" }}>
                <span onClick={() => p.openWorkoutDetail(w.workoutTemplateId)} style={{ cursor: "pointer" }}>{w.activityName}</span>
                {w.id != null && <span onClick={() => p.removeWorkout(dateIso, w.id!)} style={{ cursor: "pointer", color: C.muted2, padding: "0 4px", fontSize: 13 }}>✕</span>}
              </span>
            ))}
          </div>
        )}
        <button onClick={p.openWorkoutPicker} style={{ width: "100%", borderRadius: 11, padding: "11px", border: `1.5px solid ${C.border}`, background: "none", font: "600 12px system-ui", color: C.teal, cursor: "pointer" }}>＋ Add from library</button>
        <div style={{ font: "400 9.5px system-ui", color: C.muted2, marginTop: 8, paddingBottom: 8 }}>Log a session from the Exercises → Logs tab to see it in your history.</div>
      </div>
    </BottomSheet>
  );
}

function SlotBadge({ slot }: { slot: string }) {
  return <span style={{ font: `600 9px ${mono}`, letterSpacing: 0.5, color: C.teal, background: C.bgAlt, borderRadius: 6, padding: "3px 6px", textTransform: "uppercase" }}>{slot}</span>;
}

function DietDetail({ diet }: { diet: ReturnType<typeof usePlan>["diets"][number] }) {
  if (diet.slots.length === 0) return <div style={{ font: "400 11px system-ui", color: C.muted2, marginTop: 8 }}>This diet has no meals yet.</div>;
  return (
    <div style={{ marginTop: 10, borderTop: `1px solid ${C.bgAlt}`, paddingTop: 10 }}>
      {diet.slots.map((s) => (
        <div key={s.slot} style={{ marginBottom: 12 }}>
          <div style={{ display: "flex", alignItems: "center", marginBottom: 6 }}>
            <SlotBadge slot={s.slot} />
            <span style={{ flex: 1 }} />
            <span style={{ font: `600 10px ${mono}`, color: C.muted2 }}>{s.kcal} kcal</span>
          </div>
          {s.lines.map((li, i) => (
            <div key={i} style={{ display: "flex", alignItems: "baseline", padding: "3px 0", paddingLeft: li.header ? 0 : 8 }}>
              <span style={{ flex: 1, font: `${li.header ? 600 : 400} ${li.header ? 12 : 11.5}px system-ui`, color: li.header ? C.ink : C.muted3 }}>{li.name}</span>
              <span style={{ font: `400 10px ${mono}`, color: C.muted2 }}>{li.meta}</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  );
}

// ── Diet picker (the Diets screen as a chooser: search + tag filter + expand) ────
function DietPicker({ p, dateIso }: { p: ReturnType<typeof usePlan>; dateIso: string }) {
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const plan = p.plans[dateIso];
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 50, background: C.bg, display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", alignItems: "center", padding: "10px 12px 8px" }}>
        <button onClick={p.closePicker} style={{ font: "400 24px system-ui", color: C.ink, background: "none", border: "none", cursor: "pointer", width: 36 }}>‹</button>
        <span style={{ font: "600 17px system-ui", color: C.ink }}>Choose a diet</span>
        <span style={{ flex: 1 }} />
        <span style={{ font: "400 12px system-ui", color: C.muted2, paddingRight: 8 }}>{p.diets.length} saved</span>
      </div>
      <div style={{ padding: "4px 16px" }}>
        <div style={{ display: "flex", alignItems: "center", background: C.bgAlt, borderRadius: 12, padding: "11px 14px" }}>
          <span style={{ fontSize: 13 }}>🔍</span>
          <input value={p.pickerSearch} onChange={(e) => p.setPickerSearch(e.target.value)} placeholder="Search your diets…"
            style={{ flex: 1, marginLeft: 10, border: "none", outline: "none", background: "transparent", font: "400 14px system-ui", color: C.ink }} />
        </div>
      </div>
      {p.allTags.length > 0 && (
        <div style={{ display: "flex", gap: 6, overflowX: "auto", padding: "8px 16px" }}>
          <TagChip label="All" on={p.pickerTag == null} onClick={() => p.setPickerTag(null)} />
          {p.allTags.map((t) => <TagChip key={t} label={t} on={p.pickerTag === t} onClick={() => p.setPickerTag(p.pickerTag === t ? null : t)} />)}
        </div>
      )}
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 14px 24px" }}>
        {p.filteredDiets.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 0" }}>
            <div style={{ fontSize: 40 }}>🥗</div>
            <div style={{ font: "600 14px system-ui", color: C.muted3, marginTop: 8 }}>No diets match</div>
          </div>
        ) : p.filteredDiets.map((di) => {
          const selected = plan?.dietId === di.id;
          const expanded = expandedId === di.id;
          return (
            <div key={di.id} onClick={() => setExpandedId(expanded ? null : di.id)}
              style={{ cursor: "pointer", background: C.surface, border: `1px solid ${selected ? C.teal : C.border}`, borderRadius: 14, padding: 14, marginBottom: 8 }}>
              <div style={{ display: "flex", alignItems: "center" }}>
                <div style={{ flex: 1 }}>
                  <div style={{ font: "700 13px system-ui", color: C.ink }}>{di.name}</div>
                  {di.tags.length > 0 && (
                    <div style={{ display: "flex", gap: 4, marginTop: 4, flexWrap: "wrap" }}>
                      {di.tags.slice(0, 3).map((t) => <span key={t} style={{ font: "600 8.5px system-ui", color: C.teal, background: "rgba(45,140,150,0.12)", borderRadius: 5, padding: "2px 6px" }}>{t}</span>)}
                      {di.tags.length > 3 && <span style={{ font: "400 8.5px system-ui", color: C.muted2 }}>+{di.tags.length - 3}</span>}
                    </div>
                  )}
                </div>
                <span style={{ font: `700 13px ${mono}`, color: C.ink }}>{di.kcal}</span>
                <span style={{ font: "400 9px system-ui", color: C.muted2 }}>&nbsp;kcal</span>
              </div>
              {expanded ? (
                <>
                  <DietDetail diet={di} />
                  <button disabled={selected} onClick={(e) => { e.stopPropagation(); p.chooseDiet(dateIso, di.id); }}
                    style={{ width: "100%", marginTop: 12, borderRadius: 11, padding: "11px", border: "none", cursor: selected ? "default" : "pointer", background: selected ? C.bgAlt : C.teal, font: "600 12.5px system-ui", color: selected ? C.muted3 : "#fff" }}>
                    {selected ? "✓ Selected" : "Choose this diet"}
                  </button>
                </>
              ) : (
                <div style={{ font: "400 10px system-ui", color: selected ? C.teal : C.muted2, marginTop: 6 }}>{selected ? "✓ Selected · tap to view" : "Tap to view meals"}</div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Workout picker (choose a template from the library to plan for the day) ──────
function WorkoutPicker({ p, dateIso }: { p: ReturnType<typeof usePlan>; dateIso: string }) {
  const plannedIds = new Set((p.plans[dateIso]?.plannedWorkouts ?? []).map((w) => w.workoutTemplateId).filter((x): x is number => x != null));
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 50, background: C.bg, display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", alignItems: "center", padding: "10px 12px 8px" }}>
        <button onClick={p.closeWorkoutPicker} style={{ font: "400 24px system-ui", color: C.ink, background: "none", border: "none", cursor: "pointer", width: 36 }}>‹</button>
        <span style={{ font: "600 17px system-ui", color: C.ink }}>Add workout</span>
        <span style={{ flex: 1 }} />
        <span style={{ font: "400 12px system-ui", color: C.muted2, paddingRight: 8 }}>{p.workouts.length} saved</span>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 14px 24px" }}>
        {p.workouts.length === 0 ? (
          <div style={{ textAlign: "center", padding: "48px 0" }}>
            <div style={{ fontSize: 40 }}>📋</div>
            <div style={{ font: "600 14px system-ui", color: C.muted3, marginTop: 8 }}>No workouts yet</div>
            <div style={{ font: "400 11.5px system-ui", color: C.muted2, marginTop: 4 }}>Build one in Exercises → Workouts first.</div>
          </div>
        ) : p.workouts.map((w) => {
          const items = w.exercises ?? [];
          const totalSets = items.reduce((s, it) => s + (it.sets?.length ?? 0), 0);
          const added = w.id != null && plannedIds.has(w.id);
          return (
            <div key={w.id} onClick={() => !added && p.addWorkout(dateIso, w)}
              style={{ cursor: added ? "default" : "pointer", background: C.surface, border: `1px solid ${added ? C.green : C.border}`, borderRadius: 14, padding: 14, marginBottom: 8 }}>
              <div style={{ display: "flex", alignItems: "center" }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ font: "700 13px system-ui", color: C.ink }}>{w.name}</div>
                  <div style={{ font: "400 10.5px system-ui", color: C.muted2, marginTop: 2 }}>{items.length} exercise{items.length === 1 ? "" : "s"} · {totalSets} sets</div>
                  {items.length > 0 && <div style={{ font: "400 10px system-ui", color: C.muted, marginTop: 3, overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap" }}>{items.map((it) => it.exerciseName ?? "Exercise").join(", ")}</div>}
                </div>
                <span style={{ font: "600 12px system-ui", color: added ? C.green : C.teal, marginLeft: 8, flex: "none" }}>{added ? "✓ Added" : "+ Add"}</span>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ── Planned workout detail (read-only: exercises + per-set targets) ──────────────
function WorkoutDetail({ w, onBack }: { w: WorkoutTemplateDto; onBack: () => void }) {
  const items = [...(w.exercises ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  const totalSets = items.reduce((s, it) => s + (it.sets?.length ?? 0), 0);
  const fmtKg = (v: number) => `${v} kg`;
  return (
    <div style={{ position: "fixed", inset: 0, zIndex: 55, background: C.bg, display: "flex", flexDirection: "column" }}>
      <div style={{ display: "flex", alignItems: "center", padding: "10px 12px 8px" }}>
        <button onClick={onBack} style={{ font: "400 24px system-ui", color: C.ink, background: "none", border: "none", cursor: "pointer", width: 36 }}>‹</button>
        <span style={{ font: "600 17px system-ui", color: C.ink }}>{w.name}</span>
      </div>
      <div style={{ flex: 1, overflowY: "auto", padding: "4px 16px 24px" }}>
        <div style={{ font: "400 11px system-ui", color: C.muted2, marginBottom: 8 }}>{items.length} exercise{items.length === 1 ? "" : "s"} · {totalSets} sets</div>
        {items.map((te) => (
          <div key={te.exerciseId} style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 14, padding: 14, marginBottom: 8 }}>
            <div style={{ font: "700 12.5px system-ui", color: C.ink }}>{te.exerciseName ?? "Exercise"}</div>
            <div style={{ display: "flex", margin: "8px 0 2px", font: `600 9.5px system-ui`, color: C.muted2 }}>
              <span style={{ width: 48 }}>Set</span><span style={{ width: 64 }}>Reps</span><span>Weight</span>
            </div>
            {[...(te.sets ?? [])].sort((a, b) => a.setNumber - b.setNumber).map((s, i) => (
              <div key={i} style={{ display: "flex", alignItems: "center", padding: "3px 0", font: `400 12px ${mono}`, color: C.ink }}>
                <span style={{ width: 48, color: C.muted3 }}>{i + 1}</span>
                <span style={{ width: 64 }}>{s.reps ?? "–"}</span>
                <span>{s.weightKg != null ? fmtKg(s.weightKg) : "–"}</span>
              </div>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

function TagChip({ label, on, onClick }: { label: string; on: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{ flex: "none", cursor: "pointer", border: "none", borderRadius: 20, padding: "6px 12px", font: "600 11.5px system-ui", color: on ? "#fff" : C.muted3, background: on ? C.ink : C.bgAlt }}>{label}</button>
  );
}

function PlanInner() {
  const p = usePlan();
  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div style={{ padding: "10px 16px 8px" }}><span style={{ font: "700 21px system-ui", color: C.ink }}>Plan</span></div>
      <div className="flex-1 overflow-y-auto" style={{ padding: "0 16px", paddingBottom: 120 }}>
        {p.error && <div style={{ textAlign: "center", padding: 24, font: "400 12px system-ui", color: C.danger }}>{p.error}</div>}
        <Calendar p={p} />
        <NextSeven p={p} />
      </div>
      {p.selected && !p.pickerOpen && !p.workoutPickerOpen && !p.openWorkout && <DaySheet p={p} dateIso={p.selected} />}
      {p.selected && p.pickerOpen && <DietPicker p={p} dateIso={p.selected} />}
      {p.selected && p.workoutPickerOpen && !p.openWorkout && <WorkoutPicker p={p} dateIso={p.selected} />}
      {p.openWorkout && <WorkoutDetail w={p.openWorkout} onBack={p.closeWorkoutDetail} />}
      <NutritionNav />
    </div>
  );
}

export default function PlanPage() {
  return <AuthGuard><PlanInner /></AuthGuard>;
}
