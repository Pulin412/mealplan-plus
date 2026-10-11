"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { friendlyMessage } from "@/lib/api/errors";
import { listPlans, getCompletedDays, isoOf, type DayPlanDto } from "@/lib/api/plans";
import { listDiets, type DietDto } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import { listWorkoutSessions, type WorkoutSessionDto, type WorkoutSetDto } from "@/lib/api/sessions";
import { listExercises } from "@/lib/api/exercises";
import { listJournal, upsertJournal, deleteJournal } from "@/lib/api/journal";
import { foodMacros, unitLabel, num, MEAL_SLOTS } from "@/lib/nutrition";

export type Status = "done" | "missed" | "pending" | "none";

export interface JournalLogExercise { name: string; sets: string[] }
export interface JournalWorkout {
  key: string;
  name: string;
  templateId: number | null;
  status: "done" | "active" | "todo";
  /** Read-only logged sets, grouped by exercise — populated only for a completed session. */
  log: JournalLogExercise[];
}

/** One food line within a planned meal. */
export interface JournalFood { name: string; meta: string }
/** A meal planned for the day (from the day's diet and/or loose planned meals). */
export interface JournalMeal { slot: string; name: string; kcal: number; items: JournalFood[] }

/** One day in the diary feed. */
export interface DayEntry {
  date: string;              // ISO yyyy-mm-dd
  dietName: string | null;
  dietStatus: Status;
  meals: JournalMeal[];      // what's planned for the day (shown on tapping the diet)
  workouts: JournalWorkout[];
  workoutStatus: Status;
  note: string;
}

const iso = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
const isoOfDate = (d: Date) => iso(d.getFullYear(), d.getMonth() + 1, d.getDate());
const addDays = (isoStr: string, n: number) => { const [y, m, d] = isoStr.split("-").map(Number); return isoOfDate(new Date(y, m - 1, d + n)); };
const startOfWeek = (isoStr: string) => { const [y, m, d] = isoStr.split("-").map(Number); const dt = new Date(y, m - 1, d); const dow = (dt.getDay() + 6) % 7; return addDays(isoStr, -dow); }; // Monday
const startOfMonth = (isoStr: string) => { const [y, m] = isoStr.split("-").map(Number); return iso(y, m, 1); };

