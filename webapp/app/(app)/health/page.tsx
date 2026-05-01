"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { Plus, X } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid,
} from "recharts";

type HealthMetricDto = components["schemas"]["HealthMetricDto"];
type DailyLogDto    = components["schemas"]["DailyLogDto"];
type FoodDto        = components["schemas"]["FoodDto"];

const METRIC_TYPES = [
  { value: "WEIGHT",         label: "Weight",         unit: "kg",    emoji: "⚖️",  color: "#7C3AED", bg: "#F3EEFF" },
  { value: "HEART_RATE",     label: "Heart Rate",     unit: "bpm",   emoji: "❤️",  color: "#D32F2F", bg: "#FFF0F0" },
  { value: "STEPS",          label: "Steps",          unit: "steps", emoji: "👟",  color: "#2E7D52", bg: "#E8F5EE" },
  { value: "SLEEP",          label: "Sleep",          unit: "h",     emoji: "😴",  color: "#1E4FBF", bg: "#E8EEFF" },
  { value: "BLOOD_PRESSURE", label: "Blood Pressure", unit: "mmHg",  emoji: "💉",  color: "#C05200", bg: "#FFF0E6" },
  { value: "CUSTOM",         label: "Custom",         unit: "",      emoji: "📊",  color: "#555555", bg: "#F0F0F0" },
];

function metaMeta(type: string) {
  return METRIC_TYPES.find((m) => m.value === type) ?? METRIC_TYPES[METRIC_TYPES.length - 1];
}

