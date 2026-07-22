"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import {
  listExercises, createExercise, updateExercise, deleteExercise, type ExerciseDto,
} from "@/lib/api/exercises";
import {
  listWorkouts, createWorkout, updateWorkout, deleteWorkout,
  type WorkoutTemplateDto, type TemplateExerciseDto,
} from "@/lib/api/workouts";
import { listExerciseTags, type TagDto } from "@/lib/api/tags";
import { listWorkoutSessions, type WorkoutSessionDto } from "@/lib/api/sessions";
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
  const [loading, setLoading]     = useState(true);
  const [error, setError]         = useState<string | null>(null);

  const [editor, setEditor]   = useState<ExerciseEditor | null>(null);
  const [builder, setBuilder] = useState<WorkoutBuilder | null>(null);
  const [openLog, setOpenLog] = useState<WorkoutSessionDto | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [ex, wk, sess, tg] = await Promise.all([listExercises(), listWorkouts(), listWorkoutSessions(), listExerciseTags()]);
      setExercises(ex);
      setWorkouts(wk);
      setLogs([...sess].sort((a, b) => (b.date ? isoOf(b.date) : "").localeCompare(a.date ? isoOf(a.date) : "")));
      setTags(tg);
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
    setBuilder({ id: null, name: "", items: [], pickerOpen: false, pickerSearch: "" });
  }, []);
  const openEditWorkout = useCallback((w: WorkoutTemplateDto) => {
    setBuilder({
      id: w.id ?? null, name: w.name, pickerOpen: false, pickerSearch: "",
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
      if (builder.id == null) await createWorkout(builder.name, entries);
      else await updateWorkout(builder.id, builder.name, entries);
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

  return {
    tab, setTab,
    exercises, workouts, logs, tags, tagName, exerciseName, loading, error,
    openLog, openLogDetail, closeLogDetail,
    // exercise editor
    editor, openNewExercise, openEditExercise, closeEditor,
    setEditorName, setEditorDescription, toggleEditorTag, saveExercise, removeExercise,
    // workout builder
    builder, openNewWorkout, openEditWorkout, closeBuilder, setBuilderName,
    openPicker, closePicker, setPickerSearch, addToBuilder, removeFromBuilder,
    duplicateSet, removeSet, setReps, setWeight,
    pickerCandidates, canSaveWorkout, saveWorkout, removeWorkout,
  };
}
