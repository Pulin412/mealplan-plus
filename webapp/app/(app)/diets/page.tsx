"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { ChevronDown, ChevronUp, Trash2, Plus, X, Search, Copy } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import type { components } from "@/lib/api/types.generated";

type DietDto = components["schemas"]["DietDto"];
type MealDto = components["schemas"]["MealDto"];
type DietMealDto = components["schemas"]["DietMealDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];
type FoodDto = components["schemas"]["FoodDto"];

const SLOTS = ["Breakfast", "Lunch", "Dinner", "Snack"] as const;
type Slot = typeof SLOTS[number];

const DAY_NAMES: Record<number, string> = {
  0: "Any day", 1: "Mon", 2: "Tue", 3: "Wed", 4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun",
};

// ── Food search for adding to a meal ─────────────────────────────────────────
function FoodSearch({ foods, onAdd, onClose }: {
  foods: FoodDto[];
  onAdd: (item: MealFoodItemDto) => void;
  onClose: () => void;
}) {
  const [query, setQuery] = useState("");
  const [qty, setQty] = useState("100");
  const [selected, setSelected] = useState<FoodDto | null>(null);

  const filtered = query.length >= 2
    ? foods.filter((f) => f.name.toLowerCase().includes(query.toLowerCase())).slice(0, 8)
    : [];

  return (
    <div className="border rounded-lg p-3 space-y-2 bg-muted/30 mt-2">
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
            <Input type="number" value={qty} onChange={(e) => setQty(e.target.value)}
              className="h-8 w-24 text-sm" min={1} />
            <span className="text-sm text-muted-foreground">grams</span>
            <Button size="sm" onClick={() => {
              const q = parseFloat(qty);
              if (!isNaN(q) && q > 0 && selected.id != null) {
                onAdd({ foodId: selected.id!, quantity: q, unit: "GRAM" });
                onClose();
              }
            }}>Add</Button>
            <Button size="sm" variant="ghost" onClick={() => setSelected(null)}>Back</Button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Meals section ─────────────────────────────────────────────────────────────
function MealsSection({ meals, foods, onMealsChange }: {
  meals: MealDto[];
  foods: FoodDto[];
  onMealsChange: (meals: MealDto[]) => void;
}) {
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [showAddFood, setShowAddFood] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [showNewMeal, setShowNewMeal] = useState(false);
  const [newMealName, setNewMealName] = useState("");
  const [newMealSlot, setNewMealSlot] = useState<Slot>("Lunch");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createMeal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newMealName.trim()) return;
    setCreating(true);
    try {
      const created = await api.post<MealDto>("/api/v1/meals", { name: newMealName.trim(), slot: newMealSlot, items: [] });
      onMealsChange([created, ...meals]);
      setNewMealName(""); setShowNewMeal(false);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create meal");
    } finally {
      setCreating(false);
    }
  };

  const addFood = async (meal: MealDto, item: MealFoodItemDto) => {
    setSavingId(meal.id ?? null);
    try {
      const updated = await api.put<MealDto>(`/api/v1/meals/${meal.id}`, {
        ...meal,
        items: [...(meal.items ?? []), item],
      });
      onMealsChange(meals.map((m) => m.id === meal.id ? updated : m));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally {
      setSavingId(null);
    }
  };

  const removeFood = async (meal: MealDto, idx: number) => {
    setSavingId(meal.id ?? null);
    try {
      const updated = await api.put<MealDto>(`/api/v1/meals/${meal.id}`, {
        ...meal,
        items: (meal.items ?? []).filter((_, i) => i !== idx),
      });
      onMealsChange(meals.map((m) => m.id === meal.id ? updated : m));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally {
      setSavingId(null);
    }
  };

  const deleteMeal = async (id: number) => {
    if (!confirm("Delete this meal?")) return;
    try {
      await api.delete(`/api/v1/meals/${id}`);
      onMealsChange(meals.filter((m) => m.id !== id));
      if (expandedId === id) setExpandedId(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Meals</h2>
        <Button size="sm" variant="outline" onClick={() => setShowNewMeal((v) => !v)}>
          {showNewMeal ? "Cancel" : <><Plus className="h-4 w-4 mr-1" />New meal</>}
        </Button>
      </div>

      {error && <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>}

      {showNewMeal && (
        <Card>
          <CardContent className="pt-4">
            <form onSubmit={createMeal} className="space-y-3">
              <div className="grid grid-cols-2 gap-2">
                <div className="col-span-2 space-y-1">
                  <Label className="text-xs">Meal name *</Label>
                  <Input value={newMealName} onChange={(e) => setNewMealName(e.target.value)}
                    placeholder="e.g. High protein breakfast" className="h-8 text-sm" autoFocus />
                </div>
                <div className="col-span-2 space-y-1">
                  <Label className="text-xs">Default slot</Label>
                  <select value={newMealSlot} onChange={(e) => setNewMealSlot(e.target.value as Slot)}
                    className="w-full h-8 rounded-md border border-input bg-background px-2 text-sm">
                    {SLOTS.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
              </div>
              <Button type="submit" size="sm" disabled={creating || !newMealName.trim()}>
                {creating ? "Creating…" : "Create meal"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {meals.length === 0 ? (
        <p className="text-sm text-muted-foreground">No meals yet. Create one above.</p>
      ) : (
        meals.map((meal) => {
          const isExpanded = expandedId === meal.id;
          const isSaving = savingId === meal.id;
          const items = meal.items ?? [];
          return (
            <Card key={meal.id}>
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between gap-2">
                  <button className="flex-1 text-left" onClick={() => setExpandedId(isExpanded ? null : (meal.id ?? null))}>
                    <p className="font-medium text-sm">{meal.name}</p>
                    <p className="text-xs text-muted-foreground">{meal.slot} · {items.length} food{items.length !== 1 ? "s" : ""}</p>
                  </button>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button size="icon" variant="ghost" className="h-7 w-7"
                      onClick={() => setExpandedId(isExpanded ? null : (meal.id ?? null))}>
                      {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                    <Button size="icon" variant="ghost" className="h-7 w-7 text-destructive hover:text-destructive"
                      onClick={() => meal.id !== undefined && deleteMeal(meal.id)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              {isExpanded && (
                <CardContent className="pt-0">
                  <Separator className="mb-3" />
                  {items.length === 0 ? (
                    <p className="text-xs text-muted-foreground mb-2">No foods yet.</p>
                  ) : (
                    <ul className="space-y-1 mb-2">
                      {items.map((item, idx) => {
                        const food = foods.find((f) => f.id === item.foodId);
                        return (
                          <li key={idx} className="flex items-center justify-between text-sm">
                            <span>{food?.name ?? `Food #${item.foodId}`}
                              <span className="text-muted-foreground ml-1 text-xs">{item.quantity}g</span>
                            </span>
                            <Button size="icon" variant="ghost" className="h-6 w-6" disabled={isSaving}
                              onClick={() => removeFood(meal, idx)}>
                              <X className="h-3 w-3" />
                            </Button>
                          </li>
                        );
                      })}
                    </ul>
                  )}
                  {showAddFood === meal.id ? (
                    <FoodSearch foods={foods} onAdd={(item) => addFood(meal, item)} onClose={() => setShowAddFood(null)} />
                  ) : (
                    <Button size="sm" variant="outline" className="w-full" disabled={isSaving}
                      onClick={() => setShowAddFood(meal.id ?? null)}>
                      <Plus className="h-4 w-4 mr-1" />Add food
                    </Button>
                  )}
                </CardContent>
              )}
            </Card>
          );
        })
      )}
    </div>
  );
}

// ── Diets section ─────────────────────────────────────────────────────────────
function DietsSection({ diets, meals, onDietsChange }: {
  diets: DietDto[];
  meals: MealDto[];
  onDietsChange: (diets: DietDto[]) => void;
}) {
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formCals, setFormCals] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Assign meal form state
  const [assigningToDiet, setAssigningToDiet] = useState<number | null>(null);
  const [assignMealId, setAssignMealId] = useState<string>("");
  const [assignSlot, setAssignSlot] = useState<Slot>("Lunch");
  const [assignDay, setAssignDay] = useState<string>("0");

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) return;
    setSubmitting(true);
    try {
      const created = await api.post<DietDto>("/api/v1/diets", {
        name: formName.trim(), description: formDesc.trim() || undefined,
        targetCalories: formCals ? parseFloat(formCals) : undefined, meals: [],
      });
      onDietsChange([created, ...diets]);
      setFormName(""); setFormDesc(""); setFormCals(""); setShowForm(false);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this diet?")) return;
    try {
      await api.delete(`/api/v1/diets/${id}`);
      onDietsChange(diets.filter((d) => d.id !== id));
      if (expandedId === id) setExpandedId(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  const handleDuplicate = async (id: number) => {
    try {
      const copy = await api.post<DietDto>(`/api/v1/diets/${id}/duplicate`, {});
      onDietsChange([...diets, copy]);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to duplicate");
    }
  };

  const assignMeal = async (diet: DietDto) => {
    if (!assignMealId) return;
    setSavingId(diet.id ?? null);
    try {
      const newAssignment: DietMealDto = {
        mealId: parseInt(assignMealId),
        slot: assignSlot,
        dayOfWeek: parseInt(assignDay),
      };
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet,
        meals: [...(diet.meals ?? []), newAssignment],
      });
      onDietsChange(diets.map((d) => d.id === diet.id ? updated : d));
      setAssigningToDiet(null); setAssignMealId(""); setAssignSlot("Lunch"); setAssignDay("0");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to assign meal");
    } finally {
      setSavingId(null);
    }
  };

  const removeMealFromDiet = async (diet: DietDto, assignmentIdx: number) => {
    setSavingId(diet.id ?? null);
    try {
      const updated = await api.put<DietDto>(`/api/v1/diets/${diet.id}`, {
        ...diet,
        meals: (diet.meals ?? []).filter((_, i) => i !== assignmentIdx),
      });
      onDietsChange(diets.map((d) => d.id === diet.id ? updated : d));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to remove meal");
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">Diets</h2>
        <Button size="sm" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New diet"}
        </Button>
      </div>

      {error && <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>}

      {showForm && (
        <Card>
          <CardContent className="pt-4">
            <form onSubmit={handleCreate} className="space-y-3">
              <div className="space-y-1">
                <Label className="text-xs">Name *</Label>
                <Input value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="e.g. High protein week" className="h-8 text-sm" />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Description</Label>
                <Input value={formDesc} onChange={(e) => setFormDesc(e.target.value)} placeholder="Optional" className="h-8 text-sm" />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Target calories</Label>
                <Input type="number" value={formCals} onChange={(e) => setFormCals(e.target.value)} placeholder="e.g. 2000" className="h-8 text-sm" />
              </div>
              <Button type="submit" size="sm" disabled={submitting || !formName.trim()}>
                {submitting ? "Creating…" : "Create"}
              </Button>
            </form>
          </CardContent>
        </Card>
      )}

      {diets.length === 0 ? (
        <p className="text-sm text-muted-foreground">No diets yet.</p>
      ) : (
        diets.map((diet) => {
          const isExpanded = expandedId === diet.id;
          const isSaving = savingId === diet.id;
          const dietMeals = diet.meals ?? [];
          return (
            <Card key={diet.id} className="overflow-hidden">
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between gap-2">
                  <button className="flex-1 text-left min-w-0" onClick={() => setExpandedId(isExpanded ? null : (diet.id ?? null))}>
                    <p className="font-semibold text-sm truncate">{diet.name}</p>
                    {diet.description && <p className="text-xs text-muted-foreground truncate">{diet.description}</p>}
                    {diet.targetCalories && (
                      <Badge variant="secondary" className="mt-1 text-xs">{diet.targetCalories} kcal target</Badge>
                    )}
                  </button>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button size="icon" variant="ghost" className="h-7 w-7"
                      onClick={() => setExpandedId(isExpanded ? null : (diet.id ?? null))}>
                      {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                    <Button size="icon" variant="ghost" className="h-7 w-7 text-muted-foreground"
                      title="Duplicate diet"
                      onClick={() => diet.id !== undefined && handleDuplicate(diet.id)}>
                      <Copy className="h-4 w-4" />
                    </Button>
                    <Button size="icon" variant="ghost" className="h-7 w-7 text-destructive hover:text-destructive"
                      onClick={() => diet.id !== undefined && handleDelete(diet.id)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>

              {isExpanded && (
                <CardContent className="pt-0">
                  <Separator className="mb-3" />

                  {dietMeals.length === 0 ? (
                    <p className="text-xs text-muted-foreground mb-3">No meals assigned yet.</p>
                  ) : (
                    <div className="space-y-1 mb-3">
                      {dietMeals.map((dm, idx) => {
                        const meal = meals.find((m) => m.id === dm.mealId);
                        return (
                          <div key={idx} className="flex items-center justify-between text-sm pl-2 border-l-2 border-muted">
                            <div>
                              <span className="font-medium">{meal?.name ?? `Meal #${dm.mealId}`}</span>
                              <span className="text-muted-foreground ml-1 text-xs">{dm.slot}</span>
                              {dm.dayOfWeek !== 0 && (
                                <span className="text-muted-foreground ml-1 text-xs">· {DAY_NAMES[dm.dayOfWeek]}</span>
                              )}
                            </div>
                            <Button size="icon" variant="ghost" className="h-6 w-6" disabled={isSaving}
                              onClick={() => removeMealFromDiet(diet, idx)}>
                              <X className="h-3 w-3" />
                            </Button>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {assigningToDiet === diet.id ? (
                    <div className="border rounded-md p-3 bg-muted/30 space-y-2">
                      <p className="text-xs font-semibold">Assign meal to diet</p>
                      <div className="grid grid-cols-2 gap-2">
                        <div className="col-span-2 space-y-1">
                          <Label className="text-xs">Meal</Label>
                          <select value={assignMealId} onChange={(e) => setAssignMealId(e.target.value)}
                            className="w-full h-8 rounded-md border border-input bg-background px-2 text-sm">
                            <option value="">Select a meal…</option>
                            {meals.map((m) => (
                              <option key={m.id} value={m.id}>{m.name}</option>
                            ))}
                          </select>
                        </div>
                        <div className="space-y-1">
                          <Label className="text-xs">Slot</Label>
                          <select value={assignSlot} onChange={(e) => setAssignSlot(e.target.value as Slot)}
                            className="w-full h-8 rounded-md border border-input bg-background px-2 text-sm">
                            {SLOTS.map((s) => <option key={s} value={s}>{s}</option>)}
                          </select>
                        </div>
                        <div className="space-y-1">
                          <Label className="text-xs">Day</Label>
                          <select value={assignDay} onChange={(e) => setAssignDay(e.target.value)}
                            className="w-full h-8 rounded-md border border-input bg-background px-2 text-sm">
                            {Object.entries(DAY_NAMES).map(([k, v]) => (
                              <option key={k} value={k}>{v}</option>
                            ))}
                          </select>
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button size="sm" disabled={isSaving || !assignMealId} onClick={() => assignMeal(diet)}>
                          {isSaving ? "Saving…" : "Assign"}
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => setAssigningToDiet(null)}>Cancel</Button>
                      </div>
                    </div>
                  ) : (
                    <Button size="sm" variant="outline" className="w-full"
                      onClick={() => { setAssigningToDiet(diet.id ?? null); setAssignMealId(""); }}>
                      <Plus className="h-4 w-4 mr-1" />Assign meal
                    </Button>
                  )}
                </CardContent>
              )}
            </Card>
          );
        })
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function DietsPage() {
  const { user } = useAuth();
  const [diets, setDiets] = useState<DietDto[]>([]);
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    if (!user) return;
    try {
      const [d, m, f] = await Promise.all([
        api.get<DietDto[]>("/api/v1/diets"),
        api.get<MealDto[]>("/api/v1/meals"),
        api.get<FoodDto[]>("/api/v1/foods"),
      ]);
      setDiets(d); setMeals(m); setFoods(f);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { loadData(); }, [loadData]);

  if (loading) return (
    <div className="space-y-4 max-w-xl">
      {Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}
    </div>
  );

  return (
    <div className="space-y-8 max-w-xl">
      <h1 className="text-2xl font-bold">Diets & Meals</h1>
      {error && <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>}
      <MealsSection meals={meals} foods={foods} onMealsChange={setMeals} />
      <Separator />
      <DietsSection diets={diets} meals={meals} onDietsChange={setDiets} />
    </div>
  );
}
