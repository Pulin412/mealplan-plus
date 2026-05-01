"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState, useCallback, useRef } from "react";
import { Plus, Search, Trash2, X, ChevronDown, ChevronUp, Loader2, Globe, PenLine, Star } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type FoodDto = components["schemas"]["FoodDto"];

// ── Online lookup result (normalised from USDA or OpenFoodFacts) ──────────────
interface LookupResult {
  name: string;
  brand?: string;
  barcode?: string;
  caloriesPer100: number;
  proteinPer100: number;
  carbsPer100: number;
  fatPer100: number;
  gramsPerPiece?: number;
  source: "USDA" | "OFF";
}

// ── USDA FoodData Central ─────────────────────────────────────────────────────
const USDA_KEY = "DEMO_KEY";

interface UsdaNutrient { nutrientId?: number; value?: number; }
interface UsdaItem {
  fdcId: number; description: string; brandOwner?: string; brandName?: string;
  servingSize?: number; servingSizeUnit?: string;
  foodNutrients?: UsdaNutrient[];
}

function getUsda(id: number, nutrients: UsdaNutrient[] = []) {
  return nutrients.find((n) => n.nutrientId === id)?.value ?? 0;
}

async function searchUsda(query: string): Promise<LookupResult[]> {
  const url =
    `https://api.nal.usda.gov/fdc/v1/foods/search` +
    `?api_key=${USDA_KEY}&query=${encodeURIComponent(query)}&pageSize=12` +
    `&dataType=Foundation,SR%20Legacy,Branded`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("USDA request failed");
  const data: { foods?: UsdaItem[] } = await res.json();
  return (data.foods ?? []).map((item) => {
    const serving = item.servingSize ?? 100;
    const unit = (item.servingSizeUnit ?? "g").toLowerCase();
    const factor = serving > 0 ? 100 / serving : 1;
    const cal  = getUsda(1008, item.foodNutrients);
    const prot = getUsda(1003, item.foodNutrients);
    const carb = getUsda(1005, item.foodNutrients);
    const fat  = getUsda(1004, item.foodNutrients);
    return {
      name: item.description,
      brand: item.brandOwner ?? item.brandName,
      caloriesPer100: round1(cal * factor),
      proteinPer100:  round1(prot * factor),
      carbsPer100:    round1(carb * factor),
      fatPer100:      round1(fat * factor),
      gramsPerPiece:  unit !== "g" && unit !== "ml" ? serving : undefined,
      source: "USDA" as const,
    };
  });
}

// ── OpenFoodFacts ─────────────────────────────────────────────────────────────
interface OFFNutriments {
  "energy-kcal_100g"?: number; energy_100g?: number;
  proteins_100g?: number; carbohydrates_100g?: number; fat_100g?: number;
}
interface OFFProduct { code?: string; product_name?: string; brands?: string; nutriments?: OFFNutriments; }

async function searchOFF(query: string): Promise<LookupResult[]> {
  const url =
    `https://world.openfoodfacts.org/cgi/search.pl` +
    `?search_terms=${encodeURIComponent(query)}&search_simple=1&action=process&json=1&page_size=12`;
  const res = await fetch(url);
  if (!res.ok) throw new Error("OpenFoodFacts request failed");
  const data: { products?: OFFProduct[] } = await res.json();
  return (data.products ?? [])
    .filter((p) => p.product_name && p.nutriments)
    .map((p) => {
      const n = p.nutriments!;
      const kcal = n["energy-kcal_100g"] ?? (n.energy_100g ? n.energy_100g / 4.184 : 0);
      return {
        name: p.product_name!,
        brand: p.brands || undefined,
        barcode: p.code || undefined,
        caloriesPer100: round1(kcal),
        proteinPer100:  round1(n.proteins_100g ?? 0),
        carbsPer100:    round1(n.carbohydrates_100g ?? 0),
        fatPer100:      round1(n.fat_100g ?? 0),
        source: "OFF" as const,
      };
    });
}

function round1(v: number) { return Math.round(v * 10) / 10; }

