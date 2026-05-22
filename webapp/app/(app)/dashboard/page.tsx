"use client";
import { useEffect, useState, useCallback } from "react";
import { Flame, Footprints, Zap, ChevronDown, ChevronUp, Check, Loader2 } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";
import { todayStr, MEAL_SLOTS, PREDEFINED_SLOT_COLORS } from "@/lib/utils";

type DailyLogDto     = components["schemas"]["DailyLogDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type HealthMetricDto = components["schemas"]["HealthMetricDto"];

// ── Local DTOs (not yet in generated types) ───────────────────────────────────
interface TodayMealItemDto {
  foodId: number; foodName: string;
  quantity: number; unit: string;
  caloriesPer100: number; proteinPer100: number;
  carbsPer100: number; fatPer100: number;
  glycemicIndex: number | null; notes: string | null;
}
interface TodaySlotDto  { slot: string; mealId: number; mealName: string; items: TodayMealItemDto[]; }
interface TodayPlanDto  {
  dietId: number; dietName: string; description: string | null;
  targetCalories: number | null; targetProtein: number | null;
  targetCarbs: number | null; targetFat: number | null;
  avgGlycemicIndex: number | null; slots: TodaySlotDto[];
}
interface DayCalories   { date: string; calories: number; }
interface MacroTotals   { protein: number; carbs: number; fat: number; calories: number; }
interface DashboardDto  {
  todayLog: DailyLogDto | null; recentLogs: DailyLogDto[]; foods: FoodDto[];
  dietCount: number; latestWeight: HealthMetricDto | null;
  currentStreak: number; weeklyCalories: DayCalories[]; todayMacros: MacroTotals;
  todayPlan: TodayPlanDto | null; todaySteps: number | null; todayCaloriesBurned: number | null;
}

// ── Helpers ───────────────────────────────────────────────────────────────────
function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}
function todayLabel() {
  return new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" });
}
function normalizeSlot(slot: string): string {
  const map: Record<string, string> = {
    BREAKFAST: "Breakfast", LUNCH: "Lunch", DINNER: "Dinner",
    SNACK: "Snack", PRE_WORKOUT: "Pre-Workout", POST_WORKOUT: "Post-Workout", NOON: "Snack",
  };
  return map[slot.toUpperCase()] ?? slot;
}
function giMeta(gi: number) {
  if (gi < 55) return { label: "Low GI",  color: "#2E7D52", bg: "#E8F5EE" };
  if (gi < 70) return { label: "Med GI",  color: "#C05200", bg: "#FFF3E0" };
  return           { label: "High GI", color: "#DC2626", bg: "#FFF0F0" };
}

// ── Sub-components ────────────────────────────────────────────────────────────
function StatPill({ icon, value, label }: { icon: React.ReactNode; value: string; label: string }) {
  return (
    <div className="flex-1 bg-bg-card border border-outline rounded-lg p-3 flex flex-col items-center gap-1 min-w-0">
      <span className="text-text-muted">{icon}</span>
      <span className="text-lg font-bold text-text-primary leading-none">{value}</span>
      <span className="text-[11px] text-text-muted text-center">{label}</span>
    </div>
  );
}

function MacroBar({ label, logged, target, color, unit = "g" }: {
  label: string; logged: number; target: number | null; color: string; unit?: string;
}) {
  const pct = target ? Math.min(100, (logged / target) * 100) : 0;
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs">
        <span className="text-text-secondary font-medium">{label}</span>
        <span className="text-text-muted">
          <span className="font-semibold text-text-primary">{Math.round(logged)}</span>
          {target ? `/${Math.round(target)} ${unit}` : ` ${unit}`}
        </span>
      </div>
      <div className="h-1.5 bg-gray-100 rounded-full overflow-hidden">
        <div className="h-full rounded-full transition-all duration-500"
          style={{ width: `${pct}%`, background: color }} />
      </div>
    </div>
  );
}

function GiBadge({ gi }: { gi: number }) {
  const { label, color, bg } = giMeta(gi);
  return (
    <span className="text-xs font-semibold px-2 py-0.5 rounded-full shrink-0"
      style={{ background: bg, color }}>
      GI {gi} · {label}
    </span>
  );
}

