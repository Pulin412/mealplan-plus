"use client";
import { useEffect, useState, useCallback, useMemo } from "react";
import {
  ChevronDown, ChevronUp, Trash2, Plus, Copy, Star, Salad, X, GripVertical,
} from "lucide-react";
import {
  DndContext, closestCenter, PointerSensor, useSensor, useSensors,
  type DragEndEvent,
} from "@dnd-kit/core";
import {
  SortableContext, verticalListSortingStrategy, useSortable, arrayMove,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { SearchBar } from "@/components/SearchBar";
import { EmptyState } from "@/components/EmptyState";
import { CreateDietModal } from "./CreateDietModal";
import { PREDEFINED_SLOTS, PREDEFINED_SLOT_COLORS } from "@/lib/utils";
import type { components } from "@/lib/api/types.generated";

type DietDto         = components["schemas"]["DietDto"];
type MealDto         = components["schemas"]["MealDto"];
type DietMealDto     = components["schemas"]["DietMealDto"];
type TagDto          = components["schemas"]["TagDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];

const PAGE_SIZE = 10;

const SLOTS = PREDEFINED_SLOTS;
type Slot = string;

const DAY_NAMES: Record<number, string> = {
  0: "Any day", 1: "Mon", 2: "Tue", 3: "Wed",
  4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun",
};

const SLOT_COLORS = PREDEFINED_SLOT_COLORS;

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
      if (next.has(id)) { next.delete(id); } else { next.add(id); }
      try { localStorage.setItem(key, JSON.stringify(Array.from(next))); } catch {}
      return next;
    });
  }, [key]);
  return { favs, toggle };
}

// ── Macro calculation ─────────────────────────────────────────────────────────
function calcDietMacros(diet: DietDto, meals: MealDto[], foods: FoodDto[]) {
  let kcal = 0, protein = 0, carbs = 0, fat = 0;
  let giWeightedSum = 0, giTotalGrams = 0;

  for (const dm of diet.meals ?? []) {
    const meal = meals.find((m) => m.id === dm.mealId);
    for (const item of meal?.items ?? []) {
      const food = foods.find((f) => f.id === item.foodId);
      if (!food) continue;
      const g = item.quantity;
      kcal    += food.caloriesPer100 * g / 100;
      protein += food.proteinPer100  * g / 100;
      carbs   += food.carbsPer100    * g / 100;
      fat     += food.fatPer100      * g / 100;
      if ((food.glycemicIndex ?? 0) > 0) {
        giWeightedSum += food.glycemicIndex! * g;
        giTotalGrams  += g;
      }
    }
  }

  return {
    kcal:    Math.round(kcal),
    protein: Math.round(protein),
    carbs:   Math.round(carbs),
    fat:     Math.round(fat),
    gi:      giTotalGrams > 0 ? Math.round(giWeightedSum / giTotalGrams) : null,
  };
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

// ── Meal ingredient row (view mode) ───────────────────────────────────────────
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

// ── Sortable meal row (edit/reorder mode) ─────────────────────────────────────
function SortableMealRow({ id, dm, meal, foods, onRemove }: {
  id: string;
  dm: DietMealDto;
  meal: MealDto | undefined;
  foods: FoodDto[];
  onRemove: () => void;
}) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id });
  const style = { transform: CSS.Transform.toString(transform), transition };
  const c = SLOT_COLORS[dm.slot] ?? { bg: "#F0F0F0", text: "#555" };
  const totalKcal = (meal?.items ?? []).reduce((sum, item) => {
    const food = foods.find((f) => f.id === item.foodId);
    return sum + (food ? Math.round(food.caloriesPer100 * item.quantity / 100) : 0);
  }, 0);

  return (
    <div
      ref={setNodeRef} style={style}
      className={`flex items-center gap-2 rounded-xl border border-divider bg-bg-page px-3 py-2.5 transition-shadow ${isDragging ? "shadow-lg opacity-80 z-50" : ""}`}
    >
      <button
        {...attributes} {...listeners}
        className="p-1 text-text-muted/50 hover:text-text-muted cursor-grab active:cursor-grabbing shrink-0 touch-none"
        style={{ touchAction: "none" }}
      >
        <GripVertical size={14} />
      </button>
      <span className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
        style={{ background: c.bg, color: c.text }}>
        {dm.slot}
      </span>
      {dm.dayOfWeek !== 0 && (
        <span className="text-[11px] text-text-muted shrink-0">{DAY_NAMES[dm.dayOfWeek]}</span>
      )}
      <span className="flex-1 text-sm text-text-primary truncate">{meal?.name ?? `Meal #${dm.mealId}`}</span>
      {totalKcal > 0 && (
        <span className="text-[11px] text-text-muted shrink-0">{totalKcal} kcal</span>
      )}
      <button
        onClick={onRemove}
        className="p-1 rounded-lg hover:bg-bg-card transition-colors shrink-0"
      >
        <X size={13} className="text-text-muted" />
      </button>
    </div>
  );
}

