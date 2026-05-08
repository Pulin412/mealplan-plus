"use client";
import { useEffect, useState, useCallback, useMemo } from "react";
import { useRouter } from "next/navigation";
import { X, Search, ChevronDown, ChevronUp, Trash2, UtensilsCrossed, ExternalLink } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import { CreateMealModal } from "./CreateMealModal";
import { PREDEFINED_SLOTS, PREDEFINED_SLOT_COLORS } from "@/lib/utils";
import type { components } from "@/lib/api/types.generated";

type MealDto         = components["schemas"]["MealDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type DietDto         = components["schemas"]["DietDto"];

const PAGE_SIZE = 12;
const SLOT_COLORS = PREDEFINED_SLOT_COLORS;

interface DietAssoc { dietId: number; dietName: string; slot: string; }

// ── Filter chip ───────────────────────────────────────────────────────────────
function FilterChip({ label, active, onClick, colorBg, colorText }: {
  label: string; active: boolean; onClick: () => void;
  colorBg?: string; colorText?: string;
}) {
  if (colorBg && colorText) {
    return (
      <button
        onClick={onClick}
        className="shrink-0 px-3 h-8 rounded-full text-[12px] font-medium border transition-colors"
        style={{
          background:  active ? colorBg  : "transparent",
          borderColor: active ? colorText : "var(--color-divider)",
          color:       active ? colorText : "var(--color-text-secondary)",
        }}
      >
        {label}
      </button>
    );
  }
  return (
    <button
      onClick={onClick}
      className={`shrink-0 px-3 h-8 rounded-full text-[12px] font-medium transition-colors border ${
        active
          ? "bg-green text-white border-green"
          : "bg-bg-card border-divider text-text-secondary"
      }`}
    >
      {label}
    </button>
  );
}

// ── Inline food picker ────────────────────────────────────────────────────────
function FoodPicker({ foods, onAdd, onClose }: {
  foods: FoodDto[];
  onAdd: (item: MealFoodItemDto) => void;
  onClose: () => void;
}) {
  const [q,        setQ]        = useState("");
  const [qty,      setQty]      = useState("100");
  const [selected, setSelected] = useState<FoodDto | null>(null);

  const results = q.length >= 2
    ? foods.filter((f) => f.name.toLowerCase().includes(q.toLowerCase())).slice(0, 8)
    : [];

  return (
    <div className="mt-2 p-3 rounded-xl bg-bg-page border border-divider space-y-2">
      <div className="flex items-center gap-2">
        <Search className="h-4 w-4 text-text-muted shrink-0" />
        <input
          autoFocus value={q} onChange={(e) => { setQ(e.target.value); setSelected(null); }}
          placeholder="Search foods…"
          className="flex-1 bg-transparent text-sm text-text-primary placeholder:text-text-muted focus:outline-none"
        />
        <button onClick={onClose}><X className="h-4 w-4 text-text-muted" /></button>
      </div>

      {!selected && results.length > 0 && (
        <ul className="space-y-0.5 max-h-44 overflow-y-auto">
          {results.map((f) => (
            <li key={f.id}>
              <button
                onClick={() => setSelected(f)}
                className="w-full text-left text-sm px-2 py-1.5 rounded-lg hover:bg-bg-card transition-colors flex justify-between"
              >
                <span className="text-text-primary">
                  {f.name}
                  {f.brand && <span className="text-text-muted"> · {f.brand}</span>}
                </span>
                <span className="text-text-muted text-xs">{f.caloriesPer100} kcal/100g</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {selected && (
        <div className="flex items-center gap-2">
          <span className="text-sm font-medium text-text-primary flex-1 truncate">{selected.name}</span>
          <input
            type="number" value={qty} onChange={(e) => setQty(e.target.value)}
            className="w-20 h-8 px-2 rounded-lg border border-divider bg-bg-card text-sm text-center text-text-primary focus:outline-none"
            min={1}
          />
          <span className="text-xs text-text-muted">g</span>
          <button
            onClick={() => {
              const g = parseFloat(qty);
              if (!isNaN(g) && g > 0 && selected.id != null) {
                onAdd({ foodId: selected.id, quantity: g, unit: "GRAM" } as MealFoodItemDto);
                onClose();
              }
            }}
            className="px-3 h-8 rounded-lg bg-green text-white text-sm font-medium"
          >
            Add
          </button>
          <button onClick={() => setSelected(null)} className="text-text-muted text-sm">Back</button>
        </div>
      )}
    </div>
  );
}

// ── Meal card ─────────────────────────────────────────────────────────────────
function MealCard({ meal, foods, dietAssocs, onUpdate, onDelete, onNavigateToDiet }: {
  meal: MealDto;
  foods: FoodDto[];
  dietAssocs: DietAssoc[];
  onUpdate: (m: MealDto) => void;
  onDelete: () => void;
  onNavigateToDiet: (dietId: number) => void;
}) {
  const [expanded,   setExpanded]   = useState(false);
  const [addingFood, setAddingFood] = useState(false);
  const [saving,     setSaving]     = useState(false);

  const items = meal.items ?? [];
  const totalKcal = items.reduce((sum, item) => {
    const food = foods.find((f) => f.id === item.foodId);
    return sum + (food ? Math.round(food.caloriesPer100 * item.quantity / 100) : 0);
  }, 0);

  const uniqueDietAssocs = useMemo(() => {
    const seen = new Set<string>();
    return dietAssocs.filter((a) => {
      const key = `${a.dietId}-${a.slot}`;
      if (seen.has(key)) return false;
      seen.add(key); return true;
    });
  }, [dietAssocs]);

  const removeFood = async (idx: number) => {
    setSaving(true);
    try {
      const updated = await api.put<MealDto>(`/api/v1/meals/${meal.id}`, {
        ...meal, items: items.filter((_, i) => i !== idx),
      });
      onUpdate(updated);
    } finally { setSaving(false); }
  };

  const addFood = async (item: MealFoodItemDto) => {
    setSaving(true);
    try {
      const updated = await api.put<MealDto>(`/api/v1/meals/${meal.id}`, {
        ...meal, items: [...items, item],
      });
      onUpdate(updated);
    } finally { setSaving(false); }
  };

  return (
    <div className="bg-bg-card rounded-2xl border border-divider overflow-hidden">
      <button
        className="w-full text-left px-4 pt-3.5 pb-3 flex items-start gap-3"
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="flex-1 min-w-0">
          <p className="text-[15px] font-semibold text-text-primary truncate">{meal.name}</p>
          <div className="flex items-center gap-2 mt-0.5 flex-wrap">
            <p className="text-xs text-text-muted">{items.length} food{items.length !== 1 ? "s" : ""}</p>
            {totalKcal > 0 && (
              <span className="text-xs text-text-muted">· {totalKcal} kcal</span>
            )}
          </div>

          {uniqueDietAssocs.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-1.5">
              {uniqueDietAssocs.slice(0, 3).map((assoc, i) => {
                const c = SLOT_COLORS[assoc.slot] ?? { bg: "#F0F0F5", text: "#666" };
                return (
                  <button
                    key={i}
                    onClick={(e) => {
                      e.stopPropagation();
                      onNavigateToDiet(assoc.dietId);
                    }}
                    className="inline-flex items-center gap-1 text-[10px] font-medium px-2 py-0.5 rounded-full hover:opacity-80 transition-opacity"
                    style={{ background: c.bg, color: c.text }}
                  >
                    {assoc.dietName}
                    <span className="opacity-70">· {assoc.slot}</span>
                    <ExternalLink size={9} className="opacity-60" />
                  </button>
                );
              })}
              {uniqueDietAssocs.length > 3 && (
                <span className="text-[10px] text-text-muted px-1.5 py-0.5 rounded-full border border-divider">
                  +{uniqueDietAssocs.length - 3} more
                </span>
              )}
            </div>
          )}
        </div>
        {expanded
          ? <ChevronUp className="h-4 w-4 text-text-muted shrink-0 mt-1" />
          : <ChevronDown className="h-4 w-4 text-text-muted shrink-0 mt-1" />}
      </button>

      {expanded && (
        <div className="px-4 pb-4 pt-0">
          <div className="h-px bg-divider mb-3" />

          {items.length === 0 ? (
            <p className="text-xs text-text-muted mb-2">No foods yet.</p>
          ) : (
            <ul className="space-y-1.5 mb-3">
              {items.map((item, idx) => {
                const food = foods.find((f) => f.id === item.foodId);
                const kcal = food ? Math.round(food.caloriesPer100 * item.quantity / 100) : null;
                return (
                  <li key={idx} className="flex items-center gap-2 text-sm">
                    <span className="w-1.5 h-1.5 rounded-full bg-text-muted/40 shrink-0" />
                    <span className="flex-1 text-text-primary truncate">{food?.name ?? `Food #${item.foodId}`}</span>
                    <span className="text-xs text-text-muted shrink-0">
                      {item.quantity}g{kcal ? ` · ${kcal} kcal` : ""}
                    </span>
                    <button
                      onClick={() => removeFood(idx)} disabled={saving}
                      className="p-1 rounded-lg hover:bg-bg-page transition-colors shrink-0"
                    >
                      <X className="h-3.5 w-3.5 text-text-muted" />
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {items.length > 0 && (
            <div className="flex justify-end mb-2">
              <span className="text-[11px] font-semibold text-text-secondary">Total: {totalKcal} kcal</span>
            </div>
          )}

          {addingFood
            ? <FoodPicker foods={foods} onAdd={addFood} onClose={() => setAddingFood(false)} />
            : (
              <button
                onClick={() => setAddingFood(true)} disabled={saving}
                className="w-full h-9 rounded-xl border border-dashed border-divider text-sm text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-1"
              >
                <X className="h-3.5 w-3.5 rotate-45" />Add food
              </button>
            )}

          <button
            onClick={onDelete}
            className="flex items-center gap-1.5 px-3 h-8 rounded-lg border border-divider text-xs text-red-500 hover:bg-red-50 transition-colors mt-3 ml-auto"
          >
            <Trash2 className="h-3.5 w-3.5" />Delete
          </button>
        </div>
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function MealsPage() {
  const { user } = useAuth();
  const router = useRouter();

  const [meals,      setMeals]      = useState<MealDto[]>([]);
  const [foods,      setFoods]      = useState<FoodDto[]>([]);
  const [diets,      setDiets]      = useState<DietDto[]>([]);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState<string | null>(null);
  const [query,      setQuery]      = useState("");
  const [dietFilter, setDietFilter] = useState<number | null>(null);
  const [slotFilter, setSlotFilter] = useState<string | null>(null);
  const [showCreate, setShowCreate] = useState(false);
  const [visible,    setVisible]    = useState(PAGE_SIZE);

  const loadData = useCallback(async () => {
    if (!user) return;
    try {
      const [m, f, d] = await Promise.all([
        api.get<MealDto[]>("/api/v1/meals"),
        api.get<FoodDto[]>("/api/v1/foods"),
        api.get<DietDto[]>("/api/v1/diets"),
      ]);
      setMeals(m); setFoods(f); setDiets(d);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  // mealId → [{dietId, dietName, slot}] reverse index
  const mealDietMap = useMemo(() => {
    const map = new Map<number, DietAssoc[]>();
    for (const diet of diets) {
      if (diet.id == null) continue;
      for (const dm of diet.meals ?? []) {
        if (dm.mealId == null) continue;
        if (!map.has(dm.mealId)) map.set(dm.mealId, []);
        map.get(dm.mealId)!.push({ dietId: diet.id, dietName: diet.name, slot: dm.slot });
      }
    }
    return map;
  }, [diets]);

  const dietsWithMeals = useMemo(() =>
    diets.filter((d) => (d.meals ?? []).length > 0),
    [diets]
  );

  // Only show slot chips for slots actually in use (in predefined order)
  const slotsInUse = useMemo(() => {
    const set = new Set<string>();
    mealDietMap.forEach((assocs) => {
      assocs.forEach((a) => { if (a.slot) set.add(a.slot); });
    });
    return PREDEFINED_SLOTS.filter((s) => set.has(s));
  }, [mealDietMap]);

  const filtered = useMemo(() => {
    let list = meals;
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((m) => m.name.toLowerCase().includes(q));
    }
    if (dietFilter != null) {
      list = list.filter((m) =>
        m.id != null && (mealDietMap.get(m.id) ?? []).some((a) => a.dietId === dietFilter)
      );
    }
    if (slotFilter != null) {
      list = list.filter((m) =>
        m.id != null && (mealDietMap.get(m.id) ?? []).some((a) => a.slot === slotFilter)
      );
    }
    return list;
  }, [meals, query, dietFilter, slotFilter, mealDietMap]);

  useEffect(() => { setVisible(PAGE_SIZE); }, [filtered]);

  const shown   = filtered.slice(0, visible);
  const hasMore = visible < filtered.length;

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this meal?")) return;
    try {
      await api.delete(`/api/v1/meals/${id}`);
      setMeals((prev) => prev.filter((m) => m.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  const navigateToDiet = (dietId: number) => {
    router.push(`/diets?dietId=${dietId}`);
  };

  const hasActiveFilter = dietFilter != null || slotFilter != null;

  if (loading) return (
    <div className="space-y-3">
      <div className="h-8 w-32 rounded-lg bg-bg-card" />
      <div className="h-10 w-full rounded-xl bg-bg-card" />
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-20 w-full rounded-2xl" />
      ))}
    </div>
  );

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-bold text-text-primary">Meals</h1>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1.5 px-3 h-9 rounded-xl bg-green text-white text-sm font-medium hover:bg-green/90 transition-colors"
        >
          <X className="h-4 w-4 rotate-45" />New
        </button>
      </div>

      {error && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</p>}

      <SearchBar value={query} onChange={setQuery} placeholder="Search meals…" />

      {/* Slot filter row (only slots in use) */}
      {slotsInUse.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
          <FilterChip
            label={`All (${meals.length})`}
            active={!hasActiveFilter}
            onClick={() => { setDietFilter(null); setSlotFilter(null); }}
          />
          {slotsInUse.map((s) => {
            const c = SLOT_COLORS[s] ?? { bg: "#F0F0F5", text: "#666" };
            return (
              <FilterChip
                key={s} label={s}
                active={slotFilter === s}
                colorBg={c.bg} colorText={c.text}
                onClick={() => setSlotFilter(slotFilter === s ? null : s)}
              />
            );
          })}
        </div>
      )}

      {/* Diet filter row (only shown if diets have meals) */}
      {dietsWithMeals.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
          <span className="shrink-0 text-[11px] font-semibold text-text-muted self-center pr-1">In diet:</span>
          {dietsWithMeals.map((d) => (
            <FilterChip
              key={d.id} label={d.name}
              active={dietFilter === d.id}
              onClick={() => setDietFilter(dietFilter === d.id ? null : (d.id ?? null))}
            />
          ))}
        </div>
      )}

      {filtered.length === 0 ? (
        <EmptyState
          icon={UtensilsCrossed}
          title={query || hasActiveFilter ? "No meals found" : "No meals yet"}
          subtitle={
            query ? `No results for "${query}"` :
            dietFilter != null ? "No meals in this diet" :
            slotFilter != null ? `No meals in ${slotFilter} slot` :
            "Create your first meal"
          }
          action={!query && !hasActiveFilter ? { label: "Create meal", onClick: () => setShowCreate(true) } : undefined}
        />
      ) : (
        <>
          <div className="space-y-3">
            {shown.map((meal) => (
              <MealCard
                key={meal.id}
                meal={meal}
                foods={foods}
                dietAssocs={meal.id != null ? (mealDietMap.get(meal.id) ?? []) : []}
                onUpdate={(m) => setMeals((prev) => prev.map((x) => x.id === m.id ? m : x))}
                onDelete={() => meal.id != null && handleDelete(meal.id)}
                onNavigateToDiet={navigateToDiet}
              />
            ))}
          </div>

          {hasMore && (
            <button
              onClick={() => setVisible((v) => v + PAGE_SIZE)}
              className="w-full h-10 rounded-xl border border-divider text-sm text-text-muted hover:bg-bg-card transition-colors"
            >
              Load more ({filtered.length - visible} remaining)
            </button>
          )}
        </>
      )}

      {showCreate && (
        <CreateMealModal
          foods={foods}
          onCreated={(m) => setMeals((prev) => [m, ...prev])}
          onClose={() => setShowCreate(false)}
        />
      )}

    </div>
  );
}
