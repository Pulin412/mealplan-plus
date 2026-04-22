"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight, X, Plus, Search } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type DailyLogDto = components["schemas"]["DailyLogDto"];
type LoggedFoodDto = components["schemas"]["LoggedFoodDto"];
type FoodDto = components["schemas"]["FoodDto"];

type Slot = "Breakfast" | "Lunch" | "Dinner";
const SLOTS: Slot[] = ["Breakfast", "Lunch", "Dinner"];

function todayStr() {
  return new Date().toISOString().split("T")[0];
}

function addDays(dateStr: string, days: number): string {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + days);
  return d.toISOString().split("T")[0];
}

function formatDateLabel(dateStr: string): string {
  const today = todayStr();
  const yesterday = addDays(today, -1);
  const tomorrow = addDays(today, 1);
  if (dateStr === today) return "Today";
  if (dateStr === yesterday) return "Yesterday";
  if (dateStr === tomorrow) return "Tomorrow";
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", {
    weekday: "short", day: "numeric", month: "short",
  });
}

function calcFoodCals(food: FoodDto, lf: LoggedFoodDto): number {
  const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
  return (food.caloriesPer100 * grams) / 100;
}

function calcMacro(foods: FoodDto[], loggedFoods: LoggedFoodDto[], per: keyof Pick<FoodDto, "proteinPer100" | "carbsPer100" | "fatPer100">): number {
  return loggedFoods.reduce((sum, lf) => {
    const food = foods.find((f) => f.id === lf.foodId);
    if (!food) return sum;
    const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food[per] * grams) / 100;
  }, 0);
}

interface FoodSearchProps {
  foods: FoodDto[];
  slot: Slot;
  onAdd: (food: FoodDto, quantity: number, slot: Slot) => void;
  onClose: () => void;
}

