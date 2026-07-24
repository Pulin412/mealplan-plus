"use client";

import Link from "next/link";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";
import { BottomSheet } from "@/components/ui/BottomSheet";
import { useGroceries, CAT_ORDER, rangeLabel, type GroceryRow, type SavedGroceryList } from "@/hooks/useGroceries";
import { unitLabel } from "@/lib/nutrition";

const C = {
  ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", muted3: "#5b666e", faint: "#a2abb1",
  border: "#eaeef0", borderMuted: "#e4e8eb", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5",
  teal: "oklch(0.62 0.09 210)", tealSoft: "oklch(0.95 0.03 210)", danger: "#b23b3b",
};
const mono = "'DM Mono', monospace";
const WD = ["M", "T", "W", "T", "F", "S", "S"];
const MONTHS = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
const isoKey = (y: number, m: number, d: number) => `${y}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
const fmt = (n: number) => { const r = Math.round(n * 10) / 10; return r % 1 === 0 ? String(r) : r.toFixed(1); };

type G = ReturnType<typeof useGroceries>;

function GroceriesInner() {
  const g = useGroceries();
  const nDays = g.dateKeys.length;
  const title = g.isSaved ? g.activeName ?? "Saved list" : `${nDays === 0 ? "No" : nDays} day${nDays === 1 ? "" : "s"} of shopping`;
  const range = rangeLabel(g.dateKeys);
  const sub = g.isSaved ? `${nDays} day${nDays === 1 ? "" : "s"} · ${range}` : nDays === 0 ? "Tap to pick dates" : range;
  const actionLabel = g.isSaved ? "New list" : g.calOpen ? "Done" : "Edit dates";

  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      {/* App bar */}
      <div style={{ display: "flex", alignItems: "center", padding: "10px 16px 8px" }}>
        <span style={{ font: "700 21px system-ui", color: C.ink }}>Groceries</span>
        <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 6 }}>
          <button onClick={g.openSaved} title="Saved lists" style={{ position: "relative", border: "none", background: "none", cursor: "pointer", fontSize: 19, color: C.muted3, padding: 4 }}>
            🔖
            {g.savedLists.length > 0 && <span style={{ position: "absolute", top: 2, right: 2, width: 7, height: 7, borderRadius: "50%", background: C.teal }} />}
          </button>
          <Link href="/profile" style={{ width: 34, height: 34, borderRadius: "50%", background: C.teal, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 17 }}>👤</Link>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto" style={{ padding: "0 16px", paddingBottom: 120 }}>
        {g.error && <div style={{ textAlign: "center", padding: 24, font: "400 12px system-ui", color: C.danger }}>{g.error}</div>}

        {/* Date card */}
        <div onClick={() => { if (!g.isSaved) g.toggleCal(); }}
          style={{ display: "flex", alignItems: "center", background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, padding: 12, cursor: g.isSaved ? "default" : "pointer" }}>
          <div style={{ width: 34, height: 34, borderRadius: 9, background: C.tealSoft, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 16 }}>📅</div>
          <div style={{ marginLeft: 11, minWidth: 0, flex: 1 }}>
            <div style={{ font: "700 14.5px system-ui", color: C.ink, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{title}</div>
            <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>{sub}</div>
          </div>
          <button onClick={(e) => { e.stopPropagation(); if (g.isSaved) g.newList(); else g.toggleCal(); }}
            style={{ border: "none", background: "none", cursor: "pointer", font: "600 12.5px system-ui", color: C.teal, padding: 4 }}>{actionLabel}</button>
        </div>

        {g.calOpen && !g.isSaved && <Calendar g={g} />}

        {/* List header */}
        <div style={{ display: "flex", alignItems: "center", margin: "18px 0 12px" }}>
          <span style={{ font: "700 16px system-ui", color: C.ink }}>{g.isSaved ? "Saved list" : "Shopping list"}</span>
          {!g.isSaved && (
            <button onClick={g.refresh} disabled={g.refreshing} title="Recalculate from plan"
              style={{ marginLeft: "auto", border: "none", background: "none", cursor: g.refreshing ? "default" : "pointer", fontSize: 16, color: C.muted3, padding: 4, opacity: g.refreshing ? 0.4 : 1 }}>↻</button>
          )}
          <button onClick={() => (g.isSaved ? g.newList() : g.saveList())}
            disabled={!g.isSaved && (g.total === 0 || g.selected.size === 0)}
            style={{ marginLeft: g.isSaved ? "auto" : 4, border: "none", background: "none", cursor: "pointer", font: "600 13px system-ui", color: !g.isSaved && (g.total === 0 || g.selected.size === 0) ? C.muted2 : C.teal, padding: 4 }}>
            {g.isSaved ? "Done" : "Save"}
          </button>
        </div>
        {g.total > 0 && (
          <div style={{ display: "flex", alignItems: "center", marginBottom: 14 }}>
            <div style={{ flex: 1, height: 4, borderRadius: 4, background: C.bgAlt, overflow: "hidden" }}>
              <div style={{ width: `${g.total === 0 ? 0 : (g.boughtCount / g.total) * 100}%`, height: 4, borderRadius: 4, background: C.teal }} />
            </div>
            <span style={{ marginLeft: 10, font: `600 11px ${mono}`, color: C.muted3 }}>{g.boughtCount} / {g.total}</span>
          </div>
        )}

        {/* Tabs */}
        {g.total > 0 && <Tabs g={g} />}

        {/* List body */}
        <ShoppingList g={g} />
      </div>

      <SavedSheet g={g} />
      <NutritionNav />
    </div>
  );
}

// ── Calendar ──────────────────────────────────────────────────────────────────
function Calendar({ g }: { g: G }) {
  const { year, month } = g.ym;
  const firstDow = (new Date(year, month - 1, 1).getDay() + 6) % 7;
  const days = new Date(year, month, 0).getDate();
  const cells: (number | null)[] = [...Array(firstDow).fill(null), ...Array.from({ length: days }, (_, i) => i + 1)];
  const presetActive = (n: number) => { const k = Array.from(g.selected).sort(); return k.length === n && k[0] === g.todayIso; };

  return (
    <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 14, padding: 12, marginTop: 10 }}>
      <div style={{ display: "flex", alignItems: "center", marginBottom: 10 }}>
        <button onClick={g.prevMonth} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3, cursor: "pointer" }}>‹</button>
        <span style={{ flex: 1, textAlign: "center", font: "700 14px system-ui", color: C.ink }}>{MONTHS[month - 1]} {year}</span>
        <button onClick={g.nextMonth} style={{ width: 30, height: 30, borderRadius: "50%", background: C.bgAlt, border: "none", color: C.muted3, cursor: "pointer" }}>›</button>
      </div>
      <div style={{ display: "grid", gridTemplateColumns: "repeat(7,1fr)" }}>
        {WD.map((d, i) => <div key={i} style={{ textAlign: "center", font: "600 9.5px system-ui", color: C.faint, paddingBottom: 4 }}>{d}</div>)}
        {cells.map((day, i) => {
          if (day == null) return <div key={i} />;
          const k = isoKey(year, month, day);
          const isSel = g.selected.has(k);
          const isToday = k === g.todayIso;
          const hasDiet = g.plannedDates.has(k);
          return (
            <div key={i} style={{ aspectRatio: "1", display: "flex", justifyContent: "center", alignItems: "center" }}>
              <div onClick={() => g.toggleDay(k)} style={{
                cursor: "pointer", width: 34, height: 34, borderRadius: 9,
                background: isSel ? C.teal : "transparent", border: `1px solid ${isToday && !isSel ? C.teal : "transparent"}`,
                display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center",
              }}>
                <span style={{ font: `${isSel ? 700 : 500} 12px system-ui`, color: isSel ? "#fff" : C.ink }}>{day}</span>
                <span style={{ width: 4, height: 4, borderRadius: "50%", marginTop: 1, background: hasDiet ? (isSel ? "rgba(255,255,255,.9)" : C.teal) : "transparent" }} />
              </div>
            </div>
          );
        })}
      </div>
      <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 10 }}>
        {[["Next 7 days", 7], ["Next 14 days", 14]].map(([label, n]) => (
          <button key={n} onClick={() => g.preset(n as number)} style={{
            border: "none", borderRadius: 8, padding: "7px 11px", cursor: "pointer", font: "600 12px system-ui",
            background: presetActive(n as number) ? C.teal : C.bgAlt, color: presetActive(n as number) ? "#fff" : C.muted3,
          }}>{label}</button>
        ))}
        <button onClick={g.clearDates} style={{ marginLeft: "auto", border: "none", background: "none", cursor: "pointer", font: "600 12px system-ui", color: C.muted3, padding: "6px 4px" }}>Clear</button>
      </div>
    </div>
  );
}

// ── Tabs ──────────────────────────────────────────────────────────────────────
function Tabs({ g }: { g: G }) {
  const tabs: [G["view"], string, number][] = [["all", "All", g.total], ["remaining", "To buy", g.toBuy.length], ["bought", "Bought", g.bought.length]];
  return (
    <div style={{ display: "flex", gap: 3, background: C.bgAlt, borderRadius: 10, padding: 3, marginBottom: 14 }}>
      {tabs.map(([k, label, count]) => {
        const on = g.view === k;
        return (
          <button key={k} onClick={() => g.setView(k)} style={{
            flex: 1, border: "none", borderRadius: 8, padding: "8px 0", cursor: "pointer", font: "600 12px system-ui",
            background: on ? C.surface : "transparent", color: on ? C.ink : C.muted,
            boxShadow: on ? "0 1px 3px rgba(0,0,0,.08)" : "none",
          }}>{label} · {count}</button>
        );
      })}
    </div>
  );
}

// ── Shopping list body ─────────────────────────────────────────────────────────
function ShoppingList({ g }: { g: G }) {
  // Each row is independent: unchecked rows sit in their aisle, checked rows in Bought.
  const primary = g.view === "bought" ? g.bought : g.toBuy;
  const groups = CAT_ORDER.map((cat) => ({ cat, rows: primary.filter((r) => r.cat.key === cat.key) })).filter((grp) => grp.rows.length > 0);
  const showBought = g.view === "all" && g.bought.length > 0;

  const nDays = g.dateKeys.length;
  const empty = g.total === 0 || (groups.length === 0 && !showBought);
  if (empty) {
    const msg = nDays === 0 ? "Pick the days you want to shop for and your list builds itself."
      : g.total === 0 ? "No meals are planned on the selected days. Add a diet in Plan first."
      : g.view === "remaining" ? "Everything is checked off — you’re all set!"
      : g.view === "bought" ? "Nothing checked off yet. Tick items as you shop."
      : "Nothing here yet.";
    return <div style={{ textAlign: "center", padding: "40px 24px", font: "400 13px system-ui", color: C.muted2 }}>{msg}</div>;
  }

  return (
    <>
      {groups.map(({ cat, rows }) => (
        <div key={cat.key} style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12, marginBottom: 12, paddingBottom: 2 }}>
          <div style={{ display: "flex", alignItems: "center", padding: "12px 14px 6px" }}>
            <span style={{ width: 7, height: 7, borderRadius: "50%", background: cat.color }} />
            <span style={{ marginLeft: 7, font: "700 10.5px system-ui", letterSpacing: 0.5, color: C.muted3, textTransform: "uppercase" }}>{cat.label}</span>
            <span style={{ marginLeft: 6, font: "600 10.5px system-ui", color: C.faint }}>{rows.length}</span>
          </div>
          {rows.map((r) => <Row key={r.id} r={r} onToggle={() => g.toggleRow(r.id)} />)}
        </div>
      ))}

      {showBought && (
        <>
          <div style={{ display: "flex", alignItems: "center", margin: "4px 0 8px" }}>
            <span style={{ font: "700 12px system-ui", color: C.muted3 }}>Bought · {g.bought.length}</span>
            <button onClick={g.uncheckAll} style={{ marginLeft: "auto", border: "none", background: "none", cursor: "pointer", font: "600 11.5px system-ui", color: C.teal, padding: 4 }}>Uncheck all</button>
          </div>
          <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 12 }}>
            {g.bought.map((r) => <Row key={r.id} r={r} onToggle={() => g.toggleRow(r.id)} />)}
          </div>
        </>
      )}
    </>
  );
}

/** One independently-checkable list row. */
function Row({ r, onToggle }: { r: GroceryRow; onToggle: () => void }) {
  const bought = r.checked;
  return (
    <div onClick={onToggle} style={{ display: "flex", alignItems: "center", padding: "9px 14px", cursor: "pointer" }}>
      <span style={{
        width: 20, height: 20, borderRadius: 6, flexShrink: 0,
        background: bought ? C.teal : C.surface, border: `1.5px solid ${bought ? C.teal : C.borderMuted}`,
        display: "flex", alignItems: "center", justifyContent: "center", color: "#fff", fontSize: 12,
      }}>{bought ? "✓" : ""}</span>
      <span style={{ marginLeft: 11, font: "600 13.5px system-ui", color: bought ? C.faint : C.ink, textDecoration: bought ? "line-through" : "none" }}>{r.name}</span>
      <span style={{ marginLeft: "auto", font: `400 12px ${mono}`, color: bought ? C.faint : C.muted3 }}>{fmt(r.qty)} {unitLabel(r.unit)}</span>
    </div>
  );
}

// ── Saved lists sheet ──────────────────────────────────────────────────────────
function SavedSheet({ g }: { g: G }) {
  return (
    <BottomSheet open={g.sheetSaved} onClose={g.closeSheet} title={`Saved lists · ${g.savedLists.length}`}>
      {g.savedLists.length === 0 ? (
        <div style={{ textAlign: "center", padding: "24px 0", font: "400 13px system-ui", color: C.muted2 }}>No saved lists yet. Build one and tap Save.</div>
      ) : (
        g.savedLists.map((l) => <SavedRow key={l.id} l={l} active={l.id === g.activeId} g={g} />)
      )}
    </BottomSheet>
  );
}

function SavedRow({ l, active, g }: { l: SavedGroceryList; active: boolean; g: G }) {
  const done = Object.values(l.checked).filter(Boolean).length;
  return (
    <div onClick={() => g.loadSavedList(l.id)} style={{
      display: "flex", alignItems: "center", padding: 12, marginBottom: 8, cursor: "pointer",
      background: active ? C.tealSoft : C.surface, border: `1px solid ${active ? C.teal : C.borderMuted}`, borderRadius: 12,
    }}>
      <div style={{ width: 34, height: 34, borderRadius: 9, background: active ? C.teal : C.bgAlt, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 15 }}>🔖</div>
      <div style={{ marginLeft: 11, flex: 1, minWidth: 0 }}>
        <div style={{ font: "600 13.5px system-ui", color: C.ink, whiteSpace: "nowrap", overflow: "hidden", textOverflow: "ellipsis" }}>{l.name}</div>
        <div style={{ font: "400 11px system-ui", color: C.muted2 }}>{l.items.length} items · {done} bought · {l.days} day{l.days === 1 ? "" : "s"}</div>
      </div>
      <button onClick={(e) => { e.stopPropagation(); g.deleteSaved(l.id); }} style={{ border: "none", background: "none", cursor: "pointer", color: C.faint, fontSize: 16, padding: 4 }}>🗑</button>
    </div>
  );
}

export default function GroceriesPage() {
  return <AuthGuard><GroceriesInner /></AuthGuard>;
}