// ── Slot adder (edit mode, no immediate API call) ─────────────────────────────
function SlotAdder({ meals, label, onAdd, onCancel }: {
  meals: MealDto[];
  label: string;
  onAdd: (data: { mealId: number; slot: string; dayOfWeek: number }) => void;
  onCancel: () => void;
}) {
  const [mealId, setMealId] = useState("");
  const [slot,   setSlot]   = useState<string>("Breakfast");
  const [day,    setDay]    = useState("0");

  const selectCls = "w-full h-9 rounded-lg border border-divider bg-bg-card px-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30";

  return (
    <div className="p-3 rounded-xl border border-dashed border-green/50 bg-green/5 space-y-2">
      <p className="text-[11px] font-semibold text-green">{label}</p>
      <select value={mealId} onChange={(e) => setMealId(e.target.value)} className={selectCls}>
        <option value="">Select a meal…</option>
        {meals.map((m) => <option key={m.id} value={m.id}>{m.name}</option>)}
      </select>
      <div className="grid grid-cols-2 gap-2">
        <select value={slot} onChange={(e) => setSlot(e.target.value)} className={selectCls}>
          {SLOTS.map((s) => <option key={s} value={s}>{s}</option>)}
        </select>
        <select value={day} onChange={(e) => setDay(e.target.value)} className={selectCls}>
          {Object.entries(DAY_NAMES).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
        </select>
      </div>
      <div className="flex gap-2">
        <button
          onClick={() => {
            if (!mealId) return;
            onAdd({ mealId: parseInt(mealId), slot, dayOfWeek: parseInt(day) });
          }}
          disabled={!mealId}
          className="flex-1 h-8 rounded-xl bg-green text-white text-xs font-semibold disabled:opacity-40 hover:bg-green/90 transition-colors"
        >
          Insert here
        </button>
        <button
          onClick={onCancel}
          className="px-3 h-8 rounded-xl border border-divider text-xs text-text-secondary hover:bg-bg-page transition-colors"
        >
          Cancel
        </button>
      </div>
    </div>
  );
}

// ── Assign meal panel (view mode, immediate save) ─────────────────────────────
function AssignMealPanel({ diet, meals, onAssigned, onClose }: {
  diet: DietDto;
  meals: MealDto[];
  onAssigned: (d: DietDto) => void;
  onClose: () => void;
}) {
  const [mealId, setMealId] = useState("");
  const [slot,   setSlot]   = useState("Breakfast");
  const [day,    setDay]    = useState("0");
  const [busy,   setBusy]   = useState(false);

  const submit = async () => {
    if (!mealId) return;
    setBusy(true);
    try {
      const assignment = {
        mealId: parseInt(mealId), slot, dayOfWeek: parseInt(day),
      } as DietMealDto;
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet, meals: [...(diet.meals ?? []), assignment],
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
        <select value={slot} onChange={(e) => setSlot(e.target.value)} className={selectCls}>
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
function DietCard({ diet, meals, foods, allTags, isFav, onToggleFav, onDelete, onDuplicate, onUpdate, onTagCreated, defaultExpanded = false }: {
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
  defaultExpanded?: boolean;
}) {
  const [expanded,    setExpanded]    = useState(defaultExpanded);
  const [assigning,   setAssigning]   = useState(false);
  const [addingTag,   setAddingTag]   = useState(false);
  const [removingIdx, setRemovingIdx] = useState<number | null>(null);

  // Edit / reorder mode
  const [reordering,   setReordering]   = useState(false);
  const [editSlots,    setEditSlots]    = useState<DietMealDto[]>([]);
  const [insertAt,     setInsertAt]     = useState<number | null>(null);
  const [savingOrder,  setSavingOrder]  = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
  );

  const dietMeals  = diet.meals ?? [];
  const dietTags   = diet.tags  ?? [];
  const mealCount = dietMeals.length;
  const macros = calcDietMacros(diet, meals, foods);

  const enterReorder = () => {
    setEditSlots([...dietMeals]);
    setInsertAt(null);
    setReordering(true);
  };

  const cancelReorder = () => {
    setReordering(false);
    setEditSlots([]);
    setInsertAt(null);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    if (over && active.id !== over.id) {
      setInsertAt(null);
      setEditSlots((slots) =>
        arrayMove(slots, Number(active.id), Number(over.id))
      );
    }
  };

  const insertSlot = (beforeIndex: number, data: { mealId: number; slot: string; dayOfWeek: number }) => {
    const newSlot = { mealId: data.mealId, slot: data.slot, dayOfWeek: data.dayOfWeek, dietId: diet.id!, id: 0 } as DietMealDto;
    setEditSlots((prev) => [...prev.slice(0, beforeIndex), newSlot, ...prev.slice(beforeIndex)]);
    setInsertAt(null);
  };

  const removeEditSlot = (idx: number) => {
    setEditSlots((prev) => prev.filter((_, i) => i !== idx));
    if (insertAt !== null && insertAt > idx) setInsertAt(insertAt - 1);
    else if (insertAt === idx) setInsertAt(null);
  };

  const saveReorder = async () => {
    setSavingOrder(true);
    try {
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet, meals: editSlots,
      });
      onUpdate(updated);
      setReordering(false);
    } finally { setSavingOrder(false); }
  };

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
            {mealCount > 0 && (
              <span className="text-[11px] font-medium px-2 py-0.5 rounded-full bg-bg-page text-text-muted border border-divider">
                {mealCount} meal slot{mealCount !== 1 ? "s" : ""}
              </span>
            )}
          </div>

          {/* Macro strip */}
          {macros.kcal > 0 && (
            <div className="flex items-center flex-wrap gap-1.5 mt-2">
              <span className="text-[12px] font-bold text-text-primary">{macros.kcal} kcal</span>
              {macros.protein > 0 && (
                <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                  style={{ background: "#2E7D5218", color: "#2E7D52" }}>P {macros.protein}g</span>
              )}
              {macros.carbs > 0 && (
                <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                  style={{ background: "#C0520018", color: "#C05200" }}>C {macros.carbs}g</span>
              )}
              {macros.fat > 0 && (
                <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                  style={{ background: "#7C3AED18", color: "#7C3AED" }}>F {macros.fat}g</span>
              )}
              {macros.gi != null && (
                <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                  style={{ background: "#F59E0B18", color: "#B45309" }}>GI {macros.gi}</span>
              )}
            </div>
          )}
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
          <div className="flex items-center justify-between mb-1.5">
            <p className="text-[11px] font-semibold text-text-muted uppercase tracking-wide">
              {reordering ? "Edit schedule — drag to reorder" : "Meal schedule — tap a meal for ingredients"}
            </p>
            {!reordering && dietMeals.length > 0 && (
              <button
                onClick={enterReorder}
                className="text-[11px] font-semibold text-green flex items-center gap-0.5 hover:underline"
              >
                <GripVertical size={11} />Reorder
              </button>
            )}
          </div>

          {/* VIEW MODE */}
          {!reordering && (
            <>
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
            </>
          )}

          {/* EDIT / REORDER MODE */}
          {reordering && (
            <div className="space-y-2 mb-3">
              <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
                <SortableContext
                  items={editSlots.map((_, i) => String(i))}
                  strategy={verticalListSortingStrategy}
                >
                  {editSlots.map((dm, idx) => (
                    <div key={idx}>
                      {/* Insert before this slot */}
                      {insertAt === idx ? (
                        <div className="mb-1.5">
                          <SlotAdder
                            meals={meals}
                            label={`Insert before "${meals.find(m => m.id === dm.mealId)?.name ?? dm.slot}"`}
                            onAdd={(data) => insertSlot(idx, data)}
                            onCancel={() => setInsertAt(null)}
                          />
                        </div>
                      ) : (
                        <button
                          onClick={() => setInsertAt(idx)}
                          className="w-full flex items-center gap-2 py-0.5 mb-1 group"
                        >
                          <div className="flex-1 h-px bg-divider group-hover:bg-green/40 transition-colors" />
                          <span className="text-[10px] text-text-muted group-hover:text-green transition-colors flex items-center gap-0.5">
                            <Plus size={10} />insert
                          </span>
                          <div className="flex-1 h-px bg-divider group-hover:bg-green/40 transition-colors" />
                        </button>
                      )}

                      <SortableMealRow
                        id={String(idx)}
                        dm={dm}
                        meal={meals.find((m) => m.id === dm.mealId)}
                        foods={foods}
                        onRemove={() => removeEditSlot(idx)}
                      />
                    </div>
                  ))}
                </SortableContext>
              </DndContext>

              {/* Insert at end */}
              {insertAt === editSlots.length ? (
                <SlotAdder
                  meals={meals}
                  label="Append at end"
                  onAdd={(data) => insertSlot(editSlots.length, data)}
                  onCancel={() => setInsertAt(null)}
                />
              ) : (
                <button
                  onClick={() => setInsertAt(editSlots.length)}
                  className="w-full h-8 rounded-xl border border-dashed border-divider text-xs text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-1"
                >
                  <Plus size={11} />Add slot at end
                </button>
              )}

              <div className="flex gap-2 pt-1">
                <button
                  onClick={saveReorder}
                  disabled={savingOrder}
                  className="flex-1 h-9 rounded-xl bg-green text-white text-sm font-semibold disabled:opacity-50 hover:bg-green/90 transition-colors"
                >
                  {savingOrder ? "Saving…" : "Save schedule"}
                </button>
                <button
                  onClick={cancelReorder}
                  className="px-4 h-9 rounded-xl border border-divider text-sm text-text-secondary hover:bg-bg-page transition-colors"
                >
                  Cancel
                </button>
              </div>
            </div>
          )}

          {/* Actions */}
          {!reordering && (
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
          )}
        </div>
      )}
    </div>
  );
}

