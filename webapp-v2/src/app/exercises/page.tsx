"use client";

import { useExercises, type LibTab, type BuilderItem } from "@/hooks/useExercises";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { exerciseTagColor } from "@/lib/exerciseTags";
import type { ExerciseDto } from "@/lib/api/exercises";
import type { WorkoutTemplateDto } from "@/lib/api/workouts";
import type { TagDto } from "@/lib/api/tags";
import type { WorkoutSessionDto } from "@/lib/api/sessions";
import { isoOf } from "@/lib/api/plans";

// ─── token shortcuts ─────────────────────────────────────────────────────────
const C = {
  ink:     "#14181b",
  muted:   "#8a949b",
  muted2:  "#9aa4aa",
  muted3:  "#5b666e",
  faint:   "#a2abb1",
  border:  "#eaeef0",
  borderCool: "#dfe6e8",
  surface: "#ffffff",
  bg:      "#f7f9fa",
  bgAlt:   "#f2f4f5",
  danger:  "#b23b3b",
  teal:    "oklch(0.62 0.09 210)",
};

const TAB_LABELS: Record<LibTab, string> = { exercises: "Exercises", workouts: "Workouts", logs: "Logs" };
const TABS: LibTab[] = ["exercises", "workouts", "logs"];

// ─── Tag chips ────────────────────────────────────────────────────────────────
function TagChip({ name }: { name: string }) {
  const c = exerciseTagColor(name);
  return (
    <span className="rounded-[6px] px-[7px] py-[3px] text-[9.5px] font-semibold"
      style={{ color: c, background: `color-mix(in oklch, ${c} 12%, transparent)` }}>
      {name}
    </span>
  );
}

function TagToggle({ name, on, onClick }: { name: string; on: boolean; onClick: () => void }) {
  const c = exerciseTagColor(name);
  return (
    <button onClick={onClick}
      className="rounded-[20px] px-3 py-[7px] text-[12px] font-semibold transition-colors"
      style={{
        color: on ? "#fff" : c,
        background: on ? c : "transparent",
        border: on ? "none" : `1px solid color-mix(in oklch, ${c} 50%, transparent)`,
      }}>
      {name}
    </button>
  );
}

// ─── Tab bar ──────────────────────────────────────────────────────────────────
function LibTabBar({ tab, onSelect }: { tab: LibTab; onSelect: (t: LibTab) => void }) {
  return (
    <div className="mx-4 my-1 flex rounded-[9px] overflow-hidden" style={{ border: `1px solid ${C.borderCool}` }}>
      {TABS.map((t) => {
        const selected = t === tab;
        return (
          <button key={t} onClick={() => onSelect(t)}
            className="flex-1 h-[34px] text-[12px] transition-colors"
            style={{
              background: selected ? C.ink : C.surface,
              color:      selected ? C.surface : C.muted,
              fontWeight: selected ? 600 : 400,
            }}>
            {TAB_LABELS[t]}
          </button>
        );
      })}
    </div>
  );
}

// ─── Cards ──────────────────────────────────────────────────────────────────
function ExerciseCard({ e, tagName, onClick }: { e: ExerciseDto; tagName: Map<number, string>; onClick: () => void }) {
  const names = (e.tagIds ?? []).map((id) => tagName.get(id)).filter(Boolean) as string[];
  return (
    <div onClick={onClick} className="cursor-pointer rounded-[12px] mb-2 px-3 py-[11px]"
      style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="flex items-center gap-2">
        <div className="flex-1 min-w-0">
          <div className="text-[13px] font-bold truncate" style={{ color: C.ink }}>{e.name}</div>
          {names.length > 0 && (
            <div className="flex flex-wrap gap-[5px] mt-[5px]">
              {names.slice(0, 4).map((n) => <TagChip key={n} name={n} />)}
            </div>
          )}
        </div>
        <span className="text-[20px] leading-none" style={{ color: C.muted2 }}>›</span>
      </div>
    </div>
  );
}