// ── Online lookup panel ───────────────────────────────────────────────────────
function OnlineLookup({ onPick }: { onPick: (r: LookupResult) => void }) {
  const [query, setQuery] = useState("");
  const [source, setSource] = useState<"USDA" | "OFF">("USDA");
  const [results, setResults] = useState<LookupResult[]>([]);
  const [searching, setSearching] = useState(false);
  const [lookupError, setLookupError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const doSearch = useCallback(async (q: string, src: "USDA" | "OFF") => {
    if (q.length < 2) { setResults([]); return; }
    setSearching(true); setLookupError(null);
    try {
      const r = src === "USDA" ? await searchUsda(q) : await searchOFF(q);
      setResults(r);
      if (r.length === 0) setLookupError("No results found");
    } catch {
      setLookupError("Search failed — check your connection");
    } finally { setSearching(false); }
  }, []);

  // debounce
  useEffect(() => {
    if (timerRef.current) clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => doSearch(query, source), 500);
    return () => { if (timerRef.current) clearTimeout(timerRef.current); };
  }, [query, source, doSearch]);

  return (
    <div className="space-y-3">
      {/* Source tabs */}
      <div className="flex gap-1 p-1 bg-bg-page rounded-lg">
        {(["USDA", "OFF"] as const).map((s) => (
          <button
            key={s}
            type="button"
            onClick={() => { setSource(s); setResults([]); setLookupError(null); }}
            className="flex-1 rounded-md py-1.5 text-[12px] font-semibold transition-colors"
            style={source === s
              ? { background: "#1A1A1A", color: "#FFFFFF" }
              : { background: "transparent", color: "#888888" }}
          >
            {s === "USDA" ? "USDA (USA)" : "OpenFoodFacts"}
          </button>
        ))}
      </div>

      {/* Search box */}
      <div className="flex items-center gap-2 bg-bg-page rounded-lg border border-divider px-3">
        {searching
          ? <Loader2 size={14} className="text-text-muted shrink-0 animate-spin" />
          : <Search size={14} className="text-text-muted shrink-0" />}
        <input
          autoFocus
          placeholder={`Search ${source === "USDA" ? "USDA food database…" : "OpenFoodFacts…"}`}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="flex-1 py-2 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder"
        />
        {query && (
          <button type="button" onClick={() => { setQuery(""); setResults([]); setLookupError(null); }}
            className="text-text-muted hover:text-text-primary">
            <X size={13} />
          </button>
        )}
      </div>

      {lookupError && <p className="text-xs text-text-muted text-center py-1">{lookupError}</p>}

      {/* Results */}
      {results.length > 0 && (
        <div className="space-y-1.5 max-h-72 overflow-y-auto pr-0.5">
          {results.map((r, i) => (
            <button
              key={i}
              type="button"
              onClick={() => onPick(r)}
              className="w-full text-left bg-bg-page hover:bg-bg-card rounded-xl border border-divider px-3 py-2.5 transition-colors"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="text-[13px] font-semibold text-text-primary leading-snug">{r.name}</p>
                  {r.brand && <p className="text-[11px] text-text-muted mt-0.5">{r.brand}</p>}
                </div>
                <div className="shrink-0 text-right">
                  <p className="text-[13px] font-bold text-text-primary">{r.caloriesPer100} kcal</p>
                  <p className="text-[10px] text-text-muted">per 100g</p>
                </div>
              </div>
              <div className="flex gap-2 mt-1.5 flex-wrap">
                {[
                  { label: "P", val: r.proteinPer100, color: "#2E7D52" },
                  { label: "C", val: r.carbsPer100,   color: "#C05200" },
                  { label: "F", val: r.fatPer100,      color: "#7C3AED" },
                ].map(({ label, val, color }) => val > 0 && (
                  <span key={label} className="text-[10px] font-medium px-1.5 py-0.5 rounded-full"
                    style={{ background: `${color}18`, color }}>
                    {label} {val}g
                  </span>
                ))}
              </div>
            </button>
          ))}
        </div>
      )}

      {!searching && !lookupError && query.length < 2 && (
        <p className="text-xs text-text-muted text-center py-2">Type at least 2 characters to search</p>
      )}
    </div>
  );
}

