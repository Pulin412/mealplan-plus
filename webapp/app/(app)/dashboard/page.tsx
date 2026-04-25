"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { UtensilsCrossed, Flame, Dumbbell, Weight } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type DailyLogDto = components["schemas"]["DailyLogDto"];
type FoodDto = components["schemas"]["FoodDto"];
type DietDto = components["schemas"]["DietDto"];
type HealthMetricDto = components["schemas"]["HealthMetricDto"];

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning";
  if (h < 17) return "Good afternoon";
  return "Good evening";
}

function todayLabel() {
  return new Date().toLocaleDateString("en-GB", { weekday: "long", day: "numeric", month: "long" });
}

function todayStr() {
  return new Date().toISOString().split("T")[0];
}

function calcCalories(log: DailyLogDto, foods: FoodDto[]): number {
  const foodMap = new Map(foods.map((f) => [f.id, f]));
  return (log.loggedFoods ?? []).reduce((sum, lf) => {
    const food = foodMap.get(lf.foodId);
    if (!food) return sum;
    const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food.caloriesPer100 * grams) / 100;
  }, 0);
}

function calcMacro(
  log: DailyLogDto,
  foods: FoodDto[],
  key: "proteinPer100" | "carbsPer100" | "fatPer100"
): number {
  const foodMap = new Map(foods.map((f) => [f.id, f]));
  return (log.loggedFoods ?? []).reduce((sum, lf) => {
    const food = foodMap.get(lf.foodId);
    if (!food) return sum;
    const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food[key] * grams) / 100;
  }, 0);
}

const SLOTS = [
  { key: "Breakfast", color: "#F59E0B", bg: "#FFF8E6" },
  { key: "Lunch",     color: "#2E7D52", bg: "#E8F5EE" },
  { key: "Dinner",    color: "#7C3AED", bg: "#F3EEFF" },
] as const;

function formatDateShort(dateStr: string) {
  const today = todayStr();
  const yesterday = new Date(Date.now() - 86400000).toISOString().split("T")[0];
  if (dateStr === today) return "Today";
  if (dateStr === yesterday) return "Yesterday";
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", { weekday: "short", day: "numeric", month: "short" });
}

interface DashboardData {
  todayLog: DailyLogDto | null;
  totalCal: number;
  protein: number;
  carbs: number;
  fat: number;
  slotsLogged: Set<string>;
  dietCount: number;
  latestWeight: HealthMetricDto | null;
  recentLogs: DailyLogDto[];
  foods: FoodDto[];
}

// ── Stat card ─────────────────────────────────────────────────────────────────
function StatCard({
  icon, iconBg, iconColor, label, value, unit, loading,
}: {
  icon: React.ReactNode;
  iconBg: string;
  iconColor: string;
  label: string;
  value: string | number;
  unit?: string;
  loading: boolean;
}) {
  return (
    <div className="bg-bg-card rounded-lg border border-outline p-4 flex items-center gap-3">
      <div className="w-10 h-10 rounded-md flex items-center justify-center shrink-0" style={{ background: iconBg }}>
        <span style={{ color: iconColor }}>{icon}</span>
      </div>
      <div className="min-w-0">
        <p className="text-xs font-medium text-text-muted">{label}</p>
        {loading ? (
          <Skeleton className="h-6 w-16 mt-1" />
        ) : (
          <p className="text-xl font-semibold text-text-primary leading-tight">
            {value}
            {unit && <span className="text-sm font-normal text-text-muted ml-1">{unit}</span>}
          </p>
        )}
      </div>
    </div>
  );
}

// ── Macro bar ─────────────────────────────────────────────────────────────────
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

