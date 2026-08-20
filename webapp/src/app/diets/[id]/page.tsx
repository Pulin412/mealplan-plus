"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { getDiet, getDietUsage, type DietDto, type DietUsageDto } from "@/lib/api/diets";
import { listMeals, type MealDto } from "@/lib/api/meals";
import { listFoods } from "@/lib/api/foods";
import { foodMacros, foodExtras, sumExtras, num, unitLabel, MEAL_SLOTS, type FoodDto, type ExtraNutrients } from "@/lib/nutrition";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)",
};
const mono = "'DM Mono', monospace";

interface DietLine { name: string; meta: string; header: boolean }
interface SlotAgg { slot: string; kcal: number; p: number; c: number; f: number; extras: ExtraNutrients; lines: DietLine[] }
interface DietNutrition { slots: SlotAgg[]; totalKcal: number; totalP: number; totalC: number; totalF: number; totalExtras: ExtraNutrients }

/** Aggregate a diet's full nutrition + ingredient lines from its meals + direct food items. */
function aggregate(diet: DietDto, foodsById: Map<number, FoodDto>, mealsById: Map<number, MealDto>): DietNutrition {
  const bySlot = new Map<string, { kcal: number; p: number; c: number; f: number; extras: ExtraNutrients[]; lines: DietLine[] }>();
  const ensure = (slot: string) => {
    if (!bySlot.has(slot)) bySlot.set(slot, { kcal: 0, p: 0, c: 0, f: 0, extras: [], lines: [] });
    return bySlot.get(slot)!;
  };
  const addFood = (slot: string, food: FoodDto | undefined, quantity: number, unit: string, asLine = true) => {
    const m = foodMacros(food, quantity, unit);
    const acc = ensure(slot);
    acc.kcal += m.kcal; acc.p += m.protein; acc.c += m.carbs; acc.f += m.fat;
    acc.extras.push(foodExtras(food, quantity, unit));
    if (asLine) acc.lines.push({ name: food?.name ?? "Food", meta: `${num(quantity)} ${unitLabel(unit)}`, header: false });
  };

  (diet.meals ?? []).forEach((dm) => {
    const meal = dm.mealId != null ? mealsById.get(dm.mealId) : undefined;
    const mkcal = (meal?.items ?? []).reduce((s, it) => s + foodMacros(it.foodId != null ? foodsById.get(it.foodId) : undefined, it.quantity, it.unit).kcal, 0);
    ensure(dm.slot).lines.push({ name: meal?.name ?? "Meal", meta: `${Math.round(mkcal)} kcal`, header: true });
    (meal?.items ?? []).forEach((it) => addFood(dm.slot, it.foodId != null ? foodsById.get(it.foodId) : undefined, it.quantity, it.unit));
  });
  (diet.foodItems ?? []).forEach((fi) => addFood(fi.slot, fi.foodId != null ? foodsById.get(fi.foodId) : undefined, fi.quantity ?? 1, fi.unit));

  const present = Array.from(bySlot.keys());
  const order = [...MEAL_SLOTS.filter((s) => bySlot.has(s)), ...present.filter((s) => !MEAL_SLOTS.includes(s))];
  const slots: SlotAgg[] = order.map((slot) => {
    const a = bySlot.get(slot)!;
    return { slot, kcal: Math.round(a.kcal), p: a.p, c: a.c, f: a.f, extras: sumExtras(a.extras), lines: a.lines };
  });
  const allExtras = Array.from(bySlot.values()).flatMap((a) => a.extras);
  return {
    slots,
    totalKcal: Math.round(slots.reduce((s, x) => s + x.kcal, 0)),
    totalP: slots.reduce((s, x) => s + x.p, 0),
    totalC: slots.reduce((s, x) => s + x.c, 0),
    totalF: slots.reduce((s, x) => s + x.f, 0),
    totalExtras: sumExtras(allExtras),
  };
}

function Card({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="rounded-[14px] p-4 mb-3" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="text-[11px] font-bold uppercase tracking-wide mb-3" style={{ color: C.muted }}>{title}</div>
      {children}
    </div>
  );
}