// ── Manage slots modal ────────────────────────────────────────────────────────
function ManageSlotsModal({ diets, onUpdated, onClose }: {
  diets: DietDto[];
  onUpdated: (updated: DietDto[]) => void;
  onClose: () => void;
}) {
  const slotsInUse = useMemo(() => {
    const s = new Set<string>();
    for (const d of diets) {
      for (const dm of d.meals ?? []) { if (dm.slot) s.add(dm.slot); }
    }
    return Array.from(s).sort();
  }, [diets]);

  const [renames, setRenames] = useState<Record<string, string>>(() => {
    const init: Record<string, string> = {};
    for (const s of slotsInUse) init[s] = s;
    return init;
  });
  const [busy, setBusy] = useState(false);
  const [err,  setErr]  = useState<string | null>(null);

  const hasChanges = slotsInUse.some((s) => renames[s] && renames[s] !== s);

  const apply = async () => {
    setBusy(true); setErr(null);
    try {
      const toRename = slotsInUse.filter((s) => renames[s] && renames[s] !== s);
      const affected = diets.filter((d) =>
        (d.meals ?? []).some((dm) => toRename.includes(dm.slot))
      );
      const updated = await Promise.all(
        affected.map((diet) => {
          const meals = (diet.meals ?? []).map((dm) => {
            const nw = renames[dm.slot];
            return nw && nw !== dm.slot ? { ...dm, slot: nw } : dm;
          });
          return api.put<DietDto>(`/api/v1/diets/${diet.id}`, { ...diet, meals });
        })
      );
      onUpdated(updated);
      onClose();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Failed to apply");
    } finally { setBusy(false); }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center md:p-6 pointer-events-none">
        <div className="pointer-events-auto w-full md:max-w-md max-h-[85vh] flex flex-col bg-bg-card md:rounded-3xl rounded-t-3xl shadow-2xl">
          <div className="flex justify-center pt-3 md:hidden shrink-0">
            <div className="w-9 h-1 rounded-full bg-text-muted/30" />
          </div>
          <div className="flex items-center justify-between px-5 py-4 shrink-0">
            <div>
              <h2 className="text-[18px] font-bold text-text-primary">Manage Slots</h2>
              <p className="text-xs text-text-muted mt-0.5">Rename a slot to update it across all diets.</p>
            </div>
            <button onClick={onClose} className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-bg-page transition-colors">
              <X className="h-5 w-5 text-text-muted" />
            </button>
          </div>
          <div className="h-px bg-divider shrink-0" />

          <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
            {err && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{err}</p>}
            {slotsInUse.length === 0 ? (
              <p className="text-sm text-text-muted py-4 text-center">No slots in use yet.</p>
            ) : (
              slotsInUse.map((s) => {
                const c = SLOT_COLORS[s] ?? { bg: "#F0F0F5", text: "#666" };
                const current = renames[s] ?? s;
                const changed = current !== s;
                return (
                  <div key={s} className="flex items-center gap-3">
                    <span
                      className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
                      style={{ background: c.bg, color: c.text }}
                    >
                      {s}
                    </span>
                    <span className="text-text-muted text-xs shrink-0">→</span>
                    <select
                      value={current}
                      onChange={(e) => setRenames((prev) => ({ ...prev, [s]: e.target.value }))}
                      className={`flex-1 h-8 rounded-lg border px-2 text-sm bg-bg-card text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30 ${
                        changed ? "border-green/60" : "border-divider"
                      }`}
                    >
                      {PREDEFINED_SLOTS.map((ps) => (
                        <option key={ps} value={ps}>{ps}</option>
                      ))}
                    </select>
                  </div>
                );
              })
            )}
          </div>

          <div className="h-px bg-divider shrink-0" />
          <div className="px-5 py-4 shrink-0" style={{ paddingBottom: "max(16px, env(safe-area-inset-bottom))" }}>
            <button
              onClick={apply}
              disabled={busy || !hasChanges}
              className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors"
            >
              {busy ? "Applying…" : "Apply renames"}
            </button>
          </div>
        </div>
      </div>
    </>
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
  const [query,           setQuery]           = useState("");
  const [favOnly,         setFavOnly]         = useState(false);
  const [tagFilter,       setTagFilter]       = useState<number | null>(null);
  const [slotFilter,      setSlotFilter]      = useState<Slot | null>(null);
  const [highlightId,     setHighlightId]     = useState<number | null>(null);
  const [showCreate,      setShowCreate]      = useState(false);
  const [showManageSlots, setShowManageSlots] = useState(false);
  const [visible,         setVisible]         = useState(PAGE_SIZE);

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

  // Handle ?dietId= (exact highlight) or ?q= (search prefill) from navigation
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const dietId = params.get("dietId");
    const q = params.get("q");
    if (dietId) setHighlightId(parseInt(dietId));
    else if (q) setQuery(q);
  }, []);

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

  // Slots actually in use across all diets (in predefined order)
  const slotsInUse = useMemo(() => {
    const set = new Set<string>();
    for (const d of diets) {
      for (const dm of d.meals ?? []) { if (dm.slot) set.add(dm.slot); }
    }
    return PREDEFINED_SLOTS.filter((s) => set.has(s));
  }, [diets]);

  const filtered = useMemo(() => {
    if (highlightId != null) return diets.filter((d) => d.id === highlightId);
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
  }, [diets, query, favOnly, favs, tagFilter, slotFilter, meals, mealIngredients, highlightId]);

  // Reset pagination when filters change
  useEffect(() => { setVisible(PAGE_SIZE); }, [filtered]);

  const shown   = filtered.slice(0, visible);
  const hasMore = visible < filtered.length;

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

  const hasActiveFilter = favOnly || tagFilter != null || slotFilter != null || highlightId != null;

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
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowManageSlots(true)}
            className="px-3 h-9 rounded-xl border border-divider text-sm font-medium text-text-secondary hover:bg-bg-page transition-colors"
          >
            Slots
          </button>
          <button
            onClick={() => setShowCreate(true)}
            className="flex items-center gap-1.5 px-3 h-9 rounded-xl bg-green text-white text-sm font-medium hover:bg-green/90 transition-colors"
          >
            <Plus className="h-4 w-4" />New
          </button>
        </div>
      </div>

      {error && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{error}</p>}

      {/* Highlight mode banner */}
      {highlightId != null && (
        <div className="flex items-center justify-between px-3 py-2 rounded-xl bg-amber-50 border border-amber-200">
          <span className="text-xs text-amber-700">Showing 1 diet</span>
          <button
            onClick={() => setHighlightId(null)}
            className="text-xs font-medium text-amber-700 hover:underline"
          >
            Show all →
          </button>
        </div>
      )}

      <SearchBar value={query} onChange={setQuery} placeholder="Search name, tag, meal, ingredient…" />

      {/* Fav + Tag filter row */}
      <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
        <FilterChip
          label={`All (${diets.length})`}
          active={!hasActiveFilter}
          onClick={() => { setFavOnly(false); setTagFilter(null); setSlotFilter(null); setHighlightId(null); }}
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

      {/* Slot filter row (only slots in use) */}
      {slotsInUse.length > 0 && (
        <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-none">
          {slotsInUse.map((s) => {
            const c = SLOT_COLORS[s] ?? { text: "#666" };
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
      )}

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
        <>
          <div className="space-y-3">
            {shown.map((diet) => (
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
                defaultExpanded={highlightId === diet.id}
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
        <CreateDietModal
          meals={meals}
          foods={foods}
          onCreated={(d) => setDiets((prev) => [d, ...prev])}
          onClose={() => setShowCreate(false)}
        />
      )}

      {showManageSlots && (
        <ManageSlotsModal
          diets={diets}
          onUpdated={(updated) => {
            setDiets((prev) => prev.map((d) => updated.find((u) => u.id === d.id) ?? d));
          }}
          onClose={() => setShowManageSlots(false)}
        />
      )}
    </div>
  );
}
