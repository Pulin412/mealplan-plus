"use client";
import { useEffect, useState } from "react";
import { UtensilsCrossed, Flame, Dumbbell, Weight } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";
import { todayStr, calcCalories, formatDateShort, MEAL_SLOTS } from "@/lib/utils";

type DailyLogDto     = components["schemas"]["DailyLogDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type HealthMetricDto = components["schemas"]["HealthMetricDto"];

interface DayCalories { date: string; calories: number; }
interface MacroTotals { protein: number; carbs: number; fat: number; calories: number; }

interface DashboardDto {
  todayLog: DailyLogDto | null;
  recentLogs: DailyLogDto[];
  foods: FoodDto[];
  dietCount: number;
  latestWeight: HealthMetricDto | null;
  currentStreak: number;
  weeklyCalories: DayCalories[];
  todayMacros: MacroTotals;
}

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

function todayLabel() {
  return new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" });
}

function calcLogCalories(log: DailyLogDto, foods: FoodDto[]): number {
  const foodMap = new Map(foods.map((f) => [f.id, f]));
  return (log.loggedFoods ?? []).reduce((sum, lf) => {
    const food = foodMap.get(lf.foodId);
    if (!food) return sum;
    return sum + calcCalories(food, lf);
  }, 0);
}

// ── Stat card ─────────────────────────────────────────────────────────────────
function StatCard({ icon, iconBg, iconColor, label, value, unit, loading }: {
  icon: React.ReactNode; iconBg: string; iconColor: string;
  label: string; value: string | number; unit?: string; loading: boolean;
}) {
  return (
    <div className="bg-bg-card rounded-lg border border-outline p-4 flex items-center gap-3">
      <div className="w-10 h-10 rounded-md flex items-center justify-center shrink-0" style={{ background: iconBg }}>
        <span style={{ color: iconColor }}>{icon}</span>
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium text-text-muted">{label}</p>
        {loading ? <Skeleton className="h-6 w-16 mt-1" /> : (
          <p className="text-xl font-semibold text-text-primary leading-tight">
            {value}
            {unit && <span className="text-sm font-normal text-text-muted ml-1">{unit}</span>}
          </p>
        )}
      </div>
    </div>
  );
}

function MacroRow({ label, value, unit, color }: { label: string; value: number; unit: string; color: string }) {
  return (
    <div className="flex items-center justify-between text-sm">
      <div className="flex items-center gap-2">
        <span className="w-2 h-2 rounded-full shrink-0" style={{ background: color }} />
        <span className="text-text-secondary">{label}</span>
      </div>
      <span className="font-semibold text-text-primary">{Math.round(value)}{unit}</span>
    </div>
  );
}

function SlotRow({ label, color, bg, logged, count }: {
  label: string; color: string; bg: string; logged: boolean; count: number;
}) {
  return (
    <div className="flex items-center justify-between py-2.5 border-b border-divider last:border-0">
      <div className="flex items-center gap-3">
        <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: color }} />
        <span className="text-sm font-medium text-text-primary">{label}</span>
      </div>
      {logged ? (
        <span className="text-xs font-medium px-2.5 py-0.5 rounded-xl" style={{ background: bg, color }}>
          {count} item{count !== 1 ? "s" : ""} logged
        </span>
      ) : (
        <span className="text-xs text-text-placeholder">Not logged</span>
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

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    api.get<DashboardDto>("/api/v1/dashboard")
      .then(setData)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  const today = todayStr();
  const slotsLogged = new Set((data?.todayLog?.loggedFoods ?? []).map((lf) => lf.mealSlot));

  return (
    <div className="space-y-5">

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

      {/* 4 stat cards */}
      <div className="grid grid-cols-2 gap-3">
        <StatCard icon={<UtensilsCrossed size={18} />} iconBg="#E8F5EE" iconColor="#2E7D52"
          label="Meals today" value={loading ? "—" : `${slotsLogged.size}/3`} loading={false} />
        <StatCard icon={<Flame size={18} />} iconBg="#FFF8E6" iconColor="#F59E0B"
          label="Calories" value={loading ? "—" : Math.round(data?.todayMacros?.calories ?? 0)} unit="kcal" loading={false} />
        <StatCard icon={<Dumbbell size={18} />} iconBg="#E8EEFF" iconColor="#1E4FBF"
          label={`Streak ${loading ? "" : "🔥"}`}
          value={loading ? "—" : data?.currentStreak ?? 0} unit="days" loading={false} />
        <StatCard icon={<Weight size={18} />} iconBg="#F3EEFF" iconColor="#7C3AED"
          label="Weight" value={loading ? "—" : data?.latestWeight ? data.latestWeight.value : "—"}
          unit={data?.latestWeight?.unit ?? "kg"} loading={false} />
      </div>

      {/* Today's meals */}
      <div className="bg-bg-card rounded-lg border border-outline overflow-hidden">
        <div className="px-4 pt-4 pb-2">
          <p className="text-sm font-semibold text-text-primary">Today&apos;s meals</p>
        </div>
        <div className="px-4 pb-2">
          {loading ? (
            <div className="space-y-3 py-2">{[1,2,3].map((i) => <Skeleton key={i} className="h-5 w-full" />)}</div>
          ) : (
            MEAL_SLOTS.filter((s) => s.key !== "Snack").map((slot) => {
              const count = (data?.todayLog?.loggedFoods ?? []).filter((lf) => lf.mealSlot === slot.key).length;
              return <SlotRow key={slot.key} label={slot.key} color={slot.color} bg={slot.bg} logged={count > 0} count={count} />;
            })
          )}
        </div>
      </div>

      {/* Macros */}
      <div className="bg-bg-card rounded-lg border border-outline p-4 space-y-3">
        <p className="text-sm font-semibold text-text-primary">Macros today</p>
        {loading ? (
          <div className="space-y-2">{[1,2,3].map((i) => <Skeleton key={i} className="h-4 w-full" />)}</div>
        ) : (
          <>
            <MacroRow label="Protein" value={data?.todayMacros?.protein ?? 0} unit="g" color="#2E7D52" />
            <MacroRow label="Carbs"   value={data?.todayMacros?.carbs ?? 0}   unit="g" color="#C05200" />
            <MacroRow label="Fat"     value={data?.todayMacros?.fat ?? 0}     unit="g" color="#1E4FBF" />
          </>
        )}
      </div>

      {/* Recent logs */}
      <div className="bg-bg-card rounded-lg border border-outline overflow-hidden">
        <div className="px-4 pt-4 pb-2">
          <p className="text-sm font-semibold text-text-primary">Recent activity</p>
        </div>
        <div className="px-4 pb-2">
          {loading ? (
            <div className="space-y-3 py-2">{[1,2,3].map((i) => <Skeleton key={i} className="h-5 w-full" />)}</div>
          ) : !data || data.recentLogs.length === 0 ? (
            <p className="text-sm text-text-muted py-3">No logs yet &mdash; start tracking in the Log tab.</p>
          ) : (
            data.recentLogs.map((log) => {
              const cal = Math.round(calcLogCalories(log, data.foods));
              const slots = new Set((log.loggedFoods ?? []).map((lf) => lf.mealSlot)).size;
              return (
                <div key={log.id ?? log.date} className="flex items-center justify-between py-2.5 border-b border-divider last:border-0">
                  <span className="text-sm font-medium text-text-primary">{formatDateShort(log.date ?? today)}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-text-muted">{slots} meal{slots !== 1 ? "s" : ""}</span>
                    {cal > 0 && (
                      <span className="text-xs font-medium px-2 py-0.5 rounded-xl bg-green-light text-green">
                        {cal} kcal
                      </span>
                    )}
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

    </div>
  );
}
