"use client";
import { useState, useMemo } from "react";
import { X, ArrowLeft, Search, Star, ChevronDown, ChevronUp, ChevronRight } from "lucide-react";
import { PREDEFINED_SLOT_COLORS } from "@/lib/utils";
import type { components } from "@/lib/api/types.generated";

type DietDto     = components["schemas"]["DietDto"];
type DietMealDto = components["schemas"]["DietMealDto"];
type MealDto     = components["schemas"]["MealDto"];
type FoodDto     = components["schemas"]["FoodDto"];
type TagDto      = components["schemas"]["TagDto"];

const SLOT_COLORS = PREDEFINED_SLOT_COLORS;

const DAY_NAMES: Record<number, string> = {
  0: "Any", 1: "Mon", 2: "Tue", 3: "Wed",
  4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun",
};

function hexToRgba(hex: string, alpha: number) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

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

type Macros = ReturnType<typeof calcDietMacros>;

// ── MacroStrip ────────────────────────────────────────────────────────────────
function MacroStrip({ macros, large = false }: { macros: Macros; large?: boolean }) {
  if (macros.kcal === 0) return null;
  const boldCls  = large ? "text-[13px] font-bold" : "text-[10px] font-semibold";
  const badgeCls = large ? "text-[11px] font-medium px-2 py-0.5 rounded-full" : "text-[10px] font-medium px-1.5 py-0.5 rounded-full";
  return (
    <div className="flex items-center flex-wrap gap-1.5">
      <span className={`${boldCls} text-text-primary`}>{macros.kcal} kcal</span>
      {macros.protein > 0 && <span className={badgeCls} style={{ background: "#2E7D5218", color: "#2E7D52" }}>P {macros.protein}g</span>}
      {macros.carbs   > 0 && <span className={badgeCls} style={{ background: "#C0520018", color: "#C05200" }}>C {macros.carbs}g</span>}
      {macros.fat     > 0 && <span className={badgeCls} style={{ background: "#7C3AED18", color: "#7C3AED" }}>F {macros.fat}g</span>}
      {macros.gi != null  && <span className={badgeCls} style={{ background: "#F59E0B18", color: "#B45309" }}>GI {macros.gi}</span>}
    </div>
  );
}

// ── TagChip (read-only) ───────────────────────────────────────────────────────
function TagChip({ tag }: { tag: TagDto }) {
  const color = tag.color ?? "#9CA3AF";
  return (
    <span
      className="inline-flex items-center text-[11px] font-medium px-2 py-0.5 rounded-full"
      style={{ background: hexToRgba(color, 0.15), color }}
    >
      {tag.name}
    </span>
  );
}

// ── FilterChip ────────────────────────────────────────────────────────────────
function FilterChip({ label, active, onClick, color }: {
  label: string; active: boolean; onClick: () => void; color?: string;
}) {
  if (color) {
    return (
      <button
        onClick={onClick}
        className="shrink-0 px-3 h-7 rounded-full text-[11px] font-medium border transition-colors"
        style={{
          background:  active ? hexToRgba(color, 0.2) : "transparent",
          borderColor: active ? color : "var(--color-divider)",
          color:       active ? color : "var(--color-text-secondary)",
        }}
      >
        {label}
      </button>
    );
  }
  return (
    <button
      onClick={onClick}
      className={`shrink-0 px-3 h-7 rounded-full text-[11px] font-medium transition-colors ${
        active ? "bg-text-primary text-bg-card" : "border border-divider text-text-secondary"
      }`}
    >
      {label}
    </button>
  );
}

