"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { getPlan, addPlannedWorkout, removePlannedWorkout } from "@/lib/api/plans";
import { listWorkouts, type WorkoutTemplateDto } from "@/lib/api/workouts";
import { listExercises, type ExerciseDto } from "@/lib/api/exercises";
import { listSessionsForDate, deleteSession, type WorkoutSessionDto } from "@/lib/api/sessions";

export type WorkoutStatus = "planned" | "in_progress" | "done";
export interface HomeWorkout { templateId: number | null; exerciseId: number | null; name: string; status: WorkoutStatus; plannedId: number | null }

const todayIso = (): string => {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};

const statusOf = (s: WorkoutSessionDto | undefined): WorkoutStatus =>
  s == null ? "planned" : s.isCompleted === true ? "done" : "in_progress";

export function useTodayWorkouts() {
  const today = useMemo(todayIso, []);
  const [workouts, setWorkouts] = useState<HomeWorkout[]>([]);
  const [templates, setTemplates] = useState<WorkoutTemplateDto[]>([]);
  const [exercises, setExercises] = useState<ExerciseDto[]>([]);

  const reload = useCallback(async () => {
    const [plan, sessions] = await Promise.all([getPlan(today), listSessionsForDate(today).catch(() => [])]);
    const planned = plan?.plannedWorkouts ?? [];
    const plannedRows: HomeWorkout[] = planned.map((pw) => {
      const s = sessions.find((x) => x.name === pw.activityName);
      return { templateId: pw.workoutTemplateId ?? null, exerciseId: s?.sets?.[0]?.exerciseId ?? null, name: pw.activityName, status: statusOf(s), plannedId: pw.id ?? null };
    });
    const plannedNames = new Set(planned.map((p) => p.activityName));
    const adHocRows: HomeWorkout[] = sessions.filter((s) => !plannedNames.has(s.name)).map((s) => ({
      templateId: null, exerciseId: s.sets?.[0]?.exerciseId ?? null, name: s.name, status: statusOf(s), plannedId: null,
    }));
    setWorkouts([...plannedRows, ...adHocRows]);
  }, [today]);

  useEffect(() => {
    void reload();
    listWorkouts().then(setTemplates).catch(() => {});
    listExercises().then(setExercises).catch(() => {});
  }, [reload]);

  const addWorkout = useCallback(async (template: WorkoutTemplateDto) => {
    if (template.id == null) return;
    await addPlannedWorkout(today, template.id, template.name).catch(() => {});
    await reload();
  }, [today, reload]);

  // Remove a planned/in-progress workout: drop its plan entry (leaves the Plan too) and delete its
  // started-but-unfinished session. Completed logs are kept.
  const removeWorkout = useCallback(async (hw: HomeWorkout) => {
    if (hw.plannedId != null) await removePlannedWorkout(today, hw.plannedId).catch(() => {});
    const sessions = await listSessionsForDate(today).catch(() => []);
    const s = sessions.find((x) => x.name === hw.name && x.isCompleted !== true);
    if (s?.id != null) await deleteSession(s.id).catch(() => {});
    await reload();
  }, [today, reload]);

  return { workouts, templates, exercises, addWorkout, removeWorkout, reload };
}
