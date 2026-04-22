"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
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
    // quantity is in grams by default; caloriesPer100 is per 100g
    const grams = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food.caloriesPer100 * grams) / 100;
  }, 0);
}

function formatDate(dateStr: string) {
  return new Date(dateStr + "T00:00:00").toLocaleDateString("en-GB", {
    weekday: "short",
    day: "numeric",
    month: "short",
  });
}

interface DashboardData {
  todayLog: DailyLogDto | null;
  totalCaloriesToday: number;
  slotsLoggedToday: number;
  dietCount: number;
  latestWeight: HealthMetricDto | null;
  recentLogs: DailyLogDto[];
  foods: FoodDto[];
}

export default function DashboardPage() {
  const { user } = useAuth();
  const displayName = user?.displayName ?? user?.email?.split("@")[0] ?? "there";

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
        const slotsLoggedToday = new Set((todayLog?.loggedFoods ?? []).map((lf) => lf.mealSlot)).size;
        const totalCaloriesToday = todayLog ? calcCalories(todayLog, foods) : 0;

        const weightEntries = metrics
          .filter((m) => m.type === "WEIGHT")
          .sort((a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime());
        const latestWeight = weightEntries[0] ?? null;

        const recentLogs = [...logs]
          .sort((a, b) => (a.date < b.date ? 1 : -1))
          .slice(0, 5);

        setData({
          todayLog,
          totalCaloriesToday,
          slotsLoggedToday,
          dietCount: diets.length,
          latestWeight,
          recentLogs,
          foods,
        });
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load dashboard"))
      .finally(() => setLoading(false));
  }, [user]);

  return (
    <div className="space-y-6 max-w-2xl">
      <div>
        <h1 className="text-2xl font-bold">
          {greeting()}, {displayName}
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">{todayLabel()}</p>
      </div>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Meals logged today</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-3/4" />
            ) : (
              <p className="text-2xl font-bold">{data?.slotsLoggedToday ?? 0}<span className="text-base font-normal text-muted-foreground">/3</span></p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Calories today</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-3/4" />
            ) : (
              <p className="text-2xl font-bold">{Math.round(data?.totalCaloriesToday ?? 0)}<span className="text-base font-normal text-muted-foreground"> kcal</span></p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Active diets</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-3/4" />
            ) : (
              <p className="text-2xl font-bold">{data?.dietCount ?? 0}</p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">Latest weight</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-8 w-3/4" />
            ) : data?.latestWeight ? (
              <p className="text-2xl font-bold">
                {data.latestWeight.value}
                <span className="text-base font-normal text-muted-foreground"> {data.latestWeight.unit}</span>
              </p>
            ) : (
              <p className="text-muted-foreground text-sm">Not logged yet</p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">Recent activity</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2">
          {loading ? (
            Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-6 w-full" />)
          ) : !data || data.recentLogs.length === 0 ? (
            <p className="text-sm text-muted-foreground">No logs yet — start tracking your meals in the Log screen.</p>
          ) : (
            data.recentLogs.map((log) => {
              const slots = new Set((log.loggedFoods ?? []).map((lf) => lf.mealSlot));
              const cal = Math.round(calcCalories(log, data.foods));
              return (
                <div key={log.id ?? log.date} className="flex items-center justify-between text-sm py-1 border-b last:border-0">
                  <span className="font-medium">{formatDate(log.date)}</span>
                  <div className="flex items-center gap-2">
                    <span className="text-muted-foreground">{slots.size} meal{slots.size !== 1 ? "s" : ""}</span>
                    {cal > 0 && <Badge variant="secondary">{cal} kcal</Badge>}
                  </div>
                </div>
              );
            })
          )}
        </CardContent>
      </Card>
    </div>
  );
}
