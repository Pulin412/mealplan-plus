"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useFoods } from "@/hooks/useFoods";
import { BottomSheet, SheetField } from "@/components/ui/BottomSheet";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { lookupBarcode, type ScannedProduct } from "@/lib/api/barcode";
import type { FoodDto } from "@/lib/api/foods";
import type { FoodSort } from "@/types/food";
import { DEFAULT_FOOD_CATEGORIES } from "@/lib/foodCategories";
import { FOOD_UNITS, unitLabel, isCountUnit } from "@/lib/foodUnits";

// ─── token shortcuts ─────────────────────────────────────────────────────────
const C = {
  ink:     "#14181b",
  muted:   "#8a949b",
  muted2:  "#9aa4aa",
  muted3:  "#5b666e",
  border:  "#eaeef0",
  surface: "#ffffff",
  bg:      "#f7f9fa",
  bgAlt:   "#f2f4f5",
  teal:    "oklch(0.62 0.09 210)",
  tealDim: "oklch(0.55 0.09 220)",
  green:   "oklch(0.66 0.13 150)",
};

const SORT_LABELS: Record<FoodSort, string> = {
  recent: "Recent", name: "Name", calories: "Calories", protein: "Protein",
};

// ─── Macro text helper ────────────────────────────────────────────────────────
function macroText(f: FoodDto) {
  return `P: ${f.proteinPer100}g  C: ${f.carbsPer100}g  F: ${f.fatPer100}g  (per 100g)`;
}

// ─── Verified badge ───────────────────────────────────────────────────────────
function VerifiedBadge({ verified }: { verified: boolean }) {
  return verified
    ? <span className="text-[9px] font-semibold" style={{ color: C.green }}>✓ Verified</span>
    : <span className="text-[9px] font-semibold" style={{ color: C.muted2 }}>Custom</span>;
}

// ─── Star button ──────────────────────────────────────────────────────────────
function StarBtn({ active, onClick }: { active: boolean; onClick: (e: React.MouseEvent) => void }) {
  return (
    <button onClick={onClick} className="flex-none text-[15px] leading-none px-0.5"
      style={{ color: active ? C.teal : C.muted2 }}>
      {active ? "★" : "☆"}
    </button>
  );
}

// ─── List view card ───────────────────────────────────────────────────────────
function FoodListCard({ food, expanded, onToggleExpand, onToggleFav, onDelete }: {
  food: FoodDto; expanded: boolean;
  onToggleExpand: () => void; onToggleFav: (e: React.MouseEvent) => void; onDelete: (e: React.MouseEvent) => void;
}) {
  return (
    <div onClick={onToggleExpand} className="cursor-pointer rounded-[12px] mb-[7px] px-3 py-[9px]"
      style={{ background: C.surface, border: `1px solid ${C.border}` }}>
      <div className="flex items-center gap-[10px]">
        <div className="flex-1 min-w-0">
          <div className="text-[12.5px] font-semibold truncate" style={{ color: C.ink }}>{food.name}</div>
          <div className="text-[10.5px] truncate mt-0.5" style={{ color: C.muted2 }}>
            {[food.brand, "per 100g"].filter(Boolean).join(" · ")}
          </div>
        </div>
        <StarBtn active={food.isFavorite} onClick={onToggleFav} />
        <span className="flex-none text-[11.5px] font-semibold tabular-nums" style={{ color: C.ink, fontFamily: "'DM Mono', monospace" }}>
          {food.caloriesPer100}
        </span>
        <button onClick={onDelete} className="flex-none text-[14px] leading-none" style={{ color: "#c4ccd1" }}>✕</button>
      </div>
      {expanded && (
        <div className="flex items-center gap-[10px] mt-[9px] pt-[9px]" style={{ borderTop: `1px solid ${C.bgAlt}` }}>
          <span className="text-[10.5px]" style={{ color: C.muted3, fontFamily: "'DM Mono', monospace" }}>{macroText(food)}</span>
          <span className="flex-1" />
          <VerifiedBadge verified={food.verified} />
        </div>
      )}
    </div>
  );
}

