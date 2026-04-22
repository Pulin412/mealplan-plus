"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { ChevronDown, ChevronUp, Trash2, Plus } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Separator } from "@/components/ui/separator";
import type { components } from "@/lib/api/types.generated";

type GroceryListDto = components["schemas"]["GroceryListDto"];
type GroceryItemDto = components["schemas"]["GroceryItemDto"];

export default function GroceryPage() {
  const { user } = useAuth();
  const [lists, setLists] = useState<GroceryListDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);

  // New list form
  const [newListName, setNewListName] = useState("");
  const [creatingList, setCreatingList] = useState(false);

  // Add item form per list
  const [addingToList, setAddingToList] = useState<number | null>(null);
  const [itemName, setItemName] = useState("");
  const [itemQty, setItemQty] = useState("1");
  const [itemUnit, setItemUnit] = useState("PIECE");
  const [itemCategory, setItemCategory] = useState("");

  useEffect(() => {
    if (!user) return;
    api.get<GroceryListDto[]>("/api/v1/grocery-lists")
      .then(setLists)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  }, [user]);

  const createList = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newListName.trim()) return;
    setCreatingList(true);
    try {
      const payload: GroceryListDto = { name: newListName.trim(), items: [] };
      const created = await api.post<GroceryListDto>("/api/v1/grocery-lists", payload);
      setLists((prev) => [created, ...prev]);
      setNewListName("");
      setExpandedId(created.id ?? null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to create");
    } finally {
      setCreatingList(false);
    }
  };

  const deleteList = async (id: number) => {
    if (!confirm("Delete this grocery list?")) return;
    try {
      await api.delete(`/api/v1/grocery-lists/${id}`);
      setLists((prev) => prev.filter((l) => l.id !== id));
      if (expandedId === id) setExpandedId(null);
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to delete");
    }
  };

  // Save full list back to server (PUT-style via POST with serverId)
  const saveList = async (list: GroceryListDto) => {
    setSavingId(list.id ?? null);
    try {
      const updated = await api.post<GroceryListDto>("/api/v1/grocery-lists", list);
      setLists((prev) => prev.map((l) => (l.id === list.id ? updated : l)));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Failed to save");
    } finally {
      setSavingId(null);
    }
  };

  const toggleItem = async (list: GroceryListDto, itemIdx: number) => {
    const updated: GroceryListDto = {
      ...list,
      items: (list.items ?? []).map((item, i) =>
        i === itemIdx ? { ...item, done: !item.done } : item
      ),
    };
    await saveList(updated);
  };

  const addItem = async (list: GroceryListDto) => {
    if (!itemName.trim()) return;
    const newItem: GroceryItemDto = {
      name: itemName.trim(),
      quantity: parseFloat(itemQty) || 1,
      unit: itemUnit as GroceryItemDto["unit"],
      category: itemCategory.trim() || undefined,
      done: false,
    };
    const updated: GroceryListDto = {
      ...list,
      items: [...(list.items ?? []), newItem],
    };
    await saveList(updated);
    setItemName(""); setItemQty("1"); setItemCategory(""); setAddingToList(null);
  };

  // Group items by category
  function groupByCategory(items: GroceryItemDto[]): Map<string, GroceryItemDto[]> {
    const map = new Map<string, GroceryItemDto[]>();
    for (const item of items) {
      const cat = item.category || "Uncategorised";
      if (!map.has(cat)) map.set(cat, []);
      map.get(cat)!.push(item);
    }
    return map;
  }

  const doneCounts = (items: GroceryItemDto[]) => {
    const done = items.filter((i) => i.done).length;
    return { done, total: items.length };
  };

  return (
    <div className="space-y-6 max-w-xl">
      <h1 className="text-2xl font-bold">Grocery</h1>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {/* Create new list */}
      <form onSubmit={createList} className="flex gap-2">
        <Input
          placeholder="New list name…"
          value={newListName}
          onChange={(e) => setNewListName(e.target.value)}
          className="h-9 text-sm"
        />
        <Button type="submit" size="sm" disabled={creatingList || !newListName.trim()}>
          {creatingList ? "Creating…" : <><Plus className="h-4 w-4 mr-1" />Create</>}
        </Button>
      </form>

      {/* Lists */}
      {loading ? (
        Array.from({ length: 2 }).map((_, i) => <Skeleton key={i} className="h-20 w-full rounded-xl" />)
      ) : lists.length === 0 ? (
        <Card>
          <CardContent className="py-8 text-center text-sm text-muted-foreground">
            No grocery lists yet. Create one above.
          </CardContent>
        </Card>
      ) : (
        lists.map((list) => {
          const isExpanded = expandedId === list.id;
          const items = list.items ?? [];
          const { done, total } = doneCounts(items);
          const byCategory = groupByCategory(items);
          const isSaving = savingId === list.id;

          return (
            <Card key={list.id} className="overflow-hidden">
              <CardHeader className="pb-2">
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2 flex-1 min-w-0">
                    <button
                      className="flex-1 text-left"
                      onClick={() => setExpandedId(isExpanded ? null : (list.id ?? null))}
                    >
                      <p className="font-semibold text-sm truncate">{list.name}</p>
                      <p className="text-xs text-muted-foreground">{done}/{total} done</p>
                    </button>
                    {total > 0 && done === total && (
                      <Badge variant="secondary" className="text-xs shrink-0">Complete</Badge>
                    )}
                  </div>
                  <div className="flex items-center gap-1 shrink-0">
                    <Button size="icon" variant="ghost" className="h-7 w-7"
                      onClick={() => setExpandedId(isExpanded ? null : (list.id ?? null))}>
                      {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                    </Button>
                    <Button size="icon" variant="ghost" className="h-7 w-7 text-destructive hover:text-destructive"
                      onClick={() => list.id !== undefined && deleteList(list.id)}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>

              {isExpanded && (
                <CardContent className="pt-0">
                  <Separator className="mb-3" />

                  {items.length === 0 ? (
                    <p className="text-xs text-muted-foreground mb-3">No items yet. Add one below.</p>
                  ) : (
                    <div className="space-y-3 mb-3">
                      {Array.from(byCategory.entries()).map(([category, catItems]) => (
                        <div key={category}>
                          {byCategory.size > 1 && (
                            <p className="text-xs font-semibold text-muted-foreground uppercase mb-1">{category}</p>
                          )}
                          <ul className="space-y-1">
                            {catItems.map((item, catIdx) => {
                              // Find global index for toggling
                              const globalIdx = items.findIndex(
                                (gi) => gi === item || (gi.name === item.name && gi.quantity === item.quantity && gi.category === item.category)
                              );
                              return (
                                <li key={catIdx} className="flex items-center gap-2 text-sm">
                                  <input
                                    type="checkbox"
                                    checked={item.done}
                                    disabled={isSaving}
                                    onChange={() => toggleItem(list, globalIdx)}
                                    className="h-4 w-4 rounded border-gray-300 accent-primary cursor-pointer"
                                  />
                                  <span className={item.done ? "line-through text-muted-foreground" : ""}>
                                    {item.name}
                                  </span>
                                  <span className="text-muted-foreground text-xs">{item.quantity} {item.unit.toLowerCase()}</span>
                                </li>
                              );
                            })}
                          </ul>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Add item */}
                  {addingToList === list.id ? (
                    <div className="space-y-2 border rounded-md p-3 bg-muted/30">
                      <div className="grid grid-cols-2 gap-2">
                        <div className="col-span-2 space-y-1">
                          <Label className="text-xs">Item name</Label>
                          <Input value={itemName} onChange={(e) => setItemName(e.target.value)} placeholder="e.g. Chicken breast" className="h-8 text-sm" autoFocus />
                        </div>
                        <div className="space-y-1">
                          <Label className="text-xs">Quantity</Label>
                          <Input type="number" value={itemQty} onChange={(e) => setItemQty(e.target.value)} className="h-8 text-sm" min={0} />
                        </div>
                        <div className="space-y-1">
                          <Label className="text-xs">Unit</Label>
                          <select value={itemUnit} onChange={(e) => setItemUnit(e.target.value)}
                            className="w-full h-8 rounded-md border border-input bg-background px-2 text-sm">
                            <option value="PIECE">piece</option>
                            <option value="GRAM">gram</option>
                            <option value="ML">ml</option>
                            <option value="CUP">cup</option>
                            <option value="TBSP">tbsp</option>
                            <option value="TSP">tsp</option>
                          </select>
                        </div>
                        <div className="col-span-2 space-y-1">
                          <Label className="text-xs">Category (optional)</Label>
                          <Input value={itemCategory} onChange={(e) => setItemCategory(e.target.value)} placeholder="e.g. Meat, Dairy…" className="h-8 text-sm" />
                        </div>
                      </div>
                      <div className="flex gap-2">
                        <Button size="sm" disabled={isSaving || !itemName.trim()} onClick={() => addItem(list)}>
                          {isSaving ? "Saving…" : "Add"}
                        </Button>
                        <Button size="sm" variant="ghost" onClick={() => setAddingToList(null)}>Cancel</Button>
                      </div>
                    </div>
                  ) : (
                    <Button size="sm" variant="outline" className="w-full" onClick={() => setAddingToList(list.id ?? null)}>
                      <Plus className="h-4 w-4 mr-1" />Add item
                    </Button>
                  )}
                </CardContent>
              )}
            </Card>
          );
        })
      )}
    </div>
  );
}
