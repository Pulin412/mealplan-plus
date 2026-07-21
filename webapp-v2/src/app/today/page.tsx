"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { BottomSheet } from "@/components/ui/BottomSheet";
import { useToday } from "@/hooks/useToday";
import type { DashboardDto, SlotStatusDto } from "@/lib/api/dashboard";
import { MEAL_SLOTS, unitLabel, defaultQtyFor, foodMacros, num, type FoodDto, type FoodUnit } from "@/lib/nutrition";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", danger: "#b23b3b",
  cardDark: "#14181b", cardText: "#edf1f2", cardMuted: "#8a949b",
  protein: "oklch(0.60 0.10 200)", carbs: "oklch(0.60 0.11 255)", fat: "oklch(0.62 0.11 150)",
  streak: "oklch(0.7 0.18 45)", over: "#d98a4a",
};
const mono = "'DM Mono', monospace";
const r = (n: number) => Math.round(n);

// ── Calorie ring (SVG) ──
function Ring({ consumed, target, over }: { consumed: number; target: number; over: boolean }) {
  const R = 42.5, CIRC = 2 * Math.PI * R;
  const frac = target <= 0 ? 0 : Math.min(1, consumed / target);
  const big = over ? -( target - consumed) : target - consumed;
  return (
    <div style={{ position: "relative", width: 96, height: 96, flex: "none" }}>
      <svg width="96" height="96" viewBox="0 0 96 96">
        <circle cx="48" cy="48" r={R} fill="none" stroke="#2a3136" strokeWidth="11" />
        <circle cx="48" cy="48" r={R} fill="none" stroke={over ? C.over : C.teal} strokeWidth="11" strokeLinecap="round"
          strokeDasharray={CIRC} strokeDashoffset={CIRC * (1 - frac)} transform="rotate(-90 48 48)" />
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
        <span style={{ font: `700 26px ${mono}`, color: C.cardText, lineHeight: 1 }}>{r(big)}</span>
        <span style={{ font: "400 10px system-ui", color: C.cardMuted }}>{over ? "kcal over" : "kcal left"}</span>
      </div>
    </div>
  );
}

function MacroBar({ label, consumed, target, color }: { label: string; consumed: number; target: number | null | undefined; color: string }) {
  const frac = !target || target <= 0 ? 0 : Math.min(1, consumed / target);
  return (
    <div>
      <div style={{ display: "flex", alignItems: "center" }}>
        <span style={{ width: 52, font: "400 10px system-ui", color: C.cardMuted }}>{label}</span>
        <span style={{ font: `400 10px ${mono}`, color: C.cardText }}>{r(consumed)}{target ? ` / ${target}g` : "g"}</span>
      </div>
      <div style={{ height: 4, borderRadius: 2, background: "#2a3136", marginTop: 3, overflow: "hidden" }}>
        <div style={{ height: 4, borderRadius: 2, background: color, width: `${frac * 100}%` }} />
      </div>
    </div>
  );
}

function CalorieCard({ d }: { d: DashboardDto }) {
  const ring = d.calorieRing, over = ring.isOver;
  return (
    <div style={{ background: C.cardDark, borderRadius: 18, padding: 18, display: "flex", alignItems: "center", gap: 20 }}>
      <Ring consumed={ring.consumed} target={ring.target} over={over} />
      <div style={{ flex: 1 }}>
        <div style={{ font: "400 12px system-ui", color: C.cardMuted, marginBottom: 10 }}>{r(ring.consumed)} of {ring.target} kcal</div>
        <div style={{ display: "flex", flexDirection: "column", gap: 7 }}>
          <MacroBar label="Protein" consumed={d.macros.consumedProtein} target={d.macros.targetProtein} color={C.protein} />
          <MacroBar label="Carbs" consumed={d.macros.consumedCarbs} target={d.macros.targetCarbs} color={C.carbs} />
          <MacroBar label="Fat" consumed={d.macros.consumedFat} target={d.macros.targetFat} color={C.fat} />
        </div>
      </div>
    </div>
  );
}

function SlotBadge({ slot }: { slot: string }) {
  return <span style={{ font: "600 8.5px system-ui", color: C.teal, background: "oklch(0.62 0.09 210 / .12)", borderRadius: 5, padding: "2px 6px" }}>{slot.toUpperCase()}</span>;
}

