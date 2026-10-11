"use client";

import { useState, useEffect, useCallback, useMemo } from "react";
import { friendlyMessage } from "@/lib/api/errors";
import { listHealthMetrics, createHealthMetric, type HealthMetricDto } from "@/lib/api/health";

export type HealthTabId = "GLUCOSE" | "WEIGHT" | "BLOOD_PRESSURE";
export type RangeId = "7D" | "30D" | "90D";

export interface HealthTabMeta {
  id: HealthTabId;
  label: string;
  unit: string;
  metricLabel: string;
}

export const HEALTH_TABS: HealthTabMeta[] = [
  { id: "GLUCOSE", label: "Glucose", unit: "mg/dL", metricLabel: "Blood glucose" },
  { id: "WEIGHT", label: "Weight", unit: "kg", metricLabel: "Body weight" },
  { id: "BLOOD_PRESSURE", label: "BP", unit: "mmHg", metricLabel: "Blood pressure" },
];

export const RANGES: { id: RangeId; label: string; days: number }[] = [
  { id: "7D", label: "7D", days: 7 },
  { id: "30D", label: "30D", days: 30 },
  { id: "90D", label: "90D", days: 90 },
];

/** One combined log form — enter any of the metrics; each filled value saves to its own type. */
export interface HealthLogForm {
  glucose: string;
  weight: string;
  systolic: string;
  diastolic: string;
}
const EMPTY_FORM: HealthLogForm = { glucose: "", weight: "", systolic: "", diastolic: "" };

export function useHealth() {
  const [tab, setTabId] = useState<HealthTabId>("GLUCOSE");
  const [range, setRange] = useState<RangeId>("7D");
  const [readings, setReadings] = useState<Record<HealthTabId, HealthMetricDto[]>>({
    GLUCOSE: [], WEIGHT: [], BLOOD_PRESSURE: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [logOpen, setLogOpen] = useState(false);
  const [form, setForm] = useState<HealthLogForm>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [glu, wgt, bp] = await Promise.all(
        HEALTH_TABS.map((t) => listHealthMetrics(t.id)),
      );
      // `recordedAt` is an ISO-8601 string (per the spec) — sort by time.
      const t = (m: HealthMetricDto) => new Date(m.recordedAt).getTime();
      const sort = (xs: HealthMetricDto[]) => [...xs].sort((a, b) => t(a) - t(b));
      setReadings({ GLUCOSE: sort(glu), WEIGHT: sort(wgt), BLOOD_PRESSURE: sort(bp) });
    } catch (e) {
      setError(friendlyMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { void load(); }, [load]);

  const meta = useMemo(() => HEALTH_TABS.find((t) => t.id === tab)!, [tab]);
  const current = readings[tab];

  // ── Combined log form ───────────────────────────────────────────────────────
  const openLog = useCallback(() => { setForm(EMPTY_FORM); setError(null); setLogOpen(true); }, []);
  const closeLog = useCallback(() => setLogOpen(false), []);
  // Glucose (mg/dL) + BP (mmHg) are whole numbers; weight (kg) allows a decimal.
  const setField = useCallback((k: keyof HealthLogForm, v: string) =>
    setForm((f) => ({ ...f, [k]: cleanNum(v, k === "weight") })), []);

  const parsed = useMemo(() => {
    const num = (s: string) => (s.trim() === "" ? null : parseFloat(s));
    const g = num(form.glucose), w = num(form.weight), s = num(form.systolic), d = num(form.diastolic);
    const bpTouched = form.systolic.trim() !== "" || form.diastolic.trim() !== "";
    const bpValid = s != null && !Number.isNaN(s) && d != null && !Number.isNaN(d);
    return {
      glucose: g != null && !Number.isNaN(g) ? g : null,
      weight: w != null && !Number.isNaN(w) ? w : null,
      bp: bpValid ? { s: s!, d: d! } : null,
      bpTouched, bpValid,
    };
  }, [form]);

  // Savable when at least one metric is valid, and BP isn't half-filled.
  const canSaveLog = useMemo(() => {
    if (parsed.bpTouched && !parsed.bpValid) return false;
    return parsed.glucose != null || parsed.weight != null || parsed.bp != null;
  }, [parsed]);

  const saveLog = useCallback(async () => {
    if (!canSaveLog || saving) return;
    setSaving(true);
    try {
      const tasks: Promise<unknown>[] = [];
      if (parsed.glucose != null) tasks.push(createHealthMetric("GLUCOSE", parsed.glucose, "mg/dL"));
      if (parsed.weight != null) tasks.push(createHealthMetric("WEIGHT", parsed.weight, "kg"));
      if (parsed.bp != null) tasks.push(createHealthMetric("BLOOD_PRESSURE", parsed.bp.s, "mmHg", parsed.bp.d));
      await Promise.all(tasks);
      setLogOpen(false);
      await load();
    } catch (e) {
      setError(friendlyMessage(e));
    } finally {
      setSaving(false);
    }
  }, [canSaveLog, saving, parsed, load]);

  return {
    tab, meta, setTabId, range, setRange,
    readings, current, loading, error,
    logOpen, form, openLog, closeLog, setField, canSaveLog, saveLog, saving,
    bpIncomplete: parsed.bpTouched && !parsed.bpValid,
  };
}

/** Keep digits, and a single decimal point when allowed. */
function cleanNum(raw: string, allowDecimal: boolean): string {
  const cleaned = raw.replace(allowDecimal ? /[^0-9.]/g : /[^0-9]/g, "");
  if (!allowDecimal) return cleaned;
  const i = cleaned.indexOf(".");
  return i < 0 ? cleaned : cleaned.slice(0, i + 1) + cleaned.slice(i + 1).replace(/\./g, "");
}
