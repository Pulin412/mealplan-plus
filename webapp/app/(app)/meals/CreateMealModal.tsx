"use client";
import { useState } from "react";
import { X, Plus, Search } from "lucide-react";
import { api } from "@/lib/api/client";
import type { components } from "@/lib/api/types.generated";

type MealDto         = components["schemas"]["MealDto"];
type FoodDto         = components["schemas"]["FoodDto"];
type MealFoodItemDto = components["schemas"]["MealFoodItemDto"];

interface PendingItem { foodId: number; foodName: string; quantity: number; kcal: number; }

function FoodSearch({ foods, onAdd }: { foods: FoodDto[]; onAdd: (item: PendingItem) => void }) {
  const [q,   setQ]   = useState("");
  const [sel, setSel] = useState<FoodDto | null>(null);
  const [qty, setQty] = useState("100");

  const results = q.length >= 2
    ? foods.filter((f) => f.name.toLowerCase().includes(q.toLowerCase())).slice(0, 10)
    : [];

  const handleAdd = () => {
    if (!sel?.id) return;
    const g = parseFloat(qty);
    if (!isNaN(g) && g > 0) {
      onAdd({
        foodId: sel.id,
        foodName: sel.name,
        quantity: g,
        kcal: Math.round(sel.caloriesPer100 * g / 100),
      });
      setSel(null); setQ(""); setQty("100");
    }
  };

  return (
    <div className="rounded-xl border border-divider bg-bg-page p-3 space-y-2">
      {!sel ? (
        <>
          <div className="flex items-center gap-2 bg-bg-card rounded-lg border border-divider px-3">
            <Search size={14} className="text-text-muted shrink-0" />
            <input
              autoFocus value={q} onChange={(e) => setQ(e.target.value)}
              placeholder="Search foods (min 2 chars)…"
              className="flex-1 py-2 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder"
            />
          </div>
          {results.length > 0 && (
            <ul className="max-h-44 overflow-y-auto divide-y divide-divider rounded-lg border border-divider">
              {results.map((f) => (
                <li key={f.id}>
                  <button
                    onClick={() => setSel(f)}
                    className="w-full flex items-center justify-between px-3 py-2.5 hover:bg-bg-card text-sm text-left transition-colors"
                  >
                    <div>
                      <span className="font-medium text-text-primary">{f.name}</span>
                      {f.brand && <span className="text-text-muted ml-1.5">· {f.brand}</span>}
                    </div>
                    <span className="text-text-muted text-xs shrink-0 ml-2">{f.caloriesPer100} kcal/100g</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {q.length >= 2 && results.length === 0 && (
            <p className="text-xs text-text-muted text-center py-2">No foods found</p>
          )}
        </>
      ) : (
        <div className="space-y-2">
          <div className="flex items-center gap-2 px-3 py-2 rounded-xl border border-green/40 bg-green/5">
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-text-primary truncate">{sel.name}</p>
              {sel.brand && <p className="text-xs text-text-muted">{sel.brand}</p>}
            </div>
            <button onClick={() => setSel(null)}><X size={14} className="text-text-muted" /></button>
          </div>
          <div className="flex items-center gap-2">
            <input
              type="number" value={qty} onChange={(e) => setQty(e.target.value)} min={1}
              className="w-24 h-9 px-3 rounded-xl border border-divider bg-bg-card text-sm text-center text-text-primary focus:outline-none focus:ring-2 focus:ring-green/30"
            />
            <span className="text-sm text-text-muted">g</span>
            {(() => {
              const g = parseFloat(qty);
              return !isNaN(g) && g > 0
                ? <span className="text-xs text-text-muted">= {Math.round(sel.caloriesPer100 * g / 100)} kcal</span>
                : null;
            })()}
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleAdd} disabled={!qty || parseFloat(qty) <= 0}
              className="flex-1 h-9 rounded-xl bg-green text-white text-sm font-semibold disabled:opacity-40 hover:bg-green/90 transition-colors"
            >
              Add food
            </button>
            <button
              onClick={() => setSel(null)}
              className="px-4 h-9 rounded-xl border border-divider text-sm text-text-secondary hover:bg-bg-page transition-colors"
            >
              Back
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

export function CreateMealModal({ foods, onCreated, onClose }: {
  foods: FoodDto[];
  onCreated: (m: MealDto) => void;
  onClose: () => void;
}) {
  const [name,   setName]   = useState("");
  const [items,  setItems]  = useState<PendingItem[]>([]);
  const [adding, setAdding] = useState(false);
  const [busy,   setBusy]   = useState(false);
  const [err,    setErr]    = useState<string | null>(null);

  const totalKcal = items.reduce((s, it) => s + it.kcal, 0);

  const removeItem = (idx: number) => setItems((prev) => prev.filter((_, i) => i !== idx));

  const submit = async () => {
    if (!name.trim()) return;
    setBusy(true); setErr(null);
    try {
      const m = await api.post<MealDto>("/api/v1/meals", {
        name: name.trim(),
        items: items.map((it) => ({
          foodId: it.foodId, quantity: it.quantity, unit: "GRAM",
        } as MealFoodItemDto)),
      });
      onCreated(m);
      onClose();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Failed to create");
    } finally { setBusy(false); }
  };

  return (
    <>
      <div className="fixed inset-0 bg-black/50 z-40" onClick={onClose} />
      <div className="fixed inset-0 z-50 flex items-end md:items-center justify-center md:p-6 pointer-events-none">
        <div className="pointer-events-auto w-full md:max-w-lg max-h-[92vh] md:max-h-[85vh] flex flex-col bg-bg-card md:rounded-3xl rounded-t-3xl shadow-2xl">

          <div className="flex justify-center pt-3 md:hidden shrink-0">
            <div className="w-9 h-1 rounded-full bg-text-muted/30" />
          </div>

          <div className="flex items-center justify-between px-5 py-4 shrink-0">
            <h2 className="text-[18px] font-bold text-text-primary">New Meal</h2>
            <button
              onClick={onClose}
              className="w-8 h-8 rounded-full flex items-center justify-center hover:bg-bg-page transition-colors"
            >
              <X className="h-5 w-5 text-text-muted" />
            </button>
          </div>
          <div className="h-px bg-divider shrink-0" />

          <div className="flex-1 overflow-y-auto px-5 py-5 space-y-5">
            {err && <p className="text-sm text-red-600 bg-red-50 rounded-xl px-3 py-2">{err}</p>}

            <section className="space-y-1.5">
              <label className="text-xs font-medium text-text-secondary">Name *</label>
              <input
                value={name} onChange={(e) => setName(e.target.value)} autoFocus
                placeholder="e.g. High protein breakfast"
                className="w-full h-11 px-3 rounded-xl border border-divider bg-bg-page text-sm text-text-primary placeholder:text-text-muted focus:outline-none focus:ring-2 focus:ring-green/30"
              />
            </section>

            <div className="h-px bg-divider" />

            <section>
              <div className="flex items-center justify-between mb-3">
                <p className="text-[13px] font-semibold text-text-muted uppercase tracking-wide">Foods</p>
                {items.length > 0 && (
                  <span className="text-xs text-text-muted">
                    {items.length} item{items.length !== 1 ? "s" : ""} · {totalKcal} kcal
                  </span>
                )}
              </div>

              {items.length > 0 && (
                <ul className="space-y-1.5 mb-3">
                  {items.map((it, idx) => (
                    <li key={idx} className="flex items-center gap-2 px-3 py-2 rounded-xl border border-divider bg-bg-page">
                      <div className="flex-1 min-w-0">
                        <p className="text-sm text-text-primary truncate">{it.foodName}</p>
                        <p className="text-xs text-text-muted">{it.quantity}g · {it.kcal} kcal</p>
                      </div>
                      <button
                        onClick={() => removeItem(idx)}
                        className="p-1 rounded-lg hover:bg-bg-card transition-colors"
                      >
                        <X className="h-3.5 w-3.5 text-text-muted" />
                      </button>
                    </li>
                  ))}
                </ul>
              )}

              {adding ? (
                <FoodSearch
                  foods={foods}
                  onAdd={(item) => { setItems((prev) => [...prev, item]); setAdding(false); }}
                />
              ) : (
                <button
                  onClick={() => setAdding(true)}
                  className="w-full h-11 rounded-xl border-2 border-dashed border-divider text-sm text-text-muted hover:bg-bg-page transition-colors flex items-center justify-center gap-2"
                >
                  <Plus className="h-4 w-4" />Add food
                </button>
              )}
            </section>
          </div>

          <div className="h-px bg-divider shrink-0" />
          <div className="px-5 py-4 shrink-0" style={{ paddingBottom: "max(16px, env(safe-area-inset-bottom))" }}>
            <button
              onClick={submit} disabled={busy || !name.trim()}
              className="w-full h-12 rounded-xl bg-green text-white font-semibold text-[15px] disabled:opacity-50 hover:bg-green/90 transition-colors"
            >
              {busy
                ? "Creating…"
                : `Create meal${items.length > 0 ? ` · ${items.length} food${items.length !== 1 ? "s" : ""}` : ""}`}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