function FoodSearch({ foods, slot, onAdd, onClose }: FoodSearchProps) {
  const [query, setQuery] = useState("");
  const [qty, setQty] = useState("100");
  const [selected, setSelected] = useState<FoodDto | null>(null);

  const filtered = query.length >= 2
    ? foods.filter((f) => f.name.toLowerCase().includes(query.toLowerCase())).slice(0, 8)
    : [];

  return (
    <div className="border rounded-lg p-3 space-y-3 bg-muted/30">
      <div className="flex items-center gap-2">
        <Search className="h-4 w-4 text-muted-foreground shrink-0" />
        <Input
          placeholder="Search foods…"
          value={query}
          onChange={(e) => { setQuery(e.target.value); setSelected(null); }}
          className="h-8 text-sm"
          autoFocus
        />
        <Button size="sm" variant="ghost" onClick={onClose}><X className="h-4 w-4" /></Button>
      </div>

      {filtered.length > 0 && !selected && (
        <ul className="space-y-1">
          {filtered.map((food) => (
            <li key={food.id}>
              <button
                className="w-full text-left text-sm px-2 py-1.5 rounded hover:bg-muted transition-colors flex justify-between"
                onClick={() => setSelected(food)}
              >
                <span>{food.name}{food.brand ? <span className="text-muted-foreground"> · {food.brand}</span> : null}</span>
                <span className="text-muted-foreground">{food.caloriesPer100} kcal/100g</span>
              </button>
            </li>
          ))}
        </ul>
      )}

      {selected && (
        <div className="space-y-2">
          <p className="text-sm font-medium">{selected.name}</p>
          <div className="flex items-center gap-2">
            <Input
              type="number"
              value={qty}
              onChange={(e) => setQty(e.target.value)}
              className="h-8 w-24 text-sm"
              min={1}
            />
            <span className="text-sm text-muted-foreground">grams</span>
            <Button size="sm" onClick={() => {
              const q = parseFloat(qty);
              if (!isNaN(q) && q > 0) { onAdd(selected, q, slot); onClose(); }
            }}>Add</Button>
            <Button size="sm" variant="ghost" onClick={() => setSelected(null)}>Back</Button>
          </div>
          <p className="text-xs text-muted-foreground">
            ≈ {Math.round((selected.caloriesPer100 * parseFloat(qty || "0")) / 100)} kcal
          </p>
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
    setLoading(true);
    setError(null);
    try {
      const [logs, allFoods] = await Promise.all([
        api.get<DailyLogDto[]>("/api/v1/daily-logs"),
        api.get<FoodDto[]>("/api/v1/foods"),
      ]);
      setFoods(allFoods);
      const found = logs.find((l) => l.date === date) ?? null;
      setLog(found);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, [user, date]);

  useEffect(() => { loadData(); }, [loadData]);

  const saveLog = async (updatedLog: DailyLogDto) => {
    setSaving(true);
    setError(null);
    try {
      const saved = await api.post<DailyLogDto>("/api/v1/daily-logs", updatedLog);
      setLog(saved);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally {
      setSaving(false);
    }
  };

  const addFood = async (food: FoodDto, quantity: number, slot: Slot) => {
    const current = log ?? { date, loggedFoods: [] };
    const newItem: LoggedFoodDto = {
      foodId: food.id!,
      mealSlot: slot,
      quantity,
      unit: "GRAM",
    };
    const updated: DailyLogDto = {
      ...current,
      loggedFoods: [...(current.loggedFoods ?? []), newItem],
    };
    await saveLog(updated);
  };

  const removeFood = async (index: number) => {
    if (!log) return;
    const updated: DailyLogDto = {
      ...log,
      loggedFoods: (log.loggedFoods ?? []).filter((_, i) => i !== index),
    };
    await saveLog(updated);
  };

  const allFoods = log?.loggedFoods ?? [];
  const totalCals = allFoods.reduce((sum, lf) => {
    const food = foods.find((f) => f.id === lf.foodId);
    return food ? sum + calcFoodCals(food, lf) : sum;
  }, 0);
  const totalProtein = calcMacro(foods, allFoods, "proteinPer100");
  const totalCarbs = calcMacro(foods, allFoods, "carbsPer100");
  const totalFat = calcMacro(foods, allFoods, "fatPer100");

  return (
    <div className="space-y-4 max-w-xl">
      {/* Date selector */}
      <div className="flex items-center gap-3">
        <Button variant="outline" size="icon" onClick={() => setDate((d) => addDays(d, -1))}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <h1 className="text-xl font-bold flex-1 text-center">{formatDateLabel(date)}</h1>
        <Button variant="outline" size="icon" onClick={() => setDate((d) => addDays(d, 1))}>
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {SLOTS.map((slot) => {
        const slotFoods = (log?.loggedFoods ?? [])
          .map((lf, idx) => ({ lf, idx }))
          .filter(({ lf }) => lf.mealSlot === slot);

        return (
          <Card key={slot}>
            <CardHeader className="pb-2">
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm font-medium">{slot}</CardTitle>
                <Button
                  size="sm"
                  variant="ghost"
                  className="h-7 text-xs"
                  onClick={() => setActiveSearch(activeSearch === slot ? null : slot)}
                >
                  <Plus className="h-3 w-3 mr-1" />Add food
                </Button>
              </div>
            </CardHeader>
            <CardContent className="space-y-2">
              {loading ? (
                <Skeleton className="h-5 w-full" />
              ) : slotFoods.length === 0 ? (
                <p className="text-xs text-muted-foreground">Nothing logged</p>
              ) : (
                slotFoods.map(({ lf, idx }) => {
                  const food = foods.find((f) => f.id === lf.foodId);
                  const cals = food ? Math.round(calcFoodCals(food, lf)) : 0;
                  return (
                    <div key={idx} className="flex items-center justify-between text-sm">
                      <div>
                        <span className="font-medium">{food?.name ?? `Food #${lf.foodId}`}</span>
                        <span className="text-muted-foreground ml-2">{lf.quantity}g</span>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="text-muted-foreground text-xs">{cals} kcal</span>
                        <Button
                          size="icon"
                          variant="ghost"
                          className="h-6 w-6"
                          disabled={saving}
                          onClick={() => removeFood(idx)}
                        >
                          <X className="h-3 w-3" />
                        </Button>
                      </div>
                    </div>
                  );
                })
              )}

              {activeSearch === slot && (
                <FoodSearch
                  foods={foods}
                  slot={slot}
                  onAdd={addFood}
                  onClose={() => setActiveSearch(null)}
                />
              )}
            </CardContent>
          </Card>
        );
      })}

      {/* Totals */}
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">Daily totals</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <Skeleton className="h-6 w-full" />
          ) : (
            <div className="flex flex-wrap gap-3 text-sm">
              <span><span className="font-bold">{Math.round(totalCals)}</span> <span className="text-muted-foreground">kcal</span></span>
              <span><span className="font-bold">{Math.round(totalProtein)}g</span> <span className="text-muted-foreground">protein</span></span>
              <span><span className="font-bold">{Math.round(totalCarbs)}g</span> <span className="text-muted-foreground">carbs</span></span>
              <span><span className="font-bold">{Math.round(totalFat)}g</span> <span className="text-muted-foreground">fat</span></span>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
