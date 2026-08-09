"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { ReportDialog } from "@/components/social/ReportDialog";
import { foodMacros, unitLabel, num, MEAL_SLOTS, type FoodDto } from "@/lib/nutrition";
import {
  getSharedDiet, getSharedMeal, getSharedWorkout, copyTemplate, type ShareType,
} from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b" };
const mono = "'DM Mono', monospace";

const TYPE: Record<string, ShareType> = { diets: "DIET", meals: "MEAL", workouts: "WORKOUT_TEMPLATE" };

// The shared diet/meal is rendered with the same slot-grouped layout the owned Diets/Meals screens
// use, with each meal drilled into its ingredients (name + quantity).
type Ingredient = { name: string; meta: string };
type Entry = { kind: "meal" | "food"; name: string; meta: string; ingredients: Ingredient[] };
type SlotView = { slot: string; entries: Entry[] };
type Totals = { kcal: number; p: number; c: number; f: number };

const kcalMeta = (kcal: number) => `${Math.round(kcal)} kcal`;
const qtyLabel = (q: number, unit: string) => `${num(q)} ${unitLabel(unit)}`;
const macroLine = (t: Totals) => `P${num(Math.round(t.p))} · C${num(Math.round(t.c))} · F${num(Math.round(t.f))}`;
const slotIndex = (s: string) => { const i = MEAL_SLOTS.indexOf(s); return i < 0 ? MEAL_SLOTS.length : i; };

