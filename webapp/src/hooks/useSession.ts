"use client";

import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { getWorkout, type WorkoutTemplateDto } from "@/lib/api/workouts";
import { listExercises } from "@/lib/api/exercises";
import {
  listSessionsForDate, startWorkout, createSession, updateSession, finishSession, lastForExercise,
  type WorkoutSessionDto, type WorkoutSetDto,
} from "@/lib/api/sessions";

export type RunPhase = "loading" | "ready" | "active" | "done";
export interface RunSet { reps: number | null; weightKg: number | null }
export interface LibExercise { id: number; name: string; description: string | null }
export interface RunExercise {
  exerciseId: number;
  name: string;
  description: string | null;
  sets: RunSet[];
  templateSets: RunSet[];
  lastTime: RunSet[];
}

const todayIso = (): string => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

export function useSession(templateId: number | null, exerciseId: number | null, name: string) {
  const today = useMemo(todayIso, []);
  const [phase, setPhase] = useState<RunPhase>("loading");
  const [sessionId, setSessionId] = useState<number | null>(null);
  const [exercises, setExercises] = useState<RunExercise[]>([]);
  const [library, setLibrary] = useState<LibExercise[]>([]);   // for the "Add exercise" picker
  const [doneIds, setDoneIds] = useState<Set<number>>(new Set());   // exercises checked off this session (persisted locally)
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // "Done" checks persist per session (survive leaving/returning to the runner) via localStorage.
  const loadDone = (id: number | null) => {
    if (id == null) { setDoneIds(new Set()); return; }
    try {
      const raw = typeof window !== "undefined" ? window.localStorage.getItem(`workoutDone:${id}`) : null;
      setDoneIds(new Set(raw ? (JSON.parse(raw) as number[]) : []));
    } catch { setDoneIds(new Set()); }
  };
  const toggleDone = (exId: number) => {
    setDoneIds((prev) => {
      const next = new Set(prev);
      if (next.has(exId)) next.delete(exId); else next.add(exId);
      try { if (sessionId != null && typeof window !== "undefined") window.localStorage.setItem(`workoutDone:${sessionId}`, JSON.stringify(Array.from(next))); } catch { /* ignore */ }
      return next;
    });
  };

  const templateRef = useRef<WorkoutTemplateDto | null>(null);
  const descRef = useRef<Map<number, string | null>>(new Map());
  const libNameRef = useRef<Map<number, string>>(new Map());

  const exercisesFromTemplate = useCallback((): RunExercise[] => {
    const t = templateRef.current;
    return [...(t?.exercises ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)).map((te) => {
      const sets = [...(te.sets ?? [])].sort((a, b) => a.setNumber - b.setNumber).map((s) => ({ reps: s.reps ?? null, weightKg: s.weightKg ?? null }));
      return { exerciseId: te.exerciseId, name: te.exerciseName ?? libNameRef.current.get(te.exerciseId) ?? "Exercise", description: descRef.current.get(te.exerciseId) ?? null, sets, templateSets: sets, lastTime: [] };
    });
  }, []);

  const exerciseReadyList = useCallback((): RunExercise[] => {
    if (exerciseId == null) return [];
    const sets: RunSet[] = [0, 1, 2].map(() => ({ reps: 10, weightKg: null }));
    return [{ exerciseId, name: libNameRef.current.get(exerciseId) ?? name, description: descRef.current.get(exerciseId) ?? null, sets, templateSets: sets, lastTime: [] }];
  }, [exerciseId, name]);

  const exercisesFromSession = useCallback((session: WorkoutSessionDto): RunExercise[] => {
    const order = [...(templateRef.current?.exercises ?? [])].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0)).map((te) => te.exerciseId);
    const grouped = new Map<number, WorkoutSetDto[]>();
    (session.sets ?? []).forEach((s) => { if (!grouped.has(s.exerciseId)) grouped.set(s.exerciseId, []); grouped.get(s.exerciseId)!.push(s); });
    const ids = Array.from(new Set([...order, ...Array.from(grouped.keys())])).filter((id) => grouped.has(id));
    return ids.map((id) => ({
      exerciseId: id,
      name: libNameRef.current.get(id) ?? "Exercise",
      description: descRef.current.get(id) ?? null,
      sets: grouped.get(id)!.slice().sort((a, b) => a.setNumber - b.setNumber).map((s) => ({ reps: s.reps ?? null, weightKg: s.weightKg ?? null })),
      templateSets: [],
      lastTime: [],
    }));
  }, []);

  const loadLastTimes = useCallback(async (list: RunExercise[]) => {
    // Scope "last time" to this same workout (by name) — the last time you did *this* workout.
    const entries = await Promise.all(list.map(async (e) => [e.exerciseId, await lastForExercise(e.exerciseId, name)] as const));
    const map = new Map(entries.map(([id, sets]) => [id, sets.map((s) => ({ reps: s.reps ?? null, weightKg: s.weightKg ?? null }))]));
    setExercises((prev) => prev.map((e) => ({ ...e, lastTime: map.get(e.exerciseId) ?? e.lastTime })));
  }, [name]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      const [tpl, lib] = await Promise.all([
        templateId != null ? getWorkout(templateId).catch(() => null) : Promise.resolve(null),
        listExercises().catch(() => []),
      ]);
      if (cancelled) return;
      templateRef.current = tpl;
      descRef.current = new Map(lib.filter((e) => e.id != null).map((e) => [e.id!, e.description ?? null]));
      libNameRef.current = new Map(lib.filter((e) => e.id != null).map((e) => [e.id!, e.name]));
      setLibrary(lib.filter((e) => e.id != null).map((e) => ({ id: e.id!, name: e.name, description: e.description ?? null })));

      const sessions = await listSessionsForDate(today).catch(() => []);
      if (cancelled) return;
      const existing = sessions.find((s) => s.name === name);
      if (existing) {
        const list = exercisesFromSession(existing);
        setExercises(list);
        setSessionId(existing.id ?? null);
        const active = existing.isCompleted !== true;
        setPhase(active ? "active" : "done");
        loadDone(active ? (existing.id ?? null) : null);
        if (active) void loadLastTimes(list);
      } else {
        const list = tpl ? exercisesFromTemplate() : exerciseReadyList();
        setExercises(list);
        setPhase("ready");
        void loadLastTimes(list);
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [templateId, exerciseId, name, today]);

  // ── persistence ─────────────────────────────────────────────────────────────
  const setsPayload = (list: RunExercise[]): WorkoutSetDto[] =>
    list.flatMap((ex) => ex.sets.map((s, i) => ({ exerciseId: ex.exerciseId, setNumber: i, reps: s.reps, weightKg: s.weightKg })));

  const persist = useCallback((list: RunExercise[]) => {
    if (sessionId == null) return;
    void updateSession(sessionId, { name, date: today, isCompleted: false, sets: setsPayload(list), exerciseNotes: [] }).catch(() => {});
  }, [sessionId, name, today]);

  const mutate = useCallback((f: (list: RunExercise[]) => RunExercise[]) => {
    setExercises((prev) => { const next = f(prev); persist(next); return next; });
  }, [persist]);

  const editSet = (exId: number, index: number, f: (s: RunSet) => RunSet) =>
    mutate((list) => list.map((ex) => ex.exerciseId === exId ? { ...ex, sets: ex.sets.map((s, i) => i === index ? f(s) : s) } : ex));

  const setReps = (exId: number, index: number, reps: number | null) =>
    editSet(exId, index, (s) => ({ ...s, reps: reps == null ? null : Math.min(100, Math.max(0, reps)) }));
  const setWeight = (exId: number, index: number, weightKg: number | null) =>
    editSet(exId, index, (s) => ({ ...s, weightKg: weightKg == null ? null : Math.max(0, weightKg) }));
  const addSet = (exId: number) =>
    mutate((list) => list.map((ex) => ex.exerciseId === exId ? { ...ex, sets: [...ex.sets, { ...(ex.sets[ex.sets.length - 1] ?? { reps: 10, weightKg: null }) }] } : ex));
  const removeSet = (exId: number, index: number) =>
    mutate((list) => list.map((ex) => ex.exerciseId === exId && ex.sets.length > 1 ? { ...ex, sets: ex.sets.filter((_, i) => i !== index) } : ex));
  const copyLast = (exId: number) =>
    mutate((list) => list.map((ex) => ex.exerciseId === exId && ex.lastTime.length ? { ...ex, sets: ex.lastTime.map((s) => ({ ...s })) } : ex));

  // Add a library exercise to THIS session's log on the fly (default 3 × 10). Persisted to the
  // session only — the workout template is never touched. No-op if already present.
  const addExercise = (exId: number) => {
    const lib = library.find((l) => l.id === exId);
    if (!lib) return;
    mutate((list) => list.some((e) => e.exerciseId === exId) ? list : [
      ...list,
      { exerciseId: exId, name: lib.name, description: lib.description, sets: [0, 1, 2].map(() => ({ reps: 10, weightKg: null })), templateSets: [], lastTime: [] },
    ]);
    void lastForExercise(exId, name).then((sets) => {
      if (!sets.length) return;
      setExercises((prev) => prev.map((e) => e.exerciseId === exId
        ? { ...e, lastTime: sets.map((s) => ({ reps: s.reps ?? null, weightKg: s.weightKg ?? null })) } : e));
    }).catch(() => {});
  };

  // ── phase transitions ────────────────────────────────────────────────────────
  const start = useCallback(async () => {
    setBusy(true); setError(null);
    try {
      const session = templateRef.current
        ? await startWorkout(templateId!)
        : await createSession(name, today, setsPayload(exercises));
      const prevLast = new Map(exercises.map((e) => [e.exerciseId, e.lastTime]));
      const list = exercisesFromSession(session).map((e) => ({ ...e, lastTime: prevLast.get(e.exerciseId) ?? [] }));
      setSessionId(session.id ?? null);
      setExercises(list);
      loadDone(session.id ?? null);
      setPhase("active");
    } catch (e) { setError(e instanceof Error ? e.message : "Failed to start"); }
    finally { setBusy(false); }
  }, [templateId, name, today, exercises, exercisesFromSession]);

  const finish = useCallback(async () => {
    if (sessionId == null) return;
    setBusy(true); setError(null);
    try {
      await updateSession(sessionId, { name, date: today, isCompleted: false, sets: setsPayload(exercises), exerciseNotes: [] });
      await finishSession(sessionId);
      setPhase("done");
    } catch (e) { setError(e instanceof Error ? e.message : "Failed to finish"); }
    finally { setBusy(false); }
  }, [sessionId, name, today, exercises]);

  const edit = useCallback(() => setPhase("active"), []);

  return {
    phase, workoutName: name, exercises, library, doneIds, busy, error,
    start, finish, edit,
    setReps, setWeight, addSet, removeSet, copyLast, addExercise, toggleDone,
  };
}
