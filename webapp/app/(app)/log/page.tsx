"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight, X, Plus, Search, Check } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type DailyLogDto = components["schemas"]["DailyLogDto"];
type LoggedFoodDto = components["schemas"]["LoggedFoodDto"];
type FoodDto = components["schemas"]["FoodDto"];

type Slot = "Breakfast" | "Lunch" | "Dinner";
const SLOTS: { key: Slot; emoji: string; color: string; bg: string }[] = [
  { key: "Breakfast", emoji: "🌅", color: "#F59E0B", bg: "#FFF8E6" },
  { key: "Lunch",     emoji: "☀️",  color: "#2E7D52", bg: "#E8F5EE" },
  { key: "Dinner",   emoji: "🌙", color: "#7C3AED", bg: "#F3EEFF" },
];

function todayStr() { return new Date().toISOString().split("T")[0]; }
function addDays(dateStr: string, days: number) {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + days);
  return d.toISOString().split("T")[0];
}
function formatDateLabel(dateStr: string) {
  const today = todayStr();
  if (dateStr === today) return "Today";
  if (dateStr === addDays(today, -1)) return "Yesterday";
  if (dateStr === addDays(today, 1)) return "Tomorrow";
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "short" });
}
function calcFoodCals(food: FoodDto, lf: LoggedFoodDto) {
  const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
  return (food.caloriesPer100 * grams) / 100;
}
function calcMacro(foods: FoodDto[], loggedFoods: LoggedFoodDto[], per: keyof Pick<FoodDto, "proteinPer100" | "carbsPer100" | "fatPer100">) {
  return loggedFoods.reduce((sum, lf) => {
    const food = foods.find((f) => f.id === lf.foodId);
    if (!food) return sum;
    const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food[per] * grams) / 100;
  }, 0);
}

