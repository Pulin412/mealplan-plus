"use client";
import { useEffect, useState, useCallback, useMemo } from "react";
import { X, Search, ChevronDown, ChevronUp, Trash2, UtensilsCrossed } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import { CreateMealModal } from "./CreateMealModal";
import type { components } from "@/lib/api/types.generated";

type MealDto         = components["schemas"]["MealDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];
type FoodDto         = components["schemas"]["FoodDto"];

const PAGE_SIZE = 12;

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
function MealCard({ meal, foods, onUpdate, onDelete }: {
  meal: MealDto;
  foods: FoodDto[];
  onUpdate: (m: MealDto) => void;
  onDelete: () => void;
}) {
  const [expanded,   setExpanded]   = useState(false);
  const [addingFood, setAddingFood] = useState(false);
  const [saving,     setSaving]     = useState(false);

  const items = meal.items ?? [];
  const totalKcal = items.reduce((sum, item) => {
    const food = foods.find((f) => f.id === item.foodId);
    return sum + (food ? Math.round(food.caloriesPer100 * item.quantity / 100) : 0);
  }, 0);

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
        className="w-full text-left px-4 pt-3.5 pb-3 flex items-center gap-3"
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="flex-1 min-w-0">
          <p className="text-[15px] font-semibold text-text-primary truncate">{meal.name}</p>
          <div className="flex items-center gap-2 mt-0.5">
            <p className="text-xs text-text-muted">{items.length} food{items.length !== 1 ? "s" : ""}</p>
            {totalKcal > 0 && (
              <span className="text-xs font-medium text-text-muted">· {totalKcal} kcal</span>
            )}
          </div>
        </div>
        {expanded
          ? <ChevronUp className="h-4 w-4 text-text-muted shrink-0" />
          : <ChevronDown className="h-4 w-4 text-text-muted shrink-0" />}
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
              <span className="text-[11px] font-semibold text-text-secondary">
                Total: {totalKcal} kcal
              </span>
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
  const [meals,      setMeals]      = useState<MealDto[]>([]);
  const [foods,      setFoods]      = useState<FoodDto[]>([]);
  const [loading,    setLoading]    = useState(true);
  const [error,      setError]      = useState<string | null>(null);
  const [query,      setQuery]      = useState("");
  const [showCreate, setShowCreate] = useState(false);
  const [visible,    setVisible]    = useState(PAGE_SIZE);

  const loadData = useCallback(async () => {
    if (!user) return;
    try {
      const [m, f] = await Promise.all([
        api.get<MealDto[]>("/api/v1/meals"),
        api.get<FoodDto[]>("/api/v1/foods"),
      ]);
      setMeals(m); setFoods(f);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = useMemo(() => {
    if (!query.trim()) return meals;
    const q = query.toLowerCase();
    return meals.filter((m) => m.name.toLowerCase().includes(q));
  }, [meals, query]);

  // Reset pagination when filter changes
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

      {filtered.length === 0 ? (
        <EmptyState
          icon={UtensilsCrossed}
          title={query ? "No meals found" : "No meals yet"}
          subtitle={query ? `No results for "${query}"` : "Create your first meal"}
          action={!query ? { label: "Create meal", onClick: () => setShowCreate(true) } : undefined}
        />
      ) : (
        <>
          <div className="space-y-3">
            {shown.map((meal) => (
              <MealCard
                key={meal.id}
                meal={meal}
                foods={foods}
                onUpdate={(m) => setMeals((prev) => prev.map((x) => x.id === m.id ? m : x))}
                onDelete={() => meal.id != null && handleDelete(meal.id)}
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
