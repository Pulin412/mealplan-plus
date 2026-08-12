"use client";

import { useEffect, useState } from "react";
import { NoteBadge } from "@/components/NoteBadge";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { useMeals, type BuildItem, type MealView } from "@/hooks/useMeals";
import { useUnsavedGuard } from "@/hooks/useUnsavedGuard";
import { MEAL_SLOTS, unitLabel, defaultQtyFor, foodMacros, num, FOOD_UNITS, isCountUnit, type FoodDto } from "@/lib/nutrition";
import { Stepper } from "@/components/ui/Stepper";
import { createFood } from "@/lib/api/foods";
import type { MealDto } from "@/lib/api/meals";
import type { MealSort } from "@/types/meal";
import type { ManualFoodForm } from "@/types/food";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", tealDim: "oklch(0.55 0.09 220)", danger: "#b23b3b",
};
const mono = "'DM Mono', monospace";
const SORT_LABELS: Record<MealSort, string> = { recent: "Recent", name: "Name", calories: "Calories", protein: "Protein" };

function StarBtn({ active, onClick }: { active: boolean; onClick: (e: React.MouseEvent) => void }) {
  return <button onClick={onClick} className="flex-none text-[15px] leading-none px-0.5" style={{ color: active ? C.teal : C.muted2 }}>{active ? "★" : "☆"}</button>;
}
function GlobeBtn({ active, onClick }: { active: boolean; onClick: (e: React.MouseEvent) => void }) {
  return <button onClick={onClick} title={active ? "Shared with followers" : "Not shared"} className="flex-none text-[13px] leading-none px-0.5" style={{ color: active ? C.teal : C.muted2 }}>{active ? "🌐" : "◍"}</button>;
}
function SlotBadge({ slot }: { slot: string }) {
  return <span className="text-[8.5px] font-semibold rounded-[5px] px-[6px] py-[2px]" style={{ color: C.teal, background: "oklch(0.62 0.09 210 / .12)" }}>{slot.toUpperCase()}</span>;
}
function macroLine(p: number, c: number, f: number) {
  return `P${num(p)} · C${num(c)} · F${num(f)}`;
}