function DetailInner() {
  const router = useRouter();
  const params = useParams();
  const handle = String(params.handle);
  const type = TYPE[String(params.type)];
  const serverId = String(params.serverId);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [slots, setSlots] = useState<SlotView[]>([]);      // DIET
  const [mealItems, setMealItems] = useState<Ingredient[]>([]); // MEAL (name + qty·kcal meta)
  const [lines, setLines] = useState<string[]>([]);        // WORKOUT
  const [totals, setTotals] = useState<Totals | null>(null);
  const [copying, setCopying] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);
  const [reportOpen, setReportOpen] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      if (type === "DIET") {
        const b = await getSharedDiet(handle, serverId);
        const foodsById = new Map<string, FoodDto>((b.foods ?? []).map((f) => [f.serverId!, f as unknown as FoodDto]));
        const mealsById = new Map((b.meals ?? []).map((m) => [m.serverId!, m]));
        const total: Totals = { kcal: 0, p: 0, c: 0, f: 0 };
        const bySlot = new Map<string, Entry[]>();
        const push = (slot: string, e: Entry) => { const a = bySlot.get(slot) ?? []; a.push(e); bySlot.set(slot, a); };

        (b.diet.meals ?? []).forEach((dm) => {
          const meal = dm.mealServerId ? mealsById.get(dm.mealServerId) : undefined;
          let kcal = 0;
          const ingredients: Ingredient[] = (meal?.items ?? []).map((it) => {
            const food = it.foodServerId ? foodsById.get(it.foodServerId) : undefined;
            const m = foodMacros(food, it.quantity, String(it.unit));
            kcal += m.kcal; total.kcal += m.kcal; total.p += m.protein; total.c += m.carbs; total.f += m.fat;
            return { name: food?.name ?? "Food", meta: qtyLabel(it.quantity, String(it.unit)) };
          });
          push(dm.slot, { kind: "meal", name: meal?.name ?? "Meal", meta: kcalMeta(kcal), ingredients });
        });
        (b.diet.foodItems ?? []).forEach((fi) => {
          const food = fi.foodServerId ? foodsById.get(fi.foodServerId) : undefined;
          const m = foodMacros(food, fi.quantity ?? 1, String(fi.unit));
          total.kcal += m.kcal; total.p += m.protein; total.c += m.carbs; total.f += m.fat;
          push(fi.slot, { kind: "food", name: food?.name ?? "Food", meta: `${qtyLabel(fi.quantity ?? 1, String(fi.unit))} · ${kcalMeta(m.kcal)}`, ingredients: [] });
        });

        const ordered: SlotView[] = Array.from(bySlot.entries())
          .sort(([a], [c]) => slotIndex(a) - slotIndex(c))
          .map(([slot, entries]) => ({ slot, entries }));
        setTitle(b.diet.name); setSlots(ordered); setTotals(total); setMealItems([]); setLines([]);
      } else if (type === "MEAL") {
        const b = await getSharedMeal(handle, serverId);
        const foodsById = new Map<string, FoodDto>((b.foods ?? []).map((f) => [f.serverId!, f as unknown as FoodDto]));
        const total: Totals = { kcal: 0, p: 0, c: 0, f: 0 };
        const items: Ingredient[] = (b.meal.items ?? []).map((it) => {
          const food = it.foodServerId ? foodsById.get(it.foodServerId) : undefined;
          const m = foodMacros(food, it.quantity, String(it.unit));
          total.kcal += m.kcal; total.p += m.protein; total.c += m.carbs; total.f += m.fat;
          return { name: food?.name ?? "Food", meta: `${qtyLabel(it.quantity, String(it.unit))} · ${kcalMeta(m.kcal)}` };
        });
        setTitle(b.meal.name); setMealItems(items); setTotals(total); setSlots([]); setLines([]);
      } else {
        const b = await getSharedWorkout(handle, serverId);
        setTitle(b.workout.name);
        setLines((b.workout.exercises ?? []).map((te) => `${te.exerciseName ?? "Exercise"} — ${(te.sets ?? []).length} sets`));
        setSlots([]); setMealItems([]); setTotals(null);
      }
    } catch (e) {
      setError(String((e as Error).message).startsWith("403") ? "Follow to view this" : "Unavailable");
    } finally { setLoading(false); }
  }, [handle, type, serverId]);

  useEffect(() => { void load(); }, [load]);

  async function useThis() {
    setCopying(true); setError(null);
    try {
      const res = await copyTemplate({ entityType: type, handle, sourceServerId: serverId });
      setCopied(res.name);
    } catch { setError("Copy failed"); } finally { setCopying(false); }
  }

  const isEmpty = slots.length === 0 && mealItems.length === 0 && lines.length === 0;

  return (
    <div className="min-h-screen flex flex-col" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <span className="text-[14px]" style={{ color: C.muted }}>@{handle}</span>
        <button onClick={() => setReportOpen(true)} className="ml-auto text-[12.5px] font-semibold" style={{ color: C.muted }}>Report</button>
      </div>
      {reportOpen && <ReportDialog entityType={type} entityServerId={serverId} reportedHandle={handle} subject="this item" onClose={() => setReportOpen(false)} />}
      {loading ? <div className="p-8 text-center" style={{ color: C.muted }}>Loading…</div>
        : error && isEmpty ? <div className="p-8 text-center" style={{ color: C.muted }}>{error}</div>
        : (
        <div className="flex-1 flex flex-col max-w-md mx-auto w-full px-4">
          <div className="flex-1">
            <h1 className="text-[22px] font-bold py-2" style={{ color: C.ink }}>{title}</h1>
            <div className="rounded-[12px] p-3.5" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
              {isEmpty ? <div className="text-[13px]" style={{ color: C.muted }}>No items</div> : null}

              {/* DIET — slot groups with meal-ingredient drilldown */}
              {slots.map((s) => (
                <div key={s.slot} className="py-[3px]">
                  <div className="flex items-center mb-[2px]">
                    <span className="text-[8.5px] font-semibold rounded-[5px] px-[6px] py-[2px]" style={{ color: C.teal, background: "oklch(0.62 0.09 210 / .12)" }}>{s.slot.toUpperCase()}</span>
                  </div>
                  {s.entries.map((e, i) => (
                    <div key={i}>
                      <div className="flex items-center justify-between py-[2px]">
                        <span className="text-[13px] truncate" style={{ color: "#3f4a51" }}>{e.kind === "meal" ? `🍲 ${e.name}` : e.name}</span>
                        <span className="text-[11px] flex-none ml-2" style={{ color: C.muted2, fontFamily: mono }}>{e.meta}</span>
                      </div>
                      {e.ingredients.length > 0 && (
                        <div className="pl-[16px] pb-[3px]">
                          {e.ingredients.map((g, j) => (
                            <div key={j} className="flex items-center justify-between py-[1px]">
                              <span className="text-[12px] truncate" style={{ color: C.muted }}>• {g.name}</span>
                              <span className="text-[10.5px] flex-none ml-2" style={{ color: C.muted2, fontFamily: mono }}>{g.meta}</span>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              ))}

              {/* MEAL — food rows */}
              {mealItems.map((it, i) => (
                <div key={i} className="flex items-center justify-between py-[3px]">
                  <span className="text-[13px] truncate" style={{ color: "#3f4a51" }}>{it.name}</span>
                  <span className="text-[11px] flex-none ml-2" style={{ color: C.muted2, fontFamily: mono }}>{it.meta}</span>
                </div>
              ))}

              {/* WORKOUT — plain lines */}
              {lines.map((l, i) => <div key={i} className="text-[14px] py-1" style={{ color: C.ink }}>{l}</div>)}

              {totals && !isEmpty && (
                <div className="flex items-center mt-[8px] pt-[8px]" style={{ borderTop: `1px solid ${C.border}` }}>
                  <span className="text-[11px]" style={{ color: C.muted3, fontFamily: mono }}>{macroLine(totals)}</span>
                  <span className="flex-1" />
                  <span className="text-[13px] font-semibold" style={{ color: C.ink }}>{Math.round(totals.kcal)} kcal</span>
                </div>
              )}
            </div>
            {copied && <div className="mt-4 text-[14px] font-semibold" style={{ color: C.teal }}>Saved “{copied}” to your library ✓</div>}
            {error && <div className="mt-3 text-[13px]" style={{ color: C.danger }}>{error}</div>}
          </div>
          <div className="py-4">
            <button onClick={useThis} disabled={copying || copied != null}
              className="w-full rounded-[12px] py-3.5 font-bold text-[14px]"
              style={{ background: C.teal, color: "#fff", opacity: copying || copied != null ? 0.6 : 1 }}>
              {copying ? "Saving…" : copied != null ? "Saved ✓" : "Use this — save to my library"}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export default function Page() {
  return <AuthGuard><DetailInner /></AuthGuard>;
}
