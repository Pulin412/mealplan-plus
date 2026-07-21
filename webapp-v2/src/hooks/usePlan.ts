"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { listPlans, upsertPlan, deletePlan, isoOf, type DayPlanDto } from "@/lib/api/plans";
import { listDiets, type DietDto } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import { foodMacros, unitLabel, MEAL_SLOTS, num } from "@/lib/nutrition";

export interface DietLine { name: string; meta: string; header: boolean }
export interface DietSlotView { slot: string; kcal: number; lines: DietLine[] }
export interface DietSummary { id: number; name: string; kcal: number; slots: DietSlotView[] }

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
    lines.push({ name: meal?.name ?? "Meal", meta: `${Math.round(mkcal)} kcal`, header: true });
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
  return { id: d.id!, name: d.name, kcal: Math.round(total), slots };
}

export function usePlan() {
  const today = useMemo(() => new Date(), []);
  const [ym, setYm] = useState({ year: today.getFullYear(), month: today.getMonth() + 1 }); // month 1-12
  const [plans, setPlans] = useState<Record<string, DayPlanDto>>({});
  const [diets, setDiets] = useState<DietSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);

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
    } catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
    finally { setLoading(false); }
  }, [today, todayIso]);

  useEffect(() => {
    Promise.all([listDiets(), listMeals(), listFoods()]).then(([ds, ms, fs]) => {
      const mealsById = new Map<number, MealDto>(); ms.forEach((m) => m.id != null && mealsById.set(m.id, m));
      const foodsById = new Map<number, FoodDto>(); fs.forEach((f) => f.id != null && foodsById.set(f.id, f));
      setDiets(ds.filter((d) => d.id != null).map((d) => resolveDiet(d, mealsById, foodsById)));
    }).catch(() => {});
  }, []);

  useEffect(() => { setLoading(true); loadPlans(ym.year, ym.month); }, [ym, loadPlans]);

  const prevMonth = () => setYm(({ year, month }) => month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 });
  const nextMonth = () => setYm(({ year, month }) => month === 12 ? { year: year + 1, month: 1 } : { year, month: month + 1 });

  const setDiet = useCallback(async (dateIso: string, dietId: number | null) => {
    const existing = plans[dateIso];
    try { await upsertPlan(dateIso, dietId, existing?.plannedWorkouts ?? []); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [plans, ym, loadPlans]);

  const clearDay = useCallback(async (dateIso: string) => {
    try { await deletePlan(dateIso); setSelected(null); await loadPlans(ym.year, ym.month); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [ym, loadPlans]);

  return { ym, setYm, plans, diets, loading, error, todayIso, today, selected, setSelected, prevMonth, nextMonth, setDiet, clearDay };
}