function WorkoutCard({ w, onClick }: { w: WorkoutTemplateDto; onClick: () => void }) {
  const items = w.exercises ?? [];
  const totalSets = items.reduce((sum, it) => sum + (it.sets?.length ?? 0), 0);
  return (
    <div onClick={onClick} className="cursor-pointer rounded-[12px] mb-2 px-3 py-[11px]"
      style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="flex items-center gap-2">
        <div className="flex-1 min-w-0">
          <div className="text-[13px] font-bold truncate" style={{ color: C.ink }}>{w.name}</div>
          <div className="text-[10.5px] mt-0.5" style={{ color: C.muted2 }}>
            {items.length} exercise{items.length === 1 ? "" : "s"} · {totalSets} sets
          </div>
          {items.length > 0 && (
            <div className="text-[10px] truncate mt-[3px]" style={{ color: C.faint }}>
              {items.map((it) => it.exerciseName ?? "Exercise").join(", ")}
            </div>
          )}
        </div>
        <span className="text-[20px] leading-none" style={{ color: C.muted2 }}>›</span>
      </div>
    </div>
  );
}

// ─── Shared bits ──────────────────────────────────────────────────────────────
function EmptyState({ glyph, title, sub }: { glyph: string; title: string; sub: string }) {
  return (
    <div className="flex flex-col items-center justify-center text-center px-10 py-16">
      <div className="text-[40px]">{glyph}</div>
      <div className="text-[15px] font-bold mt-[10px]" style={{ color: C.muted3 }}>{title}</div>
      <div className="text-[12px] mt-1" style={{ color: C.muted2 }}>{sub}</div>
    </div>
  );
}

function OverlayHeader({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <div className="flex-none flex items-center gap-1 px-[6px] pr-4 py-2" style={{ borderBottom: `1px solid ${C.border}` }}>
      <button onClick={onBack} className="w-10 h-10 flex items-center justify-center text-[22px]" style={{ color: C.ink }}>‹</button>
      <span className="text-[17px] font-semibold" style={{ color: C.ink }}>{title}</span>
    </div>
  );
}

function FieldLabel({ children }: { children: React.ReactNode }) {
  return <label className="block text-[11px] font-semibold mt-[14px] mb-1" style={{ color: C.muted3 }}>{children}</label>;
}

function PrimaryButton({ label, enabled, onClick }: { label: string; enabled: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} disabled={!enabled}
      className="w-full rounded-[10px] h-[50px] text-[14px] font-semibold transition-colors"
      style={{ background: enabled ? C.teal : C.bgAlt, color: enabled ? "#fff" : C.muted2, border: "none" }}>
      {label}
    </button>
  );
}

