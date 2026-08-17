"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { listDiets, createDiet, updateDiet, deleteDiet, toggleDietFavorite, type DietDto, type DietEntryInput } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods, type FoodDto } from "@/lib/api/foods";
import { naturalCompare } from "@/lib/utils/naturalCompare";
import { listDietTags, createDietTag, type TagDto } from "@/lib/api/tags";
import { toggleDietShare } from "@/lib/api/social";
import { foodMacros, MEAL_SLOTS, unitLabel, type Macros } from "@/lib/nutrition";
import { friendlyMessage, toastApiError } from "@/lib/api/errors";
import type { DietSort, DietViewMode } from "@/types/diet";

export interface MealSummary { id: number; name: string; totals: Macros }

export interface DietEntryView { kind: "meal" | "food"; name: string; kcal: number; meta: string; foods?: { name: string; meta: string }[] }
export interface DietSlotGroup { slot: string; entries: DietEntryView[]; kcal: number }
export interface DietView {
  diet: DietDto;
  slots: DietSlotGroup[];
  totalKcal: number; totalP: number; totalC: number; totalF: number;
  entryCount: number; summary: string; tagNames: string[];
  /** Lowercased haystack for search: diet name + tags + meal names + ingredient/food names. */
  searchText: string;
}

function slotOrder(present: string[]): string[] {
  const distinct = Array.from(new Set(present));
  const known = MEAL_SLOTS.filter((s) => distinct.includes(s));
  const unknown = distinct.filter((s) => !MEAL_SLOTS.includes(s));
  return [...known, ...unknown];
}