// ─── Compact view row ─────────────────────────────────────────────────────────
function FoodCompactRow({ food, expanded, onToggleExpand, onToggleFav, onDelete, isLast }: {
  food: FoodDto; expanded: boolean; isLast: boolean;
  onToggleExpand: () => void; onToggleFav: (e: React.MouseEvent) => void; onDelete: (e: React.MouseEvent) => void;
}) {
  return (
    <div onClick={onToggleExpand} className="cursor-pointer px-[11px] py-[7px]"
      style={{ borderBottom: isLast ? "none" : `1px solid ${C.bgAlt}` }}>
      <div className="flex items-center gap-[9px]">
        <div className="flex-1 min-w-0">
          <div className="text-[12px] font-semibold truncate" style={{ color: C.ink }}>{food.name}</div>
          <div className="text-[9.5px] truncate" style={{ color: "#a2abb1" }}>per 100g</div>
        </div>
        <StarBtn active={food.isFavorite} onClick={(e) => { e.stopPropagation(); onToggleFav(e); }} />
        <span className="flex-none min-w-[42px] text-right text-[12px] font-semibold tabular-nums" style={{ color: C.ink, fontFamily: "'DM Mono', monospace" }}>
          {food.caloriesPer100}<span className="text-[8px] font-normal" style={{ color: "#a2abb1", fontFamily: "system-ui" }}> kcal</span>
        </span>
        <span className="flex-none text-[11px] leading-none" style={{ color: "#c4ccd1" }}>{expanded ? "▲" : "▼"}</span>
      </div>
      {expanded && (
        <div className="flex items-center gap-[10px] mt-[7px]">
          <span className="text-[10px]" style={{ color: C.muted3, fontFamily: "'DM Mono', monospace" }}>{macroText(food)}</span>
          <span className="flex-1" />
          <VerifiedBadge verified={food.verified} />
          <button onClick={(e) => { e.stopPropagation(); onDelete(e); }}
            className="flex-none text-[12px] leading-none" style={{ color: "#c4ccd1" }}>
            ✕ Remove
          </button>
        </div>
      )}
    </div>
  );
}

// ─── Speed-dial FAB ───────────────────────────────────────────────────────────
function FoodsFab({ open, onToggle, onManual, onOnline, onBarcode }: {
  open: boolean; onToggle: () => void;
  onManual: () => void; onOnline: () => void; onBarcode: () => void;
}) {
  const options = [
    { label: "Enter manually", icon: "✎", bg: "oklch(0.95 0.02 210)", color: "oklch(0.5 0.09 210)", action: onManual },
    { label: "Search online",  icon: "⌕", bg: "oklch(0.95 0.02 255)", color: "oklch(0.5 0.1 255)",  action: onOnline },
    { label: "Scan barcode",   icon: "▦", bg: "oklch(0.95 0.02 150)", color: "oklch(0.5 0.1 150)",  action: onBarcode },
  ];
  return (
    <>
      {open && (
        <div className="fixed inset-0 z-30" style={{ background: "rgba(247,249,250,.55)" }}
          onClick={onToggle} />
      )}
      <div className="fixed bottom-[68px] right-4 z-40 flex flex-col gap-3 items-end">
        {open && options.map((opt) => (
          <button key={opt.label} onClick={opt.action}
            className="flex items-center gap-[10px] cursor-pointer">
            <span className="rounded-[9px] px-[11px] py-[7px] text-[12px] font-semibold shadow-md"
              style={{ background: C.surface, border: `1px solid ${C.border}`, color: C.ink }}>
              {opt.label}
            </span>
            <span className="w-[42px] h-[42px] rounded-[12px] flex items-center justify-center text-[17px] shadow-md flex-none"
              style={{ background: opt.bg, color: opt.color }}>
              {opt.icon}
            </span>
          </button>
        ))}
        <button onClick={onToggle}
          className="w-14 h-14 rounded-full flex items-center justify-center text-white text-[28px] font-light shadow-lg"
          style={{ background: C.teal, boxShadow: `0 6px 18px oklch(0.62 0.09 210 / .45)` }}>
          {open ? "✕" : "+"}
        </button>
      </div>
    </>
  );
}

