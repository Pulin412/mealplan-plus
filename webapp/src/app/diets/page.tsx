"use client";

import { useState } from "react";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { useDiets, type DietView, type MealSummary } from "@/hooks/useDiets";
import { MEAL_SLOTS, unitLabel, defaultQtyFor, foodMacros, num, type FoodDto } from "@/lib/nutrition";
import type { DietDto, DietEntryInput } from "@/lib/api/diets";
import type { MealDto } from "@/lib/api/meals";
import type { TagDto } from "@/lib/api/tags";
import type { DietSort } from "@/types/diet";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e",
  border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", tealDim: "oklch(0.55 0.09 220)", danger: "#b23b3b",
};
const mono = "'DM Mono', monospace";
const SORT_LABELS: Record<DietSort, string> = { recent: "Recent", name: "Name", calories: "Calories", protein: "Protein" };
const macroLine = (p: number, c: number, f: number) => `P${num(p)} · C${num(c)} · F${num(f)}`;

function StarBtn({ active, onClick }: { active: boolean; onClick: (e: React.MouseEvent) => void }) {
  return <button onClick={onClick} className="flex-none text-[15px] leading-none px-0.5" style={{ color: active ? C.teal : C.muted2 }}>{active ? "★" : "☆"}</button>;
}
function TagPill({ name }: { name: string }) {
  return <span className="text-[8.5px] font-semibold rounded-[5px] px-[6px] py-[2px]" style={{ color: C.teal, background: "oklch(0.62 0.09 210 / .12)" }}>{name}</span>;
}
function SlotGroupView({ slot, entries, kcal }: DietView["slots"][number]) {
  return (
    <div className="py-[3px]">
      <div className="flex items-center mb-[2px]">
        <span className="text-[8.5px] font-semibold rounded-[5px] px-[6px] py-[2px]" style={{ color: C.teal, background: "oklch(0.62 0.09 210 / .12)" }}>{slot.toUpperCase()}</span>
        <span className="flex-1" />
        <span className="text-[9.5px]" style={{ color: C.muted2, fontFamily: mono }}>{kcal} kcal</span>
      </div>
      {entries.map((e, i) => (
        <div key={i} className="flex items-center justify-between py-[2px]">
          <span className="text-[11.5px] truncate" style={{ color: "#3f4a51" }}>{e.kind === "meal" ? `🍲 ${e.name}` : e.name}</span>
          <span className="text-[10px] flex-none ml-2" style={{ color: C.muted2, fontFamily: mono }}>{e.meta}</span>
        </div>
      ))}
    </div>
  );
}