function formatDate(iso: string | undefined) {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

function formatShort(iso: string | undefined) {
  if (!iso) return "";
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short" });
}

function avg(vals: number[]) { return vals.length ? vals.reduce((a, b) => a + b, 0) / vals.length : 0; }

function todayStr() { return new Date().toISOString().split("T")[0]; }

function computeStreak(logs: DailyLogDto[]): number {
  const loggedDates = new Set(logs.filter((l) => (l.loggedFoods ?? []).length > 0).map((l) => l.date));
  const today = todayStr();
  let i = loggedDates.has(today) ? 0 : 1;
  let streak = 0;
  while (true) {
    const d = new Date(today);
    d.setDate(d.getDate() - i);
    const ds = d.toISOString().split("T")[0];
    if (loggedDates.has(ds)) { streak++; i++; } else break;
  }
  return streak;
}

function calcDayCals(log: DailyLogDto, foods: FoodDto[]): number {
  return (log.loggedFoods ?? []).reduce((sum, lf) => {
    const food = foods.find((f) => f.id === lf.foodId);
    if (!food) return sum;
    const g = lf.unit === "GRAM" ? lf.quantity : lf.quantity * 100;
    return sum + (food.caloriesPer100 * g) / 100;
  }, 0);
}

export default function HealthPage() {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState<HealthMetricDto[]>([]);
  const [logs, setLogs] = useState<DailyLogDto[]>([]);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);

  // Form state
  const [formType, setFormType] = useState("WEIGHT");
  const [formValue, setFormValue] = useState("");
  const [formUnit, setFormUnit] = useState("kg");
  const [formDate, setFormDate] = useState(new Date().toISOString().split("T")[0]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!user) return;
    Promise.all([
      api.get<HealthMetricDto[]>("/api/v1/health-metrics"),
      api.get<DailyLogDto[]>("/api/v1/daily-logs"),
      api.get<FoodDto[]>("/api/v1/foods"),
    ])
      .then(([m, l, f]) => { setMetrics(m); setLogs(l); setFoods(f); })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  const handleTypeChange = (t: string) => {
    setFormType(t);
    const meta = metaMeta(t);
    if (meta.unit) setFormUnit(meta.unit);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const v = parseFloat(formValue);
    if (isNaN(v)) { setFormError("Enter a valid number"); return; }
    setSubmitting(true); setFormError(null);
    try {
      const payload: HealthMetricDto = {
        type: formType, value: v, unit: formUnit,
        recordedAt: new Date(formDate + "T12:00:00Z").toISOString(),
      };
      const created = await api.post<HealthMetricDto>("/api/v1/health-metrics", payload);
      setMetrics((prev) => [created, ...prev]);
      setFormValue(""); setShowForm(false);
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : "Failed to save");
    } finally { setSubmitting(false); }
  };

  const deleteMetric = async (id: number) => {
    try {
      await api.delete(`/api/v1/health-metrics/${id}`);
      setMetrics((prev) => prev.filter((m) => m.id !== id));
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to delete"); }
  };

  const sorted = [...metrics].sort(
    (a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime()
  );

  const weightEntries = sorted.filter((m) => m.type === "WEIGHT");
  const latestWeight  = weightEntries[0];
  const thirtyDaysAgo = Date.now() - 30 * 86400000;
  const avgWeight30   = avg(weightEntries.filter((m) => new Date(m.recordedAt ?? 0).getTime() >= thirtyDaysAgo).map((m) => m.value));
  const streak        = computeStreak(logs);
  const totalLoggedDays = new Set(logs.filter((l) => (l.loggedFoods ?? []).length > 0).map((l) => l.date)).size;

  const latestPerType = new Map<string, HealthMetricDto>();
  sorted.forEach((m) => { if (!latestPerType.has(m.type)) latestPerType.set(m.type, m); });

  // Chart data
  const weightChartData = [...weightEntries].reverse().slice(-30).map((m) => ({
    date: formatShort(m.recordedAt),
    weight: m.value,
  }));

  const calChartData = [...logs]
    .sort((a, b) => a.date < b.date ? -1 : 1)
    .slice(-30)
    .map((log) => ({
      date: new Date(log.date + "T00:00:00").toLocaleDateString("en-GB", { day: "numeric", month: "short" }),
      cal: Math.round(calcDayCals(log, foods)),
    }))
    .filter((d) => d.cal > 0);

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between pt-1">
        <h1 className="text-[22px] font-semibold text-text-primary">Health</h1>
        <button
          onClick={() => setShowForm((v) => !v)}
          className="flex items-center gap-1.5 rounded-xl bg-text-primary px-4 py-2 text-sm font-semibold text-bg-card"
        >
          <Plus size={15} /> Log metric
        </button>
      </div>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* ── Stat tiles ── */}
      <div className="grid grid-cols-2 gap-3">
        {[
          { label: "Latest weight", val: latestWeight ? `${latestWeight.value}` : "—", unit: latestWeight?.unit ?? "kg", color: "#7C3AED" },
          { label: "30-day avg",    val: avgWeight30 > 0 ? avgWeight30.toFixed(1) : "—", unit: "kg", color: "#2E7D52" },
          { label: "Streak",        val: streak > 0 ? `${streak}` : "—", unit: streak === 1 ? "day" : "days", color: "#F59E0B" },
          { label: "Days logged",   val: `${totalLoggedDays}`, unit: "total", color: "#1E4FBF" },
        ].map(({ label, val, unit, color }) => (
          <div key={label} className="bg-bg-card rounded-xl border border-divider px-3 py-3">
            {loading ? <Skeleton className="h-8 w-full" /> : (
              <>
                <p className="text-[20px] font-bold leading-tight" style={{ color }}>
                  {val}<span className="text-xs font-normal text-text-muted ml-0.5">{unit}</span>
                </p>
                <p className="text-[10px] text-text-muted mt-0.5">{label}</p>
              </>
            )}
          </div>
        ))}
      </div>

      {/* ── Log form ── */}
      {showForm && (
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3 border-b border-divider">
            <p className="text-[13px] font-semibold text-text-primary">Log a metric</p>
            <button onClick={() => setShowForm(false)} className="text-text-muted hover:text-text-primary"><X size={16} /></button>
          </div>
          <form onSubmit={handleSubmit} className="px-4 py-3 space-y-3">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Type</p>
                <select
                  value={formType}
                  onChange={(e) => handleTypeChange(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary"
                >
                  {METRIC_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Date</p>
                <input type="date" value={formDate} onChange={(e) => setFormDate(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Value</p>
                <input type="number" step="any" placeholder="0" value={formValue}
                  onChange={(e) => setFormValue(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Unit</p>
                <input value={formUnit} onChange={(e) => setFormUnit(e.target.value)} placeholder="kg"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
            </div>
            {formError && <p className="text-xs text-red-500">{formError}</p>}
            <button type="submit" disabled={submitting}
              className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-50">
              {submitting ? "Saving…" : "Save metric"}
            </button>
          </form>
        </div>
      )}

      {/* ── Weight chart ── */}
      {!loading && weightChartData.length >= 2 && (
        <div className="bg-bg-card rounded-xl border border-divider p-4">
          <p className="text-[13px] font-semibold text-text-primary mb-3">Weight trend</p>
          <ResponsiveContainer width="100%" height={160}>
            <LineChart data={weightChartData} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F0F0F0" />
              <XAxis dataKey="date" tick={{ fontSize: 10, fill: "#999" }} tickLine={false} />
              <YAxis tick={{ fontSize: 10, fill: "#999" }} tickLine={false} domain={["auto", "auto"]} />
              <Tooltip
                contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid #E5E5E5" }}
                formatter={(v) => [`${v} kg`, "Weight"]}
              />
              <Line type="monotone" dataKey="weight" stroke="#7C3AED" strokeWidth={2} dot={false} activeDot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* ── Calorie chart ── */}
      {!loading && calChartData.length >= 2 && (
        <div className="bg-bg-card rounded-xl border border-divider p-4">
          <p className="text-[13px] font-semibold text-text-primary mb-3">Daily calories (last 30 days)</p>
          <ResponsiveContainer width="100%" height={160}>
            <BarChart data={calChartData} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F0F0F0" vertical={false} />
              <XAxis dataKey="date" tick={{ fontSize: 10, fill: "#999" }} tickLine={false} />
              <YAxis tick={{ fontSize: 10, fill: "#999" }} tickLine={false} />
              <Tooltip
                contentStyle={{ fontSize: 12, borderRadius: 8, border: "1px solid #E5E5E5" }}
                formatter={(v) => [`${v} kcal`, "Calories"]}
              />
              <Bar dataKey="cal" fill="#2E7D52" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* ── Latest per type ── */}
      {!loading && latestPerType.size > 0 && (
        <div>
          <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Latest readings</p>
          <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
            {Array.from(latestPerType.entries()).map(([type, m]) => {
              const meta = metaMeta(type);
              return (
                <div key={type} className="flex items-center gap-3 px-4 py-3">
                  <div className="w-9 h-9 rounded-xl flex items-center justify-center shrink-0 text-base" style={{ background: meta.bg }}>
                    {meta.emoji}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-[13px] font-semibold text-text-primary">{meta.label}</p>
                    <p className="text-[11px] text-text-muted">{formatDate(m.recordedAt)}</p>
                  </div>
                  <p className="text-[15px] font-bold" style={{ color: meta.color }}>
                    {m.value} <span className="text-xs font-normal text-text-muted">{m.unit}</span>
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* ── Weight history ── */}
      {weightEntries.length > 0 && (
        <div>
          <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Weight history</p>
          <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
            {weightEntries.slice(0, 10).map((m) => (
              <div key={m.id} className="flex items-center justify-between px-4 py-3">
                <p className="text-[13px] text-text-muted">{formatDate(m.recordedAt)}</p>
                <div className="flex items-center gap-3">
                  <p className="text-[13px] font-semibold text-text-primary">{m.value} {m.unit}</p>
                  <button onClick={() => m.id !== undefined && deleteMetric(m.id)} className="text-text-muted hover:text-red-500 transition-colors">
                    <X size={14} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Other metrics ── */}
      {!loading && sorted.filter((m) => m.type !== "WEIGHT").length > 0 && (
        <div>
          <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Other metrics</p>
          <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
            {sorted.filter((m) => m.type !== "WEIGHT").slice(0, 20).map((m) => {
              const meta = metaMeta(m.type);
              return (
                <div key={m.id} className="flex items-center justify-between px-4 py-3">
                  <div className="flex items-center gap-3">
                    <span className="text-base">{meta.emoji}</span>
                    <div>
                      <p className="text-[13px] font-semibold text-text-primary">{m.value} {m.unit}</p>
                      <p className="text-[11px] text-text-muted">{meta.label} · {formatDate(m.recordedAt)}</p>
                    </div>
                  </div>
                  <button onClick={() => m.id !== undefined && deleteMetric(m.id)} className="text-text-muted hover:text-red-500 transition-colors">
                    <X size={14} />
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {!loading && metrics.length === 0 && (
        <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-12 gap-3">
          <span className="text-4xl">⚖️</span>
          <p className="text-sm text-text-muted">No metrics logged yet</p>
          <button onClick={() => setShowForm(true)} className="text-sm font-semibold text-green">Log your first metric →</button>
        </div>
      )}
    </div>
  );
}