// ─── Manual entry sheet ───────────────────────────────────────────────────────
function ManualSheet({ open, form, isSaveEnabled, saving, onField, onSave, onClose }: {
  open: boolean; form: ReturnType<typeof useFoods>["form"];
  isSaveEnabled: boolean; saving: boolean;
  onField: (k: keyof typeof form, v: string) => void;
  onSave: () => void; onClose: () => void;
}) {
  return (
    <BottomSheet open={open} onClose={onClose} title="New food">
      <SheetField label="Name" placeholder="e.g. Overnight oats"
        value={form.name} onChange={(v) => onField("name", v)} className="mb-[13px]" />
      {/* Measured in — count units (piece/cup/tbsp/tsp) convert to grams via the factor below */}
      <div className="mb-[13px]">
        <label className="block text-[11px] font-semibold mb-[6px]" style={{ color: C.muted }}>Measured in</label>
        <div className="flex flex-wrap gap-[6px]">
          {FOOD_UNITS.map((u) => {
            const on = form.unit === u;
            return (
              <button key={u} type="button" onClick={() => onField("unit", u)}
                className="rounded-full px-3 py-[6px] text-[12px] font-semibold"
                style={{
                  background: on ? C.teal : "transparent",
                  color:      on ? "#fff" : C.muted3,
                  border:     `1.5px solid ${on ? C.teal : C.border}`,
                }}>
                {unitLabel(u)}
              </button>
            );
          })}
        </div>
      </div>
      {isCountUnit(form.unit) && (
        <SheetField label={`Grams per ${unitLabel(form.unit)}`} placeholder="e.g. 50" inputMode="numeric"
          value={form.gramsPerUnit} onChange={(v) => onField("gramsPerUnit", v)} className="mb-[13px]" />
      )}
      <SheetField label="Serving size" placeholder="e.g. 250 g"
        value={form.servingLabel} onChange={(v) => onField("servingLabel", v)} className="mb-[13px]" />
      <SheetField label="Calories (kcal)" placeholder="0" inputMode="numeric"
        value={form.kcal} onChange={(v) => onField("kcal", v)} className="mb-[13px]" />
      <div className="flex gap-[10px] mb-5">
        <SheetField label="Protein" placeholder="g" inputMode="numeric"
          value={form.protein} onChange={(v) => onField("protein", v)} className="flex-1" />
        <SheetField label="Carbs" placeholder="g" inputMode="numeric"
          value={form.carbs} onChange={(v) => onField("carbs", v)} className="flex-1" />
        <SheetField label="Fat" placeholder="g" inputMode="numeric"
          value={form.fat} onChange={(v) => onField("fat", v)} className="flex-1" />
      </div>
      <SheetField label="Category" placeholder="Pick one below or type your own"
        value={form.category} onChange={(v) => onField("category", v)} className="mb-[8px]" />
      <div className="flex flex-wrap gap-[6px] mb-5">
        {DEFAULT_FOOD_CATEGORIES.map((cat) => {
          const on = form.category === cat;
          return (
            <button key={cat} type="button" onClick={() => onField("category", on ? "" : cat)}
              className="rounded-full px-3 py-[6px] text-[12px] font-semibold"
              style={{
                background: on ? C.teal : "transparent",
                color:      on ? "#fff" : C.muted3,
                border:     `1.5px solid ${on ? C.teal : C.border}`,
              }}>
              {cat}
            </button>
          );
        })}
      </div>
      <button onClick={onSave} disabled={!isSaveEnabled || saving}
        className="w-full rounded-[12px] py-[14px] text-[13px] font-semibold transition-colors"
        style={{
          background: isSaveEnabled ? C.teal : C.bgAlt,
          color:      isSaveEnabled ? "#fff" : C.muted2,
          border: "none",
        }}>
        {saving ? "Saving…" : "Save food"}
      </button>
    </BottomSheet>
  );
}

