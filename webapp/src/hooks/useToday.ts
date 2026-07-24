"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { getDashboard, toggleMealSlot, addLoggedFood, removeLoggedFood, isoDate, type DashboardDto } from "@/lib/api/dashboard";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import type { FoodUnit } from "@/lib/nutrition";

export function useToday() {
  const [dashboard, setDashboard] = useState<DashboardDto | null>(null);
  const [foods, setFoods] = useState<FoodDto[]>([]);
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

  const removeFood = useCallback(async (id: number) => {
    try { await removeLoggedFood(id); await reload(); }
    catch (e) { setError(e instanceof Error ? e.message : "Failed"); }
  }, [reload]);

  return { dashboard, foods, foodsById, loading, error, expanded, toggleExpand, busySlot, toggleSlot, addFood, removeFood };
}
