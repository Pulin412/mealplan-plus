"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type DietDto = components["schemas"]["DietDto"];

const DAYS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];
const PLAN_STORAGE_KEY = "mealplan_plan_assignments";

function todayStr() {
  return new Date().toISOString().split("T")[0];
}

function getMonthStart(year: number, month: number): Date {
  return new Date(year, month, 1);
}

function getMonthEnd(year: number, month: number): Date {
  return new Date(year, month + 1, 0);
}

function dateStr(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function loadPlanFromStorage(): Map<string, number> {
  try {
    const raw = localStorage.getItem(PLAN_STORAGE_KEY);
    if (!raw) return new Map();
    const obj = JSON.parse(raw) as Record<string, number>;
    return new Map(Object.entries(obj));
  } catch {
    return new Map();
  }
}

function savePlanToStorage(map: Map<string, number>) {
  const obj: Record<string, number> = {};
  map.forEach((v, k) => { obj[k] = v; });
  localStorage.setItem(PLAN_STORAGE_KEY, JSON.stringify(obj));
}

export default function PlanPage() {
  const { user } = useAuth();
  const now = new Date();
  const [year, setYear] = useState(now.getFullYear());
  const [month, setMonth] = useState(now.getMonth());
  const [diets, setDiets] = useState<DietDto[]>([]);
  const [plan, setPlan] = useState<Map<string, number>>(new Map());
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setPlan(loadPlanFromStorage());
  }, []);

  useEffect(() => {
    if (!user) return;
    api.get<DietDto[]>("/api/v1/diets")
      .then(setDiets)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load diets"))
      .finally(() => setLoading(false));
  }, [user]);

  const assignDiet = useCallback((date: string, dietId: number | null) => {
    setPlan((prev) => {
      const next = new Map(prev);
      if (dietId === null) {
        next.delete(date);
      } else {
        next.set(date, dietId);
      }
      savePlanToStorage(next);
      return next;
    });
  }, []);

  const monthStart = getMonthStart(year, month);
  const monthEnd = getMonthEnd(year, month);
  const startPadding = monthStart.getDay(); // 0=Sun
  const totalDays = monthEnd.getDate();

  // Build calendar cells: padding + days
  const cells: (number | null)[] = [
    ...Array.from({ length: startPadding }, () => null),
    ...Array.from({ length: totalDays }, (_, i) => i + 1),
  ];
  // Pad to complete the last row
  while (cells.length % 7 !== 0) cells.push(null);

  const today = todayStr();

  // Next 7 days list
  const next7: string[] = Array.from({ length: 7 }, (_, i) => {
    const d = new Date();
    d.setDate(d.getDate() + i);
    return d.toISOString().split("T")[0];
  });

  const prevMonth = () => {
    if (month === 0) { setYear(y => y - 1); setMonth(11); }
    else setMonth(m => m - 1);
    setSelectedDate(null);
  };
  const nextMonth = () => {
    if (month === 11) { setYear(y => y + 1); setMonth(0); }
    else setMonth(m => m + 1);
    setSelectedDate(null);
  };

  const monthLabel = new Date(year, month, 1).toLocaleDateString("en-GB", { month: "long", year: "numeric" });

  return (
    <div className="space-y-6 max-w-2xl">
      <h1 className="text-2xl font-bold">Plan</h1>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {/* Calendar */}
      <Card>
        <CardHeader className="pb-2">
          <div className="flex items-center justify-between">
            <Button variant="ghost" size="icon" onClick={prevMonth}><ChevronLeft className="h-4 w-4" /></Button>
            <CardTitle className="text-sm font-semibold">{monthLabel}</CardTitle>
            <Button variant="ghost" size="icon" onClick={nextMonth}><ChevronRight className="h-4 w-4" /></Button>
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <Skeleton className="h-48 w-full" />
          ) : (
            <>
              {/* Day headers */}
              <div className="grid grid-cols-7 mb-1">
                {DAYS.map((d) => (
                  <div key={d} className="text-center text-xs font-medium text-muted-foreground py-1">{d}</div>
                ))}
              </div>
              {/* Calendar grid */}
              <div className="grid grid-cols-7 gap-px">
                {cells.map((day, idx) => {
                  if (day === null) return <div key={idx} />;
                  const ds = dateStr(year, month, day);
                  const dietId = plan.get(ds);
                  const diet = dietId !== undefined ? diets.find((d) => d.id === dietId) : undefined;
                  const isToday = ds === today;
                  const isSelected = ds === selectedDate;
                  return (
                    <button
                      key={idx}
                      onClick={() => setSelectedDate(isSelected ? null : ds)}
                      className={[
                        "rounded p-1 text-left min-h-[52px] transition-colors border",
                        isSelected ? "border-primary bg-primary/10" : "border-transparent hover:bg-muted",
                      ].join(" ")}
                    >
                      <span className={["text-xs font-medium block", isToday ? "text-primary font-bold" : ""].join(" ")}>
                        {day}
                      </span>
                      {diet && (
                        <span className="text-[10px] leading-tight text-primary/80 block truncate">{diet.name}</span>
                      )}
                    </button>
                  );
                })}
              </div>
            </>
          )}
        </CardContent>
      </Card>

      {/* Day assignment panel */}
      {selectedDate && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">
              {new Date(selectedDate + "T00:00:00").toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })}
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <p className="text-xs text-muted-foreground">Assign a diet to this day:</p>
            <div className="flex flex-wrap gap-2">
              {diets.map((diet) => {
                const assigned = plan.get(selectedDate) === diet.id;
                return (
                  <button
                    key={diet.id}
                    onClick={() => assignDiet(selectedDate, assigned ? null : diet.id!)}
                    className={[
                      "text-sm px-3 py-1.5 rounded-full border transition-colors",
                      assigned
                        ? "bg-primary text-primary-foreground border-primary"
                        : "hover:bg-muted border-border",
                    ].join(" ")}
                  >
                    {diet.name}
                  </button>
                );
              })}
              {diets.length === 0 && (
                <p className="text-sm text-muted-foreground">No diets yet — create one in the Diets screen.</p>
              )}
              {plan.has(selectedDate) && (
                <button
                  onClick={() => assignDiet(selectedDate, null)}
                  className="text-sm px-3 py-1.5 rounded-full border border-destructive text-destructive hover:bg-destructive/10 transition-colors"
                >
                  Remove
                </button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Next 7 days */}
      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">Next 7 days</CardTitle>
        </CardHeader>
        <CardContent className="space-y-1">
          {loading ? (
            Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-6 w-full" />)
          ) : (
            next7.map((ds) => {
              const dietId = plan.get(ds);
              const diet = dietId !== undefined ? diets.find((d) => d.id === dietId) : undefined;
              const label = ds === today ? "Today" : new Date(ds + "T00:00:00").toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
              return (
                <div key={ds} className="flex items-center justify-between text-sm py-1 border-b last:border-0">
                  <span className={ds === today ? "font-semibold" : ""}>{label}</span>
                  {diet
                    ? <Badge variant="secondary">{diet.name}</Badge>
                    : <span className="text-muted-foreground text-xs">No diet planned</span>
                  }
                </div>
              );
            })
          )}
        </CardContent>
      </Card>
    </div>
  );
}
