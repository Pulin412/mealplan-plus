"use client";

import { useState, useEffect, useMemo, useCallback, useRef } from "react";
import { friendlyMessage } from "@/lib/api/errors";
import { listPlans, isoOf, type DayPlanDto } from "@/lib/api/plans";
import { listDiets, type DietDto } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods, type FoodDto } from "@/lib/api/foods";

// ── Aisle categories — mirrors the design's grocCats (keyword match + colour) ──
export interface GroceryCat { key: string; label: string; color: string; match: string[] }
export const GROCERY_CATS: GroceryCat[] = [
  { key: "produce", label: "Produce", color: "oklch(0.62 0.13 150)", match: ["banana", "blueberr", "broccoli", "apple", "spinach", "tomato", "lemon", "berr", "avocado", "greens", "fruit", "veg", "lettuce", "onion"] },
  { key: "protein", label: "Meat & protein", color: "oklch(0.58 0.14 25)", match: ["chicken", "salmon", "egg", "whey", "protein", "beef", "turkey", "fish", "tuna", "steak"] },
  { key: "dairy", label: "Dairy", color: "oklch(0.58 0.1 255)", match: ["yogurt", "cheese", "milk", "butter", "cream"] },
  { key: "pantry", label: "Pantry & grains", color: "oklch(0.62 0.1 75)", match: ["rice", "oatmeal", "oat", "almond", "olive oil", "honey", "granola", "quinoa", "farro", "bread", "peanut", "oil", "seed", "bar", "nut"] },
];
const OTHER: GroceryCat = { key: "other", label: "Other", color: "oklch(0.55 0.02 250)", match: [] };
export const CAT_ORDER = [...GROCERY_CATS, OTHER];

export function categoryOf(name: string): GroceryCat {
  const n = name.toLowerCase();
  return GROCERY_CATS.find((c) => c.match.some((m) => n.includes(m))) ?? OTHER;
}

/** One independently-checkable list row. An ingredient can be two rows: one bought, one to-buy. */
export interface GroceryRow { id: string; key: string; name: string; unit: string; qty: number; checked: boolean; cat: GroceryCat }

export type GroceryView = "all" | "remaining" | "bought";

interface SavedItem { key: string; name: string; unit: string; total: number; count: number }
export interface SavedGroceryList {
  id: string; name: string; dateKeys: string[]; items: SavedItem[];
  checked: Record<string, boolean>; days: number;
}

const STORE_KEY = "mp_grocery_lists";
const WORK_KEY = "mp_grocery_work";
const iso = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
const isoOfDate = (d: Date) => iso(d.getFullYear(), d.getMonth() + 1, d.getDate());
const MON = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

interface WorkRow { id: string; key: string; name: string; unit: string; qty: number; checked: boolean }
interface WorkState { selected?: string[]; activeId?: string | null; rows?: WorkRow[] }
function loadWork(): WorkState | null {
  if (typeof window === "undefined") return null;
  try { return JSON.parse(localStorage.getItem(WORK_KEY) || "null"); } catch { return null; }
}
function loadSaved(): SavedGroceryList[] {
  if (typeof window === "undefined") return [];
  try { return JSON.parse(localStorage.getItem(STORE_KEY) || "[]"); } catch { return []; }
}
function persistSaved(lists: SavedGroceryList[]) {
  if (typeof window !== "undefined") localStorage.setItem(STORE_KEY, JSON.stringify(lists));
}

function labelFor(k: string): string { const [, m, d] = k.split("-").map(Number); return `${MON[m - 1]} ${d}`; }
export function rangeLabel(keys: string[]): string {
  if (keys.length === 0) return "Tap to pick dates";
  return keys.length === 1 ? labelFor(keys[0]) : `${labelFor(keys[0])} – ${labelFor(keys[keys.length - 1])}`;
}

interface FoodTotal { name: string; unit: string; total: number }

/** Expand the selected days' planned diets into per-ingredient totals. Pure. */
function generateTotals(
  sel: Set<string>,
  plans: Record<string, DayPlanDto>,
  dietsById: Map<number, DietDto>,
  mealsById: Map<number, MealDto>,
  foodsById: Map<number, FoodDto>,
): Map<string, FoodTotal> {
  const map = new Map<string, FoodTotal>();
  const add = (name: string | undefined, qty: number, unit: string) => {
    const nm = (name ?? "Food").trim();
    const k = nm.toLowerCase() + "|" + unit;
    const cur = map.get(k);
    map.set(k, { name: nm, unit, total: (cur?.total ?? 0) + qty });
  };
  Array.from(sel).sort().forEach((key) => {
    const dietId = plans[key]?.dietId;
    if (dietId == null) return;
    const diet = dietsById.get(dietId);
    if (!diet) return;
    (diet.meals ?? []).forEach((dm) => {
      const meal = dm.mealId != null ? mealsById.get(dm.mealId) : undefined;
      (meal?.items ?? []).forEach((it) => add(it.foodId != null ? foodsById.get(it.foodId)?.name : undefined, it.quantity, it.unit));
    });
    (diet.foodItems ?? []).forEach((fi) => add(fi.foodId != null ? foodsById.get(fi.foodId)?.name : undefined, fi.quantity ?? 1, fi.unit));
  });
  return map;
}