// ─── Exercise editor (full-screen overlay) ─────────────────────────────────────
function ExerciseEditorOverlay({ ex }: { ex: ReturnType<typeof useExercises> }) {
  const ed = ex.editor!;
  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      <OverlayHeader title={ed.id == null ? "New exercise" : "Edit exercise"} onBack={ex.closeEditor} />
      <div className="flex-1 overflow-y-auto flex flex-col px-5 pb-6">
        <FieldLabel>Name</FieldLabel>
        <input value={ed.name} onChange={(e) => ex.setEditorName(e.target.value)} placeholder="e.g. Bench Press"
          className="w-full rounded-[12px] px-[14px] py-[13px] text-[14px] outline-none"
          style={{ background: C.bgAlt, color: C.ink }} />

        <FieldLabel>Description</FieldLabel>
        <textarea value={ed.description} onChange={(e) => ex.setEditorDescription(e.target.value)}
          placeholder="Optional notes / how-to" rows={3}
          className="w-full rounded-[12px] px-[14px] py-[13px] text-[14px] outline-none resize-none"
          style={{ background: C.bgAlt, color: C.ink }} />

        <FieldLabel>Tags</FieldLabel>
        {ex.tags.length === 0 ? (
          <div className="text-[12px] mt-1" style={{ color: C.faint }}>No tags available.</div>
        ) : (
          <div className="flex flex-wrap gap-2 mt-1.5">
            {ex.tags.map((t: TagDto) => (
              <TagToggle key={t.id} name={t.name} on={ed.tagIds.has(t.id)} onClick={() => ex.toggleEditorTag(t.id)} />
            ))}
          </div>
        )}

        {ex.error && <div className="text-[12px] mt-3" style={{ color: C.danger }}>{ex.error}</div>}

        <div className="flex-1" />
        <div className="pt-5">
          <PrimaryButton label="Save exercise" enabled={ed.name.trim() !== ""} onClick={() => void ex.saveExercise()} />
          {ed.id != null && (
            <button onClick={() => void ex.removeExercise(ed.id!)}
              className="w-full text-center text-[13px] font-semibold py-[10px] mt-1" style={{ color: C.danger }}>
              ✕ Delete exercise
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Workout builder (full-screen overlay) ─────────────────────────────────────
function RepsStepper({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const btn = "w-7 h-7 rounded-[8px] flex items-center justify-center text-[16px] leading-none";
  return (
    <div className="flex items-center">
      <button onClick={() => onChange(Math.max(1, value - 1))} className={btn}
        style={{ border: `1px solid ${C.borderCool}`, color: C.ink }}>−</button>
      <span className="w-[30px] text-center text-[13px] font-semibold tabular-nums" style={{ color: C.ink, fontFamily: "'DM Mono', monospace" }}>{value}</span>
      <button onClick={() => onChange(value + 1)} className={btn}
        style={{ border: `1px solid ${C.borderCool}`, color: C.ink }}>+</button>
    </div>
  );
}

function WeightField({ weightKg, onChange }: { weightKg: number | null; onChange: (v: number | null) => void }) {
  const text = weightKg == null ? "" : String(weightKg);
  return (
    <div className="flex items-center gap-1 w-[74px] rounded-[8px] px-2 py-[6px]" style={{ border: `1px solid ${C.borderCool}` }}>
      <input value={text} inputMode="decimal" placeholder="–"
        onChange={(e) => {
          const cleaned = e.target.value.replace(/[^0-9.]/g, "");
          onChange(cleaned === "" ? null : (Number.isNaN(parseFloat(cleaned)) ? weightKg : parseFloat(cleaned)));
        }}
        className="flex-1 min-w-0 bg-transparent outline-none text-[12px] tabular-nums"
        style={{ color: C.ink, fontFamily: "'DM Mono', monospace" }} />
      <span className="text-[9.5px]" style={{ color: C.muted3 }}>kg</span>
    </div>
  );
}

function BuilderRow({ item, ex }: { item: BuilderItem; ex: ReturnType<typeof useExercises> }) {
  const id = item.exerciseId;
  return (
    <div className="rounded-[12px] mb-2 px-3 py-[11px]" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="flex items-center gap-2">
        <span className="flex-1 min-w-0 text-[12.5px] font-bold truncate" style={{ color: C.ink }}>{item.name}</span>
        <span className="text-[10px]" style={{ color: C.faint }}>{item.sets.length} set{item.sets.length === 1 ? "" : "s"}</span>
        <button onClick={() => ex.removeFromBuilder(id)} className="text-[13px] pl-2" style={{ color: C.muted2 }}>✕</button>
      </div>
      {/* Column headers */}
      <div className="flex items-center mt-2 mb-0.5 text-[9.5px] font-semibold" style={{ color: C.faint }}>
        <span className="w-[40px]" />
        <span className="w-[96px]">Reps</span>
        <span className="ml-[10px]">Weight</span>
      </div>
      {item.sets.map((s, i) => (
        <div key={i} className="flex items-center py-[3px]">
          <span className="w-[40px] text-[10.5px]" style={{ color: C.muted3, fontFamily: "'DM Mono', monospace" }}>Set {i + 1}</span>
          <div className="w-[96px]"><RepsStepper value={s.reps ?? 0} onChange={(v) => ex.setReps(id, i, v)} /></div>
          <div className="ml-[10px]"><WeightField weightKg={s.weightKg} onChange={(v) => ex.setWeight(id, i, v)} /></div>
          <span className="flex-1" />
          <button onClick={() => ex.duplicateSet(id, i)} title="Copy set"
            className="text-[13px] leading-none" style={{ color: C.muted2 }}>⧉</button>
          {item.sets.length > 1 && (
            <button onClick={() => ex.removeSet(id, i)} className="text-[12px] pl-3" style={{ color: C.muted2 }}>✕</button>
          )}
        </div>
      ))}
    </div>
  );
}

function WorkoutBuilderOverlay({ ex }: { ex: ReturnType<typeof useExercises> }) {
  const b = ex.builder!;
  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      <OverlayHeader title={b.id == null ? "New workout" : "Edit workout"} onBack={ex.closeBuilder} />
      <div className="flex-1 overflow-y-auto px-5">
        <FieldLabel>Name</FieldLabel>
        <input value={b.name} onChange={(e) => ex.setBuilderName(e.target.value)} placeholder="e.g. Push Day"
          className="w-full rounded-[12px] px-[14px] py-[13px] text-[14px] outline-none"
          style={{ background: C.bgAlt, color: C.ink }} />

        <div className="flex items-center mt-[18px] mb-2">
          <span className="text-[11px] font-semibold" style={{ color: C.muted3 }}>Exercises</span>
          <span className="flex-1" />
          <button onClick={ex.openPicker} className="text-[12px] font-semibold" style={{ color: C.teal }}>＋ Add exercise</button>
        </div>

        {b.items.length === 0 ? (
          <button onClick={ex.openPicker}
            className="w-full rounded-[12px] py-[22px] text-[12.5px]"
            style={{ background: C.bgAlt, color: C.muted3 }}>
            Add exercises from your library
          </button>
        ) : (
          b.items.map((item) => <BuilderRow key={item.exerciseId} item={item} ex={ex} />)
        )}

        {ex.error && <div className="text-[12px] mt-2" style={{ color: C.danger }}>{ex.error}</div>}
        <div className="h-3" />
      </div>

      {/* Pinned footer */}
      <div className="flex-none px-5 pt-2 pb-4" style={{ borderTop: `1px solid ${C.border}` }}>
        <PrimaryButton label="Save workout" enabled={ex.canSaveWorkout} onClick={() => void ex.saveWorkout()} />
        {b.id != null && (
          <button onClick={() => void ex.removeWorkout(b.id!)}
            className="w-full text-center text-[13px] font-semibold py-[8px] mt-1" style={{ color: C.danger }}>
            ✕ Delete workout
          </button>
        )}
      </div>
    </div>
  );
}

// ─── Exercise picker (full-screen overlay, inside the builder) ──────────────────
function ExercisePickerOverlay({ ex }: { ex: ReturnType<typeof useExercises> }) {
  const b = ex.builder!;
  const candidates = ex.pickerCandidates;
  return (
    <div className="fixed inset-0 z-[60] flex flex-col" style={{ background: C.bg }}>
      <OverlayHeader title="Add exercise" onBack={ex.closePicker} />
      <div className="flex-none px-4 pt-2 pb-1">
        <div className="flex items-center gap-2 rounded-[12px] px-3 py-[12px]" style={{ background: C.bgAlt }}>
          <span style={{ color: C.muted2 }}>⌕</span>
          <input value={b.pickerSearch} onChange={(e) => ex.setPickerSearch(e.target.value)}
            placeholder="Search exercises or tags…" autoFocus
            className="flex-1 bg-transparent outline-none text-[14px]" style={{ color: C.ink }} />
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-[14px] pt-1">
        {candidates.length === 0 ? (
          <EmptyState glyph="🔍" title="No exercises" sub="Create exercises in the Exercises tab first." />
        ) : (
          candidates.map((e) => {
            const names = (e.tagIds ?? []).map((id) => ex.tagName.get(id)).filter(Boolean) as string[];
            return (
              <div key={e.id} onClick={() => ex.addToBuilder(e)}
                className="cursor-pointer rounded-[12px] mb-2 px-3 py-[11px] flex items-center gap-2"
                style={{ background: C.surface, border: `1px solid ${C.border}` }}>
                <div className="flex-1 min-w-0">
                  <div className="text-[12.5px] font-bold truncate" style={{ color: C.ink }}>{e.name}</div>
                  {names.length > 0 && (
                    <div className="flex flex-wrap gap-[5px] mt-1">
                      {names.slice(0, 4).map((n) => <TagChip key={n} name={n} />)}
                    </div>
                  )}
                </div>
                <span className="text-[20px] leading-none pr-1" style={{ color: C.teal }}>+</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}

// ─── Logs (read-only workout history) ──────────────────────────────────────────
function fmtLogDate(d: WorkoutSessionDto["date"]): string {
  if (d == null) return "";
  const [y, m, day] = isoOf(d).split("-").map(Number);
  return new Date(y, m - 1, day).toLocaleDateString(undefined, { weekday: "short", day: "numeric", month: "short" });
}

function logMeta(s: WorkoutSessionDto): string {
  const sets = s.sets ?? [];
  const exCount = new Set(sets.map((x) => x.exerciseId)).size;
  const parts: string[] = [];
  if (s.date != null) parts.push(fmtLogDate(s.date));
  parts.push(`${exCount} exercise${exCount === 1 ? "" : "s"} · ${sets.length} sets`);
  if (s.durationMinutes != null) parts.push(`${s.durationMinutes} min`);
  return parts.join(" · ");
}

function LogCard({ s, onClick }: { s: WorkoutSessionDto; onClick: () => void }) {
  return (
    <div onClick={onClick} className="cursor-pointer rounded-[12px] mb-2 px-3 py-[11px] flex items-center gap-2"
      style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-1.5">
          <span className="text-[13px] font-bold truncate" style={{ color: C.ink }}>{s.name}</span>
          {s.isCompleted && <span className="text-[11px] font-bold flex-none" style={{ color: "oklch(0.66 0.13 150)" }}>✓</span>}
        </div>
        <div className="text-[10.5px] mt-0.5" style={{ color: C.muted2 }}>{logMeta(s)}</div>
      </div>
      <span className="text-[20px] leading-none" style={{ color: C.muted2 }}>›</span>
    </div>
  );
}

function LogDetailOverlay({ ex }: { ex: ReturnType<typeof useExercises> }) {
  const s = ex.openLog!;
  const sets = s.sets ?? [];
  // Group sets by exercise, preserving first-seen order.
  const order: number[] = [];
  const byExercise = new Map<number, typeof sets>();
  sets.forEach((set) => {
    if (!byExercise.has(set.exerciseId)) { byExercise.set(set.exerciseId, []); order.push(set.exerciseId); }
    byExercise.get(set.exerciseId)!.push(set);
  });
  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      <OverlayHeader title={s.name} onBack={ex.closeLogDetail} />
      <div className="flex-1 overflow-y-auto px-4 pt-1 pb-6">
        <div className="text-[11px] mb-1.5" style={{ color: C.muted2 }}>{logMeta(s)}</div>
        {s.notes && <div className="text-[12px] mb-2" style={{ color: C.muted3 }}>{s.notes}</div>}
        {order.map((exId) => {
          const rows = byExercise.get(exId)!.slice().sort((a, b) => a.setNumber - b.setNumber);
          return (
            <div key={exId} className="rounded-[14px] mb-2 p-[14px]" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
              <div className="text-[12.5px] font-bold" style={{ color: C.ink }}>{ex.exerciseName.get(exId) ?? "Exercise"}</div>
              <div className="flex mt-2 mb-0.5 text-[9.5px] font-semibold" style={{ color: C.faint }}>
                <span className="w-12">Set</span><span className="w-16">Reps</span><span>Weight</span>
              </div>
              {rows.map((set, i) => (
                <div key={i} className="flex items-center py-[3px] text-[12px] tabular-nums" style={{ color: C.ink, fontFamily: "'DM Mono', monospace" }}>
                  <span className="w-12" style={{ color: C.muted3 }}>{i + 1}</span>
                  <span className="w-16">{set.reps ?? "–"}</span>
                  <span>{set.weightKg != null ? `${set.weightKg} kg` : "–"}</span>
                </div>
              ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
function ExercisesPageInner() {
  const ex = useExercises();

  // Full-screen overlays take over the whole screen when open.
  if (ex.editor) return <ExerciseEditorOverlay ex={ex} />;
  if (ex.builder) return ex.builder.pickerOpen ? <ExercisePickerOverlay ex={ex} /> : <WorkoutBuilderOverlay ex={ex} />;
  if (ex.openLog) return <LogDetailOverlay ex={ex} />;

  const count = ex.tab === "exercises" ? `${ex.exercises.length} saved`
    : ex.tab === "workouts" ? `${ex.workouts.length} saved`
    : ex.logs.length > 0 ? `${ex.logs.length} logged` : null;

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      {/* App bar */}
      <div className="flex-none px-4 pt-3 pb-2" style={{ background: C.surface, borderBottom: `1px solid #eef1f3` }}>
        <div className="flex items-center">
          <span className="flex-1 text-[21px] font-bold" style={{ color: C.ink }}>Exercises</span>
          {count != null && <span className="text-[13px]" style={{ color: C.muted }}>{count}</span>}
        </div>
      </div>

      <LibTabBar tab={ex.tab} onSelect={ex.setTab} />

      {/* Tab content */}
      <div className="flex-1 overflow-y-auto px-[14px] pt-1" style={{ paddingBottom: 140 }}>
        {ex.loading ? (
          <div className="text-center py-16 text-[13px]" style={{ color: C.muted2 }}>Loading…</div>
        ) : ex.tab === "exercises" ? (
          ex.exercises.length === 0
            ? <EmptyState glyph="🏋️" title="No exercises yet" sub="Tap + to add an exercise with tags." />
            : ex.exercises.map((e) => <ExerciseCard key={e.id} e={e} tagName={ex.tagName} onClick={() => ex.openEditExercise(e)} />)
        ) : ex.tab === "workouts" ? (
          ex.workouts.length === 0
            ? <EmptyState glyph="📋" title="No workouts yet" sub="Tap + to build a workout from your exercises." />
            : ex.workouts.map((w) => <WorkoutCard key={w.id} w={w} onClick={() => ex.openEditWorkout(w)} />)
        ) : (
          ex.logs.length === 0
            ? <EmptyState glyph="📆" title="No workout logs yet" sub="Completed workouts show up here as read-only history." />
            : ex.logs.map((s) => <LogCard key={s.id} s={s} onClick={() => ex.openLogDetail(s)} />)
        )}
      </div>

      {/* FAB (hidden on Logs tab) */}
      {ex.tab !== "logs" && (
        <button onClick={() => (ex.tab === "exercises" ? ex.openNewExercise() : ex.openNewWorkout())}
          className="fixed bottom-[68px] right-5 z-30 w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg"
          style={{ background: C.teal, boxShadow: `0 6px 18px oklch(0.62 0.09 210 / .45)` }}>
          +
        </button>
      )}

      <NutritionNav />
    </div>
  );
}

export default function ExercisesPage() {
  return (
    <AuthGuard>
      <ExercisesPageInner />
    </AuthGuard>
  );
}
