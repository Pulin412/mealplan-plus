"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import {
  getSharedDiet, getSharedMeal, getSharedWorkout, copyTemplate, type ShareType,
} from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b" };

const TYPE: Record<string, ShareType> = { diets: "DIET", meals: "MEAL", workouts: "WORKOUT_TEMPLATE" };

function DetailInner() {
  const router = useRouter();
  const params = useParams();
  const handle = String(params.handle);
  const type = TYPE[String(params.type)];
  const serverId = String(params.serverId);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState("");
  const [lines, setLines] = useState<string[]>([]);
  const [copying, setCopying] = useState(false);
  const [copied, setCopied] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      if (type === "DIET") {
        const b = await getSharedDiet(handle, serverId);
        const foods = new Map((b.foods ?? []).map((f) => [f.serverId, f.name]));
        const meals = new Map((b.meals ?? []).map((m) => [m.serverId, m.name]));
        const ls: string[] = [];
        if (b.diet.targetCalories != null) ls.push(`Target: ${Math.round(b.diet.targetCalories)} kcal`);
        (b.diet.meals ?? []).forEach((dm) => ls.push(`${dm.slot}: ${meals.get(dm.mealServerId ?? "") ?? "Meal"}`));
        (b.diet.foodItems ?? []).forEach((fi) => ls.push(`${fi.slot}: ${foods.get(fi.foodServerId ?? "") ?? "Food"} (${Math.round(fi.quantity)} ${String(fi.unit).toLowerCase()})`));
        setTitle(b.diet.name); setLines(ls);
      } else if (type === "MEAL") {
        const b = await getSharedMeal(handle, serverId);
        const foods = new Map((b.foods ?? []).map((f) => [f.serverId, f.name]));
        setTitle(b.meal.name);
        setLines((b.meal.items ?? []).map((it) => `${foods.get(it.foodServerId ?? "") ?? "Food"} — ${Math.round(it.quantity)} ${String(it.unit).toLowerCase()}`));
      } else {
        const b = await getSharedWorkout(handle, serverId);
        setTitle(b.workout.name);
        setLines((b.workout.exercises ?? []).map((te) => `${te.exerciseName ?? "Exercise"} — ${(te.sets ?? []).length} sets`));
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

  return (
    <div className="min-h-screen flex flex-col" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <span className="text-[14px]" style={{ color: C.muted }}>@{handle}</span>
      </div>
      {loading ? <div className="p-8 text-center" style={{ color: C.muted }}>Loading…</div>
        : error && lines.length === 0 ? <div className="p-8 text-center" style={{ color: C.muted }}>{error}</div>
        : (
        <div className="flex-1 flex flex-col max-w-md mx-auto w-full px-4">
          <div className="flex-1">
            <h1 className="text-[22px] font-bold py-2" style={{ color: C.ink }}>{title}</h1>
            <div className="rounded-[12px] p-3.5" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
              {lines.length === 0 ? <div className="text-[13px]" style={{ color: C.muted }}>No items</div>
                : lines.map((l, i) => <div key={i} className="text-[14px] py-1" style={{ color: C.ink }}>{l}</div>)}
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
