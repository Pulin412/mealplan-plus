"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { getDashboard, toggleMealSlot, toggleDayComplete as apiToggleDayComplete, addLoggedFood, removeLoggedFood, isoDate, type DashboardDto } from "@/lib/api/dashboard";
import { listFoods, createFoodFromDto, type FoodDto } from "@/lib/api/foods";
import { listMeals, type MealDto } from "@/lib/api/meals";
import type { FoodUnit } from "@/lib/nutrition";

export function useToday() {
  const [dashboard, setDashboard] = useState<DashboardDto | null>(null);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState<Set<string>>(new Set());
  const [busySlot, setBusySlot] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setDashboard(await getDashboard());
  }, []);

  useEffect(() => {
    setLoading(true);
    getDashboard()
      .then(setDashboard)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
    listFoods().then(setFoods).catch(() => {});
    listMeals().then(setMeals).catch(() => {});
  }, []);

  const date = useMemo(() => (dashboard ? isoDate(dashboard.date) : ""), [dashboard]);
  const foodsById = useMemo(() => {
    const m = new Map<number, FoodDto>();
    foods.forEach((f) => { if (f.id != null) m.set(f.id, f); });
    return m;
  }, [foods]);

  const toggleExpand = useCallback((slot: string) => {
    setExpanded((prev) => { const n = new Set(prev); if (n.has(slot)) n.delete(slot); else n.add(slot); return n; });
  }, []);

  const toggleSlot = useCallback(async (slot: string) => {
    if (!date) return;
    setBusySlot(slot);
    try { await toggleMealSlot(date, slot); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
    finally { setBusySlot(null); }
  }, [date, reload]);

  const addFood = useCallback(async (foodId: number, slot: string, quantity: number, unit: FoodUnit) => {
    if (!date) return;
    try { await addLoggedFood(date, foodId, slot, quantity, unit); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [date, reload]);

  const reloadFoods = useCallback(() => { listFoods().then(setFoods).catch(() => {}); }, []);

  /**
   * Log a whole meal into today's slot by flattening it into its foods — only touches today's log,
   * never the planned diet. Resolves each item's food id (falls back to the serverId→id map).
   */
  const addMeal = useCallback(async (meal: MealDto, slot: string) => {
    if (!date) return;
    const idByServerId = new Map<string, number>();
    foods.forEach((f) => { if (f.serverId && f.id != null) idByServerId.set(f.serverId, f.id); });
    try {
      for (const it of meal.items ?? []) {
        const foodId = it.foodId ?? (it.foodServerId ? idByServerId.get(it.foodServerId) : undefined);
        if (foodId != null) await addLoggedFood(date, foodId, slot, it.quantity, it.unit);
      }
      await reload();
    } catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [date, foods, reload]);

  /** Persist an Open Food Facts result as a food (server assigns an id), then log it to today. */
  const addOnlineFood = useCallback(async (dto: FoodDto, slot: string, quantity: number, unit: FoodUnit) => {
    if (!date) return;
    try {
      const created = await createFoodFromDto(dto);
      if (created.id != null) {
        reloadFoods();                 // so "Added today" can resolve the new food's calories
        await addLoggedFood(date, created.id, slot, quantity, unit);
        await reload();
      }
    } catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [date, reload, reloadFoods]);

  /** Mark today complete / not-complete; only completed days count toward the streak. */
  const toggleDayComplete = useCallback(async () => {
    if (!date) return;
    try { await apiToggleDayComplete(date); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [date, reload]);

  const removeFood = useCallback(async (id: number) => {
    try { await removeLoggedFood(id); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [reload]);

  return { dashboard, foods, meals, foodsById, loading, error, expanded, toggleExpand, busySlot, toggleSlot, addFood, addOnlineFood, addMeal, toggleDayComplete, removeFood };
}