function SlotRow({ slot, busy, expanded, onToggle, onExpand, isLast }: {
  slot: SlotStatusDto; busy: boolean; expanded: boolean; onToggle: () => void; onExpand: () => void; isLast: boolean;
}) {
  return (
    <div style={{ borderBottom: isLast ? "none" : `1px solid ${C.bgAlt}` }}>
      <div onClick={onExpand} style={{ cursor: "pointer", display: "flex", alignItems: "center", padding: "11px 12px", gap: 9 }}>
        <button onClick={(e) => { e.stopPropagation(); if (!busy) onToggle(); }}
          style={{ flex: "none", width: 22, height: 22, borderRadius: "50%", border: `1.5px solid ${slot.isLogged ? C.teal : "#dfe6e8"}`,
            background: slot.isLogged ? C.teal : "transparent", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 12 }}>
          {slot.isLogged ? "✓" : ""}
        </button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ font: "600 12.5px system-ui", color: C.ink }}>{slot.slot}</div>
          <div style={{ font: "400 10.5px system-ui", color: C.muted2 }}>{slot.mealName ?? "—"}</div>
        </div>
        <span style={{ font: `700 12.5px ${mono}`, color: C.ink }}>{r(slot.kcal)}<span style={{ font: "400 9px system-ui", color: C.muted2 }}> kcal</span></span>
      </div>
      {expanded && (
        <div style={{ padding: "0 12px 12px 12px" }}>
          {slot.items.map((it, i) => <ItemTick key={i} name={it.foodName} meta={`${num(it.quantity)} ${unitLabel(it.unit)}`} />)}
          <div style={{ font: `400 10px ${mono}`, color: C.muted2, marginTop: 6 }}>P{r(slot.protein)} · C{r(slot.carbs)} · F{r(slot.fat)}</div>
        </div>
      )}
    </div>
  );
}

function ItemTick({ name, meta }: { name: string; meta: string }) {
  const [on, setOn] = useState(false);
  return (
    <div onClick={() => setOn((v) => !v)} style={{ cursor: "pointer", display: "flex", alignItems: "center", gap: 9, padding: "6px 0" }}>
      <span style={{ flex: "none", width: 18, height: 18, borderRadius: 5, border: `1.5px solid ${on ? C.teal : "#dfe6e8"}`,
        background: on ? C.teal : "transparent", color: "#fff", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 11 }}>{on ? "✓" : ""}</span>
      <span style={{ flex: 1, font: "400 11.5px system-ui", color: C.muted3 }}>{name}</span>
      <span style={{ font: `400 10px ${mono}`, color: C.muted2 }}>{meta}</span>
    </div>
  );
}

