"use client";

import Link from "next/link";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { NutritionNav } from "@/components/layout/NutritionNav";

const C = { ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", border: "#eaeef0", surface: "#ffffff", bg: "#f7f9fa", bgAlt: "#f2f4f5" };

const LINKS = [
  { href: "/foods", icon: "🍎", title: "Foods", subtitle: "Your food library" },
  { href: "/meals", icon: "🍲", title: "Meals", subtitle: "Reusable meals" },
  { href: "/diets", icon: "🥗", title: "Diets", subtitle: "Day-plan templates" },
  { href: "/groceries", icon: "🛒", title: "Groceries", subtitle: "Shopping lists from your plan" },
  { href: "/help", icon: "📖", title: "Help", subtitle: "Guides for every feature" },
];

function MiscInner() {
  return (
    <div className="flex flex-col min-h-dvh" style={{ background: C.bg }}>
      <div style={{ padding: "14px 16px 4px" }}>
        <div style={{ font: "700 21px system-ui", color: C.ink }}>More</div>
        <div style={{ font: "400 12.5px system-ui", color: C.muted2, marginTop: 2 }}>Nutrition library &amp; shopping</div>
      </div>
      <div className="flex-1 overflow-y-auto" style={{ padding: "12px 16px", paddingBottom: 120 }}>
        <div style={{ background: C.surface, border: `1px solid ${C.border}`, borderRadius: 16, overflow: "hidden" }}>
          {LINKS.map((l, i) => (
            <Link key={l.href} href={l.href} data-tour={`misc-${l.href.slice(1)}`} style={{ display: "flex", alignItems: "center", padding: "12px 14px", textDecoration: "none", borderBottom: i < LINKS.length - 1 ? `1px solid ${C.border}` : "none" }}>
              <div style={{ width: 38, height: 38, borderRadius: "50%", background: C.bgAlt, display: "flex", alignItems: "center", justifyContent: "center", fontSize: 18 }}>{l.icon}</div>
              <div style={{ marginLeft: 12, flex: 1 }}>
                <div style={{ font: "600 15px system-ui", color: C.ink }}>{l.title}</div>
                <div style={{ font: "400 11.5px system-ui", color: C.muted2 }}>{l.subtitle}</div>
              </div>
              <span style={{ color: "#c4ccd1", fontSize: 18 }}>›</span>
            </Link>
          ))}
        </div>
      </div>
      <NutritionNav />
    </div>
  );
}

export default function MiscPage() {
  return <AuthGuard><MiscInner /></AuthGuard>;
}
