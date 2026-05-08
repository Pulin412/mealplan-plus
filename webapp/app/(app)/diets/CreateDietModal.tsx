"use client";
import { useState, useMemo } from "react";
import { X, Plus, Search, ChevronRight, UtensilsCrossed, Apple } from "lucide-react";
import { api } from "@/lib/api/client";
import { PREDEFINED_SLOTS, PREDEFINED_SLOT_COLORS } from "@/lib/utils";
import type { components } from "@/lib/api/types.generated";

type DietDto = components["schemas"]["DietDto"];
type MealDto = components["schemas"]["MealDto"];
type FoodDto = components["schemas"]["FoodDto"];

const STD_SLOTS = PREDEFINED_SLOTS;

const DAYS: readonly [number, string][] = [
  [0, "Any"], [1, "Mon"], [2, "Tue"], [3, "Wed"],
  [4, "Thu"], [5, "Fri"], [6, "Sat"], [7, "Sun"],
];

const SLOT_COLORS = PREDEFINED_SLOT_COLORS;

interface SlotEntry {
  slot: string;
  dayOfWeek: number;
  type: "meal" | "food";
  mealId?: number;
  mealName?: string;
  foodId?: number;
  foodName?: string;
  quantity?: number;
}

// ── Slot picker ───────────────────────────────────────────────────────────────
function SlotPicker({ meals, foods, onAdd, onCancel }: {
  meals: MealDto[];
  foods: FoodDto[];
  onAdd: (entry: SlotEntry) => void;
  onCancel: () => void;
}) {
  const [slot,       setSlot]       = useState("Breakfast");
  const [customSlot, setCustomSlot] = useState("");
  const [isCustom,   setIsCustom]   = useState(false);
  const [day,        setDay]        = useState(0);
  const [tab,        setTab]        = useState<"meals" | "foods">("meals");
  const [search,     setSearch]     = useState("");
  const [selMeal,    setSelMeal]    = useState<MealDto | null>(null);
  const [selFood,    setSelFood]    = useState<FoodDto | null>(null);
  const [qty,        setQty]        = useState("100");

  const finalSlot = isCustom ? customSlot.trim() : slot;

  const filteredMeals = useMemo(() =>
    search.trim()
      ? meals.filter((m) => m.name.toLowerCase().includes(search.toLowerCase()))
      : meals,
    [meals, search]
  );

  const filteredFoods = useMemo(() =>
    search.length >= 2
      ? foods.filter((f) => f.name.toLowerCase().includes(search.toLowerCase())).slice(0, 12)
      : [],
    [foods, search]
  );

  const canAdd = finalSlot.length > 0 && (
    (tab === "meals" && selMeal != null) ||
    (tab === "foods" && selFood != null && !isNaN(parseFloat(qty)) && parseFloat(qty) > 0)
  );

  const handleAdd = () => {
    if (!canAdd) return;
    if (tab === "meals" && selMeal?.id != null) {
      onAdd({ slot: finalSlot, dayOfWeek: day, type: "meal", mealId: selMeal.id, mealName: selMeal.name });
    } else if (tab === "foods" && selFood?.id != null) {
      onAdd({ slot: finalSlot, dayOfWeek: day, type: "food", foodId: selFood.id, foodName: selFood.name, quantity: parseFloat(qty) });
    }
  };

  const switchTab = (t: "meals" | "foods") => {
    setTab(t); setSearch(""); setSelMeal(null); setSelFood(null);
  };

  return (
    <div className="rounded-2xl border border-divider bg-bg-page p-4 space-y-4">

      {/* Slot type */}
      <div>
        <p className="text-[11px] font-semibold text-text-muted uppercase tracking-wide mb-2">Meal slot</p>
        <div className="flex flex-wrap gap-2">
          {STD_SLOTS.map((s) => {
            const c = SLOT_COLORS[s];
            const active = !isCustom && slot === s;
            return (
              <button
                key={s}
                onClick={() => { setSlot(s); setIsCustom(false); }}
                className="px-3 h-8 rounded-full text-sm font-medium border transition-colors"
                style={{
                  background:  active ? c.bg  : "transparent",
                  borderColor: active ? c.text : "var(--color-divider)",
                  color:       active ? c.text : "var(--color-text-secondary)",
                }}
              >
                {s}
              </button>
            );
          })}
          <button
            onClick={() => setIsCustom(true)}
            className={`px-3 h-8 rounded-full text-sm font-medium border transition-colors ${
              isCustom
                ? "bg-text-primary text-bg-card border-text-primary"
                : "border-divider text-text-secondary"
            }`}
          >
            Custom
          </button>
        </div>
        {isCustom && (
          <input
            autoFocus value={customSlot} onChange={(e) => setCustomSlot(e.target.value)}
            placeholder="e.g. Post-workout"
            className="mt-2 w-full h-9 px-3 rounded-xl border border-divider bg-bg-card text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
          />
        )}
      </div>

      {/* Day */}
      <div>
        <p className="text-[11px] font-semibold text-text-muted uppercase tracking-wide mb-2">Day</p>
        <div className="flex gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          {DAYS.map(([d, label]) => (
            <button
              key={d}
              onClick={() => setDay(d)}
              className={`shrink-0 px-2.5 h-7 rounded-full text-xs font-medium transition-colors ${
                day === d
                  ? "bg-text-primary text-bg-card"
                  : "border border-divider text-text-secondary bg-bg-card"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Source tabs */}
      <div>
        <div className="flex rounded-xl border border-divider overflow-hidden mb-3">
          {(["meals", "foods"] as const).map((t) => (
            <button
              key={t}
              onClick={() => switchTab(t)}
              className={`flex-1 h-9 text-sm font-medium transition-colors ${
                tab === t ? "bg-text-primary text-bg-card" : "text-text-secondary hover:bg-bg-page"
              }`}
            >
              {t === "meals" ? "Existing meal" : "Custom food"}
            </button>
          ))}
        </div>

        {/* Search */}
        <div className="relative mb-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-text-muted pointer-events-none" />
          <input
            value={search}
            onChange={(e) => { setSearch(e.target.value); setSelMeal(null); setSelFood(null); }}
            placeholder={tab === "meals" ? "Search meals…" : "Search foods (min 2 chars)…"}
            className="w-full h-10 pl-9 pr-3 rounded-xl border border-divider bg-bg-card text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
          />
        </div>

        {/* Meals */}
        {tab === "meals" && (
          selMeal ? (
            <div className="flex items-center gap-2 px-3 py-2.5 rounded-xl border border-green/40 bg-green/5">
              <UtensilsCrossed className="h-4 w-4 text-green shrink-0" />
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-text-primary truncate">{selMeal.name}</p>
                <p className="text-xs text-text-muted">{selMeal.items?.length ?? 0} foods</p>
              </div>
              <button onClick={() => setSelMeal(null)}><X className="h-4 w-4 text-text-muted" /></button>
            </div>
          ) : (
            <ul className="max-h-48 overflow-y-auto rounded-xl border border-divider divide-y divide-divider">
              {filteredMeals.length === 0 ? (
                <li className="px-3 py-4 text-xs text-text-muted text-center">
                  {search ? "No meals found" : "No meals yet — create meals on the Meals page"}
                </li>
              ) : filteredMeals.map((m) => (
                <li key={m.id}>
                  <button
                    onClick={() => setSelMeal(m)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 hover:bg-bg-page transition-colors text-left"
                  >
                    <div className="w-8 h-8 rounded-lg bg-bg-page flex items-center justify-center shrink-0">
                      <UtensilsCrossed className="h-4 w-4 text-text-muted" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-text-primary truncate">{m.name}</p>
                      <p className="text-xs text-text-muted">{m.items?.length ?? 0} foods</p>
                    </div>
                    <ChevronRight className="h-4 w-4 text-text-muted/40 shrink-0" />
                  </button>
                </li>
              ))}
            </ul>
          )
        )}

        {/* Foods */}
        {tab === "foods" && (
          selFood ? (
            <div className="space-y-2">
              <div className="flex items-center gap-2 px-3 py-2.5 rounded-xl border border-green/40 bg-green/5">
                <Apple className="h-4 w-4 text-green shrink-0" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-text-primary truncate">{selFood.name}</p>
                  {selFood.brand && <p className="text-xs text-text-muted">{selFood.brand}</p>}
                </div>
                <button onClick={() => setSelFood(null)}><X className="h-4 w-4 text-text-muted" /></button>
              </div>
              <div className="flex items-center gap-2">
                <label className="text-xs text-text-muted shrink-0">Quantity</label>
                <input
                  type="number" value={qty} onChange={(e) => setQty(e.target.value)} min={1}
                  className="w-24 h-9 px-3 rounded-xl border border-divider bg-bg-card text-sm text-center text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30"
                />
                <span className="text-sm text-text-muted">g</span>
                {(() => {
                  const q = parseFloat(qty);
                  return !isNaN(q) && q > 0
                    ? <span className="text-xs text-text-muted ml-1">= {Math.round(selFood.caloriesPer100 * q / 100)} kcal</span>
                    : null;
                })()}
              </div>
            </div>
          ) : (
            <ul className="max-h-48 overflow-y-auto rounded-xl border border-divider divide-y divide-divider">
              {search.length < 2 ? (
                <li className="px-3 py-4 text-xs text-text-muted text-center">Type at least 2 characters to search foods</li>
              ) : filteredFoods.length === 0 ? (
                <li className="px-3 py-4 text-xs text-text-muted text-center">No foods found</li>
              ) : filteredFoods.map((f) => (
                <li key={f.id}>
                  <button
                    onClick={() => setSelFood(f)}
                    className="w-full flex items-center gap-3 px-3 py-2.5 hover:bg-bg-page transition-colors text-left"
                  >
                    <div className="w-8 h-8 rounded-lg bg-bg-page flex items-center justify-center shrink-0">
                      <Apple className="h-4 w-4 text-text-muted" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm text-text-primary truncate">{f.name}</p>
                      <p className="text-xs text-text-muted">
                        {f.caloriesPer100} kcal/100g{f.brand ? ` · ${f.brand}` : ""}
                      </p>
                    </div>
                    <ChevronRight className="h-4 w-4 text-text-muted/40 shrink-0" />
                  </button>
                </li>
              ))}
            </ul>
          )
        )}
      </div>

      {/* Actions */}
      <div className="flex gap-2">
        <button
          onClick={handleAdd} disabled={!canAdd}
          className="flex-1 h-10 rounded-xl bg-green text-white text-sm font-semibold disabled:opacity-40 hover:bg-green/90 transition-colors"
        >
          Add to diet
        </button>
        <button onClick={onCancel} className="px-4 h-10 rounded-xl border border-divider text-sm text-text-secondary hover:bg-bg-page transition-colors">
          Cancel
        </button>
      </div>
    </div>
  );
}

// ── Main modal ─────────────────────────────────────────────────────────────────
export function CreateDietModal({ meals, foods, onCreated, onClose }: {
  meals: MealDto[];
  foods: FoodDto[];
  onCreated: (d: DietDto) => void;
  onClose: () => void;
}) {
  const [name,    setName]    = useState("");
  const [desc,    setDesc]    = useState("");
  const [cals,    setCals]    = useState("");
  const [slots,   setSlots]   = useState<SlotEntry[]>([]);
  const [picking, setPicking] = useState(false);
  const [busy,    setBusy]    = useState(false);
  const [err,     setErr]     = useState<string | null>(null);

  const removeSlot = (idx: number) => setSlots((prev) => prev.filter((_, i) => i !== idx));

  const submit = async () => {
    if (!name.trim()) return;
    setBusy(true);
    setErr(null);
    try {
      const mealAssignments: { slot: string; dayOfWeek: number; mealId: number }[] = [];

      for (const entry of slots) {
        if (entry.type === "meal" && entry.mealId != null) {
          mealAssignments.push({ slot: entry.slot, dayOfWeek: entry.dayOfWeek, mealId: entry.mealId });
        } else if (entry.type === "food" && entry.foodId != null) {
          const newMeal = await api.post<{ id?: number }>("/api/v1/meals", {
            name: entry.foodName ?? "Custom",
            slot: entry.slot,
            items: [{ foodId: entry.foodId, quantity: entry.quantity ?? 100, unit: "GRAM" }],
          });
          if (newMeal.id != null) {
            mealAssignments.push({ slot: entry.slot, dayOfWeek: entry.dayOfWeek, mealId: newMeal.id });
          }
        }
      }

      const d = await api.post<DietDto>("/api/v1/diets", {
        name: name.trim(),
        description: desc.trim() || undefined,
        targetCalories: cals ? parseFloat(cals) : undefined,
        meals: mealAssignments,
      });
      onCreated(d);
      onClose();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Failed to create");
    } finally { setBusy(false); }
  };

  return (
    <>
      {/* Backdrop */}
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />

      {/* Modal: bottom sheet on mobile, centered dialog on md+ */}
      <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center md:p-6 pointer-events-none">
        <div className="pointer-events-auto w-full md:max-w-lg max-h-[92vh] md:max-h-[85vh] flex flex-col bg-bg-card md:rounded-3xl rounded-t-3xl shadow-2xl">

          {/* Drag handle (mobile) */}
          <div className="flex justify-center pt-3 md:hidden shrink-0">
            <div className="w-9 h-1 rounded-full bg-text-muted/30" />
          </div>

          {/* Header */}
          <div className="flex items-center justify-between px-5 py-4 shrink-0">
            <h2 className="text-[18px] font-bold text-text-primary">New Diet</h2>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-bg-page transition-colors"
            >
              <X className="h-5 w-5 text-text-muted" />
            </button>
          </div>
          <div className="h-px bg-divider shrink-0" />

          {/* Scrollable body */}
          <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
            {err && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{err}</p>}

            {/* Basic info */}
            <section className="space-y-3">
              <p className="text-[13px] font-semibold text-text-muted uppercase tracking-wide">Basic info</p>
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
            </section>

            <div className="h-px bg-divider" />

            {/* Meal schedule */}
            <section>
              <div className="flex items-center justify-between mb-3">
                <p className="text-[13px] font-semibold text-text-muted uppercase tracking-wide">Meal schedule</p>
                {slots.length > 0 && (
                  <span className="text-xs text-text-muted">{slots.length} slot{slots.length !== 1 ? "s" : ""}</span>
                )}
              </div>

              {/* Added slots */}
              {slots.length > 0 && (
                <div className="space-y-2 mb-3">
                  {slots.map((entry, idx) => {
                    const c = SLOT_COLORS[entry.slot];
                    const dayLabel = DAYS.find(([d]) => d === entry.dayOfWeek)?.[1] ?? "";
                    return (
                      <div key={idx} className="flex items-center gap-2 px-3 py-2.5 rounded-xl border border-divider bg-bg-page">
                        <span
                          className="text-[11px] font-medium px-2 py-0.5 rounded-full shrink-0"
                          style={{ background: c?.bg ?? "#F0F0F0", color: c?.text ?? "#555" }}
                        >
                          {entry.slot}
                        </span>
                        {entry.dayOfWeek !== 0 && (
                          <span className="text-[11px] text-text-muted shrink-0">{dayLabel}</span>
                        )}
                        <span className="flex-1 text-sm text-text-primary truncate">
                          {entry.type === "meal"
                            ? entry.mealName
                            : `${entry.foodName} · ${entry.quantity}g`}
                        </span>
                        {entry.type === "food" && (
                          <span className="text-[10px] px-1.5 py-0.5 rounded-full bg-bg-card border border-divider text-text-muted shrink-0">
                            food
                          </span>
                        )}
                        <button
                          onClick={() => removeSlot(idx)}
                          className="p-1 rounded-lg hover:bg-bg-card transition-colors shrink-0"
                        >
                          <X className="h-3.5 w-3.5 text-text-muted" />
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Slot picker or add button */}
              {picking ? (
                <SlotPicker
                  meals={meals}
                  foods={foods}
                  onAdd={(entry) => { setSlots((prev) => [...prev, entry]); setPicking(false); }}
                  onCancel={() => setPicking(false)}
                />
              ) : (
                <button
                  onClick={() => setPicking(true)}
                  className="w-full h-11 rounded-xl border-2 border-dashed border-divider text-sm text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-2"
                >
                  <Plus className="h-4 w-4" />Add meal slot
                </button>
              )}
            </section>
          </div>

          {/* Footer */}
          <div className="h-px bg-divider shrink-0" />
          <div className="px-5 py-4 pb-safe shrink-0" style={{ paddingBottom: "max(16px, env(safe-area-inset-bottom))" }}>
            <button
              onClick={submit}
              disabled={busy || !name.trim()}
              className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors"
            >
              {busy ? "Creating…" : `Create diet${slots.length > 0 ? ` · ${slots.length} slot${slots.length !== 1 ? "s" : ""}` : ""}`}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