export function useDiets() {
  const [diets, setDiets] = useState<DietDto[]>([]);
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [availableTags, setAvailableTags] = useState<TagDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [query, setQuery] = useState("");
  const [sort, setSort] = useState<DietSort>("recent");
  const [viewMode, setViewMode] = useState<DietViewMode>("list");
  const [favOnly, setFavOnly] = useState(false);
  const [importedOnly, setImportedOnly] = useState(false);
  // Multi-select filters: empty = no filter, else keep diets matching ANY selected tag / slot.
  const [tagFilters, setTagFilters] = useState<string[]>([]);
  const [slotFilters, setSlotFilters] = useState<string[]>([]);
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  const [builderOpen, setBuilderOpen] = useState(false);
  const [editing, setEditing] = useState<DietDto | null>(null);
  const [saving, setSaving] = useState(false);

  const foodsById = useMemo(() => { const m = new Map<number, FoodDto>(); foods.forEach((f) => f.id != null && m.set(f.id, f)); return m; }, [foods]);

  // meal id -> name + resolved macro totals (for meal entries in a diet)
  const mealSummaries = useMemo(() => {
    const map = new Map<number, MealSummary>();
    meals.forEach((meal) => {
      if (meal.id == null) return;
      const totals: Macros = { kcal: 0, protein: 0, carbs: 0, fat: 0 };
      (meal.items ?? []).forEach((it) => {
        const food = it.foodId != null ? foodsById.get(it.foodId) : undefined;
        const m = foodMacros(food, it.quantity, it.unit);
        totals.kcal += m.kcal; totals.protein += m.protein; totals.carbs += m.carbs; totals.fat += m.fat;
      });
      map.set(meal.id, { id: meal.id, name: meal.name, totals });
    });
    return map;
  }, [meals, foodsById]);

  // meal id -> its ingredient lines (name + qty), for the "copy a diet" preview (meal → ingredients)
  const mealFoodsById = useMemo(() => {
    const map = new Map<number, { name: string; meta: string }[]>();
    meals.forEach((meal) => {
      if (meal.id == null) return;
      map.set(meal.id, (meal.items ?? []).map((it) => {
        const food = it.foodId != null ? foodsById.get(it.foodId) : undefined;
        return { name: food?.name ?? "Unknown food", meta: `${it.quantity} ${unitLabel(it.unit)}` };
      }));
    });
    return map;
  }, [meals, foodsById]);

  useEffect(() => {
    setLoading(true);
    Promise.all([listDiets(), listMeals(), listFoods()])
      .then(([d, m, f]) => { setDiets(d); setMeals(m); setFoods(f); })
      .catch((e) => setError(friendlyMessage(e)))
      .finally(() => setLoading(false));
    listDietTags().then(setAvailableTags).catch(() => {});
  }, []);

  const resolved: DietView[] = useMemo(() => diets.map((diet) => {
    let kcal = 0, p = 0, c = 0, f = 0;
    const rows: { slot: string; view: DietEntryView; kcal: number }[] = [];
    (diet.meals ?? []).forEach((dm) => {
      const s = dm.mealId != null ? mealSummaries.get(dm.mealId) : undefined;
      const t = s?.totals ?? { kcal: 0, protein: 0, carbs: 0, fat: 0 };
      kcal += t.kcal; p += t.protein; c += t.carbs; f += t.fat;
      rows.push({ slot: dm.slot, kcal: t.kcal, view: { kind: "meal", name: s?.name ?? "Unknown meal", kcal: Math.round(t.kcal), meta: `meal · ${Math.round(t.kcal)} kcal`, foods: dm.mealId != null ? mealFoodsById.get(dm.mealId) : undefined } });
    });
    (diet.foodItems ?? []).forEach((fi) => {
      const food = fi.foodId != null ? foodsById.get(fi.foodId) : undefined;
      const m = foodMacros(food, fi.quantity ?? 1, fi.unit);
      kcal += m.kcal; p += m.protein; c += m.carbs; f += m.fat;
      rows.push({ slot: fi.slot, kcal: m.kcal, view: { kind: "food", name: food?.name ?? "Unknown food", kcal: Math.round(m.kcal), meta: `${fi.quantity} · ${Math.round(m.kcal)} kcal` } });
    });
    const slots: DietSlotGroup[] = slotOrder(rows.map((r) => r.slot)).map((slot) => {
      const es = rows.filter((r) => r.slot === slot);
      return { slot, entries: es.map((e) => e.view), kcal: Math.round(es.reduce((a, e) => a + e.kcal, 0)) };
    });
    const count = rows.length;
    const names = rows.map((r) => r.view.name);
    const tagNames = (diet.tags ?? []).map((t) => t.name);
    const ingredientNames = rows.flatMap((r) => r.view.foods?.map((fd) => fd.name) ?? []);
    const searchText = [diet.name, ...tagNames, ...names, ...ingredientNames].join(" ").toLowerCase();
    const summary = count === 0 ? "No items" : `${count} item${count === 1 ? "" : "s"} · ${names.join(", ")}`;
    return { diet, slots, totalKcal: Math.round(kcal), totalP: p, totalC: c, totalF: f, entryCount: count, summary, tagNames, searchText };
  }), [diets, mealSummaries, foodsById, mealFoodsById]);

  const allTagNames = useMemo(
    () => Array.from(new Set([...availableTags.map((t) => t.name), ...diets.flatMap((d) => (d.tags ?? []).map((t) => t.name))])).sort(),
    [availableTags, diets]
  );

  // Slots actually present across the user's diets, in canonical meal-slot order (unknowns appended).
  const allSlotNames = useMemo(() => {
    const present = new Set<string>();
    resolved.forEach((v) => v.slots.forEach((s) => present.add(s.slot)));
    const known = MEAL_SLOTS.filter((s) => present.has(s));
    const unknown = Array.from(present).filter((s) => !MEAL_SLOTS.includes(s)).sort();
    return [...known, ...unknown];
  }, [resolved]);

  const toggleTagFilter = useCallback((name: string) => setTagFilters((p) => p.includes(name) ? p.filter((t) => t !== name) : [...p, name]), []);
  const clearTagFilters = useCallback(() => setTagFilters([]), []);
  const toggleSlotFilter = useCallback((name: string) => setSlotFilters((p) => p.includes(name) ? p.filter((s) => s !== name) : [...p, name]), []);
  const clearSlotFilters = useCallback(() => setSlotFilters([]), []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    const hasSlot = slotFilters.length > 0;
    const list = resolved.filter((v) => {
      // Slot filter: the diet must contain at least one selected slot.
      const scopeSlots = hasSlot ? v.slots.filter((s) => slotFilters.includes(s.slot)) : v.slots;
      if (hasSlot && scopeSlots.length === 0) return false;
      // Text search: when slots are selected, match ONLY within those slots (e.g. "oatmeal" in Evening);
      // otherwise match the whole diet (name + tags + every meal/ingredient).
      if (q) {
        const haystack = hasSlot
          ? scopeSlots.flatMap((s) => s.entries.flatMap((e) => [e.name, ...(e.foods?.map((fd) => fd.name) ?? [])])).join(" ").toLowerCase()
          : v.searchText;
        if (!haystack.includes(q)) return false;
      }
      if (favOnly && !v.diet.isFavorite) return false;
      if (importedOnly && !v.diet.imported) return false;
      if (tagFilters.length && !v.tagNames.some((t) => tagFilters.includes(t))) return false;
      return true;
    });
    switch (sort) {
      case "name": return [...list].sort((a, b) => naturalCompare(a.diet.name, b.diet.name));
      case "calories": return [...list].sort((a, b) => b.totalKcal - a.totalKcal);
      case "protein": return [...list].sort((a, b) => b.totalP - a.totalP);
      default: return list;
    }
  }, [resolved, query, favOnly, importedOnly, tagFilters, slotFilters, sort]);

  const favCount = useMemo(() => diets.filter((d) => d.isFavorite).length, [diets]);

  const toggleExpand = useCallback((id: number) => {
    setExpandedIds((prev) => { const n = new Set(prev); if (n.has(id)) n.delete(id); else n.add(id); return n; });
  }, []);

  const reload = useCallback(async () => { setDiets(await listDiets()); }, []);

  const handleToggleFav = useCallback(async (diet: DietDto) => {
    if (diet.id == null) return;
    setDiets((prev) => prev.map((d) => d.id === diet.id ? { ...d, isFavorite: !d.isFavorite } : d));
    try { await toggleDietFavorite(diet.id); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  // Per-item Share toggle — direct REST by serverId (not synced). Imported copies can't be re-shared.
  const handleToggleShare = useCallback(async (diet: DietDto) => {
    if (!diet.serverId) return;
    setDiets((prev) => prev.map((d) => d.serverId === diet.serverId ? { ...d, isShared: !d.isShared } : d));
    try { await toggleDietShare(diet.serverId); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  const handleDelete = useCallback(async (diet: DietDto) => {
    if (diet.id == null) return;
    setDiets((prev) => prev.filter((d) => d.id !== diet.id));
    try { await deleteDiet(diet.id); } catch (e) { toastApiError(e); await reload(); }
  }, [reload]);

  const createTag = useCallback(async (name: string): Promise<TagDto | null> => {
    try {
      const tag = await createDietTag(name);
      setAvailableTags((prev) => prev.some((t) => t.id === tag.id) ? prev : [...prev, tag]);
      return tag;
    } catch { return null; }
  }, []);

  const openNew = useCallback(() => { setEditing(null); setBuilderOpen(true); }, []);
  const openEdit = useCallback((diet: DietDto) => { setEditing(diet); setBuilderOpen(true); }, []);
  const closeBuilder = useCallback(() => { setBuilderOpen(false); setEditing(null); }, []);

  const saveDiet = useCallback(async (name: string, entries: DietEntryInput[], tagIds: number[], notes?: string | null) => {
    if (!name.trim() || entries.length === 0) return;
    setSaving(true);
    try {
      const input = { name: name.trim(), entries, tagIds, notes: notes?.trim() || null };
      if (editing?.id != null) await updateDiet(editing.id, input);
      else await createDiet(input);
      await reload();
      closeBuilder();
    } catch (e) {
      setError(friendlyMessage(e));
    } finally {
      setSaving(false);
    }
  }, [editing, reload, closeBuilder]);

  return {
    diets: filtered, allDiets: resolved, totalCount: diets.length, favCount, meals, foods, foodsById, mealSummaries,
    availableTags, allTagNames, allSlotNames, loading, error,
    query, setQuery, sort, setSort, viewMode, setViewMode, favOnly, setFavOnly,
    importedOnly, setImportedOnly,
    tagFilters, toggleTagFilter, clearTagFilters,
    slotFilters, toggleSlotFilter, clearSlotFilters,
    expandedIds, toggleExpand,
    handleToggleFav, handleToggleShare, handleDelete, createTag,
    builderOpen, editing, openNew, openEdit, closeBuilder, saveDiet, saving,
  };
}