// ─── Online search sheet ──────────────────────────────────────────────────────
function OnlineSheet({ open, query, results, loading, onQuery, onSearch, onAdd, onClose }: {
  open: boolean; query: string; results: FoodDto[]; loading: boolean;
  onQuery: (q: string) => void; onSearch: () => void; onAdd: (f: FoodDto) => void; onClose: () => void;
}) {
  return (
    <BottomSheet open={open} onClose={onClose} title="Search online">
      <div className="flex items-center gap-2 rounded-[11px] px-3 py-[10px] mb-[14px]"
        style={{ background: C.bgAlt }}>
        <span style={{ color: C.muted2, fontSize: 14 }}>⌕</span>
        <input value={query} onChange={(e) => onQuery(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && onSearch()}
          placeholder="Search foods & brands…"
          className="flex-1 bg-transparent border-none outline-none text-[13px]"
          style={{ color: C.ink }} />
      </div>
      <div className="overflow-y-auto">
        {loading && <p className="text-center py-4 text-[12px]" style={{ color: C.muted }}>Searching…</p>}
        {!loading && results.length === 0 && query && (
          <p className="text-center py-4 text-[12px]" style={{ color: C.muted2 }}>No results found.</p>
        )}
        {results.map((r) => (
          <div key={r.id} className="flex items-center gap-[10px] py-[11px]"
            style={{ borderBottom: `1px solid #f0f2f3` }}>
            <div className="flex-1 min-w-0">
              <div className="text-[12.5px] font-semibold" style={{ color: C.ink }}>{r.name}</div>
              <div className="text-[11px] mt-0.5" style={{ color: C.muted }}>
                {r.brand} · per 100g · {r.caloriesPer100} kcal
              </div>
              <div className="text-[10px] mt-0.5" style={{ color: "#a2abb1", fontFamily: "'DM Mono', monospace" }}>
                {macroText(r)}
              </div>
            </div>
            <button onClick={() => { onAdd(r); onClose(); }}
              className="flex-none rounded-[9px] px-3 py-[7px] text-[11.5px] font-semibold"
              style={{ border: `1.5px solid #dfe6e8`, background: C.surface, color: C.tealDim }}>
              + Add
            </button>
          </div>
        ))}
      </div>
    </BottomSheet>
  );
}

// ─── Barcode scanner ──────────────────────────────────────────────────────────
// Live scan via @zxing/browser (works on iOS Safari, unlike the native BarcodeDetector) → Open Food
// Facts lookup → product card → create in the user's foods. Manual entry as a fallback.
type ScanPhase = "scanning" | "looking_up" | "result" | "not_found";

function BarcodeSheet({ open, onClose, onAdd, saving }: {
  open: boolean; onClose: () => void; onAdd: (p: ScannedProduct) => void; saving: boolean;
}) {
  const [phase, setPhase] = useState<ScanPhase>("scanning");
  const [product, setProduct] = useState<ScannedProduct | null>(null);
  const [message, setMessage] = useState("");
  const [camError, setCamError] = useState(false);
  const [manual, setManual] = useState(false);
  const [code, setCode] = useState("");
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    if (open) { setPhase("scanning"); setProduct(null); setMessage(""); setCamError(false); setManual(false); setCode(""); }
  }, [open]);

  const lookUp = useCallback(async (raw: string) => {
    setPhase("looking_up");
    try {
      const p = await lookupBarcode(raw);
      if (p) { setProduct(p); setPhase("result"); }
      else { setMessage(`No product found for ${raw}`); setPhase("not_found"); }
    } catch {
      setMessage("Lookup failed — check your connection."); setPhase("not_found");
    }
  }, []);

  // ML-free browser barcode decode via ZXing (dynamic import keeps it out of SSR).
  useEffect(() => {
    if (!open || phase !== "scanning") return;
    let controls: { stop: () => void } | undefined;
    let cancelled = false;
    (async () => {
      try {
        const { BrowserMultiFormatReader } = await import("@zxing/browser");
        const reader = new BrowserMultiFormatReader();
        controls = await reader.decodeFromVideoDevice(undefined, videoRef.current ?? undefined, (result, _err, ctrls) => {
          if (result && !cancelled) { ctrls.stop(); lookUp(result.getText()); }
        });
        if (cancelled) controls?.stop();
      } catch {
        setCamError(true);
      }
    })();
    return () => { cancelled = true; controls?.stop(); };
  }, [open, phase, lookUp]);

  const restart = () => { setProduct(null); setManual(false); setCode(""); setMessage(""); setPhase("scanning"); };

  return (
    <BottomSheet open={open} onClose={onClose} title="Scan barcode">
      {phase === "scanning" && (
        <>
          <div className="rounded-[14px] overflow-hidden mb-3 relative" style={{ height: 260, background: "#14181b" }}>
            {!camError ? (
              <>
                <video ref={videoRef} className="w-full h-full object-cover" muted playsInline />
                <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 rounded-[10px]"
                  style={{ width: "72%", height: 92, border: `2px solid ${C.teal}` }} />
              </>
            ) : (
              <div className="w-full h-full flex flex-col items-center justify-center gap-2 px-6 text-center">
                <span className="text-[13px]" style={{ color: "rgba(255,255,255,.65)" }}>Camera unavailable</span>
                <span className="text-[11px]" style={{ color: "rgba(255,255,255,.4)" }}>Allow camera access, or enter the code below.</span>
              </div>
            )}
          </div>
          <p className="text-[12px] text-center mb-2" style={{ color: C.muted }}>
            {camError ? "Enter the barcode number" : "Point the camera at a product barcode"}
          </p>
          {!manual ? (
            <button onClick={() => setManual(true)} className="w-full text-[13px] font-semibold py-2"
              style={{ color: C.teal, background: "none", border: "none" }}>
              Enter barcode manually
            </button>
          ) : (
            <div className="flex gap-2 items-center">
              <input value={code} onChange={(e) => setCode(e.target.value.replace(/\D/g, ""))}
                inputMode="numeric" placeholder="Barcode number"
                className="flex-1 rounded-[10px] px-3 py-2 text-[13px]"
                style={{ border: `1px solid ${C.border}`, background: C.surface, color: C.ink }} />
              <button onClick={() => code && lookUp(code)} disabled={!code}
                className="text-[13px] font-semibold px-3 py-2"
                style={{ color: C.teal, background: "none", border: "none", opacity: code ? 1 : 0.4 }}>
                Look up
              </button>
            </div>
          )}
        </>
      )}

      {phase === "looking_up" && (
        <div className="flex items-center justify-center" style={{ height: 200 }}>
          <span className="text-[13px]" style={{ color: C.muted }}>Looking up product…</span>
        </div>
      )}

      {phase === "result" && product && (
        <>
          <div className="rounded-[12px] p-4 mb-3" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            <div className="text-[15px] font-semibold" style={{ color: C.ink }}>{product.name}</div>
            {product.brand && <div className="text-[12px] mb-3" style={{ color: C.muted2 }}>{product.brand}</div>}
            <div className="text-[10.5px] font-bold mb-1.5" style={{ color: C.muted2 }}>PER 100 G</div>
            <div className="flex gap-5">
              {([["kcal", product.kcal], ["P", product.protein], ["C", product.carbs], ["F", product.fat]] as const).map(([l, v]) => (
                <div key={l} className="flex flex-col items-center">
                  <span className="text-[15px] font-semibold" style={{ color: C.ink }}>{v % 1 === 0 ? v : v.toFixed(1)}</span>
                  <span className="text-[10.5px]" style={{ color: C.muted2 }}>{l}</span>
                </div>
              ))}
            </div>
          </div>
          <button onClick={() => onAdd(product)} disabled={saving}
            className="w-full rounded-[12px] py-[13px] text-[14px] font-semibold mb-2"
            style={{ background: C.teal, color: "#fff", border: "none", opacity: saving ? 0.6 : 1 }}>
            {saving ? "Adding…" : "Add to my foods"}
          </button>
          <button onClick={restart} className="w-full text-[13px] font-semibold py-1"
            style={{ color: C.teal, background: "none", border: "none" }}>
            Scan another
          </button>
        </>
      )}

      {phase === "not_found" && (
        <div className="flex flex-col items-center gap-2 py-6 text-center">
          <span className="text-[13.5px]" style={{ color: C.ink }}>{message}</span>
          <span className="text-[12px]" style={{ color: C.muted2 }}>Try again, or add it manually from the ＋ menu.</span>
          <button onClick={restart} className="text-[13px] font-semibold mt-2"
            style={{ color: C.teal, background: "none", border: "none" }}>
            Scan again
          </button>
        </div>
      )}
    </BottomSheet>
  );
}