function DietCard({ v, expanded, compact, isLast, onToggle, onFav, onEdit, onDelete }: {
  v: DietView; expanded: boolean; compact?: boolean; isLast?: boolean; onToggle: () => void;
  onFav: (e: React.MouseEvent) => void; onEdit: () => void; onDelete: () => void;
}) {
  const wrapClass = compact ? "cursor-pointer px-[11px] py-[7px]" : "cursor-pointer rounded-[12px] mb-[8px] px-3 py-[9px]";
  const wrapStyle = compact ? { borderBottom: isLast ? "none" : `1px solid ${C.bgAlt}` } : { background: C.surface, border: `1px solid ${C.border}` };
  return (
    <div onClick={onToggle} className={wrapClass} style={wrapStyle}>
      <div className="flex items-center gap-[10px]">
        <div className="flex-1 min-w-0">
          <div className="text-[12.5px] font-bold truncate" style={{ color: C.ink }}>{v.diet.name}</div>
          <div className="text-[10.5px] truncate mt-0.5" style={{ color: C.muted2 }}>{v.summary}</div>
          {v.tagNames.length > 0 && (
            <div className="flex gap-[4px] mt-[4px] flex-wrap">
              {v.tagNames.slice(0, 3).map((t) => <TagPill key={t} name={t} />)}
              {v.tagNames.length > 3 && <span className="text-[8.5px]" style={{ color: C.muted2 }}>+{v.tagNames.length - 3}</span>}
            </div>
          )}
        </div>
        <StarBtn active={!!v.diet.isFavorite} onClick={(e) => { e.stopPropagation(); onFav(e); }} />
        <span className="flex-none text-[12.5px] font-bold tabular-nums" style={{ color: C.ink, fontFamily: mono }}>
          {v.totalKcal}<span className="text-[9px] font-normal" style={{ color: C.muted2, fontFamily: "system-ui" }}> kcal</span>
        </span>
      </div>
      {expanded && (
        <div className="mt-[9px] pt-[9px]" style={{ borderTop: `1px solid ${C.bgAlt}` }}>
          {v.slots.map((s) => <SlotGroupView key={s.slot} {...s} />)}
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

// ── builder types ──
interface BuildEntry { kind: "meal" | "food"; refId: number; slot: string; quantity: number; unit: string; name: string; kcalPer100?: number }

function DietBuilder({ editing, meals, foods, foodsById, mealSummaries, availableTags, saving, onCreateTag, onSave, onDelete, onClose }: {
  editing: DietDto | null; meals: MealDto[]; foods: FoodDto[]; foodsById: Map<number, FoodDto>;
  mealSummaries: Map<number, MealSummary>; availableTags: TagDto[]; saving: boolean;
  onCreateTag: (name: string) => Promise<TagDto | null>;
  onSave: (name: string, entries: DietEntryInput[], tagIds: number[]) => void;
  onDelete: () => void; onClose: () => void;
}) {
  const [name, setName] = useState(editing?.name ?? "");
  const [entries, setEntries] = useState<BuildEntry[]>(() => {
    const out: BuildEntry[] = [];
    (editing?.meals ?? []).forEach((dm) => { if (dm.mealId != null) out.push({ kind: "meal", refId: dm.mealId, slot: dm.slot, quantity: 1, unit: "GRAM", name: mealSummaries.get(dm.mealId)?.name ?? "Meal" }); });
    (editing?.foodItems ?? []).forEach((fi) => { if (fi.foodId != null) out.push({ kind: "food", refId: fi.foodId, slot: fi.slot, quantity: fi.quantity ?? 1, unit: fi.unit, name: foodsById.get(fi.foodId)?.name ?? "Food" }); });
    return out;
  });
  const [selTags, setSelTags] = useState<TagDto[]>(editing?.tags ?? []);
  const [newTag, setNewTag] = useState("");
  const [addOpen, setAddOpen] = useState(false);
  const [addSlot, setAddSlot] = useState(MEAL_SLOTS[0]);
  const [addTab, setAddTab] = useState<"meals" | "foods">("meals");
  const [addQuery, setAddQuery] = useState("");

  const entryMacros = (e: BuildEntry) => {
    if (e.kind === "meal") { const t = mealSummaries.get(e.refId)?.totals; return t ?? { kcal: 0, protein: 0, carbs: 0, fat: 0 }; }
    return foodMacros(foodsById.get(e.refId), e.quantity, e.unit);
  };
  const totals = entries.reduce((a, e) => { const m = entryMacros(e); return { kcal: a.kcal + m.kcal, p: a.p + m.protein, c: a.c + m.carbs, f: a.f + m.fat }; }, { kcal: 0, p: 0, c: 0, f: 0 });

  const has = (kind: string, refId: number, slot: string) => entries.some((e) => e.kind === kind && e.refId === refId && e.slot === slot);
  const addMeal = (m: MealDto) => { if (m.id != null && !has("meal", m.id, addSlot)) setEntries((p) => [...p, { kind: "meal", refId: m.id!, slot: addSlot, quantity: 1, unit: "GRAM", name: m.name }]); };
  const addFood = (f: FoodDto) => { if (f.id != null && !has("food", f.id, addSlot)) setEntries((p) => [...p, { kind: "food", refId: f.id!, slot: addSlot, quantity: defaultQtyFor(f.unit ?? "GRAM"), unit: f.unit ?? "GRAM", name: f.name }]); };
  const setQty = (idx: number, q: number) => setEntries((p) => p.map((e, i) => i === idx ? { ...e, quantity: q } : e));
  const removeEntry = (idx: number) => setEntries((p) => p.filter((_, i) => i !== idx));
  const toggleTag = (t: TagDto) => setSelTags((p) => p.some((x) => x.id === t.id) ? p.filter((x) => x.id !== t.id) : [...p, t]);
  const shownTags = [...availableTags, ...selTags].filter((t, i, a) => a.findIndex((x) => x.id === t.id) === i);

  const groupedSlots = slotOrderLocal(entries.map((e) => e.slot));
  const canSave = name.trim() !== "" && entries.length > 0;
  const doSave = () => onSave(name, entries.map((e) => ({ kind: e.kind, refId: e.refId, slot: e.slot, quantity: e.quantity, unit: e.unit as DietEntryInput["unit"] })), selTags.map((t) => t.id));

  return (
    <div className="fixed inset-0 z-50 flex flex-col" style={{ background: C.bg }}>
      <div className="flex-none flex items-center gap-2 px-3 pt-3 pb-2">
        <button onClick={onClose} className="text-[20px]" style={{ color: C.ink }}>✕</button>
        <span className="text-[17px] font-semibold" style={{ color: C.ink }}>{editing ? "Edit diet" : "New diet"}</span>
        <span className="flex-1" />
        {editing && <button onClick={onDelete} className="text-[13px] font-semibold" style={{ color: C.danger }}>Delete</button>}
      </div>

      {!addOpen ? (
        <>
          <div className="flex-1 overflow-y-auto px-4">
            <label className="block text-[11px] font-semibold mb-[5px] mt-2" style={{ color: C.muted3 }}>Diet name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. High-protein day"
              className="w-full border rounded-[11px] px-3 py-[11px] text-[13px] mb-[14px]" style={{ border: "1.5px solid #dfe6e8", color: C.ink }} />

            <div className="flex items-baseline mb-2"><span className="text-[11px] font-semibold" style={{ color: C.muted3 }}>Tags</span><span className="text-[11px] ml-1" style={{ color: C.muted2 }}>· optional</span></div>
            {shownTags.length > 0 && (
              <div className="flex flex-wrap gap-[6px] mb-2">
                {shownTags.map((t) => {
                  const on = selTags.some((x) => x.id === t.id);
                  return <button key={t.id} onClick={() => toggleTag(t)} className="text-[11px] font-semibold rounded-full px-[11px] py-[6px]"
                    style={{ color: on ? "#fff" : C.muted3, background: on ? C.teal : "transparent", border: `1.5px solid ${on ? C.teal : "#dfe6e8"}` }}>{t.name}</button>;
                })}
              </div>
            )}
            <div className="flex gap-2 mb-4">
              <input value={newTag} onChange={(e) => setNewTag(e.target.value)} placeholder="New tag e.g. chicken-based"
                className="flex-1 rounded-[10px] px-3 py-[9px] text-[12px]" style={{ background: C.bgAlt, color: C.ink, border: "none", outline: "none" }} />
              <button disabled={!newTag.trim()} onClick={async () => { const t = await onCreateTag(newTag.trim()); if (t && !selTags.some((x) => x.id === t.id)) setSelTags((p) => [...p, t]); setNewTag(""); }}
                className="rounded-[9px] px-[14px] text-[12px] font-semibold" style={{ border: `1.5px solid ${newTag.trim() ? C.teal : "#dfe6e8"}`, color: newTag.trim() ? C.teal : C.muted2 }}>Add</button>
            </div>

            <div className="flex items-center mb-2"><span className="text-[12.5px] font-semibold" style={{ color: C.ink }}>Plan</span><span className="flex-1" /><span className="text-[12px] font-semibold" style={{ color: C.teal, fontFamily: mono }}>{Math.round(totals.kcal)} kcal</span></div>

            {entries.length === 0 ? (
              <div className="rounded-[12px] text-center text-[11.5px] py-[18px]" style={{ border: "1.5px dashed #cdd7da", color: C.muted2 }}>Nothing planned yet — add meals or foods into slots.</div>
            ) : groupedSlots.map((slot) => (
              <div key={slot} className="mb-1">
                <div className="text-[9px] font-semibold mt-[10px] mb-[2px]" style={{ color: C.teal }}>{slot.toUpperCase()}</div>
                {entries.map((e, idx) => e.slot !== slot ? null : (
                  <div key={idx} className="flex items-center gap-2 py-[8px]" style={{ borderBottom: `1px solid ${C.bgAlt}` }}>
                    <div className="flex-1 min-w-0">
                      <div className="text-[12px] font-semibold truncate" style={{ color: C.ink }}>{e.kind === "meal" ? `🍲 ${e.name}` : e.name}</div>
                      <div className="text-[10px]" style={{ color: C.muted2, fontFamily: mono }}>{Math.round(entryMacros(e).kcal)} kcal</div>
                    </div>
                    {e.kind === "food" && <QtyInput value={e.quantity} unit={e.unit} onChange={(n) => setQty(idx, n)} />}
                    <button onClick={() => removeEntry(idx)} className="text-[13px]" style={{ color: "#c4ccd1" }}>✕</button>
                  </div>
                ))}
              </div>
            ))}

            <button onClick={() => setAddOpen(true)} className="w-full rounded-[12px] py-[12px] text-[12.5px] font-semibold mt-3" style={{ border: "1.5px dashed #cdd7da", color: C.teal }}>＋ Add meal or food</button>
            {entries.length > 0 && <div className="text-center text-[10.5px] mt-3" style={{ color: C.muted2, fontFamily: mono }}>{macroLine(totals.p, totals.c, totals.f)}</div>}
            <div className="h-4" />
          </div>
          <div className="flex-none p-4">
            <button onClick={doSave} disabled={!canSave || saving} className="w-full rounded-[12px] py-[14px] text-[13px] font-semibold" style={{ background: canSave ? C.teal : C.bgAlt, color: canSave ? "#fff" : C.muted2 }}>
              {saving ? "Saving…" : editing ? "Save changes" : "Save diet"}
            </button>
          </div>
        </>
      ) : (
        <div className="flex-1 overflow-y-auto px-4">
          <div className="flex items-center gap-2 py-2"><button onClick={() => setAddOpen(false)} className="text-[20px]" style={{ color: C.ink }}>‹</button><span className="text-[15px] font-semibold" style={{ color: C.ink }}>Add to diet</span></div>
          <label className="block text-[11px] font-semibold mb-[5px]" style={{ color: C.muted3 }}>Slot</label>
          <div className="flex flex-wrap gap-[6px] mb-3">
            {MEAL_SLOTS.map((s) => <button key={s} onClick={() => setAddSlot(s)} className="text-[11px] font-semibold rounded-full px-[11px] py-[6px]" style={{ color: addSlot === s ? "#fff" : C.muted3, background: addSlot === s ? C.teal : "transparent", border: `1.5px solid ${addSlot === s ? C.teal : "#dfe6e8"}` }}>{s}</button>)}
          </div>
          <div className="flex gap-2 mb-3">
            {(["meals", "foods"] as const).map((t) => <button key={t} onClick={() => setAddTab(t)} className="flex-1 rounded-[9px] py-[9px] text-[12px] font-semibold capitalize" style={{ background: addTab === t ? C.ink : C.bgAlt, color: addTab === t ? "#fff" : C.muted3 }}>{t}</button>)}
          </div>
          <div className="flex items-center gap-2 rounded-[11px] px-3 py-[10px] mb-2" style={{ background: C.bgAlt }}>
            <span style={{ color: C.muted2 }}>⌕</span>
            <input value={addQuery} onChange={(e) => setAddQuery(e.target.value)} placeholder={`Search your ${addTab}…`} className="flex-1 bg-transparent border-none outline-none text-[13px]" style={{ color: C.ink }} />
          </div>
          <div className="pb-24">
            {addTab === "meals"
              ? meals.filter((m) => !addQuery || m.name.toLowerCase().includes(addQuery.toLowerCase())).map((m) => {
                  const added = m.id != null && has("meal", m.id, addSlot);
                  const kcal = m.id != null ? Math.round(mealSummaries.get(m.id)?.totals.kcal ?? 0) : 0;
                  return <PickRow key={m.id} name={m.name} meta={`${kcal} kcal · ${(m.items ?? []).length} items`} added={added} onAdd={() => addMeal(m)} />;
                })
              : foods.filter((f) => !addQuery || f.name.toLowerCase().includes(addQuery.toLowerCase())).map((f) => {
                  const added = f.id != null && has("food", f.id, addSlot);
                  return <PickRow key={f.id} name={f.name} meta={`${f.caloriesPer100} kcal / 100g`} added={added} onAdd={() => addFood(f)} />;
                })}
          </div>
          <div className="fixed bottom-0 inset-x-0 p-4" style={{ background: C.bg }}>
            <button onClick={() => { setAddOpen(false); setAddQuery(""); }} className="w-full rounded-[12px] py-[13px] text-[12.5px] font-semibold" style={{ background: C.teal, color: "#fff" }}>Done · {entries.length} added</button>
          </div>
        </div>
      )}
    </div>
  );
}

function PickRow({ name, meta, added, onAdd }: { name: string; meta: string; added: boolean; onAdd: () => void }) {
  return (
    <div className="flex items-center gap-2 py-[10px]" style={{ borderBottom: `1px solid ${C.bgAlt}` }}>
      <div className="flex-1 min-w-0"><div className="text-[12.5px] font-semibold truncate" style={{ color: C.ink }}>{name}</div><div className="text-[10.5px]" style={{ color: C.muted }}>{meta}</div></div>
      <button onClick={onAdd} disabled={added} className="rounded-[9px] px-3 py-[7px] text-[11.5px] font-semibold" style={{ border: `1.5px solid #dfe6e8`, color: added ? C.muted2 : C.tealDim }}>{added ? "✓ Added" : "+ Add"}</button>
    </div>
  );
}

function QtyInput({ value, unit, onChange }: { value: number; unit: string; onChange: (n: number) => void }) {
  const [text, setText] = useState(num(value));
  return (
    <div className="flex items-center gap-1 rounded-[8px] px-[8px] py-[3px]" style={{ border: `1px solid #dfe6e8` }}>
      <input value={text} inputMode="decimal" onChange={(e) => { const t = e.target.value.replace(/[^0-9.]/g, ""); setText(t); const n = parseFloat(t); if (!isNaN(n)) onChange(n); }}
        className="w-[42px] bg-transparent border-none outline-none text-right text-[12px] font-semibold" style={{ color: C.ink, fontFamily: mono }} />
      <span className="text-[10px]" style={{ color: C.muted2, fontFamily: mono }}>{unitLabel(unit)}</span>
    </div>
  );
}

function slotOrderLocal(present: string[]): string[] {
  const distinct = Array.from(new Set(present));
  return [...MEAL_SLOTS.filter((s) => distinct.includes(s)), ...distinct.filter((s) => !MEAL_SLOTS.includes(s))];
}

function DietsPageInner() {
  const d = useDiets();
  const [sortOpen, setSortOpen] = useState(false);

  if (d.builderOpen) {
    return <DietBuilder editing={d.editing} meals={d.meals} foods={d.foods} foodsById={d.foodsById} mealSummaries={d.mealSummaries}
      availableTags={d.availableTags} saving={d.saving} onCreateTag={d.createTag} onSave={d.saveDiet}
      onDelete={() => { if (d.editing) { void d.handleDelete(d.editing); d.closeBuilder(); } }} onClose={d.closeBuilder} />;
  }

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div className="flex-none px-[14px] pt-[8px] pb-[12px]" style={{ background: C.surface, borderBottom: `1px solid #eef1f3` }}>
        <div className="flex items-center gap-2 mb-[10px]"><span className="flex-1 text-[17px] font-semibold" style={{ color: C.ink }}>Diets</span><span className="text-[11px]" style={{ color: C.muted }}>{d.totalCount} saved</span></div>
        <div className="flex items-center gap-2 rounded-[11px] px-3 py-[9px] mb-[11px]" style={{ background: C.bgAlt }}>
          <span style={{ color: C.muted2 }}>⌕</span>
          <input value={d.query} onChange={(e) => d.setQuery(e.target.value)} placeholder="Search your diets…" className="flex-1 bg-transparent border-none outline-none text-[12.5px]" style={{ color: C.ink }} />
        </div>
        <div className="flex items-center gap-2">
          <div className="relative">
            <button onClick={() => setSortOpen((o) => !o)} className="flex items-center gap-[6px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold" style={{ background: C.bgAlt, color: C.ink }}>↕ {SORT_LABELS[d.sort]}<span style={{ color: C.muted2, fontSize: 9 }}>▾</span></button>
            {sortOpen && (<><div className="fixed inset-0 z-10" onClick={() => setSortOpen(false)} />
              <div className="absolute left-0 top-[38px] z-20 rounded-[12px] p-[6px] min-w-[158px]" style={{ background: C.surface, border: `1px solid ${C.border}`, boxShadow: "0 8px 24px rgba(0,0,0,.14)" }}>
                {(["recent", "name", "calories", "protein"] as DietSort[]).map((s) => <button key={s} onClick={() => { d.setSort(s); setSortOpen(false); }} className="w-full flex items-center justify-between px-[10px] py-[9px] rounded-[8px] text-[12px] font-semibold" style={{ color: s === d.sort ? C.teal : C.ink }}>{SORT_LABELS[s]}{s === d.sort && <span style={{ color: C.teal }}>✓</span>}</button>)}
              </div></>)}
          </div>
          <button onClick={() => d.setFavOnly(!d.favOnly)} className="flex items-center gap-[5px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold" style={{ background: d.favOnly ? "oklch(0.62 0.09 210 / .12)" : C.bgAlt, color: d.favOnly ? C.teal : C.ink }}>{d.favOnly ? "★" : "☆"} {d.favCount}</button>
          <span className="flex-1" />
          <div className="flex rounded-[9px] overflow-hidden" style={{ border: `1px solid #e2e7ea` }}>
            {(["list", "compact"] as const).map((v) => <button key={v} onClick={() => d.setViewMode(v)} className="w-[34px] h-[32px] flex items-center justify-center text-[15px]" style={{ background: d.viewMode === v ? C.surface : "transparent", color: d.viewMode === v ? C.ink : C.muted2, borderRight: v === "list" ? "1px solid #e2e7ea" : "none" }}>{v === "list" ? "☰" : "≣"}</button>)}
          </div>
        </div>
        {d.allTagNames.length > 0 && (
          <div className="flex gap-[6px] mt-[10px] overflow-x-auto">
            <TagFilterChip label="All" on={d.tagFilter === null} onClick={() => d.setTagFilter(null)} />
            {d.allTagNames.map((t) => <TagFilterChip key={t} label={t} on={d.tagFilter === t} onClick={() => d.setTagFilter(d.tagFilter === t ? null : t)} />)}
          </div>
        )}
      </div>

      <div className="flex-1 overflow-y-auto px-[14px] pt-3" style={{ paddingBottom: 120 }}>
        {d.loading && <div className="text-center py-12 text-[12px]" style={{ color: C.muted }}>Loading…</div>}
        {d.error && <div className="text-center py-12 text-[12px]" style={{ color: C.danger }}>{d.error}</div>}
        {!d.loading && d.diets.length === 0 && (
          <div className="text-center py-16"><div className="text-[34px]">🥗</div><div className="text-[14px] font-semibold mt-2" style={{ color: C.muted3 }}>No diets yet</div><div className="text-[12px] mt-1" style={{ color: C.muted2 }}>Tap + to build a day-plan from your meals & foods.</div></div>
        )}
        {d.viewMode === "list" && d.diets.map((v) => (
          <DietCard key={v.diet.id} v={v} expanded={d.expandedIds.has(v.diet.id!)}
            onToggle={() => d.toggleExpand(v.diet.id!)} onFav={() => void d.handleToggleFav(v.diet)}
            onEdit={() => d.openEdit(v.diet)} onDelete={() => void d.handleDelete(v.diet)} />
        ))}
        {d.viewMode === "compact" && d.diets.length > 0 && (
          <div className="rounded-[12px] overflow-hidden" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            {d.diets.map((v, i) => (
              <DietCard key={v.diet.id} v={v} compact isLast={i === d.diets.length - 1}
                expanded={d.expandedIds.has(v.diet.id!)}
                onToggle={() => d.toggleExpand(v.diet.id!)} onFav={() => void d.handleToggleFav(v.diet)}
                onEdit={() => d.openEdit(v.diet)} onDelete={() => void d.handleDelete(v.diet)} />
            ))}
          </div>
        )}
      </div>

      <button onClick={d.openNew} className="fixed bottom-[68px] right-4 z-40 w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg" style={{ background: C.teal, boxShadow: `0 6px 18px oklch(0.62 0.09 210 / .45)` }}>+</button>
      <NutritionNav />
    </div>
  );
}

function TagFilterChip({ label, on, onClick }: { label: string; on: boolean; onClick: () => void }) {
  return <button onClick={onClick} className="flex-none text-[11px] font-semibold rounded-full px-[12px] py-[5px]" style={{ background: on ? C.ink : C.bgAlt, color: on ? "#fff" : C.muted3 }}>{label}</button>;
}

export default function DietsPage() {
  return <AuthGuard><DietsPageInner /></AuthGuard>;
}