// ── DietRow (compact pick-list row) ───────────────────────────────────────────
function DietRow({ diet, meals, foods, isFav, isAssigned, onSelect }: {
  diet: DietDto;
  meals: MealDto[];
  foods: FoodDto[];
  isFav: boolean;
  isAssigned: boolean;
  onSelect: () => void;
}) {
  const macros    = calcDietMacros(diet, meals, foods);
  const tags      = diet.tags ?? [];
  const mealCount = (diet.meals ?? []).length;

  return (
    <button
      onClick={onSelect}
      className={`w-full text-left flex items-start gap-3 px-4 py-3.5 hover:bg-bg-page transition-colors ${isAssigned ? "bg-green/5" : ""}`}
    >
      <div className="flex-1 min-w-0 space-y-1">
        <div className="flex items-center gap-1.5">
          <p className="text-[14px] font-semibold text-text-primary truncate flex-1">{diet.name}</p>
          {isFav && <Star size={11} fill="#F59E0B" stroke="#F59E0B" className="shrink-0" />}
          {isAssigned && (
            <span className="shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded-full bg-green/10 text-green">assigned</span>
          )}
        </div>
        {diet.description && (
          <p className="text-[12px] text-text-muted line-clamp-1">{diet.description}</p>
        )}
        {tags.length > 0 && (
          <div className="flex flex-wrap gap-1">
            {tags.slice(0, 3).map((t) => <TagChip key={t.id} tag={t} />)}
            {tags.length > 3 && <span className="text-[10px] text-text-muted">+{tags.length - 3} more</span>}
          </div>
        )}
        <MacroStrip macros={macros} />
      </div>
      <div className="flex flex-col items-end gap-1.5 shrink-0 pt-0.5">
        {mealCount > 0 && (
          <span className="text-[10px] font-medium px-2 py-0.5 rounded-full bg-bg-page border border-divider text-text-muted">
            {mealCount} slot{mealCount !== 1 ? "s" : ""}
          </span>
        )}
        <ChevronRight size={15} className="text-text-muted" />
      </div>
    </button>
  );
}

