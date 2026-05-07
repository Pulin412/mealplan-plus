"use client";
import { useEffect, useState, useCallback, useMemo } from "react";
import {
  ChevronDown, ChevronUp, Trash2, Plus, Copy, Star, Salad, X, Tag as TagIcon,
} from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import { CreateDietModal } from "./CreateDietModal";
import type { components } from "@/lib/api/types.generated";

type DietDto         = components["schemas"]["DietDto"];
type MealDto         = components["schemas"]["MealDto"];
type DietMealDto     = components["schemas"]["DietMealDto"];
type TagDto          = components["schemas"]["TagDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];

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

const TAG_PALETTE = [
  "#FFEB3B", "#4CAF50", "#F44336", "#2196F3", "#FF9800",
  "#9C27B0", "#00BCD4", "#E91E63", "#607D8B", "#795548",
];

function hexToRgba(hex: string, alpha: number) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

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
      try { localStorage.setItem(key, JSON.stringify(Array.from(next))); } catch {}
      return next;
    });
  }, [key]);
  return { favs, toggle };
}

// ── Tag chip ──────────────────────────────────────────────────────────────────
function TagChip({ tag, onRemove }: { tag: TagDto; onRemove?: () => void }) {
  const color = tag.color ?? "#9CA3AF";
  return (
    <span
      className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full"
      style={{ background: hexToRgba(color, 0.15), color }}
    >
      {tag.name}
      {onRemove && (
        <button onClick={(e) => { e.stopPropagation(); onRemove(); }} className="hover:opacity-70">
          <X className="h-3 w-3" />
        </button>
      )}
    </span>
  );
}

