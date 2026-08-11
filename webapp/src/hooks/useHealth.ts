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

/** Open log form: two fields (BP) or one (glucose/weight). */
export interface HealthLogForm {
  tab: HealthTabMeta;
  value: string;
  secondary: string; // diastolic for BP
}

export function useHealth() {
  const [tab, setTabId] = useState<HealthTabId>("GLUCOSE");
  const [range, setRange] = useState<RangeId>("7D");
  const [readings, setReadings] = useState<Record<HealthTabId, HealthMetricDto[]>>({
    GLUCOSE: [], WEIGHT: [], BLOOD_PRESSURE: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [log, setLog] = useState<HealthLogForm | null>(null);

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

  // ── Log form ──────────────────────────────────────────────────────────────
  const openLog = useCallback(() => setLog({ tab: HEALTH_TABS.find((t) => t.id === tab)!, value: "", secondary: "" }), [tab]);
  const closeLog = useCallback(() => setLog(null), []);
  const setLogValue = useCallback((v: string) => setLog((l) => l && { ...l, value: cleanNum(v, l.tab.id !== "BLOOD_PRESSURE") }), []);
  const setLogSecondary = useCallback((v: string) => setLog((l) => l && { ...l, secondary: cleanNum(v, false) }), []);

  const canSaveLog = useMemo(() => {
    if (!log) return false;
    const v = parseFloat(log.value);
    if (Number.isNaN(v)) return false;
    if (log.tab.id === "BLOOD_PRESSURE") return !Number.isNaN(parseFloat(log.secondary));
    return true;
  }, [log]);

  const saveLog = useCallback(async () => {
    if (!log || !canSaveLog) return;
    const value = parseFloat(log.value);
    const secondary = log.tab.id === "BLOOD_PRESSURE" ? parseFloat(log.secondary) : null;
    try {
      await createHealthMetric(log.tab.id, value, log.tab.unit, secondary);
      setLog(null);
      await load();
    } catch (e) {
      setError(friendlyMessage(e));
    }
  }, [log, canSaveLog, load]);

  return {
    tab, meta, setTabId, range, setRange,
    readings, current, loading, error,
    log, openLog, closeLog, setLogValue, setLogSecondary, canSaveLog, saveLog,
  };
}

/** Keep digits, and a single decimal point when allowed. */
function cleanNum(raw: string, allowDecimal: boolean): string {
  const cleaned = raw.replace(allowDecimal ? /[^0-9.]/g : /[^0-9]/g, "");
  if (!allowDecimal) return cleaned;
  const i = cleaned.indexOf(".");
  return i < 0 ? cleaned : cleaned.slice(0, i + 1) + cleaned.slice(i + 1).replace(/\./g, "");
}
