"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { Plus, Trash2, ChevronDown, ChevronUp } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type GroceryListDto = components["schemas"]["GroceryListDto"];
type GroceryItemDto = components["schemas"]["GroceryItemDto"];
type DietDto = components["schemas"]["DietDto"];

const UNITS = ["PIECE", "GRAM", "ML", "CUP", "TBSP", "TSP"] as const;

export default function GroceryPage() {
  const { user } = useAuth();
  const [lists, setLists] = useState<GroceryListDto[]>([]);
  const [diets, setDiets] = useState<DietDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [showFromDiet, setShowFromDiet] = useState(false);
  const [fromDietId, setFromDietId] = useState<string>("");
  const [generatingFromDiet, setGeneratingFromDiet] = useState(false);

  // New list form
  const [newListName, setNewListName] = useState("");
  const [creatingList, setCreatingList] = useState(false);

  // Add item form per list
  const [addingToList, setAddingToList] = useState<number | null>(null);
  const [itemName, setItemName] = useState("");
  const [itemQty, setItemQty] = useState("1");
  const [itemUnit, setItemUnit] = useState<typeof UNITS[number]>("PIECE");
  const [itemCategory, setItemCategory] = useState("");

  useEffect(() => {
    if (!user) return;
    Promise.all([
      api.get<GroceryListDto[]>("/api/v1/grocery-lists"),
      api.get<DietDto[]>("/api/v1/diets"),
    ])
      .then(([l, d]) => { setLists(l); setDiets(d); if (d.length > 0) setFromDietId(String(d[0].id ?? "")); })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  const createList = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newListName.trim()) return;
    setCreatingList(true);
    try {
      const created = await api.post<GroceryListDto>("/api/v1/grocery-lists", { name: newListName.trim(), items: [] });
      setLists((prev) => [created, ...prev]);
      setNewListName("");
      setExpandedId(created.id ?? null);
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to create"); }
    finally { setCreatingList(false); }
  };

  const deleteList = async (id: number) => {
    if (!confirm("Delete this grocery list?")) return;
    try {
      await api.delete(`/api/v1/grocery-lists/${id}`);
      setLists((prev) => prev.filter((l) => l.id !== id));
      if (expandedId === id) setExpandedId(null);
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to delete"); }
  };

  const saveList = async (list: GroceryListDto) => {
    setSavingId(list.id ?? null);
    try {
      let updated: GroceryListDto;
      if (list.id && list.id > 0) {
        updated = await api.put<GroceryListDto>(`/api/v1/grocery-lists/${list.id}`, list);
      } else {
        updated = await api.post<GroceryListDto>("/api/v1/grocery-lists", list);
      }
      setLists((prev) => prev.map((l) => (l.id === list.id ? updated : l)));
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to save"); }
    finally { setSavingId(null); }
  };

  const toggleItem = (list: GroceryListDto, idx: number) =>
    saveList({ ...list, items: (list.items ?? []).map((it, i) => i === idx ? { ...it, done: !it.done } : it) });

  const generateFromDiet = async () => {
    if (!fromDietId) return;
    setGeneratingFromDiet(true);
    try {
      const created = await api.post<GroceryListDto>(`/api/v1/grocery-lists/from-diet/${fromDietId}`, {});
      setLists((prev) => [created, ...prev]);
      setExpandedId(created.id ?? null);
      setShowFromDiet(false);
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to generate"); }
    finally { setGeneratingFromDiet(false); }
  };

  const addItem = async (list: GroceryListDto) => {
    if (!itemName.trim()) return;
    const newItem: GroceryItemDto = {
      name: itemName.trim(),
      quantity: parseFloat(itemQty) || 1,
      unit: itemUnit,
      category: itemCategory.trim() || undefined,
      done: false,
    };
    await saveList({ ...list, items: [...(list.items ?? []), newItem] });
    setItemName(""); setItemQty("1"); setItemCategory(""); setAddingToList(null);
  };

  function groupByCategory(items: GroceryItemDto[]): Map<string, GroceryItemDto[]> {
    const map = new Map<string, GroceryItemDto[]>();
    for (const item of items) {
      const cat = item.category || "Uncategorised";
      if (!map.has(cat)) map.set(cat, []);
      map.get(cat)!.push(item);
    }
    return map;
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between pt-1">
        <h1 className="text-[22px] font-semibold text-text-primary">Grocery</h1>
        {diets.length > 0 && (
          <button
            onClick={() => setShowFromDiet((v) => !v)}
            className="text-[12px] font-semibold text-green border border-green/30 bg-green-light rounded-xl px-3 py-1.5"
          >
            From diet
          </button>
        )}
      </div>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* Generate from diet panel */}
      {showFromDiet && (
        <div className="bg-bg-card rounded-xl border border-divider p-4 space-y-3">
          <p className="text-[13px] font-semibold text-text-primary">Generate shopping list from diet</p>
          <select
            value={fromDietId}
            onChange={(e) => setFromDietId(e.target.value)}
            className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary"
          >
            {diets.map((d) => <option key={d.id} value={d.id}>{d.name}</option>)}
          </select>
          <div className="flex gap-2">
            <button
              onClick={generateFromDiet}
              disabled={generatingFromDiet || !fromDietId}
              className="flex-1 rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-50"
            >
              {generatingFromDiet ? "Generating…" : "Generate list"}
            </button>
            <button
              onClick={() => setShowFromDiet(false)}
              className="px-4 rounded-xl border border-divider text-sm font-medium text-text-secondary"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* Create new list */}
      <form onSubmit={createList} className="flex gap-2">
        <input
          placeholder="New list name…"
          value={newListName}
          onChange={(e) => setNewListName(e.target.value)}
          className="flex-1 rounded-xl border border-divider bg-bg-card px-4 py-2.5 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder"
        />
        <button
          type="submit"
          disabled={creatingList || !newListName.trim()}
          className="rounded-xl bg-text-primary px-4 py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40 flex items-center gap-1.5"
        >
          <Plus size={15} /> {creatingList ? "…" : "Create"}
        </button>
      </form>

      {/* Lists */}
      {loading ? (
        <div className="space-y-3">
          {[1, 2].map((i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)}
        </div>
      ) : lists.length === 0 ? (
        <div className="bg-bg-card rounded-xl border border-divider flex flex-col items-center py-12 gap-3">
          <span className="text-4xl">🛒</span>
          <p className="text-sm text-text-muted">No grocery lists yet</p>
          <p className="text-xs text-text-placeholder">Create one using the form above</p>
        </div>
      ) : (
        <div className="space-y-3">
          {lists.map((list) => {
            const isExpanded = expandedId === list.id;
            const items = list.items ?? [];
            const done = items.filter((i) => i.done).length;
            const total = items.length;
            const isSaving = savingId === list.id;
            const byCategory = groupByCategory(items);
            const progress = total > 0 ? (done / total) * 100 : 0;

            return (
              <div key={list.id} className="bg-bg-card rounded-xl border border-divider overflow-hidden">
                {/* List header */}
                <div className="flex items-center gap-3 px-4 py-3">
                  <button
                    className="flex-1 text-left min-w-0"
                    onClick={() => setExpandedId(isExpanded ? null : (list.id ?? null))}
                  >
                    <p className="text-[14px] font-semibold text-text-primary truncate">{list.name}</p>
                    <div className="flex items-center gap-2 mt-1">
                      {total > 0 && (
                        <div className="flex-1 h-1.5 rounded-full bg-bg-page overflow-hidden max-w-[80px]">
                          <div className="h-full rounded-full bg-green transition-all" style={{ width: `${progress}%` }} />
                        </div>
                      )}
                      <p className="text-[11px] text-text-muted">{done}/{total} done</p>
                    </div>
                  </button>
                  {done === total && total > 0 && (
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded-full bg-green-light text-green">Done</span>
                  )}
                  <button
                    onClick={() => setExpandedId(isExpanded ? null : (list.id ?? null))}
                    className="text-text-muted hover:text-text-primary"
                  >
                    {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                  </button>
                  <button
                    onClick={() => list.id !== undefined && deleteList(list.id)}
                    className="text-text-muted hover:text-red-500 transition-colors"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>

                {/* Expanded content */}
                {isExpanded && (
                  <div className="border-t border-divider">
                    {/* Items grouped by category */}
                    {items.length === 0 ? (
                      <p className="px-4 py-3 text-xs text-text-muted">No items yet. Add one below.</p>
                    ) : (
                      <div className="divide-y divide-divider">
                        {Array.from(byCategory.entries()).map(([category, catItems]) => (
                          <div key={category}>
                            {byCategory.size > 1 && (
                              <p className="px-4 pt-2 pb-1 text-[9px] font-bold uppercase tracking-widest text-text-muted">{category}</p>
                            )}
                            {catItems.map((item) => {
                              const globalIdx = items.findIndex(
                                (gi) => gi.name === item.name && gi.quantity === item.quantity && gi.category === item.category
                              );
                              return (
                                <div key={globalIdx} className="flex items-center gap-3 px-4 py-2.5">
                                  <button
                                    disabled={isSaving}
                                    onClick={() => toggleItem(list, globalIdx)}
                                    className={[
                                      "w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 transition-colors",
                                      item.done ? "bg-green border-green" : "border-divider hover:border-green",
                                    ].join(" ")}
                                  >
                                    {item.done && <span className="text-white text-[10px] font-bold">✓</span>}
                                  </button>
                                  <p className={["flex-1 text-[13px]", item.done ? "line-through text-text-muted" : "text-text-primary"].join(" ")}>
                                    {item.name}
                                  </p>
                                  <p className="text-[11px] text-text-muted shrink-0">{item.quantity} {item.unit.toLowerCase()}</p>
                                </div>
                              );
                            })}
                          </div>
                        ))}
                      </div>
                    )}

                    {/* Add item form */}
                    {addingToList === list.id ? (
                      <div className="border-t border-divider px-4 py-3 space-y-3 bg-bg-page">
                        <div>
                          <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Item name</p>
                          <input
                            autoFocus value={itemName} onChange={(e) => setItemName(e.target.value)}
                            placeholder="e.g. Chicken breast"
                            className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary placeholder:text-text-placeholder"
                          />
                        </div>
                        <div className="grid grid-cols-2 gap-3">
                          <div>
                            <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Qty</p>
                            <input
                              type="number" min={0} value={itemQty} onChange={(e) => setItemQty(e.target.value)}
                              className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary"
                            />
                          </div>
                          <div>
                            <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Unit</p>
                            <select
                              value={itemUnit} onChange={(e) => setItemUnit(e.target.value as typeof UNITS[number])}
                              className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none"
                            >
                              {UNITS.map((u) => <option key={u} value={u}>{u.toLowerCase()}</option>)}
                            </select>
                          </div>
                        </div>
                        <div>
                          <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Category (optional)</p>
                          <input
                            value={itemCategory} onChange={(e) => setItemCategory(e.target.value)}
                            placeholder="e.g. Meat, Dairy…"
                            className="w-full rounded-lg border border-divider bg-bg-card px-3 py-2 text-sm text-text-primary outline-none placeholder:text-text-placeholder"
                          />
                        </div>
                        <div className="flex gap-2">
                          <button
                            disabled={isSaving || !itemName.trim()}
                            onClick={() => addItem(list)}
                            className="flex-1 rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-40"
                          >
                            {isSaving ? "Saving…" : "Add item"}
                          </button>
                          <button
                            onClick={() => setAddingToList(null)}
                            className="px-4 rounded-xl border border-divider text-sm font-medium text-text-secondary"
                          >
                            Cancel
                          </button>
                        </div>
                      </div>
                    ) : (
                      <div className="border-t border-divider px-4 py-2">
                        <button
                          onClick={() => setAddingToList(list.id ?? null)}
                          className="flex items-center gap-2 text-sm text-text-muted hover:text-text-primary transition-colors py-1.5"
                        >
                          <Plus size={14} /> Add item
                        </button>
                      </div>
                    )}
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
