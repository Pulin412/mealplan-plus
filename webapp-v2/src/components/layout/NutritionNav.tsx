"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

// Temporary nutrition sub-nav (Foods · Meals · Diets) until the real 4-tab bottom nav
// (Today/Plan/Exercises/Health) lands in Phase 3. Fixed to the bottom of each screen.
const TABS = [
  { href: "/foods", label: "Foods", icon: "🍎" },
  { href: "/meals", label: "Meals", icon: "🍲" },
  { href: "/diets", label: "Diets", icon: "🥗" },
];

const teal = "oklch(0.62 0.09 210)";

export function NutritionNav() {
  const pathname = usePathname();
  return (
    <nav className="fixed bottom-0 inset-x-0 z-30 flex" style={{ background: "#fff", borderTop: "1px solid #eaeef0" }}>
      {TABS.map((t) => {
        const active = pathname === t.href;
        return (
          <Link key={t.href} href={t.href}
            className="flex-1 flex flex-col items-center justify-center gap-0.5 py-[7px] text-[10.5px] font-semibold"
            style={{ color: active ? teal : "#8a949b" }}>
            <span className="text-[16px] leading-none">{t.icon}</span>
            {t.label}
          </Link>
        );
      })}
    </nav>
  );
}
