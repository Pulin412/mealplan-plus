import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type HealthMetricDto = components["schemas"]["HealthMetricDto"];

/** All readings of one metric type (e.g. WEIGHT, GLUCOSE, BLOOD_PRESSURE). */
export function listHealthMetrics(type: string): Promise<HealthMetricDto[]> {
  return apiFetch<HealthMetricDto[]>(`/api/v1/health-metrics?type=${encodeURIComponent(type)}`);
}

/** Log a reading. `secondaryValue` carries diastolic for blood pressure. */
export function createHealthMetric(
  type: string,
  value: number,
  unit: string,
  secondaryValue: number | null = null,
  recordedAt: string = new Date().toISOString(),
): Promise<HealthMetricDto> {
  return apiFetch<HealthMetricDto>("/api/v1/health-metrics", {
    method: "POST",
    body: JSON.stringify({ type, value, unit, secondaryValue, recordedAt }),
  });
}

export function deleteHealthMetric(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/health-metrics/${id}`, { method: "DELETE" });
}
