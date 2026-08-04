"use client";

import { useState, useMemo, useRef } from "react";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { BottomSheet } from "@/components/ui/BottomSheet";
import { useHealth, HEALTH_TABS, RANGES, type HealthTabMeta, type RangeId } from "@/hooks/useHealth";
import type { HealthMetricDto } from "@/lib/api/health";

// ─── token shortcuts ─────────────────────────────────────────────────────────
const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", faint: "#a2abb1",
  border: "#eaeef0", borderCool: "#dfe6e8", borderSoft: "#eef1f3", surface: "#ffffff",
  bg: "#f7f9fa", bgAlt: "#f2f4f5", danger: "#b23b3b",
  teal: "oklch(0.62 0.09 210)", green: "oklch(0.66 0.13 150)", flame: "oklch(0.7 0.18 45)",
};
const DIASTOLIC = "#c7a4dd";
const mono = "'DM Mono', monospace";

// ─── helpers ──────────────────────────────────────────────────────────────────
// `recordedAt` is an ISO-8601 string (per the spec) — normalise to a local YYYY-MM-DD.
const dateOf = (m: HealthMetricDto) => {
  const d = new Date(m.recordedAt);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
};
const fmtLabel = (iso: string) => {
  const [y, mo, d] = iso.split("-").map(Number);
  return new Date(y, mo - 1, d).toLocaleDateString(undefined, { day: "numeric", month: "short" });
};
const fmtNum = (v: number) => (v % 1 === 0 ? String(Math.round(v)) : v.toFixed(1));
const valueText = (dual: boolean, m: HealthMetricDto) =>
  dual ? `${fmtNum(m.value)}/${fmtNum(m.secondaryValue ?? 0)}` : fmtNum(m.value);

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}
function minusDaysIso(iso: string, days: number) {
  const [y, m, d] = iso.split("-").map(Number);
  const t = new Date(y, m - 1, d - days);
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, "0")}-${String(t.getDate()).padStart(2, "0")}`;
}
function weekStartIso(iso: string) {
  const [y, m, d] = iso.split("-").map(Number);
  const t = new Date(y, m - 1, d);
  const dow = (t.getDay() + 6) % 7; // Mon=0
  t.setDate(t.getDate() - dow);
  return `${t.getFullYear()}-${String(t.getMonth() + 1).padStart(2, "0")}-${String(t.getDate()).padStart(2, "0")}`;
}

interface ChartPoint { label: string; value: number; secondary: number | null }

/** Range-aware aggregation: 7D raw · 30D daily avg · 90D weekly avg (Apple-style). */
function aggregate(window: HealthMetricDto[], range: RangeId): ChartPoint[] {
  if (window.length === 0) return [];
  if (range === "7D") return window.map((m) => ({ label: fmtLabel(dateOf(m)), value: m.value, secondary: m.secondaryValue ?? null }));
  const keyOf = range === "30D" ? dateOf : (m: HealthMetricDto) => weekStartIso(dateOf(m));
  const groups = new Map<string, HealthMetricDto[]>();
  for (const m of window) {
    const k = keyOf(m);
    (groups.get(k) ?? groups.set(k, []).get(k)!).push(m);
  }
  return Array.from(groups.keys()).sort().map((k) => {
    const rs = groups.get(k)!;
    const sec = rs.filter((r) => r.secondaryValue != null);
    return {
      label: fmtLabel(k),
      value: rs.reduce((s, r) => s + r.value, 0) / rs.length,
      secondary: sec.length ? sec.reduce((s, r) => s + (r.secondaryValue ?? 0), 0) / sec.length : null,
    };
  });
}

function currentStreak(days: Set<string>, today: string): number {
  let iso = today; let c = 0;
  while (days.has(iso)) { c++; iso = minusDaysIso(iso, 1); }
  return c;
}
function bestStreak(days: Set<string>): number {
  const sorted = Array.from(days).sort();
  if (sorted.length === 0) return 0;
  let best = 1; let run = 1;
  for (let i = 1; i < sorted.length; i++) {
    run = minusDaysIso(sorted[i], 1) === sorted[i - 1] ? run + 1 : 1;
    if (run > best) best = run;
  }
  return best;
}

// ─── Metric tabs ────────────────────────────────────────────────────────────────
function MetricTabs({ tab, onSelect }: { tab: HealthTabMeta["id"]; onSelect: (id: HealthTabMeta["id"]) => void }) {
  return (
    <div className="flex gap-1 p-1 mx-4 rounded-[11px]" style={{ background: C.borderSoft }}>
      {HEALTH_TABS.map((t) => {
        const on = t.id === tab;
        return (
          <button key={t.id} onClick={() => onSelect(t.id)}
            className="flex-1 rounded-[8px] py-[9px] text-[12px] font-semibold"
            style={{ background: on ? C.surface : "transparent", color: on ? C.ink : C.muted, boxShadow: on ? "0 1px 3px rgba(20,24,27,.10)" : "none" }}>
            {t.label}
          </button>
        );
      })}
    </div>
  );
}

// ─── Trend chart (binned + tap marker) ──────────────────────────────────────────
function TrendChart({ meta, window, range }: { meta: HealthTabMeta; window: HealthMetricDto[]; range: RangeId }) {
  const dual = meta.id === "BLOOD_PRESSURE";
  const lineColor = meta.id === "WEIGHT" ? C.green : C.teal;
  const points = useMemo(() => aggregate(window, range), [window, range]);
  const showDots = range !== "90D" && points.length <= 20;
  const [sel, setSel] = useState<number | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  if (points.length === 0) {
    return (
      <div className="rounded-[14px] mb-4 flex items-center justify-center" style={{ background: C.surface, border: `1px solid ${C.border}`, height: 128 }}>
        <span className="text-[12px]" style={{ color: C.muted2 }}>No readings in this range</span>
      </div>
    );
  }

  const primary = points.map((p) => p.value);
  const secondary = dual ? points.map((p) => p.secondary ?? p.value) : [];
  const allV = [...primary, ...secondary];
  const minV = Math.min(...allV); const maxV = Math.max(...allV);
  const span = maxV - minV > 0 ? maxV - minV : 1;
  const n = points.length;
  const X = (i: number) => (n === 1 ? 140 : 14 + (252 * i) / (n - 1));
  const Y = (v: number) => 14 + (1 - (v - minV) / span) * 76;
  const line = (vals: number[]) => vals.map((v, i) => `${X(i)},${Y(v)}`).join(" ");

  const onClick = (e: React.MouseEvent<SVGSVGElement>) => {
    const rect = svgRef.current!.getBoundingClientRect();
    const frac = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width));
    const idx = n === 1 ? 0 : Math.round(frac * (n - 1));
    setSel((s) => (s === idx ? null : idx));
  };

  const selPt = sel != null ? points[sel] : null;
  const selX = sel != null ? X(sel) : 0;

  return (
    <div className="rounded-[14px] mb-4 relative" style={{ background: C.surface, border: `1px solid ${C.border}`, padding: "12px 10px 8px" }}>
      <svg ref={svgRef} onClick={onClick} width="100%" viewBox="0 0 280 104" preserveAspectRatio="none"
        style={{ display: "block", overflow: "visible", cursor: "pointer" }}>
        {[14, 52, 90].map((y) => <line key={y} x1={14} y1={y} x2={266} y2={y} stroke="#f0f2f3" strokeWidth={1} />)}
        {dual && <polyline points={line(secondary)} fill="none" stroke={DIASTOLIC} strokeWidth={2} strokeLinejoin="round" strokeLinecap="round" />}
        <polyline points={line(primary)} fill="none" stroke={lineColor} strokeWidth={2.5} strokeLinejoin="round" strokeLinecap="round" />
        {showDots && primary.map((v, i) => <circle key={i} cx={X(i)} cy={Y(v)} r={2.4} fill={lineColor} />)}
        {selPt && (
          <>
            <line x1={selX} y1={8} x2={selX} y2={96} stroke={lineColor} strokeOpacity={0.35} strokeWidth={1.5} />
            <circle cx={selX} cy={Y(selPt.value)} r={4} fill={C.surface} stroke={lineColor} strokeWidth={2} />
            {dual && <circle cx={selX} cy={Y(selPt.secondary ?? selPt.value)} r={3.5} fill={C.surface} stroke={DIASTOLIC} strokeWidth={2} />}
          </>
        )}
      </svg>
      {selPt && (
        <div className="absolute -translate-x-1/2 px-2 py-1 rounded-[7px] text-[9px] font-semibold whitespace-nowrap pointer-events-none"
          style={{ left: `${(selX / 280) * 100}%`, top: 2, background: C.surface, border: `1px solid ${C.border}`, color: C.ink, boxShadow: "0 2px 6px rgba(20,24,27,.12)" }}>
          {selPt.label} · {dual ? `${fmtNum(selPt.value)}/${fmtNum(selPt.secondary ?? 0)}` : fmtNum(selPt.value)} {meta.unit}
        </div>
      )}
      <div className="flex items-center justify-between pt-1.5 text-[9.5px]" style={{ color: C.faint }}>
        <span>{points[0].label}</span>
        {dual && <span><span style={{ color: lineColor }}>● systolic</span>&nbsp;&nbsp;<span style={{ color: DIASTOLIC }}>● diastolic</span></span>}
        <span>{points[n - 1].label}</span>
      </div>
      {range === "90D" && <div className="text-[9px] pt-0.5" style={{ color: C.faint }}>Weekly average · tap a point for details</div>}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
function HealthPageInner() {
  const h = useHealth();
  const today = todayIso();
  const all = h.current;
  const window = useMemo(() => {
    const cutoff = minusDaysIso(today, RANGES.find((r) => r.id === h.range)!.days);
    return all.filter((m) => dateOf(m) >= cutoff);
  }, [all, h.range, today]);

  const latest = all.length ? all[all.length - 1] : null;
  const start = window.length ? window[0] : null;
  const dual = h.meta.id === "BLOOD_PRESSURE";

  // delta vs range start (green when lower / improving)
  let deltaLabel = "—"; let improving: boolean | null = null;
  if (latest) {
    if (!start || window.length < 2 || start === latest) deltaLabel = `First reading in ${h.range}`;
    else {
      const d = latest.value - start.value;
      const arrow = d < 0 ? "▼" : d > 0 ? "▲" : "•";
      deltaLabel = `${arrow} ${fmtNum(Math.abs(d))} ${h.meta.unit} vs ${h.range} start`;
      improving = d < 0;
    }
  }

  const days = useMemo(() => new Set(all.map(dateOf)), [all]);
  const streak = currentStreak(days, today);
  const best = bestStreak(days);

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      {/* App bar */}
      <div className="flex items-center px-4 pt-3 pb-2">
        <span className="flex-1 text-[21px] font-bold" style={{ color: C.ink }}>Health</span>
        <div className="w-[34px] h-[34px] rounded-full flex items-center justify-center text-white text-[16px]" style={{ background: C.teal }}>👤</div>
      </div>

      <MetricTabs tab={h.tab} onSelect={h.setTabId} />

      <div className="flex-1 overflow-y-auto px-4 pt-3.5" style={{ paddingBottom: 140 }}>
        {h.loading ? (
          <div className="text-center py-16 text-[13px]" style={{ color: C.muted2 }}>Loading…</div>
        ) : (
          <>
            {/* Latest + delta */}
            <div className="mb-3">
              <div className="text-[11px]" style={{ color: C.muted2 }}>{h.meta.metricLabel}</div>
              <div className="flex items-end gap-1.5 mt-0.5">
                <span className="text-[30px] font-bold leading-none" style={{ fontFamily: mono, color: C.ink }}>
                  {latest ? valueText(dual, latest) : "—"}
                </span>
                <span className="text-[12px] pb-0.5" style={{ color: C.muted2 }}>{h.meta.unit}</span>
              </div>
              <div className="text-[10.5px] font-semibold mt-1" style={{ color: improving === true ? C.green : C.muted2 }}>{deltaLabel}</div>
            </div>

            {/* Range toggle */}
            <div className="flex gap-1.5 mb-3">
              {RANGES.map((r) => {
                const on = r.id === h.range;
                return (
                  <button key={r.id} onClick={() => h.setRange(r.id)}
                    className="rounded-[8px] px-3.5 py-1.5 text-[11px] font-semibold"
                    style={{ background: on ? C.ink : C.bgAlt, color: on ? "#fff" : C.muted3 }}>
                    {r.label}
                  </button>
                );
              })}
            </div>

            <TrendChart meta={h.meta} window={window} range={h.range} />

            {/* Streak + count */}
            <div className="flex gap-2.5 mb-4">
              <div className="flex-1 rounded-[12px] p-3" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
                <div className="flex items-center gap-1.5">
                  <span className="text-[15px]" style={{ color: C.flame }}>🔥</span>
                  <span className="text-[18px] font-bold" style={{ fontFamily: mono, color: C.ink }}>{streak}</span>
                </div>
                <div className="text-[10.5px] mt-1.5" style={{ color: C.muted }}>Day streak · best {best}</div>
              </div>
              <div className="flex-1 rounded-[12px] p-3" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
                <div className="text-[18px] font-bold" style={{ fontFamily: mono, color: C.ink }}>{all.length}</div>
                <div className="text-[10.5px] mt-1.5" style={{ color: C.muted }}>Readings logged</div>
              </div>
            </div>

            {/* Recent readings */}
            <div className="text-[13px] font-semibold mb-2.5" style={{ color: C.ink }}>Recent readings</div>
            {all.length === 0 ? (
              <div className="text-[12px] pb-5" style={{ color: C.muted2 }}>—</div>
            ) : (
              [...all].reverse().slice(0, 8).map((m) => (
                <div key={m.id} className="flex items-center justify-between rounded-[11px] mb-[7px] px-3 py-[11px]"
                  style={{ background: C.surface, border: `1px solid ${C.border}` }}>
                  <span className="text-[12px] font-medium" style={{ color: C.muted3 }}>{fmtLabel(dateOf(m))}</span>
                  <span className="text-[13px] font-bold" style={{ fontFamily: mono, color: C.ink }}>
                    {valueText(dual, m)} <span className="text-[10px] font-normal" style={{ color: C.faint }}>{h.meta.unit}</span>
                  </span>
                </div>
              ))
            )}
          </>
        )}
      </div>

      {/* FAB → log sheet */}
      <button onClick={h.openLog}
        className="fixed bottom-[68px] right-5 z-30 w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg"
        style={{ background: C.teal, boxShadow: "0 6px 18px oklch(0.62 0.09 210 / .45)" }}>
        +
      </button>

      <BottomSheet open={!!h.log} onClose={h.closeLog} title={`Log ${h.log?.tab.metricLabel.toLowerCase() ?? ""}`}>
        {h.log && (
          <>
            {h.log.tab.id === "BLOOD_PRESSURE" ? (
              <div className="flex gap-2.5 mb-4">
                <LogField label="Systolic" value={h.log.value} onChange={h.setLogValue} placeholder="120" />
                <LogField label="Diastolic" value={h.log.secondary} onChange={h.setLogSecondary} placeholder="80" />
              </div>
            ) : (
              <div className="mb-4">
                <LogField label={`Reading · ${h.log.tab.unit}`} value={h.log.value} onChange={h.setLogValue} placeholder="Enter value" decimal />
              </div>
            )}
            {h.error && <div className="text-[12px] mb-2" style={{ color: C.danger }}>{h.error}</div>}
            <button onClick={h.saveLog} disabled={!h.canSaveLog}
              className="w-full rounded-[12px] py-3.5 text-[13px] font-semibold"
              style={{ background: h.canSaveLog ? C.teal : C.bgAlt, color: h.canSaveLog ? "#fff" : C.muted2 }}>
              Save reading
            </button>
          </>
        )}
      </BottomSheet>

      <NutritionNav />
    </div>
  );
}

function LogField({ label, value, onChange, placeholder, decimal = false }: {
  label: string; value: string; onChange: (v: string) => void; placeholder: string; decimal?: boolean;
}) {
  return (
    <div className="flex-1">
      <label className="block text-[11px] font-semibold mb-1.5" style={{ color: C.muted3 }}>{label}</label>
      <input value={value} onChange={(e) => onChange(e.target.value)} inputMode={decimal ? "decimal" : "numeric"} placeholder={placeholder}
        className="w-full box-border rounded-[12px] px-3.5 py-3 text-[15px] outline-none"
        style={{ border: `1.5px solid ${C.borderCool}`, color: C.ink, fontFamily: mono }} />
    </div>
  );
}

export default function HealthPage() {
  return (
    <AuthGuard>
      <HealthPageInner />
    </AuthGuard>
  );
}