/** One value tile — shows "—" for unknown (null) nutrients. */
function Stat({ label, value, unit }: { label: string; value: number | null; unit: string }) {
  return (
    <div className="rounded-[10px] px-3 py-2.5" style={{ background: C.bg }}>
      <div className="text-[10px] font-semibold uppercase tracking-wide" style={{ color: C.muted2 }}>{label}</div>
      <div className="text-[15px] font-bold mt-0.5" style={{ color: C.ink, fontFamily: mono }}>
        {value == null ? "—" : num(Math.round(value * 10) / 10)}
        {value != null && <span className="text-[10px] font-normal" style={{ color: C.muted2 }}> {unit}</span>}
      </div>
    </div>
  );
}

function fmtDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d).toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
}

function DietDetailInner() {
  const router = useRouter();
  const params = useParams();
  const id = Number(params.id);
  const [diet, setDiet] = useState<DietDto | null>(null);
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [meals, setMeals] = useState<MealDto[]>([]);
  const [usage, setUsage] = useState<DietUsageDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [usageWin, setUsageWin] = useState<7 | 30 | 90>(30);
  const [openSlot, setOpenSlot] = useState<string | null>(null);

  useEffect(() => {
    if (!Number.isFinite(id)) { setError("Invalid diet"); setLoading(false); return; }
    let cancelled = false;
    (async () => {
      try {
        const [d, fs, ms] = await Promise.all([getDiet(id), listFoods(), listMeals()]);
        if (cancelled) return;
        setDiet(d); setFoods(fs); setMeals(ms);
        getDietUsage(id).then((u) => !cancelled && setUsage(u)).catch(() => {});
      } catch {
        if (!cancelled) setError("Couldn't load this diet");
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [id]);

  const nutrition = useMemo(() => {
    if (!diet) return null;
    const foodsById = new Map<number, FoodDto>(); foods.forEach((f) => f.id != null && foodsById.set(f.id, f));
    const mealsById = new Map<number, MealDto>(); meals.forEach((m) => m.id != null && mealsById.set(m.id, m));
    return aggregate(diet, foodsById, mealsById);
  }, [diet, foods, meals]);

  return (
    <main className="min-h-screen" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 pt-3 pb-2">
        <button onClick={() => router.back()} className="text-[22px] w-9 h-9 flex items-center justify-center" style={{ color: C.ink }} aria-label="Back">‹</button>
        <span className="text-[17px] font-bold truncate" style={{ color: C.ink }}>{diet?.name ?? "Diet"}</span>
      </div>

      <div className="px-4 pb-24 max-w-[720px] mx-auto">
        {loading ? (
          <p className="text-center text-[13px] mt-10" style={{ color: C.muted2 }}>Loading…</p>
        ) : error || !diet || !nutrition ? (
          <p className="text-center text-[13px] mt-10" style={{ color: C.muted2 }}>{error ?? "Couldn't load this diet"}</p>
        ) : (
          <>
            {diet.description && <p className="text-[12.5px] mb-3 mt-1" style={{ color: C.muted3 }}>{diet.description}</p>}

            <Card title="Nutrition · whole diet">
              <div className="flex items-baseline gap-2 mb-3">
                <span className="text-[26px] font-bold" style={{ color: C.ink, fontFamily: mono }}>{nutrition.totalKcal}</span>
                <span className="text-[12px]" style={{ color: C.muted2 }}>kcal total</span>
              </div>
              <div className="grid grid-cols-3 gap-2">
                <Stat label="Protein" value={nutrition.totalP} unit="g" />
                <Stat label="Carbs" value={nutrition.totalC} unit="g" />
                <Stat label="Fat" value={nutrition.totalF} unit="g" />
                <Stat label="Fiber" value={nutrition.totalExtras.fiber} unit="g" />
                <Stat label="Sugars" value={nutrition.totalExtras.sugars} unit="g" />
                <Stat label="Sat. fat" value={nutrition.totalExtras.saturatedFat} unit="g" />
                <Stat label="Sodium" value={nutrition.totalExtras.sodium} unit="g" />
              </div>
            </Card>

            <Card title="Usage">
              {usage == null ? (
                <p className="text-[12px]" style={{ color: C.muted2 }}>—</p>
              ) : (
                <>
                  <div className="flex gap-1 mb-3 p-1 rounded-[10px]" style={{ background: C.bg }}>
                    {([7, 30, 90] as const).map((w) => {
                      const on = usageWin === w;
                      return (
                        <button key={w} onClick={() => setUsageWin(w)}
                          className="flex-1 text-[11.5px] font-semibold rounded-[7px] py-[6px]"
                          style={{ background: on ? C.surface : "transparent", color: on ? C.teal : C.muted2, boxShadow: on ? "0 1px 2px rgba(0,0,0,.06)" : "none" }}>
                          Last {w} days
                        </button>
                      );
                    })}
                  </div>
                  <div className="flex items-baseline gap-2">
                    <span className="text-[24px] font-bold" style={{ color: C.ink, fontFamily: mono }}>
                      {usageWin === 7 ? usage.last7Days : usageWin === 30 ? usage.last30Days : usage.last90Days}
                    </span>
                    <span className="text-[12.5px]" style={{ color: C.muted3 }}>day(s) assigned in the last {usageWin} days</span>
                  </div>
                  <div className="text-[11.5px] mt-2" style={{ color: C.muted2 }}>
                    {usage.timesAssigned} day{usage.timesAssigned === 1 ? "" : "s"} all-time
                    {usage.timesAssigned > 0 && <> · First {fmtDate(usage.firstUsedDate)} · Last {fmtDate(usage.lastUsedDate)}</>}
                  </div>
                </>
              )}
            </Card>

            <Card title="Per slot">
              {nutrition.slots.length === 0 ? (
                <p className="text-[12px]" style={{ color: C.muted2 }}>This diet has no meals yet.</p>
              ) : nutrition.slots.map((s) => {
                const open = openSlot === s.slot;
                return (
                  <div key={s.slot} style={{ borderTop: `1px solid ${C.bgAlt}` }}>
                    <div className="flex items-center py-[8px] cursor-pointer" onClick={() => setOpenSlot(open ? null : s.slot)}>
                      <span className="text-[9px] font-semibold rounded-[5px] px-[6px] py-[2px] mr-2" style={{ color: C.teal, background: "oklch(0.62 0.09 210 / .12)", letterSpacing: 0.4 }}>{s.slot.toUpperCase()}</span>
                      {s.lines.length > 0 && <span className="text-[9px]" style={{ color: C.muted2 }}>{open ? "▾" : "▸"}</span>}
                      <span className="flex-1" />
                      <span className="text-[10.5px] mr-3" style={{ color: C.muted3, fontFamily: mono }}>P{num(Math.round(s.p))} · C{num(Math.round(s.c))} · F{num(Math.round(s.f))}</span>
                      <span className="text-[12px] font-bold" style={{ color: C.ink, fontFamily: mono }}>{s.kcal}<span className="text-[9px] font-normal" style={{ color: C.muted2 }}> kcal</span></span>
                    </div>
                    {open && s.lines.map((li, i) => (
                      <div key={i} className="flex items-baseline py-[2px]" style={{ paddingLeft: li.header ? 8 : 20 }}>
                        <span className="flex-1 truncate" style={{ font: `${li.header ? 600 : 400} ${li.header ? 12 : 11.5}px system-ui`, color: li.header ? C.ink : C.muted3 }}>{li.header ? `🍲 ${li.name}` : `• ${li.name}`}</span>
                        <span className="text-[10px] ml-2" style={{ color: C.muted2, fontFamily: mono }}>{li.meta}</span>
                      </div>
                    ))}
                  </div>
                );
              })}
            </Card>
          </>
        )}
      </div>
    </main>
  );
}

export default function DietDetailPage() {
  return <AuthGuard><DietDetailInner /></AuthGuard>;
}