function MealSlotCard({ slotDto, logged, loggingThis, onLog }: {
  slotDto: TodaySlotDto; logged: boolean; loggingThis: boolean; onLog: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  const slotName   = normalizeSlot(slotDto.slot);
  const slotMeta   = MEAL_SLOTS.find((s) => s.key === slotName) ?? MEAL_SLOTS[1];
  const slotColors = PREDEFINED_SLOT_COLORS[slotName] ?? { bg: "#F5F5F5", text: "#555" };
  const totalCals  = slotDto.items.reduce((s, item) => {
    const g = item.unit === "GRAM" ? item.quantity : item.quantity * 100;
    return s + (item.caloriesPer100 * g / 100);
  }, 0);

  return (
    <div className={`bg-bg-card rounded-lg border overflow-hidden ${logged ? "border-green-200" : "border-outline"}`}>
      <button className="w-full flex items-center gap-3 px-4 py-3 text-left"
        onClick={() => setExpanded((v) => !v)}>
        <span className="text-lg shrink-0">{slotMeta.emoji}</span>
        <div className="flex-1 min-w-0">
          <span className="text-[11px] font-medium" style={{ color: slotColors.text }}>{slotName}</span>
          <p className="text-sm font-semibold text-text-primary truncate">{slotDto.mealName}</p>
          <p className="text-xs text-text-muted">
            {Math.round(totalCals)} kcal · {slotDto.items.length} item{slotDto.items.length !== 1 ? "s" : ""}
          </p>
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {logged ? (
            <span className="flex items-center gap-1 text-xs font-medium text-green-700 bg-green-50 border border-green-200 px-2 py-0.5 rounded-full">
              <Check size={10} /> Logged
            </span>
          ) : (
            <button
              onClick={(e) => { e.stopPropagation(); onLog(); }}
              disabled={loggingThis}
              className="flex items-center gap-1 text-xs font-semibold px-2.5 py-1 rounded-full text-white disabled:opacity-60 transition-opacity"
              style={{ background: "#2E7D52" }}
            >
              {loggingThis ? <Loader2 size={10} className="animate-spin" /> : null}
              Log
            </button>
          )}
          {expanded
            ? <ChevronUp size={14} className="text-text-muted" />
            : <ChevronDown size={14} className="text-text-muted" />}
        </div>
      </button>

      {expanded && (
        <div className="px-4 pb-3 border-t border-divider space-y-1.5 pt-2">
          {slotDto.items.length === 0
            ? <p className="text-xs text-text-muted">No ingredients added</p>
            : slotDto.items.map((item, i) => {
                const g    = item.unit === "GRAM" ? item.quantity : item.quantity * 100;
                const kcal = Math.round(item.caloriesPer100 * g / 100);
                return (
                  <div key={i} className="flex items-center justify-between gap-2 text-xs">
                    <div className="flex items-center gap-1.5 min-w-0">
                      <span className="text-text-secondary truncate">{item.foodName}</span>
                      {item.glycemicIndex != null && <GiBadge gi={item.glycemicIndex} />}
                    </div>
                    <span className="text-text-muted shrink-0">
                      {item.quantity}{item.unit === "GRAM" ? "g" : ` ${item.unit.toLowerCase()}`}
                      {" "}· {kcal} kcal
                    </span>
                  </div>
                );
              })
          }
        </div>
      )}
    </div>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const { user } = useAuth();
  const displayName = user?.displayName?.split(" ")[0] ?? user?.email?.split("@")[0] ?? "there";

  const [data,    setData]    = useState<DashboardDto | null>(null);
  const [error,   setError]   = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingSlot, setLoggingSlot] = useState<string | null>(null);

  const today = todayStr();

  const load = useCallback(() => {
    if (!user) return;
    setLoading(true);
    api.get<DashboardDto>(`/api/v1/dashboard?date=${today}`)
      .then(setData)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  useEffect(() => { load(); }, [load]);

  // ── Derived ───────────────────────────────────────────────────────────────
  const loggedSlotSet = new Set(
    (data?.todayLog?.loggedFoods ?? []).map((lf) => normalizeSlot(lf.mealSlot ?? ""))
  );
  const planSlots = data?.todayPlan?.slots ?? [];
  const allPlanSlotsLogged = planSlots.length > 0 &&
    planSlots.every((s) => loggedSlotSet.has(normalizeSlot(s.slot)));

  // ── Log a meal ────────────────────────────────────────────────────────────
  async function logMeal(slotDto: TodaySlotDto) {
    setLoggingSlot(slotDto.slot);
    try {
      const slotKey  = normalizeSlot(slotDto.slot);
      const newItems = slotDto.items.map((item) => ({
        id: 0, dailyLogId: data?.todayLog?.id ?? 0,
        foodId: item.foodId, mealSlot: slotKey,
        quantity: item.quantity, unit: item.unit,
      }));
      const existing    = data?.todayLog;
      const mergedFoods = [...(existing?.loggedFoods ?? []), ...newItems];

      if (existing) {
        await api.put<DailyLogDto>(`/api/v1/daily-logs/${existing.id}`, { ...existing, loggedFoods: mergedFoods });
      } else {
        await api.post<DailyLogDto>("/api/v1/daily-logs", { date: today, loggedFoods: newItems });
      }
      load();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to log meal");
    } finally {
      setLoggingSlot(null);
    }
  }

  const plan = data?.todayPlan;

  return (
    <div className="space-y-4">

      {/* Header */}
      <div>
        <h1 className="text-[22px] font-semibold text-text-primary leading-tight">
          {greeting()}, {displayName} 👋
        </h1>
        <p className="text-sm text-text-muted mt-0.5">{todayLabel()}</p>
      </div>

      {error && (
        <div className="text-sm text-text-destructive bg-red-50 border border-red-200 rounded-md px-3 py-2">
          {error}
        </div>
      )}

      {/* Streak / Steps / Calories burned */}
      {loading ? (
        <div className="flex gap-2">{[1,2,3].map((i) => <Skeleton key={i} className="flex-1 h-20 rounded-lg" />)}</div>
      ) : (
        <div className="flex gap-2">
          <StatPill icon={<Flame size={16} />}
            value={`${data?.currentStreak ?? 0}${(data?.currentStreak ?? 0) > 0 ? "🔥" : ""}`}
            label="streak" />
          <StatPill icon={<Footprints size={16} />}
            value={data?.todaySteps != null ? Math.round(data.todaySteps).toLocaleString() : "—"}
            label="steps today" />
          <StatPill icon={<Zap size={16} />}
            value={data?.todayCaloriesBurned != null ? `${Math.round(data.todayCaloriesBurned)}` : "—"}
            label="kcal burned" />
        </div>
      )}

      {/* Today's plan header */}
      {loading ? (
        <Skeleton className="h-14 w-full rounded-lg" />
      ) : plan ? (
        <div className="bg-bg-card border border-outline rounded-lg px-4 py-3 flex items-start justify-between gap-3">
          <div className="min-w-0">
            <p className="text-[11px] text-text-muted font-medium uppercase tracking-wide">Today&apos;s plan</p>
            <p className="text-sm font-semibold text-text-primary">{plan.dietName}</p>
            {plan.description && <p className="text-xs text-text-muted mt-0.5 truncate">{plan.description}</p>}
          </div>
          {plan.avgGlycemicIndex != null && <GiBadge gi={plan.avgGlycemicIndex} />}
        </div>
      ) : (
        <div className="bg-bg-card border border-outline rounded-lg px-4 py-3">
          <p className="text-xs font-medium text-text-muted">No diet planned for today</p>
          <p className="text-xs text-text-placeholder mt-0.5">Assign a plan in the <strong>Plan</strong> tab</p>
        </div>
      )}

      {/* Macros: logged vs target */}
      <div className="bg-bg-card border border-outline rounded-lg p-4 space-y-3">
        <p className="text-sm font-semibold text-text-primary">
          Macros {plan ? "vs target" : "today"}
        </p>
        {loading ? (
          <div className="space-y-3">{[1,2,3,4].map((i) => <Skeleton key={i} className="h-5 w-full" />)}</div>
        ) : (
          <>
            <MacroBar label="Calories" logged={data?.todayMacros?.calories ?? 0} target={plan?.targetCalories ?? null} color="#F59E0B" unit="kcal" />
            <MacroBar label="Protein"  logged={data?.todayMacros?.protein  ?? 0} target={plan?.targetProtein  ?? null} color="#2E7D52" />
            <MacroBar label="Carbs"    logged={data?.todayMacros?.carbs    ?? 0} target={plan?.targetCarbs    ?? null} color="#C05200" />
            <MacroBar label="Fat"      logged={data?.todayMacros?.fat      ?? 0} target={plan?.targetFat      ?? null} color="#1E4FBF" />
          </>
        )}
      </div>

      {/* Today's meals */}
      {loading ? (
        <div className="space-y-2">{[1,2,3].map((i) => <Skeleton key={i} className="h-16 w-full rounded-lg" />)}</div>
      ) : plan && plan.slots.length > 0 ? (
        <div className="space-y-2">
          <p className="text-sm font-semibold text-text-primary">Today&apos;s meals</p>
          {plan.slots.map((slotDto) => (
            <MealSlotCard
              key={slotDto.slot}
              slotDto={slotDto}
              logged={loggedSlotSet.has(normalizeSlot(slotDto.slot))}
              loggingThis={loggingSlot === slotDto.slot}
              onLog={() => logMeal(slotDto)}
            />
          ))}
        </div>
      ) : !loading && (
        <div className="bg-bg-card rounded-lg border border-outline overflow-hidden">
          <div className="px-4 pt-4 pb-2">
            <p className="text-sm font-semibold text-text-primary">Today&apos;s meals</p>
          </div>
          <div className="px-4 pb-2">
            {MEAL_SLOTS.filter((s) => s.key !== "Snack").map((slot) => {
              const count = (data?.todayLog?.loggedFoods ?? []).filter((lf) => lf.mealSlot === slot.key).length;
              return (
                <div key={slot.key} className="flex items-center justify-between py-2.5 border-b border-divider last:border-0">
                  <div className="flex items-center gap-3">
                    <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: slot.color }} />
                    <span className="text-sm font-medium text-text-primary">{slot.key}</span>
                  </div>
                  {count > 0 ? (
                    <span className="text-xs font-medium px-2.5 py-0.5 rounded-xl" style={{ background: slot.bg, color: slot.color }}>
                      {count} item{count !== 1 ? "s" : ""} logged
                    </span>
                  ) : (
                    <span className="text-xs text-text-placeholder">Not logged</span>
                  )}
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Mark day complete */}
      {!loading && allPlanSlotsLogged && (
        <div className="w-full flex items-center justify-center gap-2 py-3 rounded-lg font-semibold text-sm bg-green-50 text-green-700 border border-green-200 cursor-default">
          <Check size={16} />
          Day Complete! 🎉
        </div>
      )}


    </div>
  );
}