function FoodSearch({ foods, slot, onAdd, onClose }: {
  foods: FoodDto[]; slot: Slot;
  onAdd: (food: FoodDto, qty: number, slot: Slot) => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState("");
  const [qty, setQty] = useState("100");
  const [selected, setSelected] = useState<FoodDto | null>(null);
  const filtered = query.length >= 2
    ? foods.filter((f) => f.name.toLowerCase().includes(query.toLowerCase())).slice(0, 8)
    : [];

  return (
    <div className="mt-3 rounded-xl bg-bg-page p-3 space-y-3 border border-divider">
      {!selected ? (
        <>
          <div className="flex items-center gap-2 bg-bg-card rounded-lg border border-divider px-3">
            <Search size={14} className="text-text-muted shrink-0" />
            <input
              autoFocus
              placeholder="Search foods…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="flex-1 py-2 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder"
            />
            <button onClick={onClose} className="text-text-muted hover:text-text-primary">
              <X size={14} />
            </button>
          </div>
          {filtered.length > 0 && (
            <ul className="divide-y divide-divider">
              {filtered.map((food) => (
                <li key={food.id}>
                  <button
                    onClick={() => setSelected(food)}
                    className="w-full flex items-center justify-between py-2.5 px-1 text-sm hover:bg-bg-card rounded transition-colors"
                  >
                    <div className="text-left">
                      <span className="font-medium text-text-primary">{food.name}</span>
                      {food.brand && <span className="text-text-muted ml-1.5">· {food.brand}</span>}
                    </div>
                    <span className="text-text-muted text-xs">{food.caloriesPer100} kcal/100g</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {query.length >= 2 && filtered.length === 0 && (
            <p className="text-sm text-text-muted text-center py-2">No results</p>
          )}
        </>
      ) : (
        <div className="space-y-3">
          <p className="text-sm font-semibold text-text-primary">{selected.name}</p>
          <div className="flex items-center gap-3">
            <div className="flex-1">
              <p className="text-[10px] font-bold text-text-muted uppercase mb-1">GRAMS</p>
              <input
                type="number"
                value={qty}
                min={1}
                onChange={(e) => setQty(e.target.value)}
                className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary"
              />
            </div>
            <div className="text-xs text-text-muted pt-5">
              ≈ {Math.round((selected.caloriesPer100 * parseFloat(qty || "0")) / 100)} kcal
            </div>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => {
                const q = parseFloat(qty);
                if (!isNaN(q) && q > 0) { onAdd(selected, q, slot); onClose(); }
              }}
              className="flex-1 rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card flex items-center justify-center gap-1.5"
            >
              <Check size={14} /> Add to {slot}
            </button>
            <button
              onClick={() => setSelected(null)}
              className="px-4 rounded-xl border border-divider text-sm font-medium text-text-secondary"
            >
              Back
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function LogPage() {
  const { user } = useAuth();
  const [date, setDate] = useState(todayStr());
  const [log, setLog] = useState<DailyLogDto | null>(null);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [activeSearch, setActiveSearch] = useState<Slot | null>(null);

  const loadData = useCallback(async () => {
    if (!user) return;
    setLoading(true); setError(null);
    try {
      const [logs, allFoods] = await Promise.all([
        api.get<DailyLogDto[]>("/api/v1/daily-logs"),
        api.get<FoodDto[]>("/api/v1/foods"),
      ]);
      setFoods(allFoods);
      setLog(logs.find((l) => l.date === date) ?? null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user, date]);

  useEffect(() => { loadData(); }, [loadData]);

  const saveLog = async (updatedLog: DailyLogDto) => {
    setSaving(true); setError(null);
    try {
      let saved: DailyLogDto;
      if (updatedLog.id && updatedLog.id > 0) {
        saved = await api.put<DailyLogDto>(`/api/v1/daily-logs/${updatedLog.id}`, updatedLog);
      } else {
        saved = await api.post<DailyLogDto>("/api/v1/daily-logs", updatedLog);
      }
      setLog(saved);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally { setSaving(false); }
  };

  const addFood = async (food: FoodDto, quantity: number, slot: Slot) => {
    const current = log ?? { date, loggedFoods: [] };
    await saveLog({
      ...current,
      loggedFoods: [...(current.loggedFoods ?? []), { foodId: food.id!, mealSlot: slot, quantity, unit: "GRAM" }],
    });
  };

  const removeFood = async (index: number) => {
    if (!log) return;
    await saveLog({ ...log, loggedFoods: (log.loggedFoods ?? []).filter((_, i) => i !== index) });
  };

  const allLogged = log?.loggedFoods ?? [];
  const totalCals   = allLogged.reduce((s, lf) => { const f = foods.find((x) => x.id === lf.foodId); return f ? s + calcFoodCals(f, lf) : s; }, 0);
  const totalProtein = calcMacro(foods, allLogged, "proteinPer100");
  const totalCarbs   = calcMacro(foods, allLogged, "carbsPer100");
  const totalFat     = calcMacro(foods, allLogged, "fatPer100");
  const isToday      = date === todayStr();

  return (
    <div className="space-y-4">
      {/* Date nav */}
      <div className="flex items-center gap-3 pt-1">
        <button
          onClick={() => setDate((d) => addDays(d, -1))}
          className="w-8 h-8 rounded-lg border border-divider flex items-center justify-center text-text-muted hover:text-text-primary transition-colors"
        >
          <ChevronLeft size={16} />
        </button>
        <div className="flex-1 text-center">
          <p className="text-[17px] font-semibold text-text-primary">{formatDateLabel(date)}</p>
          {!isToday && (
            <p className="text-xs text-text-muted">
              {new Date(date + "T00:00:00").toLocaleDateString("en-GB", { day: "numeric", month: "long", year: "numeric" })}
            </p>
          )}
        </div>
        <button
          onClick={() => setDate((d) => addDays(d, 1))}
          className="w-8 h-8 rounded-lg border border-divider flex items-center justify-center text-text-muted hover:text-text-primary transition-colors"
        >
          <ChevronRight size={16} />
        </button>
      </div>

      {error && (
        <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>
      )}

      {/* Macro summary strip */}
      {!loading && totalCals > 0 && (
        <div className="bg-bg-card rounded-xl border border-divider px-4 py-3 flex justify-between">
          {[
            { label: "Calories", val: Math.round(totalCals), unit: "kcal", color: "#F59E0B" },
            { label: "Protein",  val: Math.round(totalProtein), unit: "g", color: "#2E7D52" },
            { label: "Carbs",    val: Math.round(totalCarbs),   unit: "g", color: "#C05200" },
            { label: "Fat",      val: Math.round(totalFat),     unit: "g", color: "#7C3AED" },
          ].map(({ label, val, unit }) => (
            <div key={label} className="text-center">
              <p className="text-[15px] font-bold text-text-primary">{val}<span className="text-xs font-normal text-text-muted ml-0.5">{unit}</span></p>
              <p className="text-[10px] text-text-muted">{label}</p>
            </div>
          ))}
        </div>
      )}

      {/* Meal slots */}
      {SLOTS.map((slot) => {
        const slotFoods = allLogged.map((lf, idx) => ({ lf, idx })).filter(({ lf }) => lf.mealSlot === slot.key);
        const isActive = activeSearch === slot.key;

        return (
          <div key={slot.key} className="bg-bg-card rounded-xl border border-divider overflow-hidden">
            {/* Slot header */}
            <div className="flex items-center justify-between px-4 py-3">
              <div className="flex items-center gap-2.5">
                <span className="text-base">{slot.emoji}</span>
                <div>
                  <p className="text-[13px] font-semibold text-text-primary">{slot.key}</p>
                  {slotFoods.length > 0 && (
                    <p className="text-[11px] text-text-muted">
                      {slotFoods.length} item{slotFoods.length !== 1 ? "s" : ""}
                    </p>
                  )}
                </div>
              </div>
              <button
                onClick={() => setActiveSearch(isActive ? null : slot.key)}
                className="flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-semibold transition-colors"
                style={{ background: isActive ? slot.bg : "#F5F5F5", color: isActive ? slot.color : "#555555" }}
              >
                {isActive ? <X size={12} /> : <Plus size={12} />}
                {isActive ? "Cancel" : "Add"}
              </button>
            </div>

            {/* Food items */}
            {loading ? (
              <div className="px-4 pb-3 space-y-2">
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-3/4" />
              </div>
            ) : slotFoods.length > 0 ? (
              <div className="border-t border-divider divide-y divide-divider">
                {slotFoods.map(({ lf, idx }) => {
                  const food = foods.find((f) => f.id === lf.foodId);
                  const cals = food ? Math.round(calcFoodCals(food, lf)) : 0;
                  return (
                    <div key={idx} className="flex items-center justify-between px-4 py-2.5">
                      <div>
                        <p className="text-[13px] font-medium text-text-primary">{food?.name ?? `Food #${lf.foodId}`}</p>
                        <p className="text-[11px] text-text-muted">{lf.quantity}g · {cals} kcal</p>
                      </div>
                      <button
                        disabled={saving}
                        onClick={() => removeFood(idx)}
                        className="text-text-muted hover:text-red-500 p-1 transition-colors"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  );
                })}
              </div>
            ) : !isActive ? (
              <div className="border-t border-divider px-4 py-3">
                <p className="text-xs text-text-muted">Nothing logged yet</p>
              </div>
            ) : null}

            {/* Search panel */}
            {isActive && (
              <div className="border-t border-divider px-4 pb-4">
                <FoodSearch foods={foods} slot={slot.key} onAdd={addFood} onClose={() => setActiveSearch(null)} />
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
