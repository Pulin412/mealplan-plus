"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { ChevronDown, ChevronUp, Trash2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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

const SLOT_ORDER = ["Breakfast", "Lunch", "Dinner", "Snack"] as const;

const DAY_NAMES: Record<number, string> = {
  0: "Any day", 1: "Mon", 2: "Tue", 3: "Wed", 4: "Thu", 5: "Fri", 6: "Sat", 7: "Sun",
};

export default function DietsPage() {
  const { user } = useAuth();
  const [diets, setDiets] = useState<DietDto[]>([]);
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  // New diet form
  const [showForm, setShowForm] = useState(false);
  const [formName, setFormName] = useState("");
  const [formDesc, setFormDesc] = useState("");
  const [formCals, setFormCals] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    Promise.all([
      api.get<DietDto[]>("/api/v1/diets"),
      api.get<MealDto[]>("/api/v1/meals"),
    ])
      .then(([d, m]) => { setDiets(d); setMeals(m); })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formName.trim()) { setFormError("Name is required"); return; }
    setSubmitting(true);
    setFormError(null);
    try {
      const payload: DietDto = {
        name: formName.trim(),
        description: formDesc.trim() || undefined,
        targetCalories: formCals ? parseFloat(formCals) : undefined,
        meals: [],
      };
      const created = await api.post<DietDto>("/api/v1/diets", payload);
      setDiets((prev) => [created, ...prev]);
      setFormName(""); setFormDesc(""); setFormCals(""); setShowForm(false);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : "Failed to create");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm("Delete this diet?")) return;
    try {
      await api.delete(`/api/v1/diets/${id}`);
      setDiets((prev) => prev.filter((d) => d.id !== id));
      if (expandedId === id) setExpandedId(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  // Group diet meals by slot
  function groupBySlot(dietMeals: DietMealDto[]): Map<string, { dm: DietMealDto; meal: MealDto | undefined }[]> {
    const map = new Map<string, { dm: DietMealDto; meal: MealDto | undefined }[]>();
    for (const dm of dietMeals) {
      const slot = dm.slot;
      if (!map.has(slot)) map.set(slot, []);
      map.get(slot)!.push({ dm, meal: meals.find((m) => m.id === dm.mealId) });
    }
    return map;
  }

  return (
    <div className="space-y-6 max-w-xl">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Diets</h1>
        <Button size="sm" onClick={() => setShowForm((v) => !v)}>
          {showForm ? "Cancel" : "New diet"}
        </Button>
      </div>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {/* Create form */}
      {showForm && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">New diet</CardTitle></CardHeader>
          <CardContent>
            <form onSubmit={handleCreate} className="space-y-3">
              <div className="space-y-1">
                <Label className="text-xs">Name *</Label>
                <Input value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="e.g. High protein week" className="h-9 text-sm" />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Description</Label>
                <Input value={formDesc} onChange={(e) => setFormDesc(e.target.value)} placeholder="Optional description" className="h-9 text-sm" />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Target calories</Label>
                <Input type="number" value={formCals} onChange={(e) => setFormCals(e.target.value)} placeholder="e.g. 2000" className="h-9 text-sm" />
              </div>
              {formError && <p className="text-xs text-destructive">{formError}</p>}
              <Button type="submit" size="sm" disabled={submitting}>{submitting ? "Creating…" : "Create"}</Button>
            </form>
          </CardContent>
        </Card>
      )}

      {/* Diet list */}
      {loading ? (
        Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)
      ) : diets.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center text-sm text-muted-foreground">
            No diets yet. Create your first diet above.
          </CardContent>
        </Card>
      ) : (
        diets.map((diet) => {
          const isExpanded = expandedId === diet.id;
          const dietMeals = diet.meals ?? [];
          const bySlot = groupBySlot(dietMeals);
          return (
            <Card key={diet.id} className="overflow-hidden">
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    <CardTitle className="text-sm font-semibold truncate">{diet.name}</CardTitle>
                    {diet.description && <p className="text-xs text-muted-foreground mt-0.5 truncate">{diet.description}</p>}
                    {diet.targetCalories && (
                      <Badge variant="secondary" className="mt-1 text-xs">{diet.targetCalories} kcal target</Badge>
                    )}
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-7 w-7"
                      onClick={() => setExpandedId(isExpanded ? null : (diet.id ?? null))}
                    >
                      {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                    <Button
                      size="icon"
                      variant="ghost"
                      className="h-7 w-7 text-destructive hover:text-destructive"
                      onClick={() => diet.id !== undefined && handleDelete(diet.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>

              {isExpanded && (
                <CardContent className="pt-0">
                  <Separator className="mb-3" />
                  {dietMeals.length === 0 ? (
                    <p className="text-xs text-muted-foreground">No meals assigned to this diet.</p>
                  ) : (
                    <div className="space-y-3">
                      {SLOT_ORDER.filter((slot) => bySlot.has(slot)).map((slot) => (
                        <div key={slot}>
                          <p className="text-xs font-semibold text-muted-foreground uppercase mb-1">{slot}</p>
                          {bySlot.get(slot)!.map(({ dm, meal }) => (
                            <div key={dm.id ?? dm.mealId} className="text-sm pl-2 border-l-2 border-muted mb-1">
                              <span className="font-medium">{meal?.name ?? `Meal #${dm.mealId}`}</span>
                              {dm.dayOfWeek !== 0 && (
                                <span className="text-muted-foreground ml-1 text-xs">({DAY_NAMES[dm.dayOfWeek] ?? `Day ${dm.dayOfWeek}`})</span>
                              )}
                              {meal?.items && meal.items.length > 0 && (
                                <ul className="text-xs text-muted-foreground mt-0.5 space-y-0.5">
                                  {meal.items.map((item) => (
                                    <li key={item.id ?? item.foodId}>• {item.quantity} {item.unit} food #{item.foodId}</li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          ))}
                        </div>
                      ))}
                    </div>
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