/**
 * Reconcile existing rows against fresh totals: keep every checked (bought) row (capped to what the
 * plan now needs), and for each ingredient set one to-buy row to (total − already-bought). New
 * ingredients get a to-buy row; ingredients dropped from the plan keep only their bought rows.
 */
function reconcile(fresh: Map<string, FoodTotal>, existing: GroceryRow[]): GroceryRow[] {
  const byKey = new Map<string, GroceryRow[]>();
  existing.forEach((r) => { const a = byKey.get(r.key) ?? []; a.push(r); byKey.set(r.key, a); });
  const out: GroceryRow[] = [];
  fresh.forEach((ft, key) => {
    const ex = byKey.get(key) ?? [];
    const bought = Math.min(ex.filter((r) => r.checked).reduce((s, r) => s + r.qty, 0), ft.total);
    const remaining = Math.max(0, ft.total - bought);
    const cat = categoryOf(ft.name);
    if (bought > 0) out.push({ id: `${key}#b`, key, name: ft.name, unit: ft.unit, qty: bought, checked: true, cat });
    if (remaining > 0) out.push({ id: `${key}#t`, key, name: ft.name, unit: ft.unit, qty: remaining, checked: false, cat });
  });
  byKey.forEach((ex, key) => {
    if (!fresh.has(key)) {
      const bought = ex.filter((r) => r.checked).reduce((s, r) => s + r.qty, 0);
      if (bought > 0) { const f = ex[0]; out.push({ id: `${key}#b`, key, name: f.name, unit: f.unit, qty: bought, checked: true, cat: categoryOf(f.name) }); }
    }
  });
  return out.sort((a, b) => a.name.localeCompare(b.name));
}

