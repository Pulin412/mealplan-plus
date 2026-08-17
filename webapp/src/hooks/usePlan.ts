"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { friendlyMessage } from "@/lib/api/errors";
import { listPlans, upsertPlan, deletePlan, addPlannedWorkout, removePlannedWorkout, addPlannedMeal, removePlannedMeal, removeMealFromDay, getCompletedDays, getLoggedSlots, isoOf, type DayPlanDto, type LoggedMealSlotDto } from "@/lib/api/plans";
import { listDiets, type DietDto } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import { listWorkouts, type WorkoutTemplateDto } from "@/lib/api/workouts";
import { listSessionsForDate, deleteSession } from "@/lib/api/sessions";
import { foodMacros, unitLabel, MEAL_SLOTS, num } from "@/lib/nutrition";

export interface DietLine { name: string; meta: string; header: boolean; mealId?: number }
export interface DietSlotView { slot: string; kcal: number; lines: DietLine[] }
export interface DietSummary { id: number; name: string; kcal: number; slots: DietSlotView[]; tags: string[]; description?: string | null; searchText: string }
/** A meal offered by the "add meal to a day" picker, with its total kcal and ingredient lines. */
export interface MealSummary { id: number; name: string; kcal: number; lines: DietLine[]; searchText: string; slots: string[] }
/** A planned meal already assigned to a day, resolved for display (with its ingredient lines). */
export interface PlannedMealView { id: number; slot: string; name: string; kcal: number; lines: DietLine[] }

const iso = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;

/** Resolve a diet into slot-grouped meals/foods with kcal — mirrors the Home diet view. */
function resolveDiet(d: DietDto, mealsById: Map<number, MealDto>, foodsById: Map<number, FoodDto>): DietSummary {
  const bySlot = new Map<string, DietLine[]>();
  const slotKcal = new Map<string, number>();
  let total = 0;
  const ensure = (slot: string) => { if (!bySlot.has(slot)) bySlot.set(slot, []); return bySlot.get(slot)!; };
  const bump = (slot: string, k: number) => { slotKcal.set(slot, (slotKcal.get(slot) ?? 0) + k); total += k; };

  (d.meals ?? []).forEach((dm) => {
    const meal = dm.mealId != null ? mealsById.get(dm.mealId) : undefined;
    let mkcal = 0;
    const items: DietLine[] = [];
    (meal?.items ?? []).forEach((it) => {
      const f = it.foodId != null ? foodsById.get(it.foodId) : undefined;
      mkcal += foodMacros(f, it.quantity, it.unit).kcal;
      items.push({ name: f?.name ?? "Food", meta: `${num(it.quantity)} ${unitLabel(it.unit)}`, header: false });
    });
    const lines = ensure(dm.slot);
    lines.push({ name: meal?.name ?? "Meal", meta: `${Math.round(mkcal)} kcal`, header: true, mealId: dm.mealId ?? undefined });
    items.forEach((li) => lines.push(li));
    bump(dm.slot, mkcal);
  });
  (d.foodItems ?? []).forEach((fi) => {
    const f = fi.foodId != null ? foodsById.get(fi.foodId) : undefined;
    const k = foodMacros(f, fi.quantity ?? 1, fi.unit).kcal;
    ensure(fi.slot).push({ name: f?.name ?? "Food", meta: `${num(fi.quantity ?? 1)} ${unitLabel(fi.unit)}`, header: false });
    bump(fi.slot, k);
  });

  const allSlots = Array.from(bySlot.keys());
  const order = [...MEAL_SLOTS.filter((s) => bySlot.has(s)), ...allSlots.filter((s) => !MEAL_SLOTS.includes(s))];
  const slots = order.map((slot) => ({ slot, kcal: Math.round(slotKcal.get(slot) ?? 0), lines: bySlot.get(slot)! }));
  const tags = (d.tags ?? []).map((t) => t.name);
  // Haystack for picker search: diet name + tags + every meal/ingredient name across its slots.
  const searchText = [d.name, ...tags, ...slots.flatMap((s) => s.lines.map((li) => li.name))].join(" ").toLowerCase();
  return { id: d.id!, name: d.name, kcal: Math.round(total), slots, tags, description: d.description ?? null, searchText };
}