// ── List card ─────────────────────────────────────────────────────────────────
function MealCard({ v, expanded, compact, isLast, onToggle, onFav, onShare, onEdit, onDelete }: {
  v: MealView; expanded: boolean; compact?: boolean; isLast?: boolean; onToggle: () => void;
  onFav: (e: React.MouseEvent) => void; onShare: (e: React.MouseEvent) => void; onEdit: () => void; onDelete: () => void;
}) {
  const slots = v.meal.slots ?? [];
  const wrapClass = compact ? "cursor-pointer px-[11px] py-[7px]" : "cursor-pointer rounded-[12px] mb-[8px] px-3 py-[9px]";
  const wrapStyle = compact
    ? { borderBottom: isLast ? "none" : `1px solid ${C.bgAlt}` }
    : { background: C.surface, border: `1px solid ${C.border}` };
  return (
    <div onClick={onToggle} className={wrapClass} style={wrapStyle}>
      <div className="flex items-center gap-[10px]">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-[6px]">
            <span className="text-[12.5px] font-bold truncate" style={{ color: C.ink }}>{v.meal.name}</span>
            {slots[0] && <SlotBadge slot={slots[0]} />}
            {slots.length > 1 && <span className="text-[9px]" style={{ color: C.muted2 }}>+{slots.length - 1}</span>}
            <NoteBadge note={v.meal.notes} />
          </div>
          <div className="text-[10.5px] truncate mt-0.5" style={{ color: C.muted2 }}>{v.summary}</div>
        </div>
        {!v.meal.imported && <GlobeBtn active={!!v.meal.isShared} onClick={(e) => { e.stopPropagation(); onShare(e); }} />}
        <StarBtn active={!!v.meal.isFavorite} onClick={(e) => { e.stopPropagation(); onFav(e); }} />
        <span className="flex-none text-[12.5px] font-bold tabular-nums" style={{ color: C.ink, fontFamily: mono }}>
          {v.totalKcal}<span className="text-[9px] font-normal" style={{ color: C.muted2, fontFamily: "system-ui" }}> kcal</span>
        </span>
      </div>
      {expanded && (
        <div className="mt-[9px] pt-[9px]" style={{ borderTop: `1px solid ${C.bgAlt}` }}>
          {v.items.map((it, i) => (
            <div key={i} className="flex items-center justify-between py-[2px]">
              <span className="text-[11.5px] truncate" style={{ color: "#3f4a51" }}>{it.name}</span>
              <span className="text-[10px] flex-none ml-2" style={{ color: C.muted2, fontFamily: mono }}>{num(it.quantity)} {unitLabel(it.unit)} · {it.kcal} kcal</span>
            </div>
          ))}
          <div className="flex items-center mt-[8px]">
            <span className="text-[10.5px]" style={{ color: C.muted3, fontFamily: mono }}>{macroLine(v.totalP, v.totalC, v.totalF)}</span>
            <span className="flex-1" />
            <button onClick={(e) => { e.stopPropagation(); onEdit(); }} className="text-[12px] font-semibold mr-[14px]" style={{ color: C.teal }}>✎ Edit</button>
            <button onClick={(e) => { e.stopPropagation(); onDelete(); }} className="text-[12px]" style={{ color: "#c4ccd1" }}>✕ Remove</button>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Qty input ───────────────────────────────────────────────────────────────
function QtyInput({ value, unit, onChange }: { value: number; unit: string; onChange: (n: number) => void }) {
  const gramLike = unit === "GRAM" || unit === "ML";
  return <Stepper value={value} onChange={onChange} min={0} step={gramLike ? 5 : 1} decimals={1} suffix={unitLabel(unit)} dense />;
}

// ── Inline "create a food" while building a meal ────────────────────────────────
function FForm({ label, value, onChange, placeholder, numeric, wrap = "mb-[11px]" }: {
  label: string; value: string; onChange: (v: string) => void; placeholder?: string; numeric?: boolean; wrap?: string;
}) {
  return (
    <div className={wrap}>
      <label className="block text-[11px] font-semibold mb-[5px]" style={{ color: C.muted3 }}>{label}</label>
      <input value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder} inputMode={numeric ? "decimal" : undefined}
        className="w-full border rounded-[10px] px-3 py-[9px] text-[13px]" style={{ border: "1.5px solid #dfe6e8", color: C.ink }} />
    </div>
  );
}

function NewFoodForm({ onCancel, onCreate }: { onCancel: () => void; onCreate: (form: ManualFoodForm) => Promise<void> }) {
  const [f, setF] = useState<ManualFoodForm>({ name: "", servingLabel: "", kcal: "", protein: "", carbs: "", fat: "", category: "", unit: "GRAM", gramsPerUnit: "" });
  const [saving, setSaving] = useState(false);
  const set = (k: keyof ManualFoodForm, v: string) => setF((p) => ({ ...p, [k]: v }));
  const gpuOk = !isCountUnit(f.unit) || (parseFloat(f.gramsPerUnit) || 0) > 0;
  const ok = f.name.trim() !== "" && f.kcal.trim() !== "" && gpuOk;
  const submit = async () => { if (!ok || saving) return; setSaving(true); try { await onCreate(f); } finally { setSaving(false); } };
  return (
    <div>
      <div className="flex items-center gap-2 mb-3">
        <button onClick={onCancel} className="text-[18px] leading-none" style={{ color: C.ink }}>‹</button>
        <span className="text-[13px] font-semibold" style={{ color: C.ink }}>New food</span>
      </div>
      <FForm label="Name" value={f.name} onChange={(v) => set("name", v)} placeholder="e.g. Overnight oats" />
      <div className="mb-[11px]">
        <label className="block text-[11px] font-semibold mb-[6px]" style={{ color: C.muted3 }}>Measured in</label>
        <div className="flex flex-wrap gap-[6px]">
          {FOOD_UNITS.map((u) => {
            const on = f.unit === u;
            return (
              <button key={u} type="button" onClick={() => set("unit", u)} className="rounded-full px-3 py-[6px] text-[12px] font-semibold"
                style={{ background: on ? C.teal : "transparent", color: on ? "#fff" : C.muted3, border: `1.5px solid ${on ? C.teal : "#dfe6e8"}` }}>
                {unitLabel(u)}
              </button>
            );
          })}
        </div>
      </div>
      {isCountUnit(f.unit) && (
        <FForm label={`Grams per ${unitLabel(f.unit)}`} value={f.gramsPerUnit} onChange={(v) => set("gramsPerUnit", v)} placeholder="e.g. 50" numeric />
      )}
      <FForm label="Serving size" value={f.servingLabel} onChange={(v) => set("servingLabel", v)} placeholder="e.g. 250 g" />
      <FForm label="Calories /100g" value={f.kcal} onChange={(v) => set("kcal", v)} placeholder="0" numeric />
      <div className="flex gap-[10px] mb-3">
        <FForm label="Protein" value={f.protein} onChange={(v) => set("protein", v)} placeholder="g" numeric wrap="flex-1" />
        <FForm label="Carbs" value={f.carbs} onChange={(v) => set("carbs", v)} placeholder="g" numeric wrap="flex-1" />
        <FForm label="Fat" value={f.fat} onChange={(v) => set("fat", v)} placeholder="g" numeric wrap="flex-1" />
      </div>
      <button onClick={submit} disabled={!ok || saving} className="w-full rounded-[10px] py-[11px] text-[12.5px] font-semibold"
        style={{ background: ok ? C.teal : C.bgAlt, color: ok ? "#fff" : C.muted2 }}>{saving ? "Adding…" : "Add to meal"}</button>
    </div>
  );
}

// ── Full-screen builder ────────────────────────────────────────────────────────
function MealBuilder({ editing, foods, foodsById, saving, onSave, onDelete, onClose }: {
  editing: MealDto | null; foods: FoodDto[]; foodsById: Map<number, FoodDto>;
  saving: boolean; onSave: (name: string, slots: string[], items: BuildItem[], notes: string) => void;
  onDelete: () => void; onClose: () => void;
}) {
  const [name, setName] = useState(editing?.name ?? "");
  const [notes, setNotes] = useState(editing?.notes ?? "");
  const [slots, setSlots] = useState<string[]>(editing?.slots ?? []);
  const [items, setItems] = useState<BuildItem[]>(
    (editing?.items ?? []).filter((it) => it.foodId != null).map((it) => ({ foodId: it.foodId!, quantity: it.quantity, unit: it.unit }))
  );
  const [dirty, setDirty] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickQuery, setPickQuery] = useState("");
  const [pickMode, setPickMode] = useState<"search" | "new">("search");
  // Foods created inline while building this meal — merged into lookups so they render immediately
  // (the parent's foods list only refreshes on next load).
  const [localFoods, setLocalFoods] = useState<Map<number, FoodDto>>(new Map());
  const getFood = (id: number) => localFoods.get(id) ?? foodsById.get(id);

  const totals = items.reduce((acc, it) => {
    const m = foodMacros(getFood(it.foodId), it.quantity, it.unit);
    return { kcal: acc.kcal + m.kcal, p: acc.p + m.protein, c: acc.c + m.carbs, f: acc.f + m.fat };
  }, { kcal: 0, p: 0, c: 0, f: 0 });

  const toggleSlot = (s: string) => { setSlots((prev) => prev.includes(s) ? prev.filter((x) => x !== s) : [...prev, s]); setDirty(true); };
  const setQty = (foodId: number, q: number) => { setItems((prev) => prev.map((it) => it.foodId === foodId ? { ...it, quantity: q } : it)); setDirty(true); };
  const removeItem = (foodId: number) => { setItems((prev) => prev.filter((it) => it.foodId !== foodId)); setDirty(true); };
  const addFood = (f: FoodDto) => { if (f.id != null && !items.some((it) => it.foodId === f.id)) { setItems((prev) => [...prev, { foodId: f.id!, quantity: defaultQtyFor(f.unit ?? "GRAM"), unit: (f.unit ?? "GRAM") }]); setDirty(true); } };

  const canSave = name.trim() !== "" && items.length > 0;
  const doSave = () => onSave(name, slots, items, notes);

  // Guard bottom-nav taps / ✕ while there are unsaved changes.
  const { setGuard, attempt } = useUnsavedGuard();
  useEffect(() => {
    setGuard(dirty ? { canSave, onSave: doSave, onDiscard: onClose } : null);
    return () => setGuard(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dirty, canSave, name, slots, items, notes]);

  const pickList = foods.filter((f) => !pickQuery || f.name.toLowerCase().includes(pickQuery.toLowerCase()));

  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      {/* header */}
      <div className="flex-none flex items-center gap-2 px-3 pt-3 pb-2">
        <button onClick={() => attempt(onClose)} className="text-[20px]" style={{ color: C.ink }}>✕</button>
        <span className="text-[17px] font-semibold" style={{ color: C.ink }}>{editing ? "Edit meal" : "New meal"}</span>
        <span className="flex-1" />
        {editing && <button onClick={onDelete} className="text-[13px] font-semibold" style={{ color: C.danger }}>Delete</button>}
      </div>

      <div className="flex-1 overflow-y-auto px-4">
        <label className="block text-[11px] font-semibold mb-[5px] mt-2" style={{ color: C.muted3 }}>Meal name</label>
        <input value={name} onChange={(e) => { setName(e.target.value); setDirty(true); }} placeholder="e.g. Chicken rice bowl"
          className="w-full border rounded-[11px] px-3 py-[11px] text-[13px] mb-[14px]" style={{ border: "1.5px solid #dfe6e8", color: C.ink }} />

        <div className="flex items-baseline mb-2">
          <span className="text-[11px] font-semibold" style={{ color: C.muted3 }}>Meal slot</span>
          <span className="text-[11px] ml-1" style={{ color: C.muted2 }}>· optional</span>
        </div>
        <div className="flex flex-wrap gap-[6px] mb-4">
          {MEAL_SLOTS.map((s) => {
            const on = slots.includes(s);
            return (
              <button key={s} onClick={() => toggleSlot(s)} className="text-[11px] font-semibold rounded-full px-[11px] py-[6px]"
                style={{ color: on ? "#fff" : C.muted3, background: on ? C.teal : "transparent", border: `1.5px solid ${on ? C.teal : "#dfe6e8"}` }}>{s}</button>
            );
          })}
        </div>

        <div className="flex items-baseline mb-2">
          <span className="text-[11px] font-semibold" style={{ color: C.muted3 }}>Notes</span>
          <span className="text-[11px] ml-1" style={{ color: C.muted2 }}>· optional</span>
        </div>
        <textarea value={notes} onChange={(e) => { setNotes(e.target.value); setDirty(true); }} rows={2}
          placeholder="e.g. prep the night before"
          className="w-full border rounded-[11px] px-3 py-[10px] text-[13px] mb-4 resize-none" style={{ border: "1.5px solid #dfe6e8", color: C.ink }} />

        <div className="flex items-center mb-2">
          <span className="text-[12.5px] font-semibold" style={{ color: C.ink }}>Food items</span>
          <span className="flex-1" />
          <span className="text-[12px] font-semibold" style={{ color: C.teal, fontFamily: mono }}>{Math.round(totals.kcal)} kcal</span>
        </div>

        {items.length === 0 ? (
          <div className="rounded-[12px] text-center text-[11.5px] py-[18px] mb-2" style={{ border: "1.5px dashed #cdd7da", color: C.muted2 }}>No items yet — add food to build this meal.</div>
        ) : (
          items.map((it) => {
            const food = getFood(it.foodId);
            const kcal = Math.round(foodMacros(food, it.quantity, it.unit).kcal);
            return (
              <div key={it.foodId} className="flex items-center gap-2 py-[8px]" style={{ borderBottom: `1px solid ${C.bgAlt}` }}>
                <div className="flex-1 min-w-0">
                  <div className="text-[12px] font-semibold truncate" style={{ color: C.ink }}>{food?.name ?? "Unknown"}</div>
                  <div className="text-[10px]" style={{ color: C.muted2, fontFamily: mono }}>{kcal} kcal</div>
                </div>
                <QtyInput value={it.quantity} unit={it.unit} onChange={(n) => setQty(it.foodId, n)} />
                <button onClick={() => removeItem(it.foodId)} className="text-[13px]" style={{ color: "#c4ccd1" }}>✕</button>
              </div>
            );
          })
        )}

        {!pickerOpen ? (
          <button onClick={() => setPickerOpen(true)} className="w-full rounded-[12px] py-[12px] text-[12.5px] font-semibold mt-3" style={{ border: "1.5px dashed #cdd7da", color: C.teal }}>＋ Add food item</button>
        ) : (
          <div className="rounded-[12px] mt-3 p-3" style={{ border: `1px solid ${C.border}`, background: C.surface }}>
            {pickMode === "search" ? (
              <>
                <div className="flex items-center gap-2 rounded-[10px] px-3 py-[9px] mb-2" style={{ background: C.bgAlt }}>
                  <span style={{ color: C.muted2 }}>⌕</span>
                  <input value={pickQuery} onChange={(e) => setPickQuery(e.target.value)} placeholder="Search your foods…"
                    className="flex-1 bg-transparent border-none outline-none text-[13px]" style={{ color: C.ink }} />
                  {/* Clear the search → empties the box and shows all foods again. */}
                  {pickQuery && <button onClick={() => setPickQuery("")} className="text-[13px] leading-none" style={{ color: C.muted2 }}>✕</button>}
                </div>
                {/* Create a new food inline while building the meal — added to this meal only (also saved to your foods). */}
                <button onClick={() => setPickMode("new")} className="w-full rounded-[10px] py-[9px] text-[12px] font-bold mb-2" style={{ border: `1.5px solid ${C.teal}`, color: C.teal }}>＋ New food</button>
                <div className="max-h-[240px] overflow-y-auto">
                  {pickList.length === 0 && <p className="text-center text-[12px] py-3" style={{ color: C.muted2 }}>No foods yet — tap “＋ New food” to create one.</p>}
                  {pickList.map((f) => {
                    const added = items.some((it) => it.foodId === f.id);
                    return (
                      <div key={f.id} className="flex items-center gap-2 py-[9px]" style={{ borderBottom: `1px solid ${C.bgAlt}` }}>
                        <div className="flex-1 min-w-0">
                          <div className="text-[12.5px] font-semibold truncate" style={{ color: C.ink }}>{f.name}</div>
                          <div className="text-[10.5px]" style={{ color: C.muted }}>{f.caloriesPer100} kcal / 100g</div>
                        </div>
                        <button onClick={() => addFood(f)} disabled={added} className="rounded-[9px] px-3 py-[7px] text-[11.5px] font-semibold"
                          style={{ border: `1.5px solid #dfe6e8`, color: added ? C.muted2 : C.tealDim }}>{added ? "✓ Added" : "+ Add"}</button>
                      </div>
                    );
                  })}
                </div>
                <button onClick={() => { setPickerOpen(false); setPickQuery(""); }} className="w-full rounded-[10px] py-[10px] text-[12px] font-semibold mt-2" style={{ background: C.teal, color: "#fff" }}>Done · {items.length} added</button>
              </>
            ) : (
              <NewFoodForm
                onCancel={() => setPickMode("search")}
                onCreate={async (form) => {
                  const dto = await createFood(form);
                  if (dto.id != null) { setLocalFoods((prev) => new Map(prev).set(dto.id!, dto)); addFood(dto); }
                  setPickMode("search");
                }}
              />
            )}
          </div>
        )}

        {items.length > 0 && <div className="text-center text-[10.5px] mt-3" style={{ color: C.muted2, fontFamily: mono }}>{macroLine(totals.p, totals.c, totals.f)}</div>}
        <div className="h-4" />
      </div>

      <div className="flex-none p-4">
        <button onClick={() => onSave(name, slots, items, notes)} disabled={!canSave || saving}
          className="w-full rounded-[12px] py-[14px] text-[13px] font-semibold" style={{ background: canSave ? C.teal : C.bgAlt, color: canSave ? "#fff" : C.muted2 }}>
          {saving ? "Saving…" : editing ? "Save changes" : "Save meal"}
        </button>
      </div>
    </div>
  );
}

function MealsPageInner() {
  const m = useMeals();
  const [sortOpen, setSortOpen] = useState(false);

  if (m.builderOpen) {
    return (
      <MealBuilder editing={m.editing} foods={m.foods} foodsById={m.foodsById} saving={m.saving}
        onSave={m.saveMeal}
        onDelete={() => { if (m.editing) { void m.handleDelete(m.editing); m.closeBuilder(); } }}
        onClose={m.closeBuilder} />
    );
  }

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div className="flex-none px-[14px] pt-[8px] pb-[12px]" style={{ background: C.surface, borderBottom: `1px solid #eef1f3` }}>
        <div className="flex items-center gap-2 mb-[10px]">
          <span className="flex-1 text-[17px] font-semibold" style={{ color: C.ink }}>Meals</span>
          <span className="text-[11px]" style={{ color: C.muted }}>{m.totalCount} saved</span>
        </div>
        <div className="flex items-center gap-2 rounded-[11px] px-3 py-[9px] mb-[11px]" style={{ background: C.bgAlt }}>
          <span style={{ color: C.muted2 }}>⌕</span>
          <input value={m.query} onChange={(e) => m.setQuery(e.target.value)} placeholder="Search your meals…"
            className="flex-1 bg-transparent border-none outline-none text-[12.5px]" style={{ color: C.ink }} />
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <button onClick={() => setSortOpen((o) => !o)} className="flex items-center gap-[6px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold" style={{ background: C.bgAlt, color: C.ink }}>
              ↕ {SORT_LABELS[m.sort]}<span style={{ color: C.muted2, fontSize: 9 }}>▾</span>
            </button>
            {sortOpen && (
              <>
                <div className="fixed inset-0 z-10" onClick={() => setSortOpen(false)} />
                <div className="absolute left-0 top-[38px] z-20 rounded-[12px] p-[6px] min-w-[158px]" style={{ background: C.surface, border: `1px solid ${C.border}`, boxShadow: "0 8px 24px rgba(0,0,0,.14)" }}>
                  {(["recent", "name", "calories", "protein"] as MealSort[]).map((s) => (
                    <button key={s} onClick={() => { m.setSort(s); setSortOpen(false); }} className="w-full flex items-center justify-between px-[10px] py-[9px] rounded-[8px] text-[12px] font-semibold" style={{ color: s === m.sort ? C.teal : C.ink }}>
                      {SORT_LABELS[s]}{s === m.sort && <span style={{ color: C.teal }}>✓</span>}
                    </button>
                  ))}
                </div>
              </>
            )}
          </div>
          <button onClick={() => m.setImportedOnly(!m.importedOnly)} className="rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold"
            style={{ background: m.importedOnly ? "oklch(0.62 0.09 210 / .12)" : C.bgAlt, color: m.importedOnly ? C.teal : C.ink }}>Imported</button>
          <button onClick={() => m.setFavOnly(!m.favOnly)} className="flex items-center gap-[5px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold"
            style={{ background: m.favOnly ? "oklch(0.62 0.09 210 / .12)" : C.bgAlt, color: m.favOnly ? C.teal : C.ink }}>
            {m.favOnly ? "★" : "☆"} {m.favCount}
          </button>
          <span className="flex-1" />
          <div className="flex rounded-[9px] overflow-hidden" style={{ border: `1px solid #e2e7ea` }}>
            {(["list", "compact"] as const).map((v) => (
              <button key={v} onClick={() => m.setViewMode(v)} className="w-[34px] h-[32px] flex items-center justify-center text-[15px]"
                style={{ background: m.viewMode === v ? C.surface : "transparent", color: m.viewMode === v ? C.ink : C.muted2, borderRight: v === "list" ? "1px solid #e2e7ea" : "none" }}>
                {v === "list" ? "☰" : "≣"}
              </button>
            ))}
          </div>
        </div>
        {m.allSlots.length > 0 && (
          <div className="flex gap-[6px] mt-[10px] overflow-x-auto">
            <SlotChip label="All" on={m.slotFilter === null} onClick={() => m.setSlotFilter(null)} />
            {m.allSlots.map((s) => <SlotChip key={s} label={s} on={m.slotFilter === s} onClick={() => m.setSlotFilter(m.slotFilter === s ? null : s)} />)}
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto px-[14px] pt-3" style={{ paddingBottom: 120 }}>
        {m.loading && <div className="text-center py-12 text-[12px]" style={{ color: C.muted }}>Loading…</div>}
        {m.error && <div className="text-center py-12 text-[12px]" style={{ color: C.danger }}>{m.error}</div>}
        {!m.loading && m.meals.length === 0 && (
          <div className="text-center py-16">
            <div className="text-[34px]">🍲</div>
            <div className="text-[14px] font-semibold mt-2" style={{ color: C.muted3 }}>No meals yet</div>
            <div className="text-[12px] mt-1" style={{ color: C.muted2 }}>Tap + to build a meal from your foods.</div>
          </div>
        )}
        {m.viewMode === "list" && m.meals.map((v) => (
          <MealCard key={v.meal.id} v={v} expanded={m.expandedIds.has(v.meal.id!)}
            onToggle={() => m.toggleExpand(v.meal.id!)}
            onFav={() => void m.handleToggleFav(v.meal)}
            onShare={() => void m.handleToggleShare(v.meal)}
            onEdit={() => m.openEdit(v.meal)}
            onDelete={() => void m.handleDelete(v.meal)} />
        ))}
        {m.viewMode === "compact" && m.meals.length > 0 && (
          <div className="rounded-[12px] overflow-hidden" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            {m.meals.map((v, i) => (
              <MealCard key={v.meal.id} v={v} compact isLast={i === m.meals.length - 1}
                expanded={m.expandedIds.has(v.meal.id!)}
                onToggle={() => m.toggleExpand(v.meal.id!)}
                onFav={() => void m.handleToggleFav(v.meal)}
                onShare={() => void m.handleToggleShare(v.meal)}
                onEdit={() => m.openEdit(v.meal)}
                onDelete={() => void m.handleDelete(v.meal)} />
            ))}
          </div>
        )}
      </div>

      <button onClick={m.openNew} className="fixed bottom-[68px] right-4 z-40 w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg"
        style={{ background: C.teal, boxShadow: `0 6px 18px oklch(0.62 0.09 210 / .45)` }}>+</button>

      <NutritionNav />
    </div>
  );
}

function SlotChip({ label, on, onClick }: { label: string; on: boolean; onClick: () => void }) {
  return <button onClick={onClick} className="flex-none text-[11px] font-semibold rounded-full px-[12px] py-[5px]" style={{ background: on ? C.ink : C.bgAlt, color: on ? "#fff" : C.muted3 }}>{label}</button>;
}

export default function MealsPage() {
  return <AuthGuard><MealsPageInner /></AuthGuard>;
}
