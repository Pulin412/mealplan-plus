"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import {
  listExercises, createExercise, updateExercise, deleteExercise, type ExerciseDto,
} from "@/lib/api/exercises";
import {
  listWorkouts, createWorkout, updateWorkout, deleteWorkout,
  type WorkoutTemplateDto, type TemplateExerciseDto,
} from "@/lib/api/workouts";
import { listExerciseTags, createExerciseTag, listWorkoutTags, createWorkoutTag, type TagDto } from "@/lib/api/tags";
import { listWorkoutSessions, updateSession, deleteSession, type WorkoutSessionDto } from "@/lib/api/sessions";
import { isoOf } from "@/lib/api/plans";

export type LibTab = "exercises" | "workouts" | "logs";

/** Open exercise editor: create (id null) or edit (id set). */
export interface ExerciseEditor {
  id: number | null;
  name: string;
  description: string;
  tagIds: Set<number>;
}

/** One target set inside the builder: reps + optional weight (kg). */
export interface BuilderSet {
  reps: number | null;
  weightKg: number | null;
}

/** One exercise row inside the workout builder, with its ordered per-set targets. */
export interface BuilderItem {
  exerciseId: number;
  name: string;
  sets: BuilderSet[];
}

/** Open workout builder: create (id null) or edit (id set). */
export interface WorkoutBuilder {
  id: number | null;
  name: string;
  items: BuilderItem[];
  tagIds: Set<number>;
  pickerOpen: boolean;
  pickerSearch: string;
}

const newSet = (): BuilderSet => ({ reps: 10, weightKg: null });

