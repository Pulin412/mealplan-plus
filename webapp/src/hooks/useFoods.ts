"use client";

import { useState, useEffect, useMemo, useCallback } from "react";
import { listFoods, createFood, deleteFood, toggleFavorite, searchFoodsOnline, type FoodDto } from "@/lib/api/foods";
import { createScannedFood, type ScannedProduct } from "@/lib/api/barcode";
import type { FoodSort, FoodViewMode, FoodSheet, ManualFoodForm } from "@/types/food";

const EMPTY_FORM: ManualFoodForm = { name: "", servingLabel: "", kcal: "", protein: "", carbs: "", fat: "", category: "" };

export function useFoods() {
  const [foods, setFoods]               = useState<FoodDto[]>([]);
  const [loading, setLoading]           = useState(true);
  const [error, setError]               = useState<string | null>(null);

  // toolbar
  const [query, setQuery]               = useState("");
  const [sort, setSort]                 = useState<FoodSort>("recent");
  const [viewMode, setViewMode]         = useState<FoodViewMode>("list");
  const [favOnly, setFavOnly]           = useState(false);
  const [sortOpen, setSortOpen]         = useState(false);
  const [categoryFilter, setCategoryFilter] = useState<string | null>(null);

  // row state
  const [expandedIds, setExpandedIds]   = useState<Set<number>>(new Set());

  // FAB
  const [fanOpen, setFanOpen]           = useState(false);
  const [activeSheet, setActiveSheet]   = useState<FoodSheet>(null);

  // manual form
  const [form, setForm]                 = useState<ManualFoodForm>(EMPTY_FORM);
  const [saving, setSaving]             = useState(false);

  // online search
  const [onlineQuery, setOnlineQuery]   = useState("");
  const [onlineResults, setOnlineResults] = useState<FoodDto[]>([]);
  const [onlineLoading, setOnlineLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    listFoods()
      .then(setFoods)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  const filtered = useMemo(() => {
    let result = foods;
    if (favOnly) result = result.filter((f) => f.isFavorite);
    if (categoryFilter) result = result.filter((f) => f.category === categoryFilter);
    if (query.trim()) {
      const q = query.toLowerCase();
      result = result.filter((f) =>
        f.name.toLowerCase().includes(q) || (f.brand ?? "").toLowerCase().includes(q)
      );
    }
    switch (sort) {
      case "name":     return [...result].sort((a, b) => a.name.localeCompare(b.name));
      case "calories": return [...result].sort((a, b) => b.caloriesPer100 - a.caloriesPer100);
      case "protein":  return [...result].sort((a, b) => b.proteinPer100 - a.proteinPer100);
      default:         return result;
    }
  }, [foods, favOnly, categoryFilter, query, sort]);

  const favCount = useMemo(() => foods.filter((f) => f.isFavorite).length, [foods]);

  // Distinct categories actually present — drives the filter chips.
  const usedCategories = useMemo(
    () => Array.from(new Set(foods.map((f) => f.category).filter((c): c is string => !!c))).sort(),
    [foods]
  );

  const toggleExpand = useCallback((id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      return next;
    });
  }, []);

  const handleToggleFav = useCallback(async (food: FoodDto) => {
    if (food.id == null) return;
    setFoods((prev) => prev.map((f) => f.id === food.id ? { ...f, isFavorite: !f.isFavorite } : f));
    try {
      const updated = await toggleFavorite(food.id);
      setFoods((prev) => prev.map((f) => f.id === updated.id ? updated : f));
    } catch {
      setFoods((prev) => prev.map((f) => f.id === food.id ? food : f));
    }
  }, []);

  const handleDelete = useCallback(async (food: FoodDto) => {
    if (food.id == null) return;
    setFoods((prev) => prev.filter((f) => f.id !== food.id));
    try {
      await deleteFood(food.id);
    } catch {
      setFoods((prev) => [food, ...prev]);
    }
  }, []);

  const openSheet = useCallback((sheet: FoodSheet) => {
    setFanOpen(false);
    setActiveSheet(sheet);
  }, []);

  const closeSheet = useCallback(() => {
    setActiveSheet(null);
    setForm(EMPTY_FORM);
    setOnlineQuery("");
    setOnlineResults([]);
  }, []);

  const updateForm = useCallback((field: keyof ManualFoodForm, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
  }, []);

  const isSaveEnabled = form.name.trim() !== "" && form.kcal.trim() !== "";

  const saveManual = useCallback(async () => {
    if (!isSaveEnabled) return;
    setSaving(true);
    try {
      const created = await createFood(form);
      setFoods((prev) => [created, ...prev]);
      closeSheet();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save food");
    } finally {
      setSaving(false);
    }
  }, [form, isSaveEnabled, closeSheet]);

  const runOnlineSearch = useCallback(async () => {
    if (!onlineQuery.trim()) return;
    setOnlineLoading(true);
    try {
      const results = await searchFoodsOnline(onlineQuery);
      setOnlineResults(results);
    } catch {
      setOnlineResults([]);
    } finally {
      setOnlineLoading(false);
    }
  }, [onlineQuery]);

  // Online results now come from Open Food Facts (via the backend proxy) and have no id, so adding
  // one creates it in the user's foods.
  const addOnlineFood = useCallback(async (food: FoodDto) => {
    setSaving(true);
    try {
      const created = await createScannedFood({
        name: food.name,
        brand: food.brand ?? null,
        barcode: food.barcode ?? "",
        kcal: food.caloriesPer100,
        protein: food.proteinPer100,
        carbs: food.carbsPer100,
        fat: food.fatPer100,
      });
      setFoods((prev) => [created, ...prev]);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to add food");
    } finally {
      setSaving(false);
    }
  }, []);

  /** Persist a scanned Open Food Facts product as a new food (it doesn't exist server-side yet). */
  const addScannedFood = useCallback(async (product: ScannedProduct) => {
    setSaving(true);
    try {
      const created = await createScannedFood(product);
      setFoods((prev) => [created, ...prev]);
      closeSheet();
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to add food");
    } finally {
      setSaving(false);
    }
  }, [closeSheet]);

  return {
    foods: filtered, totalCount: foods.length, favCount,
    loading, error,
    query, setQuery,
    sort, setSort, sortOpen, setSortOpen,
    viewMode, setViewMode,
    favOnly, setFavOnly,
    categoryFilter, setCategoryFilter, usedCategories,
    expandedIds, toggleExpand,
    handleToggleFav, handleDelete,
    fanOpen, setFanOpen,
    activeSheet, openSheet, closeSheet,
    form, updateForm, isSaveEnabled, saving, saveManual,
    onlineQuery, setOnlineQuery, onlineResults, onlineLoading, runOnlineSearch, addOnlineFood,
    addScannedFood,
  };
}