export function usePlan() {
  const today = useMemo(() => new Date(), []);
  const [ym, setYm] = useState({ year: today.getFullYear(), month: today.getMonth() + 1 }); // month 1-12
  const [plans, setPlans] = useState<Record<string, DayPlanDto>>({});
  const [completedDays, setCompletedDays] = useState<Set<string>>(new Set());
  const [selectedSlots, setSelectedSlots] = useState<LoggedMealSlotDto[]>([]);
  const [diets, setDiets] = useState<DietSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickerSearch, setPickerSearch] = useState("");
  // Multi-select diet-picker filters (match ANY, mirrors the Diets screen); empty = no filter.
  const [pickerTags, setPickerTags] = useState<string[]>([]);
  const [pickerSlots, setPickerSlots] = useState<string[]>([]);
  const [workouts, setWorkouts] = useState<WorkoutTemplateDto[]>([]);
  const [workoutPickerOpen, setWorkoutPickerOpen] = useState(false);
  const [openWorkout, setOpenWorkout] = useState<WorkoutTemplateDto | null>(null);
  const [meals, setMeals] = useState<MealSummary[]>([]);
  const [mealPickerOpen, setMealPickerOpen] = useState(false);
  const [mealPickerSlot, setMealPickerSlot] = useState<string>("Breakfast");
  const [mealPickerSearch, setMealPickerSearch] = useState("");

  const allTags = useMemo(() => Array.from(new Set(diets.flatMap((d) => d.tags))).sort(), [diets]);
  const allSlots = useMemo(() => {
    const present = new Set<string>();
    diets.forEach((d) => d.slots.forEach((s) => present.add(s.slot)));
    const known = MEAL_SLOTS.filter((s) => present.has(s));
    const unknown = Array.from(present).filter((s) => !MEAL_SLOTS.includes(s)).sort();
    return [...known, ...unknown];
  }, [diets]);
  const togglePickerTag = useCallback((name: string) => setPickerTags((p) => p.includes(name) ? p.filter((t) => t !== name) : [...p, name]), []);
  const clearPickerTags = useCallback(() => setPickerTags([]), []);
  const togglePickerSlot = useCallback((name: string) => setPickerSlots((p) => p.includes(name) ? p.filter((s) => s !== name) : [...p, name]), []);
  const clearPickerSlots = useCallback(() => setPickerSlots([]), []);

  const filteredDiets = useMemo(() => {
    const q = pickerSearch.trim().toLowerCase();
    const hasSlot = pickerSlots.length > 0;
    return diets.filter((d) => {
      // Slot filter: the diet must contain at least one selected slot.
      const scopeSlots = hasSlot ? d.slots.filter((s) => pickerSlots.includes(s.slot)) : d.slots;
      if (hasSlot && scopeSlots.length === 0) return false;
      // When slots are selected, search only within those slots; otherwise the whole diet.
      if (q) {
        const haystack = hasSlot
          ? scopeSlots.flatMap((s) => s.lines.map((li) => li.name)).join(" ").toLowerCase()
          : d.searchText;
        if (!haystack.includes(q)) return false;
      }
      if (pickerTags.length && !pickerTags.some((t) => d.tags.includes(t))) return false;
      return true;
    });
  }, [diets, pickerSearch, pickerTags, pickerSlots]);

  const todayIso = iso(today.getFullYear(), today.getMonth() + 1, today.getDate());

  const loadPlans = useCallback(async (year: number, month: number) => {
    const first = new Date(year, month - 1, 1);
    const last = new Date(year, month, 0);
    const from = iso(year, month, 1);
    const to = iso(year, month, last.getDate());
    // widen to cover the next 7 days from today
    const t7 = new Date(today); t7.setDate(today.getDate() + 6);
    const lo = first < today ? from : todayIso;
    const hi = last > t7 ? to : iso(t7.getFullYear(), t7.getMonth() + 1, t7.getDate());
    try {
      const list = await listPlans(lo, hi);
      const map: Record<string, DayPlanDto> = {};
      list.forEach((p) => { map[isoOf(p.date)] = p; });
      setPlans(map);
      getCompletedDays(lo, hi).then((cd) => setCompletedDays(new Set(cd.map((d) => isoOf(d))))).catch(() => {});
    } catch (e) { setError(friendlyMessage(e)); }
    finally { setLoading(false); }
  }, [today, todayIso]);

  // Past-day recap: load which meal slots were logged on the selected day.
  useEffect(() => {
    if (!selected) { setSelectedSlots([]); return; }
    getLoggedSlots(selected).then(setSelectedSlots).catch(() => setSelectedSlots([]));
  }, [selected]);

  useEffect(() => {
    Promise.all([listDiets(), listMeals(), listFoods()]).then(([ds, ms, fs]) => {
      const mealsById = new Map<number, MealDto>(); ms.forEach((m) => m.id != null && mealsById.set(m.id, m));
      const foodsById = new Map<number, FoodDto>(); fs.forEach((f) => f.id != null && foodsById.set(f.id, f));
      setDiets(ds.filter((d) => d.id != null).map((d) => resolveDiet(d, mealsById, foodsById)));
      setMeals(ms.filter((m) => m.id != null).map((m) => {
        const items = m.items ?? [];
        const lines: DietLine[] = items.map((it) => {
          const f = it.foodId != null ? foodsById.get(it.foodId) : undefined;
          return { name: f?.name ?? "Food", meta: `${num(it.quantity)} ${unitLabel(it.unit)}`, header: false };
        });
        return {
          id: m.id!, name: m.name, lines, slots: m.slots ?? [],
          searchText: [m.name, ...lines.map((l) => l.name)].join(" ").toLowerCase(),
          kcal: Math.round(items.reduce((acc, it) => acc + foodMacros(it.foodId != null ? foodsById.get(it.foodId) : undefined, it.quantity, it.unit).kcal, 0)),
        };
      }));
    }).catch(() => {});
    listWorkouts().then(setWorkouts).catch(() => {});
  }, []);

  useEffect(() => { setLoading(true); loadPlans(ym.year, ym.month); }, [ym, loadPlans]);

  const prevMonth = () => setYm(({ year, month }) => month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 });
  const nextMonth = () => setYm(({ year, month }) => month === 12 ? { year: year + 1, month: 1 } : { year, month: month + 1 });

  const setDiet = useCallback(async (dateIso: string, dietId: number | null) => {
    const existing = plans[dateIso];
    try { await upsertPlan(dateIso, dietId, existing?.plannedWorkouts ?? [], existing?.plannedMeals ?? []); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [plans, ym, loadPlans]);

  // ── Planned meals ─────────────────────────────────────────────────────────────
  const mealsById = useMemo(() => new Map(meals.map((m) => [m.id, m])), [meals]);
  const filteredMeals = useMemo(() => {
    const q = mealPickerSearch.trim().toLowerCase();
    // Only meals tagged for the currently-selected assign slot, then the ingredient-aware search.
    return meals.filter((m) => m.slots.includes(mealPickerSlot) && (q === "" || m.searchText.includes(q)));
  }, [meals, mealPickerSearch, mealPickerSlot]);

  /** This day's planned meals resolved to name + kcal, in canonical slot order. */
  const plannedMealsFor = useCallback((dateIso: string): PlannedMealView[] => {
    const list = (plans[dateIso]?.plannedMeals ?? []).flatMap((pm) => {
      if (pm.id == null) return [];
      const m = mealsById.get(pm.mealId);
      return [{ id: pm.id, slot: pm.slot, name: m?.name ?? "Meal", kcal: m?.kcal ?? 0, lines: m?.lines ?? [] }];
    });
    return list.sort((a, b) => {
      const ia = MEAL_SLOTS.indexOf(a.slot), ib = MEAL_SLOTS.indexOf(b.slot);
      return (ia < 0 ? Number.MAX_SAFE_INTEGER : ia) - (ib < 0 ? Number.MAX_SAFE_INTEGER : ib);
    });
  }, [plans, mealsById]);

  const openMealPicker = useCallback(() => { setMealPickerSearch(""); setMealPickerSlot("Breakfast"); setMealPickerOpen(true); }, []);
  const closeMealPicker = useCallback(() => setMealPickerOpen(false), []);

  const addMeal = useCallback(async (dateIso: string, slot: string, mealId: number) => {
    try { await addPlannedMeal(dateIso, mealId, slot); setMealPickerOpen(false); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans]);

  const removeMeal = useCallback(async (dateIso: string, mealPlanId: number) => {
    try { await removePlannedMeal(dateIso, mealPlanId); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans]);

  /** Cancel one meal from a single day: deletes a loose planned meal, or detaches the day from its diet. */
  const removeMealFromDayCb = useCallback(async (dateIso: string, slot: string, mealId: number) => {
    try { await removeMealFromDay(dateIso, slot, mealId); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans]);

  const openPicker = useCallback(() => { setPickerSearch(""); setPickerTags([]); setPickerSlots([]); setPickerOpen(true); }, []);
  const closePicker = useCallback(() => setPickerOpen(false), []);
  const chooseDiet = useCallback(async (dateIso: string, dietId: number) => { await setDiet(dateIso, dietId); setPickerOpen(false); }, [setDiet]);

  const clearDay = useCallback(async (dateIso: string) => {
    try { await deletePlan(dateIso); setSelected(null); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans]);

  // ── Planned workouts ──────────────────────────────────────────────────────────
  const openWorkoutPicker = useCallback(() => setWorkoutPickerOpen(true), []);
  const closeWorkoutPicker = useCallback(() => setWorkoutPickerOpen(false), []);

  const addWorkout = useCallback(async (dateIso: string, template: WorkoutTemplateDto) => {
    if (template.id == null) return;
    try { await addPlannedWorkout(dateIso, template.id, template.name); setWorkoutPickerOpen(false); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans]);

  const removeWorkout = useCallback(async (dateIso: string, workoutId: number) => {
    try {
      // Also drop a started-but-unfinished session for it, else it lingers on Home as an ad-hoc workout.
      const name = plans[dateIso]?.plannedWorkouts?.find((pw) => pw.id === workoutId)?.activityName;
      await removePlannedWorkout(dateIso, workoutId);
      if (name) {
        const sessions = await listSessionsForDate(dateIso).catch(() => []);
        const s = sessions.find((x) => x.name === name && x.isCompleted !== true);
        if (s?.id != null) await deleteSession(s.id);
      }
      await loadPlans(ym.year, ym.month);
    } catch (e) { setError(friendlyMessage(e)); }
  }, [ym, loadPlans, plans]);

  /** Open the read-only detail of a planned workout by its template id (no-op if not a template). */
  const openWorkoutDetail = useCallback((templateId: number | null | undefined) => {
    const t = workouts.find((w) => w.id === templateId);
    if (t) setOpenWorkout(t);
  }, [workouts]);
  const closeWorkoutDetail = useCallback(() => setOpenWorkout(null), []);

  return {
    ym, setYm, plans, completedDays, selectedSlots, diets, loading, error, todayIso, today, selected, setSelected,
    prevMonth, nextMonth, setDiet, clearDay,
    pickerOpen, pickerSearch, setPickerSearch, allTags, allSlots, filteredDiets,
    pickerTags, togglePickerTag, clearPickerTags, pickerSlots, togglePickerSlot, clearPickerSlots,
    openPicker, closePicker, chooseDiet,
    workouts, workoutPickerOpen, openWorkoutPicker, closeWorkoutPicker,
    addWorkout, removeWorkout, openWorkout, openWorkoutDetail, closeWorkoutDetail,
    meals, filteredMeals, plannedMealsFor,
    mealPickerOpen, mealPickerSlot, setMealPickerSlot, mealPickerSearch, setMealPickerSearch,
    openMealPicker, closeMealPicker, addMeal, removeMeal, removeMealFromDay: removeMealFromDayCb,
  };
}