export function useExercises() {
  const [tab, setTab]             = useState<LibTab>("exercises");
  const [exercises, setExercises] = useState<ExerciseDto[]>([]);
  const [workouts, setWorkouts]   = useState<WorkoutTemplateDto[]>([]);
  const [logs, setLogs]           = useState<WorkoutSessionDto[]>([]);
  const [tags, setTags]           = useState<TagDto[]>([]);
  const [workoutTags, setWorkoutTags] = useState<TagDto[]>([]);
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState<string | null>(null);
  const [exerciseTagFilter, setExerciseTagFilter] = useState<number | null>(null);
  const [workoutTagFilter, setWorkoutTagFilter]   = useState<number | null>(null);

  const [editor, setEditor]   = useState<ExerciseEditor | null>(null);
  const [builder, setBuilder] = useState<WorkoutBuilder | null>(null);
  const [openLog, setOpenLog] = useState<WorkoutSessionDto | null>(null);

  // Logs-tab calendar month (1-based), defaulting to the current month.
  const [logsMonth, setLogsMonth] = useState<{ year: number; month: number }>(() => {
    const d = new Date();
    return { year: d.getFullYear(), month: d.getMonth() + 1 };
  });

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ex, wk, sess, tg, wtg] = await Promise.all([listExercises(), listWorkouts(), listWorkoutSessions(), listExerciseTags(), listWorkoutTags()]);
      setExercises(ex);
      setWorkouts(wk);
      setLogs([...sess].sort((a, b) => (b.date ? isoOf(b.date) : "").localeCompare(a.date ? isoOf(a.date) : "")));
      setTags(tg);
      setWorkoutTags(wtg);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const tagName = useMemo(
    () => new Map(tags.map((t) => [t.id, t.name])),
    [tags],
  );

  const exerciseName = useMemo(
    () => new Map(exercises.filter((e) => e.id != null).map((e) => [e.id!, e.name])),
    [exercises],
  );

  /** Tags used by at least one exercise, alphabetical — the filter chips. */
  const filterTags = useMemo(
    () => tags.filter((t) => exercises.some((e) => (e.tagIds ?? []).includes(t.id)))
             .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())),
    [tags, exercises],
  );

  /** Exercises after the active tag filter (all when none selected). */
  const filteredExercises = useMemo(
    () => exerciseTagFilter == null
      ? exercises
      : exercises.filter((e) => (e.tagIds ?? []).includes(exerciseTagFilter)),
    [exercises, exerciseTagFilter],
  );

  const workoutTagName = useMemo(() => new Map(workoutTags.map((t) => [t.id, t.name])), [workoutTags]);

  /** WORKOUT tags used by at least one template, alphabetical — the Workouts filter chips. */
  const workoutFilterTags = useMemo(
    () => workoutTags.filter((t) => workouts.some((w) => (w.tagIds ?? []).includes(t.id)))
             .sort((a, b) => a.name.toLowerCase().localeCompare(b.name.toLowerCase())),
    [workoutTags, workouts],
  );

  const filteredWorkouts = useMemo(
    () => workoutTagFilter == null
      ? workouts
      : workouts.filter((w) => (w.tagIds ?? []).includes(workoutTagFilter)),
    [workouts, workoutTagFilter],
  );

  // ── Exercise editor ──────────────────────────────────────────────────────────
  const openNewExercise = useCallback(() => {
    setEditor({ id: null, name: "", description: "", tagIds: new Set() });
  }, []);
  const openEditExercise = useCallback((e: ExerciseDto) => {
    setEditor({
      id: e.id ?? null, name: e.name, description: e.description ?? "",
      tagIds: new Set(e.tagIds ?? []),
    });
  }, []);
  const closeEditor = useCallback(() => setEditor(null), []);
  const setEditorName = useCallback((name: string) => setEditor((ed) => ed && { ...ed, name }), []);
  const setEditorDescription = useCallback((description: string) => setEditor((ed) => ed && { ...ed, description }), []);
  const toggleEditorTag = useCallback((tagId: number) => {
    setEditor((ed) => {
      if (!ed) return ed;
      const next = new Set(ed.tagIds);
      if (next.has(tagId)) next.delete(tagId); else next.add(tagId);
      return { ...ed, tagIds: next };
    });
  }, []);
  /** Create a new EXERCISE tag and immediately assign it to the open editor. */
  const createEditorTag = useCallback(async (name: string) => {
    const n = name.trim();
    if (!n) return;
    try {
      const t = await createExerciseTag(n);
      setTags((prev) => (prev.some((x) => x.id === t.id) ? prev : [...prev, t]));
      setEditor((ed) => ed && { ...ed, tagIds: new Set(ed.tagIds).add(t.id) });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to create tag");
    }
  }, []);

  const saveExercise = useCallback(async () => {
    if (!editor || !editor.name.trim()) return;
    const desc = editor.description.trim() || null;
    const tagIds = Array.from(editor.tagIds);
    try {
      if (editor.id == null) await createExercise(editor.name, desc, tagIds);
      else await updateExercise(editor.id, editor.name, desc, tagIds);
      setEditor(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to save exercise");
    }
  }, [editor, load]);

  const removeExercise = useCallback(async (id: number) => {
    try {
      await deleteExercise(id);
      setEditor(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to delete exercise");
    }
  }, [load]);

  // ── Workout builder ──────────────────────────────────────────────────────────
  const openNewWorkout = useCallback(() => {
    setBuilder({ id: null, name: "", items: [], tagIds: new Set(), pickerOpen: false, pickerSearch: "" });
  }, []);
  const openEditWorkout = useCallback((w: WorkoutTemplateDto) => {
    setBuilder({
      id: w.id ?? null, name: w.name, pickerOpen: false, pickerSearch: "",
      tagIds: new Set(w.tagIds ?? []),
      items: (w.exercises ?? []).map((te) => {
        const sets = [...(te.sets ?? [])]
          .sort((a, b) => a.setNumber - b.setNumber)
          .map((s) => ({ reps: s.reps ?? null, weightKg: s.weightKg ?? null }));
        return {
          exerciseId: te.exerciseId,
          name: te.exerciseName ?? "Exercise",
          sets: sets.length ? sets : [newSet()],
        };
      }),
    });
  }, []);
  const closeBuilder = useCallback(() => setBuilder(null), []);
  const setBuilderName = useCallback((name: string) => setBuilder((b) => b && { ...b, name }), []);
  const openPicker = useCallback(() => setBuilder((b) => b && { ...b, pickerOpen: true, pickerSearch: "" }), []);
  const closePicker = useCallback(() => setBuilder((b) => b && { ...b, pickerOpen: false }), []);
  const setPickerSearch = useCallback((pickerSearch: string) => setBuilder((b) => b && { ...b, pickerSearch }), []);

  const addToBuilder = useCallback((e: ExerciseDto) => {
    setBuilder((b) => {
      if (!b || e.id == null) return b;
      if (b.items.some((it) => it.exerciseId === e.id)) return b;
      return { ...b, pickerOpen: false, items: [...b.items, { exerciseId: e.id, name: e.name, sets: [newSet()] }] };
    });
  }, []);
  const removeFromBuilder = useCallback((exerciseId: number) => {
    setBuilder((b) => b && { ...b, items: b.items.filter((it) => it.exerciseId !== exerciseId) });
  }, []);
  const toggleBuilderTag = useCallback((tagId: number) => {
    setBuilder((b) => {
      if (!b) return b;
      const next = new Set(b.tagIds);
      if (next.has(tagId)) next.delete(tagId); else next.add(tagId);
      return { ...b, tagIds: next };
    });
  }, []);
  /** Create a new WORKOUT tag and immediately assign it to the open builder. */
  const createBuilderTag = useCallback(async (name: string) => {
    const n = name.trim();
    if (!n) return;
    try {
      const t = await createWorkoutTag(n);
      setWorkoutTags((prev) => (prev.some((x) => x.id === t.id) ? prev : [...prev, t]));
      setBuilder((b) => b && { ...b, tagIds: new Set(b.tagIds).add(t.id) });
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to create tag");
    }
  }, []);

  // ── Per-set editing ──────────────────────────────────────────────────────────
  const updateItem = useCallback((exerciseId: number, f: (it: BuilderItem) => BuilderItem) => {
    setBuilder((b) => b && { ...b, items: b.items.map((it) => (it.exerciseId === exerciseId ? f(it) : it)) });
  }, []);
  const updateSet = useCallback((exerciseId: number, index: number, f: (s: BuilderSet) => BuilderSet) => {
    updateItem(exerciseId, (it) => ({ ...it, sets: it.sets.map((s, i) => (i === index ? f(s) : s)) }));
  }, [updateItem]);

  /** Duplicate a specific set (reps + weight), inserting the copy right after it. */
  const duplicateSet = useCallback((exerciseId: number, index: number) => {
    updateItem(exerciseId, (it) => {
      const s = it.sets[index];
      if (!s) return it;
      const sets = [...it.sets];
      sets.splice(index + 1, 0, { ...s });
      return { ...it, sets };
    });
  }, [updateItem]);
  const removeSet = useCallback((exerciseId: number, index: number) => {
    updateItem(exerciseId, (it) => (it.sets.length <= 1 ? it : { ...it, sets: it.sets.filter((_, i) => i !== index) }));
  }, [updateItem]);
  const setReps = useCallback((exerciseId: number, index: number, reps: number | null) => {
    updateSet(exerciseId, index, (s) => ({ ...s, reps: reps == null ? null : Math.min(100, Math.max(1, reps)) }));
  }, [updateSet]);
  const setWeight = useCallback((exerciseId: number, index: number, weightKg: number | null) => {
    updateSet(exerciseId, index, (s) => ({ ...s, weightKg: weightKg == null ? null : Math.max(0, weightKg) }));
  }, [updateSet]);

  /** Library exercises not already in the builder, filtered by the picker search. */
  const pickerCandidates = useMemo(() => {
    if (!builder) return [];
    const chosen = new Set(builder.items.map((it) => it.exerciseId));
    const q = builder.pickerSearch.trim().toLowerCase();
    return exercises.filter((e) =>
      (e.id == null || !chosen.has(e.id)) &&
      (q === "" ||
        e.name.toLowerCase().includes(q) ||
        (e.tagIds ?? []).some((id) => tagName.get(id)?.toLowerCase().includes(q))),
    );
  }, [builder, exercises, tagName]);

  const canSaveWorkout = !!builder && builder.name.trim() !== "" && builder.items.length > 0;

  const saveWorkout = useCallback(async () => {
    if (!builder || builder.name.trim() === "" || builder.items.length === 0) return;
    const entries: TemplateExerciseDto[] = builder.items.map((item, orderIndex) => ({
      exerciseId: item.exerciseId,
      orderIndex,
      sets: item.sets.map((s, i) => ({ setNumber: i, reps: s.reps, weightKg: s.weightKg })),
    }));
    try {
      const tagIds = Array.from(builder.tagIds);
      if (builder.id == null) await createWorkout(builder.name, entries, tagIds);
      else await updateWorkout(builder.id, builder.name, entries, tagIds);
      setBuilder(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to save workout");
    }
  }, [builder, load]);

  const removeWorkout = useCallback(async (id: number) => {
    try {
      await deleteWorkout(id);
      setBuilder(null);
      await load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to delete workout");
    }
  }, [load]);

  // ── Logs (read-only) ──────────────────────────────────────────────────────────
  const openLogDetail = useCallback((s: WorkoutSessionDto) => setOpenLog(s), []);
  const closeLogDetail = useCallback(() => setOpenLog(null), []);

  /** Save edits (reps/weight) to a logged session, then refresh. */
  const updateLog = useCallback(async (session: WorkoutSessionDto) => {
    if (session.id == null) return;
    try { await updateSession(session.id, session); setOpenLog(session); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed to save"); }
  }, [load]);

  /** Delete a single logged session, then refresh. */
  const deleteLog = useCallback(async (id: number) => {
    try { await deleteSession(id); setOpenLog(null); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed to delete"); }
  }, [load]);

  /** Delete every logged session. */
  const clearAllLogs = useCallback(async () => {
    try { await Promise.all(logs.map((s) => (s.id != null ? deleteSession(s.id) : Promise.resolve()))); setOpenLog(null); await load(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed to clear"); }
  }, [logs, load]);
  const prevLogsMonth = useCallback(() => setLogsMonth((m) => (m.month === 1 ? { year: m.year - 1, month: 12 } : { ...m, month: m.month - 1 })), []);
  const nextLogsMonth = useCallback(() => setLogsMonth((m) => (m.month === 12 ? { year: m.year + 1, month: 1 } : { ...m, month: m.month + 1 })), []);

  const todayIso = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
  }, []);

  /** Sessions grouped by their logged ISO date. */
  const logsByDate = useMemo(() => {
    const m = new Map<string, WorkoutSessionDto[]>();
    for (const s of logs) {
      if (s.date == null) continue;
      const k = isoOf(s.date);
      const arr = m.get(k);
      if (arr) arr.push(s); else m.set(k, [s]);
    }
    return m;
  }, [logs]);

  return {
    tab, setTab,
    exercises, workouts, logs, tags, tagName, exerciseName, loading, error,
    filterTags, filteredExercises, exerciseTagFilter, setExerciseTagFilter,
    workoutTags, workoutTagName, workoutFilterTags, filteredWorkouts, workoutTagFilter, setWorkoutTagFilter,
    openLog, openLogDetail, closeLogDetail, updateLog, deleteLog, clearAllLogs,
    logsMonth, prevLogsMonth, nextLogsMonth, todayIso, logsByDate,
    // exercise editor
    editor, openNewExercise, openEditExercise, closeEditor,
    setEditorName, setEditorDescription, toggleEditorTag, createEditorTag, saveExercise, removeExercise,
    // workout builder
    builder, openNewWorkout, openEditWorkout, closeBuilder, setBuilderName,
    openPicker, closePicker, setPickerSearch, addToBuilder, removeFromBuilder,
    toggleBuilderTag, createBuilderTag,
    duplicateSet, removeSet, setReps, setWeight,
    pickerCandidates, canSaveWorkout, saveWorkout, removeWorkout,
  };
}
