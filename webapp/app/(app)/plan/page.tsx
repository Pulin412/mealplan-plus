"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback } from "react";
import { ChevronLeft, ChevronRight, Check, X } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type DietDto = components["schemas"]["DietDto"];

const DAYS_HEADER = ["M", "T", "W", "T", "F", "S", "S"]; // Mon-first
const PLAN_KEY = "mealplan_plan_assignments";

function todayStr() { return new Date().toISOString().split("T")[0]; }

function isoDate(year: number, month: number, day: number) {
  return `${year}-${String(month + 1).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function addDaysToStr(dateStr: string, n: number) {
  const d = new Date(dateStr + "T00:00:00");
  d.setDate(d.getDate() + n);
  return d.toISOString().split("T")[0];
}


function loadPlan(): Map<string, number> {
  try {
    const raw = localStorage.getItem(PLAN_KEY);
    if (!raw) return new Map();
    return new Map(Object.entries(JSON.parse(raw) as Record<string, number>));
  } catch { return new Map(); }
}

function savePlan(map: Map<string, number>) {
  const obj: Record<string, number> = {};
  map.forEach((v, k) => { obj[k] = v; });
  localStorage.setItem(PLAN_KEY, JSON.stringify(obj));
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

  useEffect(() => { setPlan(loadPlan()); }, []);

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
      if (dietId === null) next.delete(date); else next.set(date, dietId);
      savePlan(next);
      return next;
    });
  }, []);

  const today = todayStr();
  const monthStart = new Date(year, month, 1);
  // Monday-first offset (0=Mon…6=Sun)
  const startOffset = (monthStart.getDay() + 6) % 7;
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  const cells: (number | null)[] = [
    ...Array.from({ length: startOffset }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => i + 1),
  ];
  while (cells.length % 7 !== 0) cells.push(null);

  const monthLabel = new Date(year, month, 1).toLocaleDateString("en-GB", { month: "long", year: "numeric" });

  const prevMonth = () => { if (month === 0) { setYear(y => y - 1); setMonth(11); } else setMonth(m => m - 1); setSelectedDate(null); };
  const nextMonth = () => { if (month === 11) { setYear(y => y + 1); setMonth(0); } else setMonth(m => m + 1); setSelectedDate(null); };

  const upcoming = Array.from({ length: 7 }, (_, i) => addDaysToStr(today, i));

  const selectedDietId = selectedDate ? plan.get(selectedDate) : undefined;

  return (
    <div className="space-y-4">
      {/* Page title */}
      <h1 className="text-[22px] font-semibold text-text-primary pt-1">Plan</h1>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* ── Mini calendar ── */}
      <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
        {/* Month header */}
        <div className="flex items-center justify-between px-4 py-3 border-b border-divider">
          <button onClick={prevMonth} className="w-8 h-8 rounded-lg flex items-center justify-center text-text-muted hover:text-text-primary transition-colors">
            <ChevronLeft size={16} />
          </button>
          <p className="text-[14px] font-semibold text-text-primary">{monthLabel}</p>
          <button onClick={nextMonth} className="w-8 h-8 rounded-lg flex items-center justify-center text-text-muted hover:text-text-primary transition-colors">
            <ChevronRight size={16} />
          </button>
        </div>

        <div className="px-3 py-3">
          {/* Day-of-week headers */}
          <div className="grid grid-cols-7 mb-1">
            {DAYS_HEADER.map((d, i) => (
              <div key={i} className="text-center text-[10px] font-bold text-text-muted py-1">{d}</div>
            ))}
          </div>

          {/* Calendar grid */}
          {loading ? <Skeleton className="h-40 w-full rounded-lg" /> : (
            <div className="grid grid-cols-7">
              {cells.map((day, idx) => {
                if (day === null) return <div key={idx} />;
                const ds = isoDate(year, month, day);
                const dietId = plan.get(ds);
                const diet = dietId !== undefined ? diets.find((d) => d.id === dietId) : undefined;
                const isToday = ds === today;
                const isSelected = ds === selectedDate;
                return (
                  <button
                    key={idx}
                    onClick={() => setSelectedDate(isSelected ? null : ds)}
                    className={[
                      "relative flex flex-col items-center py-1.5 rounded-lg transition-colors min-h-[44px]",
                      isSelected ? "bg-text-primary" : isToday ? "bg-green-light" : "hover:bg-bg-page",
                    ].join(" ")}
                  >
                    <span className={[
                      "text-[13px] font-semibold leading-none",
                      isSelected ? "text-bg-card" : isToday ? "text-green" : "text-text-primary",
                    ].join(" ")}>{day}</span>
                    {diet && (
                      <span className="mt-1 w-1.5 h-1.5 rounded-full bg-green shrink-0" />
                    )}
                  </button>
                );
              })}
            </div>
          )}

          {/* Legend */}
          <div className="flex items-center gap-4 mt-3 pt-3 border-t border-divider">
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-green" />
              <span className="text-[10px] text-text-muted">Diet planned</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-2 h-2 rounded-full bg-green-light border border-green" />
              <span className="text-[10px] text-text-muted">Today</span>
            </div>
          </div>
        </div>
      </div>

      {/* ── Day assignment panel ── */}
      {selectedDate && (
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-divider">
            <div>
              <p className="text-[13px] font-semibold text-text-primary">
                {new Date(selectedDate + "T00:00:00").toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" })}
              </p>
              {selectedDietId ? (
                <p className="text-[11px] text-green mt-0.5">
                  {diets.find((d) => d.id === selectedDietId)?.name ?? "Diet planned"}
                </p>
              ) : (
                <p className="text-[11px] text-text-muted mt-0.5">No diet planned</p>
              )}
            </div>
            <button onClick={() => setSelectedDate(null)} className="text-text-muted hover:text-text-primary">
              <X size={16} />
            </button>
          </div>
          <div className="px-4 py-3">
            <p className="text-[10px] font-bold text-text-muted uppercase mb-2">Assign a diet</p>
            <div className="flex flex-wrap gap-2">
              {diets.length === 0 ? (
                <p className="text-sm text-text-muted">No diets yet — create one in the Diets screen.</p>
              ) : diets.map((diet) => {
                const assigned = plan.get(selectedDate) === diet.id;
                return (
                  <button
                    key={diet.id}
                    onClick={() => assignDiet(selectedDate, assigned ? null : diet.id!)}
                    className={[
                      "flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium border transition-colors",
                      assigned
                        ? "bg-text-primary text-bg-card border-text-primary"
                        : "border-divider text-text-secondary hover:bg-bg-page",
                    ].join(" ")}
                  >
                    {assigned && <Check size={12} />}
                    {diet.name}
                  </button>
                );
              })}
              {plan.has(selectedDate) && (
                <button
                  onClick={() => assignDiet(selectedDate, null)}
                  className="px-3 py-1.5 rounded-full text-sm font-medium border border-red-200 text-red-500 hover:bg-red-50 transition-colors"
                >
                  Remove
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ── Upcoming 7 days ── */}
      <div>
        <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Upcoming</p>
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
          {loading
            ? Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)
            : upcoming.map((ds) => {
              const dietId = plan.get(ds);
              const diet = dietId !== undefined ? diets.find((d) => d.id === dietId) : undefined;
              const isToday = ds === today;
              const isSelected = ds === selectedDate;
              const dayOfWeek = new Date(ds + "T00:00:00").toLocaleDateString("en-GB", { weekday: "short" });
              const dayNum = new Date(ds + "T00:00:00").getDate();

              return (
                <button
                  key={ds}
                  onClick={() => setSelectedDate(isSelected ? null : ds)}
                  className={[
                    "w-full flex items-center gap-3 px-4 py-3 text-left transition-colors",
                    isSelected ? "bg-bg-page" : "hover:bg-bg-page",
                  ].join(" ")}
                >
                  {/* Date column */}
                  <div className="w-9 text-center shrink-0">
                    <p className="text-[9px] font-bold text-text-muted uppercase">{dayOfWeek}</p>
                    <p className={["text-[16px] font-bold leading-tight", isToday ? "text-green" : "text-text-primary"].join(" ")}>{dayNum}</p>
                  </div>
                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <p className={["text-[13px] font-semibold truncate", diet ? "text-text-primary" : "text-text-muted"].join(" ")}>
                      {diet ? `Diet · ${diet.name}` : "No diet planned"}
                    </p>
                  </div>
                  {/* Badge */}
                  {isToday && (
                    <span className="shrink-0 text-[11px] font-semibold px-2 py-0.5 rounded-lg bg-green-light text-green">Today</span>
                  )}
                  {!diet && !isToday && (
                    <span className="shrink-0 text-[11px] font-semibold px-2 py-0.5 rounded-lg bg-bg-page text-text-muted border border-divider">+ Plan</span>
                  )}
                </button>
              );
            })}
        </div>
      </div>
    </div>
  );
}