function StreakCard({ d }: { d: DashboardDto }) {
  const s = d.streak;
  const iso = Array.isArray(d.date) ? (d.date as number[]) : null;
  const base = iso ? new Date(iso[0], iso[1] - 1, iso[2]) : new Date();
  const initials = ["S", "M", "T", "W", "T", "F", "S"];
  return (
    <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, padding: 14 }}>
      <div style={{ display: "flex", alignItems: "center", gap: 6 }}>
        <span style={{ fontSize: 16 }}>🔥</span>
        <span style={{ font: `700 18px ${mono}`, color: C.ink }}>{s.current}</span>
        <span style={{ font: "400 10.5px system-ui", color: C.muted2, marginLeft: 4 }}>Day streak · best {s.best}</span>
      </div>
      <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12 }}>
        {s.dots.map((on, i) => {
          const day = new Date(base); day.setDate(base.getDate() - (6 - i));
          const isToday = i === s.dots.length - 1;
          return (
            <div key={i} style={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
              <div style={{ width: 26, height: 26, borderRadius: "50%", background: on ? C.teal : C.bgAlt,
                border: isToday ? `1.5px solid ${C.teal}` : "none", display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 12 }}>
                {on ? "✓" : ""}
              </div>
              <span style={{ font: "400 9px system-ui", color: isToday ? C.teal : C.muted2, marginTop: 4 }}>{initials[day.getDay()]}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function AddToTodaySheet({ open, foods, plannedSlots, onAdd, onClose }: {
  open: boolean; foods: FoodDto[]; plannedSlots: string[];
  onAdd: (foodId: number, slot: string, qty: number, unit: FoodUnit) => void; onClose: () => void;
}) {
  const [slot, setSlot] = useState(plannedSlots[0] ?? MEAL_SLOTS[1]);
  const [query, setQuery] = useState("");
  const list = foods.filter((f) => !query || f.name.toLowerCase().includes(query.toLowerCase()));
  return (
    <BottomSheet open={open} onClose={onClose} title="Add to today">
      <div style={{ font: "400 11px system-ui", color: C.muted2, marginTop: -8, marginBottom: 10 }}>Log an unplanned food into a slot</div>
      <div style={{ font: "600 11px system-ui", color: C.muted3, marginBottom: 6 }}>Slot</div>
      <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginBottom: 12 }}>
        {MEAL_SLOTS.map((s) => (
          <button key={s} onClick={() => setSlot(s)} style={{ font: "600 11px system-ui", borderRadius: 20, padding: "6px 11px",
            color: s === slot ? "#fff" : C.muted3, background: s === slot ? C.teal : "transparent", border: `1.5px solid ${s === slot ? C.teal : "#dfe6e8"}` }}>{s}</button>
        ))}
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 8, background: C.bgAlt, borderRadius: 11, padding: "10px 12px", marginBottom: 8 }}>
        <span style={{ color: C.muted2 }}>⌕</span>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search your foods…"
          style={{ flex: 1, border: "none", background: "transparent", outline: "none", fontSize: 13, color: C.ink }} />
      </div>
      <div style={{ maxHeight: 260, overflowY: "auto" }}>
        {list.length === 0 && <p style={{ textAlign: "center", font: "400 12px system-ui", color: C.muted2, padding: 12 }}>No foods.</p>}
        {list.map((f) => (
          <div key={f.id} style={{ display: "flex", alignItems: "center", gap: 8, padding: "10px 0", borderBottom: `1px solid ${C.bgAlt}` }}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ font: "600 12.5px system-ui", color: C.ink }}>{f.name}</div>
              <div style={{ font: "400 10.5px system-ui", color: C.muted }}>{f.caloriesPer100} kcal / 100g</div>
            </div>
            <button onClick={() => { const u = f.unit ?? "GRAM"; if (f.id != null) { onAdd(f.id, slot, defaultQtyFor(u), u); onClose(); } }}
              style={{ font: "600 11.5px system-ui", color: C.teal, border: "1.5px solid #dfe6e8", borderRadius: 9, padding: "7px 12px" }}>+ Add</button>
          </div>
        ))}
      </div>
    </BottomSheet>
  );
}

function DietSheet({ open, d, onClose }: { open: boolean; d: DashboardDto; onClose: () => void }) {
  return (
    <BottomSheet open={open} onClose={onClose} title={d.dietName ?? "Diet"}>
      <div style={{ font: "400 11px system-ui", color: C.muted2, marginTop: -8, marginBottom: 12 }}>{d.calorieRing.target} kcal target · {d.slots.length} slots</div>
      {d.slots.map((slot, i) => (
        <div key={i} style={{ paddingTop: 8, borderTop: i > 0 ? `1px solid ${C.bgAlt}` : "none", marginTop: i > 0 ? 8 : 0 }}>
          <div style={{ display: "flex", alignItems: "center" }}>
            <SlotBadge slot={slot.slot} /><span style={{ flex: 1 }} />
            <span style={{ font: `400 10px ${mono}`, color: C.muted2 }}>{r(slot.kcal)} kcal</span>
          </div>
          <div style={{ font: "600 12.5px system-ui", color: C.ink, marginTop: 2 }}>{slot.mealName ?? "—"}</div>
          {slot.items.map((it, j) => (
            <div key={j} style={{ display: "flex", justifyContent: "space-between", padding: "2px 0" }}>
              <span style={{ font: "400 11px system-ui", color: C.muted3 }}>{it.foodName}</span>
              <span style={{ font: `400 9.5px ${mono}`, color: C.muted2 }}>{num(it.quantity)} {unitLabel(it.unit)}</span>
            </div>
          ))}
        </div>
      ))}
    </BottomSheet>
  );
}

