"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback, useMemo } from "react";
import {
  ChevronDown, ChevronUp, Trash2, Plus, Copy, Star, Salad, X,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import type { components } from "@/lib/api/types.generated";

type DietDto     = components["schemas"]["DietDto"];
type MealDto     = components["schemas"]["MealDto"];
type DietMealDto = components["schemas"]["DietMealDto"];

const SLOTS = ["Breakfast", "Lunch", "Dinner", "Snack"] as const;
type Slot = typeof SLOTS[number];

const DAY_NAMES: Record<number, string> = {
  0: "Any day", 1: "Mon", 2: "Tue", 3: "Wed",
  4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun",
};

const SLOT_COLORS: Record<string, { bg: string; text: string }> = {
  Breakfast: { bg: "#FFF8E6", text: "#D97706" },
  Lunch:     { bg: "#E8F5EE", text: "#2E7D52" },
  Dinner:    { bg: "#F3EEFF", text: "#7C3AED" },
  Snack:     { bg: "#FFF0F0", text: "#DC2626" },
};

function useFavorites(key: string) {
  const [favs, setFavs] = useState<Set<number>>(() => {
    try {
      const stored = localStorage.getItem(key);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch { return new Set(); }
  });
  const toggle = useCallback((id: number) => {
    setFavs((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      try { localStorage.setItem(key, JSON.stringify([...next])); } catch {}
      return next;
    });
  }, [key]);
  return { favs, toggle };
}

// ── Create diet modal ─────────────────────────────────────────────────────────
function CreateDietSheet({ onCreated, onClose }: {
  onCreated: (d: DietDto) => void;
  onClose: () => void;
}) {
  const [name, setName]   = useState("");
  const [desc, setDesc]   = useState("");
  const [cals, setCals]   = useState("");
  const [busy, setBusy]   = useState(false);
  const [err,  setErr]    = useState<string | null>(null);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return;
    setBusy(true);
    try {
      const d = await api.post<DietDto>("/api/v1/diets", {
        name: name.trim(),
        description: desc.trim() || undefined,
        targetCalories: cals ? parseFloat(cals) : undefined,
        meals: [],
      });
      onCreated(d);
      onClose();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Failed to create");
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
          <p className="text-[17px] font-semibold text-text-primary mb-5">New diet</p>
          {err && <p className="text-sm text-red-600 mb-3">{err}</p>}
          <form onSubmit={submit} className="space-y-4">
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-text-secondary">Name *</label>
              <input
                value={name} onChange={(e) => setName(e.target.value)} autoFocus
                placeholder="e.g. High protein week"
                className="w-full h-11 px-3 rounded-xl border border-divider bg-bg-page text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-text-secondary">Description</label>
              <input
                value={desc} onChange={(e) => setDesc(e.target.value)}
                placeholder="Optional"
                className="w-full h-11 px-3 rounded-xl border border-divider bg-bg-page text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
              />
            </div>
            <div className="space-y-1.5">
              <label className="text-xs font-medium text-text-secondary">Target calories</label>
              <input
                type="number" value={cals} onChange={(e) => setCals(e.target.value)}
                placeholder="e.g. 2000"
                className="w-full h-11 px-3 rounded-xl border border-divider bg-bg-page text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
              />
            </div>
            <button
              type="submit" disabled={busy || !name.trim()}
              className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors mt-2"
            >
              {busy ? "Creating…" : "Create diet"}
            </button>
          </form>
        </div>
      </div>
    </>
  );
}

// ── Assign meal panel (inside expanded card) ──────────────────────────────────
function AssignMealPanel({ diet, meals, onAssigned, onClose }: {
  diet: DietDto;
  meals: MealDto[];
  onAssigned: (d: DietDto) => void;
  onClose: () => void;
}) {
  const [mealId, setMealId] = useState("");
  const [slot,   setSlot]   = useState<Slot>("Breakfast");
  const [day,    setDay]    = useState("0");
  const [busy,   setBusy]   = useState(false);

  const submit = async () => {
    if (!mealId) return;
    setBusy(true);
    try {
      const assignment: DietMealDto = {
        mealId: parseInt(mealId), slot, dayOfWeek: parseInt(day),
      };
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet,
        meals: [...(diet.meals ?? []), assignment],
      });
      onAssigned(updated);
      onClose();
    } finally { setBusy(false); }
  };

  const selectCls = "w-full h-9 rounded-lg border border-divider bg-bg-page px-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30";

  return (
    <div className="mt-3 p-3 rounded-xl bg-bg-page border border-divider space-y-3">
      <p className="text-xs font-semibold text-text-secondary">Add meal to this diet</p>
      <select value={mealId} onChange={(e) => setMealId(e.target.value)} className={selectCls}>
        <option value="">Select a meal…</option>
        {meals.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
      </select>
      <div className="grid grid-cols-2 gap-2">
        <select value={slot} onChange={(e) => setSlot(e.target.value as Slot)} className={selectCls}>
          {SLOTS.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={day} onChange={(e) => setDay(e.target.value)} className={selectCls}>
          {Object.entries(DAY_NAMES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      <div className="flex gap-2">
        <button
          onClick={submit} disabled={busy || !mealId}
          className="flex-1 h-9 rounded-xl bg-green text-white text-sm font-medium disabled:opacity-50"
        >
          {busy ? "Adding…" : "Add"}
        </button>
        <button onClick={onClose} className="px-4 h-9 rounded-xl border border-divider text-sm text-text-secondary">
          Cancel
        </button>
      </div>
    </div>
  );
}

// ── Diet card ─────────────────────────────────────────────────────────────────
function DietCard({ diet, meals, isFav, onToggleFav, onDelete, onDuplicate, onUpdate }: {
  diet: DietDto;
  meals: MealDto[];
  isFav: boolean;
  onToggleFav: () => void;
  onDelete: () => void;
  onDuplicate: () => void;
  onUpdate: (d: DietDto) => void;
}) {
  const [expanded,   setExpanded]   = useState(false);
  const [assigning,  setAssigning]  = useState(false);
  const [removingIdx, setRemovingIdx] = useState<number | null>(null);

  const dietMeals = diet.meals ?? [];
  const mealCount = dietMeals.length;

  const uniqueSlots = [...new Set(dietMeals.map((dm) => dm.slot))];

  const removeAssignment = async (idx: number) => {
    setRemovingIdx(idx);
    try {
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet,
        meals: dietMeals.filter((_, i) => i !== idx),
      });
      onUpdate(updated);
    } finally { setRemovingIdx(null); }
  };

  return (
    <div className="bg-bg-card rounded-2xl border border-divider overflow-hidden">
      {/* Main row */}
      <button
        className="w-full text-left px-4 pt-4 pb-3 flex items-start gap-3"
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="flex-1 min-w-0">
          <p className="text-[15px] font-semibold text-text-primary truncate">{diet.name}</p>
          {diet.description && (
            <p className="text-xs text-text-muted mt-0.5 line-clamp-2">{diet.description}</p>
          )}
          {/* Slot badges + calorie target */}
          <div className="flex flex-wrap gap-1.5 mt-2">
            {uniqueSlots.map((s) => {
              const c = SLOT_COLORS[s] ?? { bg: "#F0F0F0", text: "#555" };
              return (
                <span key={s} className="text-[11px] font-medium px-2 py-0.5 rounded-full"
                  style={{ background: c.bg, color: c.text }}>
                  {s}
                </span>
              );
            })}
            {mealCount > 0 && (
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-bg-page text-text-muted border border-divider">
                {mealCount} meal{mealCount !== 1 ? "s" : ""}
              </span>
            )}
            {diet.targetCalories && (
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-bg-page text-text-muted border border-divider">
                {diet.targetCalories} kcal
              </span>
            )}
          </div>
        </div>

        <div className="flex items-center gap-1 shrink-0 pt-0.5">
          {/* Favorite */}
          <button
            onClick={(e) => { e.stopPropagation(); onToggleFav(); }}
            className="p-1.5 rounded-lg hover:bg-bg-page transition-colors"
          >
            <Star className="h-4 w-4" fill={isFav ? "#F59E0B" : "none"} stroke={isFav ? "#F59E0B" : "currentColor"}
              style={{ color: isFav ? "#F59E0B" : "#9CA3AF" }} />
          </button>
          {expanded ? <ChevronUp className="h-4 w-4 text-text-muted" /> : <ChevronDown className="h-4 w-4 text-text-muted" />}
        </div>
      </button>

      {/* Expanded: meal schedule */}
      {expanded && (
        <div className="px-4 pb-4 pt-0">
          <div className="h-px bg-divider mb-3" />

          {dietMeals.length === 0 ? (
            <p className="text-xs text-text-muted mb-3">No meals assigned yet.</p>
          ) : (
            <div className="space-y-1.5 mb-3">
              {dietMeals.map((dm, idx) => {
                const meal = meals.find((m) => m.id === dm.mealId);
                const c = SLOT_COLORS[dm.slot] ?? { bg: "#F0F0F0", text: "#555" };
                return (
                  <div key={idx} className="flex items-center gap-2 text-sm">
                    <span className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
                      style={{ background: c.bg, color: c.text }}>
                      {dm.slot}
                    </span>
                    {dm.dayOfWeek !== 0 && (
                      <span className="text-[11px] text-text-muted shrink-0">{DAY_NAMES[dm.dayOfWeek]}</span>
                    )}
                    <span className="text-text-primary truncate flex-1">{meal?.name ?? `Meal #${dm.mealId}`}</span>
                    <button
                      onClick={() => removeAssignment(idx)}
                      disabled={removingIdx === idx}
                      className="p-1 rounded-lg hover:bg-bg-page transition-colors shrink-0"
                    >
                      <X className="h-3.5 w-3.5 text-text-muted" />
                    </button>
                  </div>
                );
              })}
            </div>
          )}

          {assigning ? (
            <AssignMealPanel
              diet={diet} meals={meals}
              onAssigned={(d) => { onUpdate(d); setAssigning(false); }}
              onClose={() => setAssigning(false)}
            />
          ) : (
            <button
              onClick={() => setAssigning(true)}
              className="w-full h-9 rounded-xl border border-dashed border-divider text-sm text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-1"
            >
              <Plus className="h-3.5 w-3.5" />Add meal
            </button>
          )}

          {/* Action row */}
          <div className="flex gap-2 mt-3">
            <button
              onClick={onDuplicate}
              className="flex items-center gap-1.5 px-3 h-8 rounded-lg border border-divider text-xs text-text-secondary hover:bg-bg-page transition-colors"
            >
              <Copy className="h-3.5 w-3.5" />Duplicate
            </button>
            <button
              onClick={onDelete}
              className="flex items-center gap-1.5 px-3 h-8 rounded-lg border border-divider text-xs text-red-500 hover:bg-red-50 transition-colors ml-auto"
            >
              <Trash2 className="h-3.5 w-3.5" />Delete
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function DietsPage() {
  const { user } = useAuth();
  const [diets,   setDiets]   = useState<DietDto[]>([]);
  const [meals,   setMeals]   = useState<MealDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);
  const [query,   setQuery]   = useState("");
  const [favOnly, setFavOnly] = useState(false);
  const [showCreate, setShowCreate] = useState(false);

  const { favs, toggle: toggleFav } = useFavorites("diet_favorites");

  const loadData = useCallback(async () => {
    if (!user) return;
    try {
      const [d, m] = await Promise.all([
        api.get<DietDto[]>("/api/v1/diets"),
        api.get<MealDto[]>("/api/v1/meals"),
      ]);
      setDiets(d); setMeals(m);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  const filtered = useMemo(() => {
    let list = diets;
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((d) =>
        d.name.toLowerCase().includes(q) ||
        (d.description ?? "").toLowerCase().includes(q)
      );
    }
    if (favOnly) list = list.filter((d) => d.id != null && favs.has(d.id));
    return list;
  }, [diets, query, favOnly, favs]);

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this diet?")) return;
    try {
      await api.delete(`/api/v1/diets/${id}`);
      setDiets((prev) => prev.filter((d) => d.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  const handleDuplicate = async (id: number) => {
    try {
      const copy = await api.post<DietDto>(`/api/v1/diets/${id}/duplicate`, {});
      setDiets((prev) => [copy, ...prev]);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to duplicate");
    }
  };

  // ── Render ──────────────────────────────────────────────────────────────────
  if (loading) return (
    <div className="space-y-3">
      <div className="h-8 w-32 rounded-lg bg-bg-card" />
      <div className="h-10 w-full rounded-xl bg-bg-card" />
      {Array.from({ length: 4 }).map((_, i) => (
        <Skeleton key={i} className="h-24 w-full rounded-2xl" />
      ))}
    </div>
  );

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-[22px] font-bold text-text-primary">Diets</h1>
        <button
          onClick={() => setShowCreate(true)}
          className="flex items-center gap-1.5 px-3 h-9 rounded-xl bg-green text-white text-sm font-medium hover:bg-green/90 transition-colors"
        >
          <Plus className="h-4 w-4" />New
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</p>
      )}

      {/* Search + filter */}
      <SearchBar value={query} onChange={setQuery} placeholder="Search diets…" />

      <div className="flex gap-2">
        <button
          onClick={() => setFavOnly(false)}
          className={`px-3 h-8 rounded-full text-sm font-medium transition-colors ${
            !favOnly ? "bg-green text-white" : "bg-bg-card border border-divider text-text-secondary"
          }`}
        >
          All ({diets.length})
        </button>
        <button
          onClick={() => setFavOnly(true)}
          className={`flex items-center gap-1 px-3 h-8 rounded-full text-sm font-medium transition-colors ${
            favOnly ? "bg-green text-white" : "bg-bg-card border border-divider text-text-secondary"
          }`}
        >
          <Star className="h-3.5 w-3.5" fill={favOnly ? "white" : "none"} />
          Favorites
        </button>
      </div>

      {/* List */}
      {filtered.length === 0 ? (
        <EmptyState
          icon={Salad}
          title={query || favOnly ? "No diets found" : "No diets yet"}
          subtitle={query ? `No results for "${query}"` : favOnly ? "Star a diet to save it here" : "Create your first diet plan"}
          action={!query && !favOnly ? { label: "Create diet", onClick: () => setShowCreate(true) } : undefined}
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((diet) => (
            <DietCard
              key={diet.id}
              diet={diet}
              meals={meals}
              isFav={diet.id != null && favs.has(diet.id)}
              onToggleFav={() => diet.id != null && toggleFav(diet.id)}
              onDelete={() => diet.id != null && handleDelete(diet.id)}
              onDuplicate={() => diet.id != null && handleDuplicate(diet.id)}
              onUpdate={(d) => setDiets((prev) => prev.map((x) => x.id === d.id ? d : x))}
            />
          ))}
        </div>
      )}

      {/* Create sheet */}
      {showCreate && (
        <CreateDietSheet
          onCreated={(d) => setDiets((prev) => [d, ...prev])}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}
