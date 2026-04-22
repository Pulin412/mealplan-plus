"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type HealthMetricDto = components["schemas"]["HealthMetricDto"];

const METRIC_TYPES = [
  { value: "WEIGHT", label: "Weight", defaultUnit: "kg" },
  { value: "HEART_RATE", label: "Heart Rate", defaultUnit: "bpm" },
  { value: "STEPS", label: "Steps", defaultUnit: "steps" },
  { value: "SLEEP", label: "Sleep", defaultUnit: "h" },
  { value: "BLOOD_PRESSURE", label: "Blood Pressure", defaultUnit: "mmHg" },
  { value: "CUSTOM", label: "Custom", defaultUnit: "" },
];

function formatDateTime(iso: string | undefined): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("en-GB", { day: "numeric", month: "short", year: "numeric" });
}

function avg(values: number[]): number {
  if (values.length === 0) return 0;
  return values.reduce((a, b) => a + b, 0) / values.length;
}

export default function HealthPage() {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState<HealthMetricDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [formType, setFormType] = useState("WEIGHT");
  const [formValue, setFormValue] = useState("");
  const [formUnit, setFormUnit] = useState("kg");
  const [formDate, setFormDate] = useState(new Date().toISOString().split("T")[0]);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const loadMetrics = () => {
    if (!user) return;
    api.get<HealthMetricDto[]>("/api/v1/health-metrics")
      .then(setMetrics)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  };

  useEffect(() => { loadMetrics(); }, [user]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleTypeChange = (t: string) => {
    setFormType(t);
    const meta = METRIC_TYPES.find((m) => m.value === t);
    if (meta) setFormUnit(meta.defaultUnit);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const v = parseFloat(formValue);
    if (isNaN(v)) { setFormError("Enter a valid number"); return; }
    setSubmitting(true);
    setFormError(null);
    try {
      const payload: HealthMetricDto = {
        type: formType,
        value: v,
        unit: formUnit,
        recordedAt: new Date(formDate + "T12:00:00Z").toISOString(),
      };
      const created = await api.post<HealthMetricDto>("/api/v1/health-metrics", payload);
      setMetrics((prev) => [created, ...prev]);
      setFormValue("");
    } catch (e: unknown) {
      setFormError(e instanceof Error ? e.message : "Failed to save");
    } finally {
      setSubmitting(false);
    }
  };

  const deleteMetric = async (id: number) => {
    try {
      await api.delete(`/api/v1/health-metrics/${id}`);
      setMetrics((prev) => prev.filter((m) => m.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  // Weight-specific stats
  const weightEntries = metrics
    .filter((m) => m.type === "WEIGHT")
    .sort((a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime());

  const thirtyDaysAgo = new Date();
  thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
  const weightLast30 = weightEntries.filter((m) => {
    const d = new Date(m.recordedAt ?? m.updatedAt ?? 0);
    return d >= thirtyDaysAgo;
  });
  const avgWeight30 = avg(weightLast30.map((m) => m.value));
  const latestWeight = weightEntries[0];

  // Group latest per type
  const latestPerType = new Map<string, HealthMetricDto>();
  [...metrics]
    .sort((a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime())
    .forEach((m) => {
      if (!latestPerType.has(m.type)) latestPerType.set(m.type, m);
    });

  return (
    <div className="space-y-6 max-w-xl">
      <h1 className="text-2xl font-bold">Health</h1>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {/* Stats overview */}
      <div className="grid gap-3 sm:grid-cols-3">
        <Card>
          <CardHeader className="pb-1"><CardTitle className="text-xs text-muted-foreground">Latest weight</CardTitle></CardHeader>
          <CardContent>
            {loading ? <Skeleton className="h-6 w-20" /> : latestWeight
              ? <p className="text-lg font-bold">{latestWeight.value} <span className="text-sm font-normal text-muted-foreground">{latestWeight.unit}</span></p>
              : <p className="text-sm text-muted-foreground">—</p>
            }
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-1"><CardTitle className="text-xs text-muted-foreground">Avg weight (30d)</CardTitle></CardHeader>
          <CardContent>
            {loading ? <Skeleton className="h-6 w-20" /> : weightLast30.length > 0
              ? <p className="text-lg font-bold">{avgWeight30.toFixed(1)} <span className="text-sm font-normal text-muted-foreground">kg</span></p>
              : <p className="text-sm text-muted-foreground">—</p>
            }
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-1"><CardTitle className="text-xs text-muted-foreground">Total entries</CardTitle></CardHeader>
          <CardContent>
            {loading ? <Skeleton className="h-6 w-12" /> : <p className="text-lg font-bold">{metrics.length}</p>}
          </CardContent>
        </Card>
      </div>

      {/* Latest per type */}
      {!loading && latestPerType.size > 0 && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Latest readings</CardTitle></CardHeader>
          <CardContent className="space-y-2">
            {Array.from(latestPerType.entries()).map(([type, m]) => (
              <div key={type} className="flex items-center justify-between text-sm border-b last:border-0 py-1">
                <span className="font-medium">{METRIC_TYPES.find((t) => t.value === type)?.label ?? type}</span>
                <div className="flex items-center gap-2">
                  <span>{m.value} {m.unit}</span>
                  <span className="text-muted-foreground text-xs">{formatDateTime(m.recordedAt)}</span>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* Log metric form */}
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Log a metric</CardTitle></CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="space-y-1">
                <Label className="text-xs">Type</Label>
                <select
                  value={formType}
                  onChange={(e) => handleTypeChange(e.target.value)}
                  className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                >
                  {METRIC_TYPES.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Date</Label>
                <Input type="date" value={formDate} onChange={(e) => setFormDate(e.target.value)} className="h-9 text-sm" />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Value</Label>
                <Input
                  type="number"
                  step="any"
                  placeholder="0"
                  value={formValue}
                  onChange={(e) => setFormValue(e.target.value)}
                  className="h-9 text-sm"
                />
              </div>
              <div className="space-y-1">
                <Label className="text-xs">Unit</Label>
                <Input
                  value={formUnit}
                  onChange={(e) => setFormUnit(e.target.value)}
                  placeholder="kg"
                  className="h-9 text-sm"
                />
              </div>
            </div>
            {formError && <p className="text-xs text-destructive">{formError}</p>}
            <Button type="submit" size="sm" disabled={submitting}>{submitting ? "Saving…" : "Save"}</Button>
          </form>
        </CardContent>
      </Card>

      {/* Weight history */}
      {weightEntries.length > 0 && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Weight history</CardTitle></CardHeader>
          <CardContent className="space-y-1">
            {weightEntries.slice(0, 10).map((m) => (
              <div key={m.id} className="flex items-center justify-between text-sm border-b last:border-0 py-1.5">
                <span className="text-muted-foreground">{formatDateTime(m.recordedAt)}</span>
                <div className="flex items-center gap-3">
                  <span className="font-medium">{m.value} {m.unit}</span>
                  <button
                    onClick={() => m.id !== undefined && deleteMetric(m.id)}
                    className="text-muted-foreground hover:text-destructive text-xs"
                  >×</button>
                </div>
              </div>
            ))}
          </CardContent>
        </Card>
      )}

      {/* All other metric types history */}
      {!loading && metrics.filter((m) => m.type !== "WEIGHT").length > 0 && (
        <Card>
          <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Other metrics</CardTitle></CardHeader>
          <CardContent className="space-y-1">
            {metrics
              .filter((m) => m.type !== "WEIGHT")
              .sort((a, b) => new Date(b.recordedAt ?? b.updatedAt ?? 0).getTime() - new Date(a.recordedAt ?? a.updatedAt ?? 0).getTime())
              .slice(0, 20)
              .map((m) => (
                <div key={m.id} className="flex items-center justify-between text-sm border-b last:border-0 py-1.5">
                  <div className="flex items-center gap-2">
                    <Badge variant="outline" className="text-xs">{METRIC_TYPES.find((t) => t.value === m.type)?.label ?? m.type}</Badge>
                    <span className="font-medium">{m.value} {m.unit}</span>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="text-muted-foreground text-xs">{formatDateTime(m.recordedAt)}</span>
                    <button
                      onClick={() => m.id !== undefined && deleteMetric(m.id)}
                      className="text-muted-foreground hover:text-destructive text-xs"
                    >×</button>
                  </div>
                </div>
              ))
            }
          </CardContent>
        </Card>
      )}
    </div>
  );
}