function TodayInner() {
  const t = useToday();
  const router = useRouter();
  const [addOpen, setAddOpen] = useState(false);
  const [dietOpen, setDietOpen] = useState(false);
  const d = t.dashboard;

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      {/* app bar */}
      <div style={{ display: "flex", flexDirection: "column", padding: "8px 12px 6px 6px" }}>
        <div style={{ display: "flex", alignItems: "center" }}>
          <button style={{ fontSize: 22, color: C.ink, padding: "0 8px" }}>☰</button>
          <span style={{ flex: 1 }} />
          <button style={{ fontSize: 18, color: C.muted3, padding: "0 8px" }}>🔔</button>
          <div onClick={() => router.push("/profile")} style={{ cursor: "pointer", width: 34, height: 34, borderRadius: "50%", background: C.teal, display: "flex", alignItems: "center", justifyContent: "center", color: "#fff" }}>●</div>
        </div>
        <div style={{ display: "flex", alignItems: "baseline", paddingLeft: 10, marginTop: 2 }}>
          <span style={{ font: "700 19px system-ui", color: C.ink }}>
            {d ? new Date(isoOf(d.date)).toLocaleDateString(undefined, { weekday: "long", day: "numeric", month: "short" }) : ""}
          </span>
          {d?.dietName && <span onClick={() => setDietOpen(true)} style={{ cursor: "pointer", font: "600 12px system-ui", color: C.teal, marginLeft: 8 }}>· {d.dietName}</span>}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: "4px 16px", paddingBottom: 120 }}>
        {t.loading && <div style={{ textAlign: "center", padding: 48, font: "400 12px system-ui", color: C.muted }}>Loading today…</div>}
        {t.error && <div style={{ textAlign: "center", padding: 48, font: "400 12px system-ui", color: C.danger }}>{t.error}</div>}
        {d && (
          <>
            <CalorieCard d={d} />
            <div style={{ font: "600 12.5px system-ui", color: C.ink, margin: "16px 0 8px 2px" }}>Today&apos;s meals</div>
            {d.slots.length === 0 ? (
              <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, padding: "22px", textAlign: "center", font: "400 12px system-ui", color: C.muted2 }}>No diet planned for today.</div>
            ) : (
              <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, overflow: "hidden" }}>
                {d.slots.map((slot, i) => (
                  <SlotRow key={slot.slot} slot={slot} busy={t.busySlot === slot.slot} expanded={t.expanded.has(slot.slot)}
                    onToggle={() => t.toggleSlot(slot.slot)} onExpand={() => t.toggleExpand(slot.slot)} isLast={i === d.slots.length - 1} />
                ))}
              </div>
            )}

            {d.additionalFoods.length > 0 && (
              <>
                <div style={{ font: "600 12.5px system-ui", color: C.ink, margin: "16px 0 8px 2px" }}>Added today</div>
                <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, overflow: "hidden" }}>
                  {d.additionalFoods.map((lf, i) => {
                    const food = lf.foodId != null ? t.foodsById.get(lf.foodId) : undefined;
                    const unit = (lf.unit ?? "GRAM") as FoodUnit;
                    const kcal = r(foodMacros(food, lf.quantity, unit).kcal);
                    return (
                      <div key={lf.id} style={{ display: "flex", alignItems: "center", gap: 8, padding: "10px 12px", borderBottom: i < d.additionalFoods.length - 1 ? `1px solid ${C.bgAlt}` : "none" }}>
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ font: "600 12.5px system-ui", color: C.ink }}>{food?.name ?? "Food"}</div>
                          <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 2 }}>
                            <SlotBadge slot={lf.mealSlot} />
                            <span style={{ font: `400 10px ${mono}`, color: C.muted2 }}>{num(lf.quantity)} {unitLabel(unit)}</span>
                          </div>
                        </div>
                        <span style={{ font: `700 12.5px ${mono}`, color: C.ink }}>{kcal}<span style={{ font: "400 9px system-ui", color: C.muted2 }}> kcal</span></span>
                        <button onClick={() => t.removeFood(lf.id)} style={{ fontSize: 13, color: C.muted2, marginLeft: 6 }}>✕</button>
                      </div>
                    );
                  })}
                </div>
              </>
            )}

            <div style={{ marginTop: 16 }}><StreakCard d={d} /></div>
          </>
        )}
      </div>

      {d && (
        <button onClick={() => setAddOpen(true)} className="fixed bottom-[68px] right-4 z-40 w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg"
          style={{ background: C.teal, boxShadow: "0 6px 18px oklch(0.62 0.09 210 / .45)" }}>+</button>
      )}
      {d && <AddToTodaySheet open={addOpen} foods={t.foods} plannedSlots={d.slots.map((s) => s.slot)} onAdd={t.addFood} onClose={() => setAddOpen(false)} />}
      {d && <DietSheet open={dietOpen} d={d} onClose={() => setDietOpen(false)} />}
      <NutritionNav />
    </div>
  );
}

function isoOf(date: unknown): string {
  if (Array.isArray(date)) { const [y, m, day] = date as number[]; return `${y}-${String(m).padStart(2, "0")}-${String(day).padStart(2, "0")}`; }
  return String(date);
}

export default function TodayPage() {
  return <AuthGuard><TodayInner /></AuthGuard>;
}