export function useJournal() {
  const today = useMemo(() => new Date(), []);
  const todayIso = isoOfDate(today);

  // Feed runs from `fromDate` (bottom) to `endDate` (top). Default window = the current week;
  // "load older" expands to the start of the month, then back a month at a time.
  const [endDate, setEndDate] = useState<string>(todayIso);
  const [fromDate, setFromDate] = useState<string>(startOfWeek(todayIso));

  const [plansByDate, setPlansByDate] = useState<Record<string, DayPlanDto>>({});
  const [completedDays, setCompletedDays] = useState<Set<string>>(new Set());
  const [dietsById, setDietsById] = useState<Map<number, DietDto>>(new Map());
  const [mealsById, setMealsById] = useState<Map<number, { name: string; kcal: number; items: JournalFood[] }>>(new Map());
  const [sessionsByDate, setSessionsByDate] = useState<Map<string, WorkoutSessionDto[]>>(new Map());
  const [exNameById, setExNameById] = useState<Map<number, string>>(new Map());
  const [notes, setNotes] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // One-time library load: diet names, meal names + kcal, and all workout sessions grouped by date.
  useEffect(() => {
    Promise.all([listDiets(), listMeals(), listFoods()]).then(([ds, ms, fs]) => {
      const dm = new Map<number, DietDto>(); ds.forEach((d) => d.id != null && dm.set(d.id, d));
      setDietsById(dm);
      const foodsById = new Map<number, FoodDto>(); fs.forEach((f) => f.id != null && foodsById.set(f.id, f));
      const mm = new Map<number, { name: string; kcal: number; items: JournalFood[] }>();
      ms.forEach((m: MealDto) => {
        if (m.id == null) return;
        let kcal = 0;
        const items: JournalFood[] = (m.items ?? []).map((it) => {
          const food = it.foodId != null ? foodsById.get(it.foodId) : undefined;
          kcal += foodMacros(food, it.quantity, it.unit).kcal;
          return { name: food?.name ?? "Food", meta: `${num(it.quantity)} ${unitLabel(it.unit)}` };
        });
        mm.set(m.id, { name: m.name, kcal: Math.round(kcal), items });
      });
      setMealsById(mm);
    }).catch(() => {});
    listWorkoutSessions().then((ss) => {
      const m = new Map<string, WorkoutSessionDto[]>();
      ss.forEach((s) => { const k = isoOf(s.date); if (!k) return; const arr = m.get(k) ?? []; arr.push(s); m.set(k, arr); });
      setSessionsByDate(m);
    }).catch(() => {});
    listExercises().then((xs) => { const m = new Map<number, string>(); xs.forEach((x) => x.id != null && m.set(x.id, x.name)); setExNameById(m); }).catch(() => {});
  }, []);

  // Plans + completed-day flags for the visible window (two requests, any window size).
  const loadWindow = useCallback(async (from: string, end: string) => {
    setLoading(true);
    try {
      const [plans, completed, journal] = await Promise.all([listPlans(from, end), getCompletedDays(from, end), listJournal(from, end)]);
      const byDate: Record<string, DayPlanDto> = {};
      plans.forEach((p) => { byDate[isoOf(p.date)] = p; });
      setPlansByDate(byDate);
      setCompletedDays(new Set(completed.map((d) => isoOf(d))));
      // Merge fetched notes for this window, keeping any from other windows already loaded.
      setNotes((prev) => { const next = { ...prev }; journal.forEach((j) => { if (j.date) next[String(j.date)] = j.note; }); return next; });
    } catch (e) { setError(friendlyMessage(e)); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { loadWindow(fromDate, endDate); }, [fromDate, endDate, loadWindow]);

  const dietStatusFor = useCallback((date: string, plan: DayPlanDto | undefined): Status => {
    const hasDiet = plan?.dietId != null || (plan?.plannedMeals?.length ?? 0) > 0;
    if (!hasDiet) return "none";
    if (completedDays.has(date)) return "done";
    return date < todayIso ? "missed" : "pending";
  }, [completedDays, todayIso]);

  const mealsFor = useCallback((plan: DayPlanDto | undefined): JournalMeal[] => {
    if (!plan) return [];
    const rows: JournalMeal[] = [];
    if (plan.dietId != null) {
      (dietsById.get(plan.dietId)?.meals ?? []).forEach((dm) => {
        const meal = dm.mealId != null ? mealsById.get(dm.mealId) : undefined;
        rows.push({ slot: dm.slot, name: meal?.name ?? "Meal", kcal: meal?.kcal ?? 0, items: meal?.items ?? [] });
      });
    }
    (plan.plannedMeals ?? []).forEach((pm) => {
      const meal = mealsById.get(pm.mealId);
      rows.push({ slot: pm.slot, name: meal?.name ?? "Meal", kcal: meal?.kcal ?? 0, items: meal?.items ?? [] });
    });
    return rows.sort((a, b) => {
      const ia = MEAL_SLOTS.indexOf(a.slot), ib = MEAL_SLOTS.indexOf(b.slot);
      return (ia < 0 ? 99 : ia) - (ib < 0 ? 99 : ib);
    });
  }, [dietsById, mealsById]);

  const setLabel = (s: WorkoutSetDto): string => {
    const km = s.distanceMeters && s.distanceMeters > 0 ? ` · ${(s.distanceMeters / 1000).toFixed(1)} km` : "";
    if (s.durationSeconds && s.durationSeconds > 0) return `${Math.floor(s.durationSeconds / 60)}:${String(s.durationSeconds % 60).padStart(2, "0")}${km}`;
    if (s.reps != null || s.weightKg != null) {
      const r = s.reps != null ? String(s.reps) : "–";
      return s.weightKg && s.weightKg > 0 ? `${r} × ${num(s.weightKg)} kg` : `${r} reps`;
    }
    return km ? km.replace(" · ", "") : "–";
  };

  const logFor = useCallback((s: WorkoutSessionDto | undefined, status: string): JournalLogExercise[] => {
    if (!s || status !== "done") return [];
    const byEx = new Map<number, WorkoutSetDto[]>();
    (s.sets ?? []).forEach((st) => { const a = byEx.get(st.exerciseId) ?? []; a.push(st); byEx.set(st.exerciseId, a); });
    return Array.from(byEx.entries()).map(([exId, sets]) => ({
      name: exNameById.get(exId) ?? "Exercise",
      sets: [...sets].sort((a, b) => a.setNumber - b.setNumber).map(setLabel),
    }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [exNameById]);

  const workoutsFor = useCallback((date: string, plan: DayPlanDto | undefined): JournalWorkout[] => {
    const planned = plan?.plannedWorkouts ?? [];
    const sess = sessionsByDate.get(date) ?? [];
    const byName = new Map<string, WorkoutSessionDto>();
    sess.forEach((s) => { const cur = byName.get(s.name); if (!cur || (s.isCompleted && !cur.isCompleted)) byName.set(s.name, s); });
    const out: JournalWorkout[] = planned.map((w) => {
      const s = byName.get(w.activityName);
      byName.delete(w.activityName);
      const status: JournalWorkout["status"] = s ? (s.isCompleted ? "done" : "active") : "todo";
      return { key: `p${w.id ?? w.activityName}`, name: w.activityName, templateId: w.workoutTemplateId ?? null, status, log: logFor(s, status) };
    });
    byName.forEach((s, name) => { const status: JournalWorkout["status"] = s.isCompleted ? "done" : "active"; out.push({ key: `s${s.id ?? name}`, name, templateId: null, status, log: logFor(s, status) }); });
    return out;
  }, [sessionsByDate, logFor]);

  const workoutStatusFor = useCallback((date: string, ws: JournalWorkout[]): Status => {
    if (ws.length === 0) return "none";
    if (ws.some((w) => w.status === "done")) return "done";
    return date < todayIso ? "missed" : "pending";
  }, [todayIso]);

  // Feed: from endDate back to fromDate, keeping days with content / a note (+ always today & anchor).
  const entries: DayEntry[] = useMemo(() => {
    const out: DayEntry[] = [];
    let date = endDate;
    while (date >= fromDate) {
      const plan = plansByDate[date];
      const note = notes[date] ?? "";
      const workouts = workoutsFor(date, plan);
      const dietStatus = dietStatusFor(date, plan);
      const hasContent = dietStatus !== "none" || workouts.length > 0 || note.trim() !== "";
      if (hasContent || date === todayIso || date === endDate) {
        out.push({
          date,
          dietName: plan?.dietId != null ? dietsById.get(plan.dietId)?.name ?? null : null,
          dietStatus,
          meals: mealsFor(plan),
          workouts,
          workoutStatus: workoutStatusFor(date, workouts),
          note,
        });
      }
      date = addDays(date, -1);
    }
    return out;
  }, [endDate, fromDate, plansByDate, notes, dietsById, mealsFor, workoutsFor, dietStatusFor, workoutStatusFor, todayIso]);

  // ── Navigation ──────────────────────────────────────────────────────────────────
  const jumpTo = useCallback((date: string) => { setEndDate(date); setFromDate(startOfWeek(date)); }, []);
  const resetToToday = useCallback(() => { setEndDate(todayIso); setFromDate(startOfWeek(todayIso)); }, [todayIso]);
  /** First tap expands the week to the month start; after that, one month further back each tap. */
  const loadOlder = useCallback(() => {
    setFromDate((cur) => {
      const monthStart = startOfMonth(endDate);
      if (cur > monthStart) return monthStart;
      return startOfMonth(addDays(cur, -1));
    });
  }, [endDate]);
  const atToday = endDate === todayIso;

  // ── Notes CRUD (server-persisted) — optimistic, with revert on failure ────────────
  const saveNote = useCallback(async (date: string, text: string) => {
    const trimmed = text.trim();
    const prevVal = notes[date];
    setNotes((prev) => { const next = { ...prev }; if (trimmed) next[date] = text; else delete next[date]; return next; });
    try {
      if (trimmed) await upsertJournal(date, text); else await deleteJournal(date);
    } catch (e) {
      setError(friendlyMessage(e));
      setNotes((prev) => { const next = { ...prev }; if (prevVal != null) next[date] = prevVal; else delete next[date]; return next; });
    }
  }, [notes]);

  const deleteNote = useCallback(async (date: string) => {
    const prevVal = notes[date];
    setNotes((prev) => { const next = { ...prev }; delete next[date]; return next; });
    try { await deleteJournal(date); }
    catch (e) { setError(friendlyMessage(e)); if (prevVal != null) setNotes((prev) => ({ ...prev, [date]: prevVal })); }
  }, [notes]);

  return {
    todayIso, endDate, atToday, loading, error, entries,
    jumpTo, resetToToday, loadOlder, saveNote, deleteNote,
  };
}