// ── MealSlotRow (expandable, detail phase) ────────────────────────────────────
function MealSlotRow({ dm, meal, foods, isOpen, onToggle }: {
  dm: DietMealDto;
  meal: MealDto | undefined;
  foods: FoodDto[];
  isOpen: boolean;
  onToggle: () => void;
}) {
  const c = SLOT_COLORS[dm.slot] ?? { bg: "#F0F0F0", text: "#555" };
  const items = meal?.items ?? [];
  const totalKcal = items.reduce((sum, item) => {
    const food = foods.find((f) => f.id === item.foodId);
    return sum + (food ? Math.round(food.caloriesPer100 * item.quantity / 100) : 0);
  }, 0);

  return (
    <div className="rounded-xl border border-divider overflow-hidden">
      <button
        className="w-full flex items-center gap-2 px-3 py-2.5 bg-bg-page hover:bg-bg-card transition-colors text-left"
        onClick={onToggle}
      >
        <span
          className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
          style={{ background: c.bg, color: c.text }}
        >
          {dm.slot}
        </span>
        {dm.dayOfWeek !== 0 && (
          <span className="text-[11px] text-text-muted shrink-0">{DAY_NAMES[dm.dayOfWeek]}</span>
        )}
        <span className="text-sm text-text-primary truncate flex-1">
          {meal?.name ?? `Meal #${dm.mealId}`}
        </span>
        {totalKcal > 0 && (
          <span className="text-[11px] text-text-muted shrink-0">{totalKcal} kcal</span>
        )}
        {isOpen
          ? <ChevronUp size={14} className="text-text-muted shrink-0" />
          : <ChevronDown size={14} className="text-text-muted shrink-0" />}
      </button>

      {isOpen && (
        <div className="px-3 pb-2 pt-1 bg-bg-card space-y-1">
          {items.length === 0 ? (
            <p className="text-xs text-text-muted py-1">No ingredients</p>
          ) : (
            items.map((item, i) => {
              const food = foods.find((f) => f.id === item.foodId);
              const kcal = food ? Math.round(food.caloriesPer100 * item.quantity / 100) : null;
              return (
                <div key={i} className="flex items-center gap-2 text-xs">
                  <span className="w-1.5 h-1.5 rounded-full bg-text-muted/40 shrink-0" />
                  <span className="text-text-primary flex-1 truncate">
                    {food?.name ?? `Food #${item.foodId}`}
                  </span>
                  <span className="text-text-muted shrink-0">
                    {item.quantity}g{kcal ? ` · ${kcal} kcal` : ""}
                  </span>
                </div>
              );
            })
          )}
          {items.length > 0 && (
            <div className="flex justify-end mt-1 pt-1.5 border-t border-divider">
              <span className="text-[11px] font-semibold text-text-secondary">Total: {totalKcal} kcal</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

// ── Main modal ────────────────────────────────────────────────────────────────
export function DietPickerModal({ date, currentDietId, diets, meals, foods, tags, favs, onClose, onAssign }: {
  date:          string;
  currentDietId: number | null;
  diets:         DietDto[];
  meals:         MealDto[];
  foods:         FoodDto[];
  tags:          TagDto[];
  favs:          Set<number>;
  onClose:       () => void;
  onAssign:      (dietId: number | null) => void;
}) {
  const assignedDiet = currentDietId != null ? (diets.find((d) => d.id === currentDietId) ?? null) : null;
  const [phase,     setPhase]     = useState<"pick" | "detail">(assignedDiet ? "detail" : "pick");
  const [selected,  setSelected]  = useState<DietDto | null>(assignedDiet);
  const [query,     setQuery]     = useState("");
  const [favOnly,   setFavOnly]   = useState(false);
  const [tagFilter, setTagFilter] = useState<number | null>(null);
  const [openSlots, setOpenSlots] = useState<Set<number>>(new Set());

  const dateLabel = new Date(date + "T00:00:00").toLocaleDateString("en-GB", {
    weekday: "short", day: "numeric", month: "short",
  });

  const filtered = useMemo(() => {
    let list = diets;
    if (query.trim()) {
      const q = query.toLowerCase();
      list = list.filter((d) =>
        d.name.toLowerCase().includes(q) ||
        (d.description ?? "").toLowerCase().includes(q) ||
        (d.tags ?? []).some((t) => t.name.toLowerCase().includes(q))
      );
    }
    if (favOnly) list = list.filter((d) => d.id != null && favs.has(d.id));
    if (tagFilter != null) list = list.filter((d) => (d.tagIds ?? []).includes(tagFilter));
    return list;
  }, [diets, query, favOnly, favs, tagFilter]);

  const toggleSlot = (i: number) => {
    setOpenSlots((prev) => {
      const s = new Set(prev);
      if (s.has(i)) { s.delete(i); } else { s.add(i); }
      return s;
    });
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center md:p-6 pointer-events-none">
        <div className="pointer-events-auto w-full md:max-w-lg max-h-[92vh] md:max-h-[85vh] flex flex-col bg-bg-card md:rounded-3xl rounded-t-3xl shadow-2xl">

          {/* Drag handle */}
          <div className="flex justify-center pt-3 md:hidden shrink-0">
            <div className="w-9 h-1 rounded-full bg-text-muted/30" />
          </div>

          {/* Header */}
          <div className="flex items-center gap-2 px-5 py-4 shrink-0">
            {phase === "detail" && (
              <button
                onClick={() => setPhase("pick")}
                className="p-1 -ml-1 rounded-lg hover:bg-bg-page transition-colors shrink-0"
              >
                <ArrowLeft size={18} className="text-text-muted" />
              </button>
            )}
            <div className="flex-1 min-w-0">
              <p className="text-[10px] font-bold text-text-muted uppercase tracking-widest">{dateLabel}</p>
              <h2 className="text-[17px] font-bold text-text-primary leading-tight truncate">
                {phase === "pick" ? "Pick a diet" : (selected?.name ?? "")}
              </h2>
            </div>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-bg-page transition-colors shrink-0"
            >
              <X size={18} className="text-text-muted" />
            </button>
          </div>
          <div className="h-px bg-divider shrink-0" />

          {/* ── Phase: pick ── */}
          {phase === "pick" && (
            <>
              <div className="px-4 pt-3 pb-2 shrink-0 space-y-2">
                {/* Search bar */}
                <div className="flex items-center gap-2 bg-bg-page rounded-xl border border-divider px-3">
                  <Search size={14} className="text-text-muted shrink-0" />
                  <input
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder="Search name, tag, description…"
                    className="flex-1 py-2.5 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder"
                  />
                  {query && (
                    <button onClick={() => setQuery("")}>
                      <X size={13} className="text-text-muted" />
                    </button>
                  )}
                </div>

                {/* Filter chips */}
                <div className="flex gap-2 overflow-x-auto pb-0.5 scrollbar-none">
                  <FilterChip
                    label={`All (${diets.length})`}
                    active={!favOnly && tagFilter == null}
                    onClick={() => { setFavOnly(false); setTagFilter(null); }}
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
              </div>

              <div className="h-px bg-divider shrink-0" />

              {/* Diet list */}
              <div className="flex-1 overflow-y-auto divide-y divide-divider">
                {filtered.length === 0 ? (
                  <p className="text-sm text-text-muted text-center py-10">
                    {query ? `No results for "${query}"` : "No diets yet"}
                  </p>
                ) : (
                  filtered.map((diet) => (
                    <DietRow
                      key={diet.id}
                      diet={diet}
                      meals={meals}
                      foods={foods}
                      isFav={diet.id != null && favs.has(diet.id)}
                      isAssigned={diet.id === currentDietId}
                      onSelect={() => {
                        setSelected(diet);
                        setOpenSlots(new Set());
                        setPhase("detail");
                      }}
                    />
                  ))
                )}
              </div>
            </>
          )}

          {/* ── Phase: detail ── */}
          {phase === "detail" && selected && (
            <>
              <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
                {selected.description && (
                  <p className="text-sm text-text-muted">{selected.description}</p>
                )}

                {(selected.tags ?? []).length > 0 && (
                  <div className="flex flex-wrap gap-1.5">
                    {(selected.tags ?? []).map((t) => <TagChip key={t.id} tag={t} />)}
                  </div>
                )}

                {/* Full macro strip */}
                {(() => {
                  const macros = calcDietMacros(selected, meals, foods);
                  return macros.kcal > 0 ? (
                    <div className="p-3 bg-bg-page rounded-xl border border-divider">
                      <MacroStrip macros={macros} large />
                    </div>
                  ) : null;
                })()}

                {/* Meal schedule */}
                <div>
                  <p className="text-[10px] font-bold tracking-widest text-text-muted uppercase mb-2">
                    Meal schedule · {(selected.meals ?? []).length} slot{(selected.meals ?? []).length !== 1 ? "s" : ""}
                  </p>
                  {(selected.meals ?? []).length === 0 ? (
                    <p className="text-sm text-text-muted">No meals assigned to this diet.</p>
                  ) : (
                    <div className="space-y-1.5">
                      {(selected.meals ?? []).map((dm, i) => (
                        <MealSlotRow
                          key={i}
                          dm={dm}
                          meal={meals.find((m) => m.id === dm.mealId)}
                          foods={foods}
                          isOpen={openSlots.has(i)}
                          onToggle={() => toggleSlot(i)}
                        />
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Footer */}
              <div className="h-px bg-divider shrink-0" />
              <div
                className="px-5 py-4 shrink-0 flex flex-col gap-2"
                style={{ paddingBottom: "max(16px, env(safe-area-inset-bottom))" }}
              >
                <button
                  onClick={() => { onAssign(selected.id!); onClose(); }}
                  disabled={selected.id === currentDietId}
                  className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors"
                >
                  {selected.id === currentDietId ? "Already assigned" : `Assign to ${dateLabel}`}
                </button>
                {currentDietId != null && (
                  <button
                    onClick={() => { onAssign(null); onClose(); }}
                    className="w-full h-10 rounded-xl border border-red-200 text-red-500 text-sm font-medium hover:bg-red-50 transition-colors"
                  >
                    Remove assignment
                  </button>
                )}
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
}
