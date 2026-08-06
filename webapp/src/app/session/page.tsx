"use client";

import { Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { Stepper } from "@/components/ui/Stepper";
import { useSession, type RunExercise, type RunSet } from "@/hooks/useSession";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", faint: "#a2abb1",
  border: "#eaeef0", borderCool: "#dfe6e8", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", green: "oklch(0.66 0.13 150)", danger: "#b23b3b",
};
const mono = "'DM Mono', monospace";
const fmtKg = (v: number) => `${v} kg`;
const setSummary = (s: RunSet) => (s.weightKg != null ? `${s.reps ?? "–"}×${fmtKg(s.weightKg)}` : `${s.reps ?? "–"}`);

function Header({ title, sub, onBack }: { title: string; sub: string; onBack: () => void }) {
  return (
    <div className="flex-none flex items-center gap-1 px-[6px] pr-4 py-2" style={{ borderBottom: `1px solid ${C.border}` }}>
      <button onClick={onBack} className="w-10 h-10 flex items-center justify-center text-[22px]" style={{ color: C.ink }}>‹</button>
      <div>
        <div className="text-[17px] font-semibold" style={{ color: C.ink }}>{title}</div>
        {sub && <div className="text-[10.5px]" style={{ color: C.faint }}>{sub}</div>}
      </div>
    </div>
  );
}

function ColHeaders({ actions }: { actions: boolean }) {
  return (
    <div className="flex mt-2 mb-0.5 text-[9.5px] font-semibold" style={{ color: C.faint }}>
      <span className="w-11" />
      <span style={{ width: actions ? 100 : 64 }}>Reps</span>
      <span style={{ marginLeft: actions ? 10 : 0 }}>Weight</span>
    </div>
  );
}

function Desc({ text }: { text: string | null }) {
  return text ? <div className="text-[10.5px] mt-[3px]" style={{ color: C.muted }}>{text}</div> : null;
}

function PrimaryButton({ label, enabled, onClick }: { label: string; enabled: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} disabled={!enabled} className="w-full rounded-[10px] h-[50px] text-[14px] font-semibold"
      style={{ background: enabled ? C.teal : C.bgAlt, color: enabled ? "#fff" : C.muted2 }}>
      {label}
    </button>
  );
}

function Card({ children }: { children: React.ReactNode }) {
  return <div className="rounded-[12px] mb-2 px-3 py-[11px]" style={{ background: C.surface, border: `1px solid ${C.border}` }}>{children}</div>;
}

function ReadOnlyRow({ n, reps, weightKg }: { n: number; reps: number | null; weightKg: number | null }) {
  return (
    <div className="flex items-center py-[3px] text-[12px] tabular-nums" style={{ color: C.ink, fontFamily: mono }}>
      <span className="w-11" style={{ color: C.muted3 }}>Set {n}</span>
      <span className="w-16">{reps ?? "–"}</span>
      <span>{weightKg != null ? fmtKg(weightKg) : "–"}</span>
    </div>
  );
}

// ── phases ──────────────────────────────────────────────────────────────────────
function ReadyPhase({ s }: { s: ReturnType<typeof useSession> }) {
  return (
    <>
      <div className="flex-1 overflow-y-auto px-[14px] pt-1.5 pb-2">
        {s.exercises.map((ex) => (
          <Card key={ex.exerciseId}>
            <div className="text-[13px] font-bold" style={{ color: C.ink }}>{ex.name}</div>
            <Desc text={ex.description} />
            <ColHeaders actions={false} />
            {ex.templateSets.map((set, i) => <ReadOnlyRow key={i} n={i + 1} reps={set.reps} weightKg={set.weightKg} />)}
            {ex.lastTime.length > 0 && (
              <div className="text-[10px] mt-1.5" style={{ color: C.faint }}>Last time: {ex.lastTime.map(setSummary).join("  ")}</div>
            )}
          </Card>
        ))}
      </div>
      <div className="flex-none px-5 pt-2 pb-4" style={{ borderTop: `1px solid ${C.border}` }}>
        {s.error && <div className="text-[12px] mb-2" style={{ color: C.danger }}>{s.error}</div>}
        <PrimaryButton label={s.busy ? "Starting…" : "▶  Start workout"} enabled={!s.busy && s.exercises.length > 0} onClick={s.start} />
      </div>
    </>
  );
}