// ── Tag picker ────────────────────────────────────────────────────────────────
function TagPicker({ allTags, assignedIds, onAssign, onCreateTag, onClose }: {
  allTags: TagDto[];
  assignedIds: number[];
  onAssign: (tag: TagDto) => void;
  onCreateTag: (name: string, color: string) => Promise<TagDto>;
  onClose: () => void;
}) {
  const [creating, setCreating] = useState(false);
  const [newName,  setNewName]  = useState("");
  const [newColor, setNewColor] = useState(TAG_PALETTE[0]);
  const [busy,     setBusy]     = useState(false);

  const available = allTags.filter((t) => t.id != null && !assignedIds.includes(t.id));

  const handleCreate = async () => {
    if (!newName.trim()) return;
    setBusy(true);
    try {
      const tag = await onCreateTag(newName.trim(), newColor);
      onAssign(tag);
      onClose();
    } finally { setBusy(false); }
  };

  return (
    <div className="mt-2 p-3 rounded-xl bg-bg-page border border-divider space-y-2">
      <div className="flex items-center justify-between">
        <p className="text-xs font-semibold text-text-secondary">Add tag</p>
        <button onClick={onClose}><X className="h-4 w-4 text-text-muted" /></button>
      </div>
      {!creating ? (
        <>
          {available.length > 0 ? (
            <div className="flex flex-wrap gap-1.5">
              {available.map((t) => (
                <button key={t.id} onClick={() => { onAssign(t); onClose(); }}>
                  <TagChip tag={t} />
                </button>
              ))}
            </div>
          ) : (
            <p className="text-xs text-text-muted">No tags available</p>
          )}
          <button
            onClick={() => setCreating(true)}
            className="flex items-center gap-1 text-xs text-green font-medium"
          >
            <Plus className="h-3.5 w-3.5" />New tag
          </button>
        </>
      ) : (
        <div className="space-y-2">
          <input
            autoFocus value={newName} onChange={(e) => setNewName(e.target.value)}
            placeholder="Tag name"
            className="w-full h-8 px-2 rounded-lg border border-divider bg-bg-card text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30"
          />
          <div className="flex flex-wrap gap-1.5">
            {TAG_PALETTE.map((c) => (
              <button
                key={c} onClick={() => setNewColor(c)}
                className="w-6 h-6 rounded-full border-2 transition-transform hover:scale-110"
                style={{ background: c, borderColor: newColor === c ? "#000" : "transparent" }}
              />
            ))}
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleCreate} disabled={busy || !newName.trim()}
              className="flex-1 h-8 rounded-xl bg-green text-white text-xs font-medium disabled:opacity-50"
            >
              {busy ? "Creating…" : "Create"}
            </button>
            <button onClick={() => setCreating(false)} className="px-3 h-8 rounded-xl border border-divider text-xs text-text-secondary">
              Back
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Meal ingredient row (expandable) ──────────────────────────────────────────
function MealRow({ dm, meal, foods, onRemove, removing }: {
  dm: DietMealDto;
  meal: MealDto | undefined;
  foods: FoodDto[];
  onRemove: () => void;
  removing: boolean;
}) {
  const [open, setOpen] = useState(false);
  const c = SLOT_COLORS[dm.slot] ?? { bg: "#F0F0F0", text: "#555" };
  const items: MealFoodItemDto[] = meal?.items ?? [];

  const totalKcal = items.reduce((sum, item) => {
    const food = foods.find((f) => f.id === item.foodId);
    return sum + (food ? Math.round(food.caloriesPer100 * item.quantity / 100) : 0);
  }, 0);

  return (
    <div className="rounded-xl border border-divider overflow-hidden">
      <button
        className="w-full flex items-center gap-2 px-3 py-2 text-sm bg-bg-page hover:bg-bg-card transition-colors text-left"
        onClick={() => setOpen((v) => !v)}
      >
        <span className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
          style={{ background: c.bg, color: c.text }}>
          {dm.slot}
        </span>
        {dm.dayOfWeek !== 0 && (
          <span className="text-[11px] text-text-muted shrink-0">{DAY_NAMES[dm.dayOfWeek]}</span>
        )}
        <span className="text-text-primary truncate flex-1">{meal?.name ?? `Meal #${dm.mealId}`}</span>
        {items.length > 0 && (
          <span className="text-[11px] text-text-muted shrink-0">{totalKcal} kcal</span>
        )}
        {open
          ? <ChevronUp className="h-3.5 w-3.5 text-text-muted shrink-0" />
          : <ChevronDown className="h-3.5 w-3.5 text-text-muted shrink-0" />}
        <button
          onClick={(e) => { e.stopPropagation(); onRemove(); }}
          disabled={removing}
          className="p-1 rounded-lg hover:bg-bg-page transition-colors shrink-0 ml-1"
        >
          <X className="h-3.5 w-3.5 text-text-muted" />
        </button>
      </button>

      {open && (
        <div className="px-3 pb-2 pt-1 bg-bg-card">
          {items.length === 0 ? (
            <p className="text-xs text-text-muted py-1">No ingredients</p>
          ) : (
            <ul className="space-y-1">
              {items.map((item, i) => {
                const food = foods.find((f) => f.id === item.foodId);
                const kcal = food ? Math.round(food.caloriesPer100 * item.quantity / 100) : null;
                return (
                  <li key={i} className="flex items-center gap-2 text-xs">
                    <span className="w-1.5 h-1.5 rounded-full bg-text-muted/40 shrink-0" />
                    <span className="text-text-primary flex-1 truncate">
                      {food?.name ?? `Food #${item.foodId}`}
                    </span>
                    <span className="text-text-muted shrink-0">
                      {item.quantity}g{kcal ? ` · ${kcal} kcal` : ""}
                    </span>
                  </li>
                );
              })}
            </ul>
          )}
          {items.length > 0 && (
            <div className="flex justify-end mt-1.5 pt-1.5 border-t border-divider">
              <span className="text-[11px] font-semibold text-text-secondary">
                Total: {totalKcal} kcal · {items.length} ingredient{items.length !== 1 ? "s" : ""}
              </span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Assign meal panel ─────────────────────────────────────────────────────────
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
function DietCard({ diet, meals, foods, allTags, isFav, onToggleFav, onDelete, onDuplicate, onUpdate, onTagCreated }: {
  diet: DietDto;
  meals: MealDto[];
  foods: FoodDto[];
  allTags: TagDto[];
  isFav: boolean;
  onToggleFav: () => void;
  onDelete: () => void;
  onDuplicate: () => void;
  onUpdate: (d: DietDto) => void;
  onTagCreated: (t: TagDto) => void;
}) {
  const [expanded,    setExpanded]    = useState(false);
  const [assigning,   setAssigning]   = useState(false);
  const [addingTag,   setAddingTag]   = useState(false);
  const [removingIdx, setRemovingIdx] = useState<number | null>(null);

  const dietMeals  = diet.meals ?? [];
  const dietTags   = diet.tags  ?? [];
  const mealCount  = dietMeals.length;
  const uniqueSlots = Array.from(new Set(dietMeals.map((dm) => dm.slot)));

  const removeAssignment = async (idx: number) => {
    setRemovingIdx(idx);
    try {
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet, meals: dietMeals.filter((_, i) => i !== idx),
      });
      onUpdate(updated);
    } finally { setRemovingIdx(null); }
  };

  const assignTag = async (tag: TagDto) => {
    if (dietTags.some((t) => t.id === tag.id)) return;
    const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
      ...diet, tagIds: [...(diet.tagIds ?? []), tag.id!],
    });
    onUpdate(updated);
  };

  const removeTag = async (tagId: number) => {
    const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
      ...diet, tagIds: (diet.tagIds ?? []).filter((id) => id !== tagId),
    });
    onUpdate(updated);
  };

  const handleCreateTag = async (name: string, color: string): Promise<TagDto> => {
    const tag = await api.post<TagDto>("/api/v1/tags", { name, color });
    onTagCreated(tag);
    return tag;
  };

  return (
    <div className="bg-bg-card rounded-2xl border border-divider overflow-hidden">
      <button
        className="w-full text-left px-4 pt-4 pb-3 flex items-start gap-3"
        onClick={() => setExpanded((v) => !v)}
      >
        <div className="flex-1 min-w-0">
          <p className="text-[15px] font-semibold text-text-primary truncate">{diet.name}</p>
          {diet.description && (
            <p className="text-xs text-text-muted mt-0.5 line-clamp-2">{diet.description}</p>
          )}
          {dietTags.length > 0 && (
            <div className="flex flex-wrap gap-1 mt-1.5">
              {dietTags.map((t) => <TagChip key={t.id} tag={t} />)}
            </div>
          )}
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
          <button
            onClick={(e) => { e.stopPropagation(); onToggleFav(); }}
            className="p-1.5 rounded-lg hover:bg-bg-page transition-colors"
          >
            <Star className="h-4 w-4" fill={isFav ? "#F59E0B" : "none"} stroke={isFav ? "#F59E0B" : "currentColor"}
              style={{ color: isFav ? "#F59E0B" : "#9CA3AF" }} />
          </button>
          {expanded
            ? <ChevronUp className="h-4 w-4 text-text-muted" />
            : <ChevronDown className="h-4 w-4 text-text-muted" />}
        </div>
      </button>

      {expanded && (
        <div className="px-4 pb-4 pt-0">
          <div className="h-px bg-divider mb-3" />

          {/* Tags */}
          <div className="mb-3">
            <p className="text-[11px] font-semibold text-text-muted uppercase tracking-wide mb-1.5">Tags</p>
            <div className="flex flex-wrap gap-1.5">
              {dietTags.map((t) => (
                <TagChip key={t.id} tag={t} onRemove={() => t.id != null && removeTag(t.id)} />
              ))}
              {!addingTag && (
                <button
                  onClick={() => setAddingTag(true)}
                  className="inline-flex items-center gap-0.5 text-[11px] font-medium px-2 py-0.5 rounded-full border border-dashed border-divider text-text-muted hover:bg-bg-page transition-colors"
                >
                  <Plus className="h-3 w-3" />tag
                </button>
              )}
            </div>
            {addingTag && (
              <TagPicker
                allTags={allTags}
                assignedIds={dietTags.map((t) => t.id!).filter(Boolean)}
                onAssign={assignTag}
                onCreateTag={handleCreateTag}
                onClose={() => setAddingTag(false)}
              />
            )}
          </div>

          <div className="h-px bg-divider mb-3" />

          {/* Meal schedule */}
          <p className="text-[11px] font-semibold text-text-muted uppercase tracking-wide mb-1.5">
            Meal schedule — tap a meal for ingredients
          </p>
          {dietMeals.length === 0 ? (
            <p className="text-xs text-text-muted mb-3">No meals assigned yet.</p>
          ) : (
            <div className="space-y-1.5 mb-3">
              {dietMeals.map((dm, idx) => (
                <MealRow
                  key={idx}
                  dm={dm}
                  meal={meals.find((m) => m.id === dm.mealId)}
                  foods={foods}
                  onRemove={() => removeAssignment(idx)}
                  removing={removingIdx === idx}
                />
              ))}
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

          {/* Actions */}
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

// ── Filter chip ───────────────────────────────────────────────────────────────
function FilterChip({ label, active, onClick, color }: {
  label: string; active: boolean; onClick: () => void; color?: string;
}) {
  if (color) {
    return (
      <button
        onClick={onClick}
        className="shrink-0 flex items-center gap-1 px-3 h-8 rounded-full text-sm font-medium transition-colors border"
        style={{
          background: active ? hexToRgba(color, 0.2) : "transparent",
          borderColor: active ? color : "var(--color-divider)",
          color: active ? color : "var(--color-text-secondary)",
        }}
      >
        {label}
      </button>
    );
  }
  return (
    <button
      onClick={onClick}
      className={`shrink-0 px-3 h-8 rounded-full text-sm font-medium transition-colors ${
        active ? "bg-green text-white" : "bg-bg-card border border-divider text-text-secondary"
      }`}
    >
      {label}
    </button>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function DietsPage() {
  const { user } = useAuth();
  const [diets,   setDiets]   = useState<DietDto[]>([]);
  const [meals,   setMeals]   = useState<MealDto[]>([]);
  const [foods,   setFoods]   = useState<FoodDto[]>([]);
  const [tags,    setTags]    = useState<TagDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);
  const [query,      setQuery]      = useState("");
  const [favOnly,    setFavOnly]    = useState(false);
  const [tagFilter,  setTagFilter]  = useState<number | null>(null);
  const [slotFilter, setSlotFilter] = useState<Slot | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const { favs, toggle: toggleFav } = useFavorites("diet_favorites");

  const loadData = useCallback(async () => {
    if (!user) return;
    try {
      const [d, m, f, t] = await Promise.all([
        api.get<DietDto[]>("/api/v1/diets"),
        api.get<MealDto[]>("/api/v1/meals"),
        api.get<FoodDto[]>("/api/v1/foods"),
        api.get<TagDto[]>("/api/v1/tags"),
      ]);
      setDiets(d); setMeals(m); setFoods(f); setTags(t);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  // Build a lookup: mealId → food names (for ingredient search)
  const mealIngredients = useMemo(() => {
    const map = new Map<number, string[]>();
    for (const meal of meals) {
      if (meal.id == null) continue;
      const names = (meal.items ?? []).map((item) => {
        const food = foods.find((f) => f.id === item.foodId);
        return food?.name.toLowerCase() ?? "";
      }).filter(Boolean);
      map.set(meal.id, names);
    }
    return map;
  }, [meals, foods]);

  const filtered = useMemo(() => {
    let list = diets;

    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((d) => {
        if (d.name.toLowerCase().includes(q)) return true;
        if ((d.description ?? "").toLowerCase().includes(q)) return true;
        if ((d.tags ?? []).some((t) => t.name.toLowerCase().includes(q))) return true;
        return (d.meals ?? []).some((dm) => {
          const meal = meals.find((m) => m.id === dm.mealId);
          if (meal?.name.toLowerCase().includes(q)) return true;
          const ingredients = mealIngredients.get(dm.mealId!) ?? [];
          return ingredients.some((name) => name.includes(q));
        });
      });
    }

    if (favOnly) list = list.filter((d) => d.id != null && favs.has(d.id));
    if (tagFilter != null) list = list.filter((d) => (d.tagIds ?? []).includes(tagFilter));
    if (slotFilter != null) list = list.filter((d) => (d.meals ?? []).some((dm) => dm.slot === slotFilter));

    return list;
  }, [diets, query, favOnly, favs, tagFilter, slotFilter, meals, mealIngredients]);

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

  const hasActiveFilter = favOnly || tagFilter != null || slotFilter != null;

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

      {error && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</p>}

      <SearchBar value={query} onChange={setQuery} placeholder="Search name, tag, meal, ingredient…" />

      {/* Fav + Tag filter row */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
        <FilterChip
          label={`All (${diets.length})`}
          active={!hasActiveFilter}
          onClick={() => { setFavOnly(false); setTagFilter(null); setSlotFilter(null); }}
        />
        <FilterChip
          label="⭐ Favorites"
          active={favOnly}
          onClick={() => { setFavOnly((v) => !v); setTagFilter(null); }}
        />
        {tags.map((t) => (
          <FilterChip
            key={t.id}
            label={t.name}
            active={tagFilter === t.id}
            color={t.color ?? "#9CA3AF"}
            onClick={() => {
              setFavOnly(false);
              setTagFilter(tagFilter === t.id ? null : (t.id ?? null));
            }}
          />
        ))}
      </div>

      {/* Slot filter row */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
        {SLOTS.map((s) => {
          const c = SLOT_COLORS[s];
          return (
            <FilterChip
              key={s}
              label={s}
              active={slotFilter === s}
              color={c.text}
              onClick={() => setSlotFilter(slotFilter === s ? null : s)}
            />
          );
        })}
      </div>

      {/* List */}
      {filtered.length === 0 ? (
        <EmptyState
          icon={Salad}
          title={query || hasActiveFilter ? "No diets found" : "No diets yet"}
          subtitle={
            query ? `No results for "${query}"` :
            favOnly ? "Star a diet to save it here" :
            slotFilter ? `No diets with ${slotFilter} meals` :
            tagFilter != null ? "No diets with this tag" :
            "Create your first diet plan"
          }
          action={!query && !hasActiveFilter ? { label: "Create diet", onClick: () => setShowCreate(true) } : undefined}
        />
      ) : (
        <div className="space-y-3">
          {filtered.map((diet) => (
            <DietCard
              key={diet.id}
              diet={diet}
              meals={meals}
              foods={foods}
              allTags={tags}
              isFav={diet.id != null && favs.has(diet.id)}
              onToggleFav={() => diet.id != null && toggleFav(diet.id)}
              onDelete={() => diet.id != null && handleDelete(diet.id)}
              onDuplicate={() => diet.id != null && handleDuplicate(diet.id)}
              onUpdate={(d) => setDiets((prev) => prev.map((x) => x.id === d.id ? d : x))}
              onTagCreated={(t) => setTags((prev) => [...prev, t])}
            />
          ))}
        </div>
      )}

      {showCreate && (
        <CreateDietModal
          meals={meals}
          foods={foods}
          onCreated={(d) => setDiets((prev) => [d, ...prev])}
          onClose={() => setShowCreate(false)}
        />
      )}
    </div>
  );
}