// ─── Sort dropdown ────────────────────────────────────────────────────────────
function SortDropdown({ current, open, onToggle, onPick }: {
  current: FoodSort; open: boolean; onToggle: () => void; onPick: (s: FoodSort) => void;
}) {
  const sorts: FoodSort[] = ["recent", "name", "calories", "protein"];
  return (
    <div className="relative">
      <button onClick={onToggle}
        className="flex items-center gap-[6px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold"
        style={{ background: C.bgAlt, color: C.ink }}>
        ↕ {SORT_LABELS[current]}<span style={{ color: C.muted2, fontSize: 9 }}>▾</span>
      </button>
      {open && (
        <>
          <div className="fixed inset-0 z-10" onClick={onToggle} />
          <div className="absolute left-0 top-[38px] z-20 rounded-[12px] p-[6px] min-w-[158px]"
            style={{ background: C.surface, border: `1px solid ${C.border}`, boxShadow: "0 8px 24px rgba(0,0,0,.14)" }}>
            {sorts.map((s) => (
              <button key={s} onClick={() => { onPick(s); onToggle(); }}
                className="w-full flex items-center justify-between px-[10px] py-[9px] rounded-[8px] text-[12px] font-semibold"
                style={{ color: s === current ? C.teal : C.ink, background: "transparent", border: "none" }}>
                {SORT_LABELS[s]}
                {s === current && <span style={{ color: C.teal, fontSize: 11 }}>✓</span>}
              </button>
            ))}
          </div>
        </>
      )}
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────
function FoodsPageInner() {
  const router = useRouter();
  const {
    foods, totalCount, favCount, loading, error,
    query, setQuery, sort, setSort, sortOpen, setSortOpen,
    viewMode, setViewMode, favOnly, setFavOnly,
    categoryFilter, setCategoryFilter, usedCategories,
    expandedIds, toggleExpand,
    handleToggleFav, handleDelete,
    fanOpen, setFanOpen, activeSheet, openSheet, closeSheet,
    form, updateForm, isSaveEnabled, saving, saveManual,
    onlineQuery, setOnlineQuery, onlineResults, onlineLoading, runOnlineSearch, addOnlineFood,
    addScannedFood,
  } = useFoods();

  const showFavEmpty = favOnly && foods.length === 0 && !loading;

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>

      {/* App bar */}
      <div className="flex-none px-[14px] pt-[8px] pb-[12px]" style={{ background: C.surface, borderBottom: `1px solid #eef1f3` }}>
        <div className="flex items-center gap-2 mb-[10px]">
          <button onClick={() => router.back()}
            className="text-[22px] leading-none mr-1" style={{ color: C.ink }}>‹</button>
          <span className="flex-1 text-[17px] font-semibold" style={{ color: C.ink }}>Food items</span>
          <span className="text-[11px]" style={{ color: C.muted }}>{totalCount} saved</span>
        </div>

        {/* Search */}
        <div className="flex items-center gap-2 rounded-[11px] px-3 py-[9px] mb-[11px]"
          style={{ background: C.bgAlt }}>
          <span style={{ color: C.muted2 }}>⌕</span>
          <input value={query} onChange={(e) => setQuery(e.target.value)}
            placeholder="Search your foods…"
            className="flex-1 bg-transparent border-none outline-none text-[12.5px]"
            style={{ color: C.ink }} />
          {query && (
            <button onClick={() => setQuery("")} className="text-[12px]" style={{ color: C.muted2 }}>✕</button>
          )}
        </div>

        {/* Toolbar */}
        <div className="flex items-center gap-2">
          <SortDropdown current={sort} open={sortOpen}
            onToggle={() => setSortOpen((o) => !o)}
            onPick={(s) => setSort(s)} />
          <button onClick={() => setFavOnly((f) => !f)}
            className="flex items-center gap-[5px] rounded-[9px] px-[11px] py-[7px] text-[11.5px] font-semibold transition-colors"
            style={{
              background: favOnly ? "oklch(0.62 0.09 210 / .12)" : C.bgAlt,
              color:      favOnly ? C.teal : C.ink,
              border:     favOnly ? `1px solid oklch(0.62 0.09 210 / .3)` : "none",
            }}>
            {favOnly ? "★" : "☆"} {favCount}
          </button>
          <span className="flex-1" />
          {/* View toggle */}
          <div className="flex rounded-[9px] overflow-hidden" style={{ border: `1px solid #e2e7ea` }}>
            {(["list", "compact"] as const).map((v) => (
              <button key={v} onClick={() => setViewMode(v)}
                className="w-[34px] h-[32px] flex items-center justify-center text-[15px] transition-colors"
                style={{
                  background: viewMode === v ? C.surface : "transparent",
                  color:      viewMode === v ? C.ink : C.muted2,
                  borderRight: v === "list" ? `1px solid #e2e7ea` : "none",
                }}>
                {v === "list" ? "☰" : "≣"}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Category filter chips — only shown once some foods have categories */}
      {usedCategories.length > 0 && (
        <div className="flex-none px-[14px] py-[10px] flex gap-[6px] overflow-x-auto"
          style={{ background: C.surface, borderBottom: `1px solid #eef1f3` }}>
          {usedCategories.map((cat) => {
            const on = categoryFilter === cat;
            return (
              <button key={cat} type="button"
                onClick={() => setCategoryFilter(on ? null : cat)}
                className="rounded-full px-3 py-[6px] text-[12px] font-semibold whitespace-nowrap"
                style={{
                  background: on ? C.teal : "transparent",
                  color:      on ? "#fff" : C.muted3,
                  border:     `1.5px solid ${on ? C.teal : C.border}`,
                }}>
                {cat}
              </button>
            );
          })}
        </div>
      )}

      {/* List */}
      <div className="flex-1 overflow-y-auto px-[14px] pt-3" style={{ paddingBottom: 120 }}>
        {loading && (
          <div className="text-center py-12 text-[12px]" style={{ color: C.muted }}>Loading…</div>
        )}
        {error && (
          <div className="text-center py-12 text-[12px]" style={{ color: "#b23b3b" }}>{error}</div>
        )}

        {showFavEmpty && (
          <div className="text-center py-12 px-6">
            <div className="text-[28px]" style={{ color: "oklch(0.82 0.09 75)" }}>★</div>
            <div className="text-[13px] font-semibold mt-2" style={{ color: C.muted3 }}>No favourites yet</div>
            <div className="text-[11.5px] mt-1" style={{ color: C.muted2 }}>Tap the ☆ on any food to save it here.</div>
          </div>
        )}

        {!showFavEmpty && viewMode === "list" && foods.map((f) => (
          <FoodListCard key={f.id} food={f}
            expanded={expandedIds.has(f.id!)}
            onToggleExpand={() => toggleExpand(f.id!)}
            onToggleFav={(e) => { e.stopPropagation(); void handleToggleFav(f); }}
            onDelete={(e) => { e.stopPropagation(); void handleDelete(f); }} />
        ))}

        {!showFavEmpty && viewMode === "compact" && foods.length > 0 && (
          <div className="rounded-[12px] overflow-hidden"
            style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            {foods.map((f, i) => (
              <FoodCompactRow key={f.id} food={f}
                expanded={expandedIds.has(f.id!)}
                isLast={i === foods.length - 1}
                onToggleExpand={() => toggleExpand(f.id!)}
                onToggleFav={(e) => { e.stopPropagation(); void handleToggleFav(f); }}
                onDelete={(e) => { e.stopPropagation(); void handleDelete(f); }} />
            ))}
          </div>
        )}
      </div>

      {/* FAB */}
      <FoodsFab open={fanOpen} onToggle={() => setFanOpen((o) => !o)}
        onManual={() => openSheet("manual")}
        onOnline={() => openSheet("online")}
        onBarcode={() => openSheet("barcode")} />

      {/* Sheets */}
      <ManualSheet open={activeSheet === "manual"} form={form}
        isSaveEnabled={isSaveEnabled} saving={saving}
        onField={updateForm} onSave={saveManual} onClose={closeSheet} />
      <OnlineSheet open={activeSheet === "online"} query={onlineQuery}
        results={onlineResults} loading={onlineLoading}
        onQuery={setOnlineQuery} onSearch={runOnlineSearch}
        onAdd={addOnlineFood} onClose={closeSheet} />
      <BarcodeSheet open={activeSheet === "barcode"} onClose={closeSheet} onAdd={addScannedFood} saving={saving} />

      <NutritionNav />
    </div>
  );
}

export default function FoodsPage() {
  return (
    <AuthGuard>
      <FoodsPageInner />
    </AuthGuard>
  );
}