export function useGroceries() {
  const today = useMemo(() => new Date(), []);
  const todayIso = isoOfDate(today);

  const [ym, setYm] = useState({ year: today.getFullYear(), month: today.getMonth() + 1 });
  const [selected, setSelected] = useState<Set<string>>(() => {
    const s = new Set<string>();
    for (let i = 0; i < 7; i++) { const d = new Date(today); d.setDate(today.getDate() + i); s.add(isoOfDate(d)); }
    return s;
  });
  const [calOpen, setCalOpen] = useState(false);
  const [view, setView] = useState<GroceryView>("all");
  const [liveRows, setLiveRows] = useState<GroceryRow[]>([]); // stable; only day-change / refresh rebuild
  const [savedLists, setSavedLists] = useState<SavedGroceryList[]>([]);
  const [activeId, setActiveId] = useState<string | null>(null);
  const [sheetSaved, setSheetSaved] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [plannedDates, setPlannedDates] = useState<Set<string>>(new Set());

  // Server data in refs so reconcile reads the latest without auto-rebuilding on every fetch.
  const dietsRef = useRef(new Map<number, DietDto>());
  const mealsRef = useRef(new Map<number, MealDto>());
  const foodsRef = useRef(new Map<number, FoodDto>());
  const plansRef = useRef<Record<string, DayPlanDto>>({});
  const liveRowsRef = useRef<GroceryRow[]>([]);
  liveRowsRef.current = liveRows;

  const [hydrated, setHydrated] = useState(false);
  const [dataReady, setDataReady] = useState(false);
  const initedRef = useRef(false);

  const regenerate = useCallback((sel: Set<string>) => {
    setLiveRows(reconcile(generateTotals(sel, plansRef.current, dietsRef.current, mealsRef.current, foodsRef.current), liveRowsRef.current));
  }, []);

  const loadLibrary = useCallback(async () => {
    try {
      const [ds, ms, fs] = await Promise.all([listDiets(), listMeals(), listFoods()]);
      const d = new Map<number, DietDto>(); ds.forEach((x) => x.id != null && d.set(x.id, x));
      const m = new Map<number, MealDto>(); ms.forEach((x) => x.id != null && m.set(x.id, x));
      const f = new Map<number, FoodDto>(); fs.forEach((x) => x.id != null && f.set(x.id, x));
      dietsRef.current = d; mealsRef.current = m; foodsRef.current = f;
    } catch (e) { setError(friendlyMessage(e)); }
  }, []);

  const loadPlansFor = useCallback(async (y: number, mo: number) => {
    const first = new Date(y, mo - 1, 1);
    const last = new Date(y, mo, 0);
    const t7 = new Date(today); t7.setDate(today.getDate() + 6);
    const lo = first < today ? isoOfDate(first) : todayIso;
    const hi = last > t7 ? isoOfDate(last) : isoOfDate(t7);
    try {
      const list = await listPlans(lo, hi);
      const merged = { ...plansRef.current };
      list.forEach((p) => { merged[isoOf(p.date)] = p; });
      plansRef.current = merged;
      setPlannedDates(new Set(Object.values(merged).filter((p) => p.dietId != null).map((p) => isoOf(p.date))));
    } catch (e) { setError(friendlyMessage(e)); }
  }, [today, todayIso]);

  // Restore working state (selection + rows) after mount.
  useEffect(() => {
    setSavedLists(loadSaved());
    const w = loadWork();
    if (w) {
      if (Array.isArray(w.selected) && w.selected.length) setSelected(new Set(w.selected));
      if (w.activeId === null || typeof w.activeId === "string") setActiveId(w.activeId);
      if (Array.isArray(w.rows)) setLiveRows(w.rows.map((r) => ({ ...r, cat: categoryOf(r.name) })));
    }
    setHydrated(true);
  }, []);

  // Persist working state (post-hydration).
  useEffect(() => {
    if (!hydrated || typeof window === "undefined") return;
    const rows: WorkRow[] = liveRows.map((r) => ({ id: r.id, key: r.key, name: r.name, unit: r.unit, qty: r.qty, checked: r.checked }));
    localStorage.setItem(WORK_KEY, JSON.stringify({ selected: Array.from(selected), activeId, rows } as WorkState));
  }, [hydrated, selected, activeId, liveRows]);

  useEffect(() => { loadLibrary().then(() => setDataReady(true)); }, [loadLibrary]);
  useEffect(() => { loadPlansFor(ym.year, ym.month); }, [ym, loadPlansFor]);

  // First-ever open (no restored rows): build once when data is ready. Never auto-rebuilds after.
  useEffect(() => {
    if (initedRef.current || !hydrated || !dataReady) return;
    initedRef.current = true;
    if (liveRows.length === 0 && !activeId) regenerate(selected);
  }, [hydrated, dataReady, liveRows.length, activeId, selected, regenerate]);

  /** Re-pull plan + library and reconcile: bought rows kept, to-buy rows set to the difference. */
  const refresh = useCallback(async () => {
    if (activeId) return;
    setRefreshing(true);
    await Promise.all([loadLibrary(), loadPlansFor(ym.year, ym.month)]);
    regenerate(selected);
    setRefreshing(false);
  }, [activeId, loadLibrary, loadPlansFor, ym, regenerate, selected]);

  const active = useMemo(() => savedLists.find((l) => l.id === activeId) ?? null, [savedLists, activeId]);
  const isSaved = active != null;

  const rows = useMemo<GroceryRow[]>(() => {
    if (active) {
      return active.items
        .map((it) => ({ id: it.key, key: it.key, name: it.name, unit: it.unit, qty: it.total, checked: active.checked[it.key] === true, cat: categoryOf(it.name) }))
        .sort((a, b) => a.name.localeCompare(b.name));
    }
    return liveRows;
  }, [active, liveRows]);

  const toBuy = useMemo(() => rows.filter((r) => !r.checked), [rows]);
  const bought = useMemo(() => rows.filter((r) => r.checked), [rows]);
  const dateKeys = isSaved ? (active?.dateKeys ?? []) : Array.from(selected).sort();

  // ── Date picking (a day change reconciles the list from cached data) ────────────
  const toggleCal = useCallback(() => setCalOpen((o) => !o), []);
  const prevMonth = useCallback(() => setYm(({ year, month }) => (month === 1 ? { year: year - 1, month: 12 } : { year, month: month - 1 })), []);
  const nextMonth = useCallback(() => setYm(({ year, month }) => (month === 12 ? { year: year + 1, month: 1 } : { year, month: month + 1 })), []);

  const toggleDay = useCallback((key: string) => {
    setSelected((prev) => {
      const s = new Set(prev); if (s.has(key)) s.delete(key); else s.add(key);
      regenerate(s);
      return s;
    });
  }, [regenerate]);

  const preset = useCallback((n: number) => {
    const s = new Set<string>();
    for (let i = 0; i < n; i++) { const d = new Date(today); d.setDate(today.getDate() + i); s.add(isoOfDate(d)); }
    setSelected(s); setYm({ year: today.getFullYear(), month: today.getMonth() + 1 });
    regenerate(s);
  }, [today, regenerate]);

  const clearDates = useCallback(() => { setSelected(new Set()); regenerate(new Set()); }, [regenerate]);

  // ── Checking (one row on/off; the ingredient's rows are consolidated) ───────────
  const toggleRow = useCallback((id: string) => {
    if (activeId) {
      const row = rows.find((r) => r.id === id);
      if (!row) return;
      setSavedLists((prev) => {
        const next = prev.map((l) => (l.id === activeId ? { ...l, checked: { ...l.checked, [row.key]: !l.checked[row.key] } } : l));
        persistSaved(next); return next;
      });
    } else {
      setLiveRows((prev) => {
        const row = prev.find((r) => r.id === id);
        if (!row) return prev;
        const key = row.key;
        const food = prev.filter((r) => r.key === key);
        let boughtQty = food.filter((r) => r.checked).reduce((s, r) => s + r.qty, 0);
        let toBuyQty = food.filter((r) => !r.checked).reduce((s, r) => s + r.qty, 0);
        if (row.checked) { boughtQty -= row.qty; toBuyQty += row.qty; } else { boughtQty += row.qty; toBuyQty -= row.qty; }
        const rebuilt: GroceryRow[] = [];
        if (boughtQty > 0) rebuilt.push({ id: `${key}#b`, key, name: row.name, unit: row.unit, qty: boughtQty, checked: true, cat: row.cat });
        if (toBuyQty > 0) rebuilt.push({ id: `${key}#t`, key, name: row.name, unit: row.unit, qty: toBuyQty, checked: false, cat: row.cat });
        return [...prev.filter((r) => r.key !== key), ...rebuilt].sort((a, b) => a.name.localeCompare(b.name));
      });
    }
  }, [activeId, rows]);

  const uncheckAll = useCallback(() => {
    if (activeId) {
      setSavedLists((prev) => { const next = prev.map((l) => (l.id === activeId ? { ...l, checked: {} } : l)); persistSaved(next); return next; });
    } else {
      setLiveRows((prev) => {
        const byKey = new Map<string, GroceryRow[]>();
        prev.forEach((r) => { const a = byKey.get(r.key) ?? []; a.push(r); byKey.set(r.key, a); });
        const out: GroceryRow[] = [];
        byKey.forEach((rs, key) => { const f = rs[0]; out.push({ id: `${key}#t`, key, name: f.name, unit: f.unit, qty: rs.reduce((s, r) => s + r.qty, 0), checked: false, cat: f.cat }); });
        return out.sort((a, b) => a.name.localeCompare(b.name));
      });
    }
  }, [activeId]);

  // ── Saved lists ────────────────────────────────────────────────────────────────
  const openSaved = useCallback(() => setSheetSaved(true), []);
  const closeSheet = useCallback(() => setSheetSaved(false), []);

  const saveList = useCallback(() => {
    const keys = Array.from(selected).sort();
    if (keys.length === 0 || liveRows.length === 0) return;
    const label = rangeLabel(keys);
    const byKey = new Map<string, GroceryRow[]>();
    liveRows.forEach((r) => { const a = byKey.get(r.key) ?? []; a.push(r); byKey.set(r.key, a); });
    const items: SavedItem[] = [];
    const chk: Record<string, boolean> = {};
    byKey.forEach((rs, key) => {
      const f = rs[0];
      items.push({ key, name: f.name, unit: f.unit, total: rs.reduce((s, r) => s + r.qty, 0), count: 1 });
      if (rs.every((r) => r.checked)) chk[key] = true;
    });
    const id = "gl" + Date.now();
    const list: SavedGroceryList = { id, name: `Groceries · ${label}`, dateKeys: keys, items, checked: chk, days: keys.length };
    setSavedLists((prev) => { const next = [list, ...prev]; persistSaved(next); return next; });
    setActiveId(id);
  }, [selected, liveRows]);

  const loadSavedList = useCallback((id: string) => { setActiveId(id); setSheetSaved(false); setView("all"); setCalOpen(false); }, []);
  const deleteSaved = useCallback((id: string) => {
    setSavedLists((prev) => { const next = prev.filter((l) => l.id !== id); persistSaved(next); return next; });
    setActiveId((cur) => (cur === id ? null : cur));
  }, []);
  const newList = useCallback(() => { setActiveId(null); setView("all"); }, []);

  return {
    today, todayIso, ym, selected, plannedDates, calOpen, view, setView, refreshing, refresh,
    rows, toBuy, bought, dateKeys, boughtCount: bought.length, total: rows.length,
    isSaved, activeName: active?.name ?? null, savedLists, activeId, sheetSaved, error,
    toggleCal, prevMonth, nextMonth, toggleDay, preset, clearDates,
    toggleRow, uncheckAll, openSaved, closeSheet, saveList, loadSavedList, deleteSaved, newList,
  };
}