// ── Main form ─────────────────────────────────────────────────────────────────
interface FoodForm {
  name: string; brand: string; caloriesPer100: string; proteinPer100: string;
  carbsPer100: string; fatPer100: string; barcode: string; gramsPerPiece: string;
}
const emptyForm: FoodForm = {
  name: "", brand: "", caloriesPer100: "", proteinPer100: "", carbsPer100: "", fatPer100: "",
  barcode: "", gramsPerPiece: "",
};

function MacroBadge({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full" style={{ background: `${color}18`, color }}>
      {label} {value}g
    </span>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────
export default function FoodsPage() {
  const { user } = useAuth();
  const [foods, setFoods] = useState<FoodDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [formTab, setFormTab] = useState<"lookup" | "manual">("lookup");
  const [form, setForm] = useState<FoodForm>(emptyForm);
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);

  const load = useCallback(async () => {
    if (!user) return;
    setLoading(true); setError(null);
    try {
      setFoods(await api.get<FoodDto[]>("/api/v1/foods"));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to load");
    } finally { setLoading(false); }
  }, [user]);

  useEffect(() => { load(); }, [load]);

  const filtered = foods.filter((f) =>
    !query || f.name.toLowerCase().includes(query.toLowerCase()) ||
    (f.brand?.toLowerCase().includes(query.toLowerCase()) ?? false) ||
    (f.barcode?.includes(query) ?? false)
  );

  const setField = (k: keyof FoodForm) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((p) => ({ ...p, [k]: e.target.value }));

  /** Pre-fill form from online lookup result, then switch to manual tab for review */
  const pickResult = (r: LookupResult) => {
    setForm({
      name: r.name,
      brand: r.brand ?? "",
      caloriesPer100: String(r.caloriesPer100),
      proteinPer100:  String(r.proteinPer100),
      carbsPer100:    String(r.carbsPer100),
      fatPer100:      String(r.fatPer100),
      barcode:        r.barcode ?? "",
      gramsPerPiece:  r.gramsPerPiece ? String(r.gramsPerPiece) : "",
    });
    setFormTab("manual");
  };

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name.trim()) return;
    setSaving(true); setError(null);
    try {
      const payload = {
        name: form.name.trim(),
        brand: form.brand.trim() || undefined,
        barcode: form.barcode.trim() || undefined,
        caloriesPer100: parseFloat(form.caloriesPer100) || 0,
        proteinPer100:  parseFloat(form.proteinPer100)  || 0,
        carbsPer100:    parseFloat(form.carbsPer100)    || 0,
        fatPer100:      parseFloat(form.fatPer100)      || 0,
        gramsPerPiece:  form.gramsPerPiece ? parseFloat(form.gramsPerPiece) : undefined,
      };
      const created = await api.post<FoodDto>("/api/v1/foods", payload);
      setFoods((p) => [created, ...p]);
      setForm(emptyForm); setShowForm(false); setFormTab("lookup");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally { setSaving(false); }
  };

  const deleteFood = async (id: number) => {
    if (!confirm("Delete this food item?")) return;
    setDeletingId(id);
    try {
      await api.delete(`/api/v1/foods/${id}`);
      setFoods((p) => p.filter((f) => f.id !== id));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    } finally { setDeletingId(null); }
  };

  const toggleFavorite = async (id: number) => {
    try {
      const updated = await api.patch<FoodDto>(`/api/v1/foods/${id}/favorite`);
      setFoods((p) => p.map((f) => f.id === id ? updated : f));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to update favourite");
    }
  };

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between pt-1">
        <h1 className="text-[22px] font-semibold text-text-primary">Foods</h1>
        <button
          onClick={() => { setShowForm((v) => !v); setForm(emptyForm); setFormTab("lookup"); }}
          className="flex items-center gap-1.5 rounded-xl bg-text-primary px-4 py-2 text-sm font-semibold text-bg-card"
        >
          {showForm ? <X size={14} /> : <Plus size={14} />}
          {showForm ? "Cancel" : "Add food"}
        </button>
      </div>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* Add food form */}
      {showForm && (
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
          {/* Tab header */}
          <div className="flex border-b border-divider">
            <button
              type="button"
              onClick={() => setFormTab("lookup")}
              className="flex-1 flex items-center justify-center gap-1.5 py-3 text-[12px] font-semibold transition-colors"
              style={formTab === "lookup"
                ? { borderBottom: "2px solid #1A1A1A", color: "#1A1A1A" }
                : { borderBottom: "2px solid transparent", color: "#888888" }}
            >
              <Globe size={13} /> Search online
            </button>
            <button
              type="button"
              onClick={() => setFormTab("manual")}
              className="flex-1 flex items-center justify-center gap-1.5 py-3 text-[12px] font-semibold transition-colors"
              style={formTab === "manual"
                ? { borderBottom: "2px solid #1A1A1A", color: "#1A1A1A" }
                : { borderBottom: "2px solid transparent", color: "#888888" }}
            >
              <PenLine size={13} /> Manual entry
              {form.name && <span className="ml-1 w-1.5 h-1.5 rounded-full bg-green inline-block" />}
            </button>
          </div>

          <div className="p-4">
            {formTab === "lookup" ? (
              <OnlineLookup onPick={pickResult} />
            ) : (
              <form onSubmit={submit} className="space-y-4">
                {form.name && (
                  <div className="flex items-center gap-2 bg-green-light rounded-lg px-3 py-2">
                    <span className="text-[11px] text-green font-semibold">Pre-filled from online lookup — review and confirm</span>
                  </div>
                )}
                <div className="grid grid-cols-2 gap-3">
                  <div className="col-span-2">
                    <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Name *</label>
                    <input autoFocus value={form.name} onChange={setField("name")} placeholder="e.g. Chicken breast"
                      className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
                  </div>
                  <div>
                    <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Brand</label>
                    <input value={form.brand} onChange={setField("brand")} placeholder="Optional"
                      className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
                  </div>
                  <div>
                    <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">Barcode</label>
                    <input value={form.barcode} onChange={setField("barcode")} placeholder="Optional"
                      className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  {([
                    { label: "Calories/100g *", key: "caloriesPer100" as const, placeholder: "e.g. 165" },
                    { label: "Protein/100g (g)", key: "proteinPer100"  as const, placeholder: "e.g. 31" },
                    { label: "Carbs/100g (g)",   key: "carbsPer100"   as const, placeholder: "e.g. 0" },
                    { label: "Fat/100g (g)",      key: "fatPer100"     as const, placeholder: "e.g. 3.6" },
                    { label: "Grams/piece",       key: "gramsPerPiece" as const, placeholder: "Optional" },
                  ]).map(({ label, key, placeholder }) => (
                    <div key={key}>
                      <label className="text-[10px] font-bold text-text-muted uppercase mb-1 block">{label}</label>
                      <input type="number" min={0} step="0.1" value={form[key]} onChange={setField(key)} placeholder={placeholder}
                        className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder" />
                    </div>
                  ))}
                </div>
                <button type="submit" disabled={saving || !form.name.trim()}
                  className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40">
                  {saving ? "Saving…" : "Save food"}
                </button>
              </form>
            )}
          </div>
        </div>
      )}

      {/* Catalogue search */}
      <div className="flex items-center gap-2 bg-bg-card rounded-xl border border-divider px-3">
        <Search size={14} className="text-text-muted shrink-0" />
        <input
          placeholder="Filter your catalogue…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="flex-1 py-2.5 text-sm bg-transparent outline-none text-text-primary placeholder:text-text-placeholder"
        />
        {query && <button onClick={() => setQuery("")} className="text-text-muted"><X size={14} /></button>}
      </div>

      {!loading && (
        <p className="text-xs text-text-muted px-0.5">
          {filtered.length} food{filtered.length !== 1 ? "s" : ""}
          {query ? ` matching "${query}"` : " in your catalogue"}
        </p>
      )}

      {/* List */}
      {loading ? (
        <div className="space-y-2">
          {[1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-16 w-full rounded-xl" />)}
        </div>
      ) : filtered.length === 0 ? (
        <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-12 gap-3">
          <span className="text-4xl">🥗</span>
          <p className="text-sm text-text-muted">{query ? "No foods match your filter" : "No foods yet"}</p>
          {!query && <p className="text-xs text-text-placeholder">Tap &quot;Add food&quot; to search or create one</p>}
        </div>
      ) : (
        <div className="space-y-2">
          {filtered.map((food) => {
            const isExpanded = expandedId === food.id;
            return (
              <div key={food.id} className="bg-bg-card rounded-xl border border-divider overflow-hidden">
                <div className="flex items-center gap-3 px-4 py-3 cursor-pointer"
                  onClick={() => setExpandedId(isExpanded ? null : (food.id ?? null))}>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <p className="text-[14px] font-semibold text-text-primary">{food.name}</p>
                      {food.brand && <span className="text-[11px] text-text-muted">· {food.brand}</span>}
                      {food.isSystemFood && (
                        <span className="text-[9px] font-bold px-1.5 py-0.5 rounded-full bg-green-light text-green">SYSTEM</span>
                      )}
                    </div>
                    <div className="flex items-center gap-1.5 mt-1 flex-wrap">
                      <span className="text-[13px] font-bold text-text-primary">{Math.round(food.caloriesPer100)}</span>
                      <span className="text-[11px] text-text-muted">kcal/100g</span>
                      {(food.proteinPer100 ?? 0) > 0 && <MacroBadge label="P" value={Math.round(food.proteinPer100 ?? 0)} color="#2E7D52" />}
                      {(food.carbsPer100 ?? 0) > 0 && <MacroBadge label="C" value={Math.round(food.carbsPer100 ?? 0)} color="#C05200" />}
                      {(food.fatPer100 ?? 0) > 0 && <MacroBadge label="F" value={Math.round(food.fatPer100 ?? 0)} color="#7C3AED" />}
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    {isExpanded ? <ChevronUp size={14} className="text-text-muted" /> : <ChevronDown size={14} className="text-text-muted" />}
                    <button
                      onClick={(e) => { e.stopPropagation(); if (food.id) toggleFavorite(food.id); }}
                      className="p-1 transition-colors"
                    >
                      <Star size={14} className={food.isFavorite ? "fill-yellow-400 text-yellow-400" : "text-text-muted"} />
                    </button>
                    {!food.isSystemFood && (
                      <button
                        disabled={deletingId === food.id}
                        onClick={(e) => { e.stopPropagation(); if (food.id) deleteFood(food.id); }}
                        className="text-text-muted hover:text-red-500 transition-colors p-1 -mr-1"
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                </div>

                {isExpanded && (
                  <div className="border-t border-divider px-4 py-3 grid grid-cols-2 gap-2">
                    {[
                      { label: "Calories",      val: `${food.caloriesPer100} kcal/100g` },
                      { label: "Protein",       val: `${food.proteinPer100}g/100g` },
                      { label: "Carbs",         val: `${food.carbsPer100}g/100g` },
                      { label: "Fat",           val: `${food.fatPer100}g/100g` },
                      ...(food.gramsPerPiece  ? [{ label: "Per piece",     val: `${food.gramsPerPiece}g` }] : []),
                      ...(food.barcode        ? [{ label: "Barcode",       val: food.barcode }] : []),
                      ...(food.glycemicIndex  ? [{ label: "Glycemic Index",val: String(food.glycemicIndex) }] : []),
                    ].map(({ label, val }) => (
                      <div key={label}>
                        <p className="text-[9px] font-bold text-text-muted uppercase mb-0.5">{label}</p>
                        <p className="text-[13px] text-text-primary">{val}</p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