// ── Slot row ──────────────────────────────────────────────────────────────────
function SlotRow({
  label, color, bg, logged, count,
}: {
  label: string; color: string; bg: string; logged: boolean; count: number;
}) {
  return (
    <div className="flex items-center justify-between py-2.5 border-b border-divider last:border-0">
      <div className="flex items-center gap-3">
        <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: color }} />
        <span className="text-sm font-medium text-text-primary">{label}</span>
      </div>
      {logged ? (
        <span
          className="text-xs font-medium px-2.5 py-0.5 rounded-xl"
          style={{ background: bg, color }}
        >
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

  const [data, setData] = useState<DashboardData | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;
    setLoading(true);
    Promise.all([
      api.get<DailyLogDto[]>("/api/v1/daily-logs"),
      api.get<FoodDto[]>("/api/v1/foods"),
      api.get<DietDto[]>("/api/v1/diets"),
      api.get<HealthMetricDto[]>("/api/v1/health-metrics"),
    ])
      .then(([logs, foods, diets, metrics]) => {
        const today = todayStr();
        const todayLog = logs.find((l) => l.date === today) ?? null;
        const slotsLogged = new Set((todayLog?.loggedFoods ?? []).map((lf) => lf.mealSlot));
        const totalCal = todayLog ? calcCalories(todayLog, foods) : 0;
        const protein = todayLog ? calcMacro(todayLog, foods, "proteinPer100") : 0;
        const carbs = todayLog ? calcMacro(todayLog, foods, "carbsPer100") : 0;
        const fat = todayLog ? calcMacro(todayLog, foods, "fatPer100") : 0;

        const latestWeight = metrics
          .filter((m) => m.type === "WEIGHT")
          .sort((a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime())[0] ?? null;

        const recentLogs = [...logs].sort((a, b) => (a.date < b.date ? 1 : -1)).slice(0, 5);

        setData({ todayLog, totalCal, protein, carbs, fat, slotsLogged, dietCount: diets.length, latestWeight, recentLogs, foods });
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

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
        <StatCard
          icon={<UtensilsCrossed size={18} />}
          iconBg="#E8F5EE" iconColor="#2E7D52"
          label="Meals today"
          value={loading ? "—" : `${data?.slotsLogged.size ?? 0}/3`}
          loading={false}
        />
        <StatCard
          icon={<Flame size={18} />}
          iconBg="#FFF8E6" iconColor="#F59E0B"
          label="Calories"
          value={loading ? "—" : Math.round(data?.totalCal ?? 0)}
          unit="kcal"
          loading={false}
        />
        <StatCard
          icon={<Dumbbell size={18} />}
          iconBg="#E8EEFF" iconColor="#1E4FBF"
          label="Active diets"
          value={loading ? "—" : data?.dietCount ?? 0}
          loading={false}
        />
        <StatCard
          icon={<Weight size={18} />}
          iconBg="#F3EEFF" iconColor="#7C3AED"
          label="Weight"
          value={loading ? "—" : data?.latestWeight ? data.latestWeight.value : "—"}
          unit={data?.latestWeight?.unit ?? "kg"}
          loading={false}
        />
      </div>

      {/* Today's meals */}
      <div className="bg-bg-card rounded-lg border border-outline overflow-hidden">
        <div className="px-4 pt-4 pb-2">
          <p className="text-sm font-semibold text-text-primary">Today&apos;s meals</p>
        </div>
        <div className="px-4 pb-2">
          {loading ? (
            <div className="space-y-3 py-2">
              {[1,2,3].map((i) => <Skeleton key={i} className="h-5 w-full" />)}
            </div>
          ) : (
            SLOTS.map((slot) => {
              const count = (data?.todayLog?.loggedFoods ?? []).filter((lf) => lf.mealSlot === slot.key).length;
              return (
                <SlotRow
                  key={slot.key}
                  label={slot.key}
                  color={slot.color}
                  bg={slot.bg}
                  logged={count > 0}
                  count={count}
                />
              );
            })
          )}
        </div>
      </div>

      {/* Macros */}
      <div className="bg-bg-card rounded-lg border border-outline p-4 space-y-3">
        <p className="text-sm font-semibold text-text-primary">Macros today</p>
        {loading ? (
          <div className="space-y-2">
            {[1,2,3].map((i) => <Skeleton key={i} className="h-4 w-full" />)}
          </div>
        ) : (
          <>
            <MacroRow label="Protein" value={data?.protein ?? 0} unit="g" color="#2E7D52" />
            <MacroRow label="Carbs"   value={data?.carbs ?? 0}   unit="g" color="#C05200" />
            <MacroRow label="Fat"     value={data?.fat ?? 0}     unit="g" color="#1E4FBF" />
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
            <div className="space-y-3 py-2">
              {[1,2,3].map((i) => <Skeleton key={i} className="h-5 w-full" />)}
            </div>
          ) : !data || data.recentLogs.length === 0 ? (
            <p className="text-sm text-text-muted py-3">No logs yet &mdash; start tracking in the Log tab.</p>
          ) : (
            data.recentLogs.map((log) => {
              const cal = Math.round(calcCalories(log, data.foods));
              const slots = new Set((log.loggedFoods ?? []).map((lf) => lf.mealSlot)).size;
              return (
                <div key={log.id ?? log.date} className="flex items-center justify-between py-2.5 border-b border-divider last:border-0">
                  <span className="text-sm font-medium text-text-primary">{formatDateShort(log.date)}</span>
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
