"use client";
import { useEffect, useState, useCallback, useMemo } from "react";
import { Plus, X, Search, ChevronDown, ChevronUp, Trash2, UtensilsCrossed } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import type { components } from "@/lib/api/types.generated";

type MealDto         = components["schemas"]["MealDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];
type FoodDto         = components["schemas"]["FoodDto"];

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
                <span className="text-text-primary">{f.name}
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
              const q = parseFloat(qty);
              if (!isNaN(q) && q > 0 && selected.id != null) {
                onAdd({ foodId: selected.id, quantity: q, unit: "GRAM" });
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
          <p className="text-xs text-text-muted mt-0.5">{items.length} food{items.length !== 1 ? "s" : ""}</p>
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
                    <span className="flex-1 text-text-primary truncate">{food?.name ?? `Food #${item.foodId}`}</span>
                    <span className="text-xs text-text-muted shrink-0">{item.quantity}g{kcal ? ` · ${kcal} kcal` : ""}</span>
                    <button onClick={() => removeFood(idx)} disabled={saving}
                      className="p-1 rounded-lg hover:bg-bg-page transition-colors shrink-0">
                      <X className="h-3.5 w-3.5 text-text-muted" />
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {addingFood
            ? <FoodPicker foods={foods} onAdd={addFood} onClose={() => setAddingFood(false)} />
            : (
              <button
                onClick={() => setAddingFood(true)} disabled={saving}
                className="w-full h-9 rounded-xl border border-dashed border-divider text-sm text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-1"
              >
                <Plus className="h-3.5 w-3.5" />Add food
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

// ── Create meal sheet ─────────────────────────────────────────────────────────
function CreateMealSheet({ onCreated, onClose }: {
  onCreated: (m: MealDto) => void;
  onClose: () => void;
}) {
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      const m = await api.post<MealDto>("/api/v1/meals", { name: name.trim(), items: [] });
      onCreated(m);
      onClose();
    } finally { setBusy(false); }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />
      <div className="fixed bottom-0 inset-x-0 z-50 bg-bg-card rounded-t-3xl shadow-xl animate-in slide-in-from-bottom duration-200">
        <div className="flex justify-center pt-3 pb-1">
          <div className="w-9 h-1 rounded-full bg-text-muted/30" />
        </div>
        <div className="px-5 pb-10 pt-2">
          <p className="text-[17px] font-semibold text-text-primary mb-5">New meal</p>
          <form onSubmit={submit} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-text-secondary">Name *</label>
              <input
                value={name} onChange={(e) => setName(e.target.value)} autoFocus
                placeholder="e.g. High protein breakfast"
                className="w-full h-11 px-3 rounded-xl border border-divider bg-bg-page text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
              />
            </div>
            <button
              type="submit" disabled={busy || !name.trim()}
              className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors mt-2"
            >
              {busy ? "Creating…" : "Create meal"}
            </button>
          </form>
        </div>
      </div>
    </>
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
          <Plus className="h-4 w-4" />New
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
        <div className="space-y-3">
          {filtered.map((meal) => (
            <MealCard
              key={meal.id}
              meal={meal}
              foods={foods}
              onUpdate={(m) => setMeals((prev) => prev.map((x) => x.id === m.id ? m : x))}
              onDelete={() => meal.id != null && handleDelete(meal.id)}
            />
          ))}
        </div>
      )}

      {showCreate && (
        <CreateMealSheet
          onCreated={(m) => setMeals((prev) => [m, ...prev])}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}