function ActivePhase({ s }: { s: ReturnType<typeof useSession> }) {
  return (
    <>
      <div className="flex-1 overflow-y-auto px-[14px] pt-1.5 pb-2">
        {s.exercises.map((ex: RunExercise) => (
          <Card key={ex.exerciseId}>
            <div className="flex items-center gap-2">
              <span className="flex-1 text-[13px] font-bold" style={{ color: C.ink }}>{ex.name}</span>
              {ex.lastTime.length > 0 && (
                <button onClick={() => s.copyLast(ex.exerciseId)} className="text-[11px] font-semibold" style={{ color: C.teal }}>Copy last</button>
              )}
            </div>
            <Desc text={ex.description} />
            <ColHeaders actions />
            {ex.sets.map((set, i) => (
              <div key={i} className="flex items-center py-[3px]">
                <span className="w-11 text-[10.5px]" style={{ color: C.muted3, fontFamily: mono }}>Set {i + 1}</span>
                <Stepper value={set.reps ?? 0} onChange={(v) => s.setReps(ex.exerciseId, i, v)} min={0} max={100} dense />
                <div className="ml-[8px]"><Stepper value={set.weightKg ?? 0} onChange={(v) => s.setWeight(ex.exerciseId, i, v > 0 ? v : null)} min={0} max={1000} step={0.5} decimals={1} suffix="kg" dense /></div>
                <span className="flex-1" />
                {ex.sets.length > 1 && <button onClick={() => s.removeSet(ex.exerciseId, i)} className="text-[12px] pl-2" style={{ color: C.muted2 }}>✕</button>}
              </div>
            ))}
            <button onClick={() => s.addSet(ex.exerciseId)} className="text-[11.5px] font-semibold mt-1.5" style={{ color: C.teal }}>＋ Add set</button>
          </Card>
        ))}
      </div>
      <div className="flex-none px-5 pt-2 pb-4" style={{ borderTop: `1px solid ${C.border}` }}>
        {s.error && <div className="text-[12px] mb-2" style={{ color: C.danger }}>{s.error}</div>}
        <PrimaryButton label={s.busy ? "Finishing…" : "✓  Finish workout"} enabled={!s.busy} onClick={s.finish} />
      </div>
    </>
  );
}

function DonePhase({ s, onDone }: { s: ReturnType<typeof useSession>; onDone: () => void }) {
  return (
    <>
      <div className="px-4 pt-2">
        <div className="flex items-center gap-2 rounded-[10px] p-3" style={{ background: "color-mix(in oklch, oklch(0.66 0.13 150) 12%, transparent)" }}>
          <span className="text-[14px] font-bold" style={{ color: C.green }}>✓</span>
          <span className="text-[12px]" style={{ color: C.muted3 }}>Workout complete — logged to your history.</span>
        </div>
      </div>
      <div className="flex-1 overflow-y-auto px-[14px] pt-2 pb-2">
        {s.exercises.map((ex) => (
          <Card key={ex.exerciseId}>
            <div className="text-[13px] font-bold" style={{ color: C.ink }}>{ex.name}</div>
            <ColHeaders actions={false} />
            {ex.sets.map((set, i) => <ReadOnlyRow key={i} n={i + 1} reps={set.reps} weightKg={set.weightKg} />)}
          </Card>
        ))}
      </div>
      <div className="flex-none px-5 pt-2 pb-4" style={{ borderTop: `1px solid ${C.border}` }}>
        <PrimaryButton label="Done" enabled onClick={onDone} />
        <button onClick={s.edit} className="w-full text-center text-[13px] font-semibold py-[8px] mt-1" style={{ color: C.teal }}>Edit workout</button>
      </div>
    </>
  );
}

function Runner() {
  const params = useSearchParams();
  const router = useRouter();
  const templateId = params.get("templateId");
  const exerciseId = params.get("exerciseId");
  const name = params.get("name") ?? "Workout";
  const s = useSession(templateId ? Number(templateId) : null, exerciseId ? Number(exerciseId) : null, name);
  const goBack = () => router.push("/today");

  const sub = s.phase === "ready" ? "Ready" : s.phase === "active" ? "In progress" : s.phase === "done" ? "Completed" : "";
  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <Header title={s.workoutName} sub={sub} onBack={goBack} />
      {s.phase === "loading" && <div className="flex-1 flex items-center justify-center text-[13px]" style={{ color: C.muted2 }}>Loading…</div>}
      {s.phase === "ready" && <ReadyPhase s={s} />}
      {s.phase === "active" && <ActivePhase s={s} />}
      {s.phase === "done" && <DonePhase s={s} onDone={goBack} />}
    </div>
  );
}

export default function SessionPage() {
  return (
    <AuthGuard>
      <Suspense fallback={<div className="min-h-dvh flex items-center justify-center text-[13px]" style={{ color: C.muted2 }}>Loading…</div>}>
        <Runner />
      </Suspense>
    </AuthGuard>
  );
}
