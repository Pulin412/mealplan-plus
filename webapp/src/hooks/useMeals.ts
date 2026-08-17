"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { listMeals, createMeal, updateMeal, deleteMeal, toggleMealFavorite, type MealDto } from "@/lib/api/meals";
import { toggleMealShare } from "@/lib/api/social";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import { foodMacros, defaultQtyFor, type FoodUnit } from "@/lib/nutrition";
import { naturalCompare } from "@/lib/utils/naturalCompare";
import { friendlyMessage, toastApiError } from "@/lib/api/errors";
import type { MealSort, MealViewMode } from "@/types/meal";

export interface BuildItem { foodId: number; quantity: number; unit: FoodUnit }

export interface ResolvedItem { name: string; quantity: number; unit: string; kcal: number }
export interface MealView {
  meal: MealDto;
  items: ResolvedItem[];
  totalKcal: number; totalP: number; totalC: number; totalF: number;
  summary: string;
  /** Lowercased haystack for search: meal name + ingredient/food names. */
  searchText: string;
}

export function useMeals() {
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<MealSort>("recent");
  const [sortOpen, setSortOpen] = useState(false);
  const [viewMode, setViewMode] = useState<MealViewMode>("list");
  const [favOnly, setFavOnly] = useState(false);
  const [importedOnly, setImportedOnly] = useState(false);
  const [slotFilter, setSlotFilter] = useState<string | null>(null);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  // builder
  const [builderOpen, setBuilderOpen] = useState(false);
  const [editing, setEditing] = useState<MealDto | null>(null);
  const [saving, setSaving] = useState(false);

  const foodsById = useMemo(() => {
    const m = new Map<number, FoodDto>();
    foods.forEach((f) => { if (f.id != null) m.set(f.id, f); });
    return m;
  }, [foods]);

  useEffect(() => {
    setLoading(true);
    Promise.all([listMeals(), listFoods()])
      .then(([m, f]) => { setMeals(m); setFoods(f); })
      .catch((e) => setError(friendlyMessage(e)))
      .finally(() => setLoading(false));
  }, []);

  const resolved: MealView[] = useMemo(() => meals.map((meal) => {
    let kcal = 0, p = 0, c = 0, f = 0;
    const items: ResolvedItem[] = (meal.items ?? []).map((it) => {
      const food = it.foodId != null ? foodsById.get(it.foodId) : undefined;
      const m = foodMacros(food, it.quantity, it.unit);
      kcal += m.kcal; p += m.protein; c += m.carbs; f += m.fat;
      return { name: food?.name ?? "Unknown food", quantity: it.quantity, unit: it.unit, kcal: Math.round(m.kcal) };
    });
    const names = items.map((i) => i.name);
    const summary = items.length === 0 ? "No items"
      : `${items.length} item${items.length === 1 ? "" : "s"} · ${names.join(", ")}`;
    const searchText = [meal.name, ...names].join(" ").toLowerCase();
    return { meal, items, totalKcal: Math.round(kcal), totalP: p, totalC: c, totalF: f, summary, searchText };
  }), [meals, foodsById]);

  const allSlots = useMemo(
    () => Array.from(new Set(meals.flatMap((m) => m.slots ?? []))),
    [meals]
  );

  const filtered = useMemo(() => {
    let list = resolved;
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((v) => v.searchText.includes(q));
    }
    if (favOnly) list = list.filter((v) => v.meal.isFavorite);
    if (importedOnly) list = list.filter((v) => v.meal.imported);
    if (slotFilter) list = list.filter((v) => (v.meal.slots ?? []).includes(slotFilter));
    switch (sort) {
      case "name": return [...list].sort((a, b) => naturalCompare(a.meal.name, b.meal.name));
      case "calories": return [...list].sort((a, b) => b.totalKcal - a.totalKcal);
      case "protein": return [...list].sort((a, b) => b.totalP - a.totalP);
      default: return list;
    }
  }, [resolved, query, favOnly, importedOnly, slotFilter, sort]);

  const favCount = useMemo(() => meals.filter((m) => m.isFavorite).length, [meals]);

  const toggleExpand = useCallback((id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }, []);

  const reload = useCallback(async () => {
    setMeals(await listMeals());
  }, []);

  const handleToggleFav = useCallback(async (meal: MealDto) => {
    if (meal.id == null) return;
    setMeals((prev) => prev.map((m) => m.id === meal.id ? { ...m, isFavorite: !m.isFavorite } : m));
    try { await toggleMealFavorite(meal.id); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  const handleToggleShare = useCallback(async (meal: MealDto) => {
    if (!meal.serverId) return;
    setMeals((prev) => prev.map((m) => m.serverId === meal.serverId ? { ...m, isShared: !m.isShared } : m));
    try { await toggleMealShare(meal.serverId); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  const handleDelete = useCallback(async (meal: MealDto) => {
    if (meal.id == null) return;
    setMeals((prev) => prev.filter((m) => m.id !== meal.id));
    try { await deleteMeal(meal.id); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  const openNew = useCallback(() => { setEditing(null); setBuilderOpen(true); }, []);
  const openEdit = useCallback((meal: MealDto) => { setEditing(meal); setBuilderOpen(true); }, []);
  const closeBuilder = useCallback(() => { setBuilderOpen(false); setEditing(null); }, []);

  const saveMeal = useCallback(async (name: string, slots: string[], items: BuildItem[], notes?: string | null) => {
    if (!name.trim() || items.length === 0) return;
    setSaving(true);
    try {
      const input = { name: name.trim(), slots, items, notes: notes?.trim() || null };
      if (editing?.id != null) await updateMeal(editing.id, input);
      else await createMeal(input);
      // Refetch meals AND foods: a food created inline while building resolves only once the parent
      // foods list includes it — otherwise the just-saved meal renders its item as "Unknown food".
      const [m, f] = await Promise.all([listMeals(), listFoods()]);
      setMeals(m); setFoods(f);
      closeBuilder();
    } catch (e) {
      setError(friendlyMessage(e));
    } finally {
      setSaving(false);
    }
  }, [editing, closeBuilder]);

  return {
    meals: filtered, allMeals: resolved, totalCount: meals.length, favCount, foods, foodsById,
    loading, error,
    query, setQuery, sort, setSort, sortOpen, setSortOpen,
    viewMode, setViewMode, favOnly, setFavOnly,
    importedOnly, setImportedOnly,
    allSlots, slotFilter, setSlotFilter,
    expandedIds, toggleExpand,
    handleToggleFav, handleToggleShare, handleDelete,
    builderOpen, editing, openNew, openEdit, closeBuilder, saveMeal, saving,
    defaultQtyFor,
  };
}
