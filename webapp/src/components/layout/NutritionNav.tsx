"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

// Bottom nav: Today / Plan / Exercises / Health / More. The library pages
// (Foods · Meals · Diets · Groceries) live under the "More" tab.
const TABS = [
  { href: "/today", label: "Today", icon: "🏠" },
  { href: "/plan", label: "Plan", icon: "📅" },
  { href: "/exercises", label: "Exercises", icon: "🏋️" },
  { href: "/health", label: "Health", icon: "❤️" },
  { href: "/misc", label: "More", icon: "▦" },
];
// Sub-pages that keep the "More" tab highlighted.
const MISC_PAGES = ["/misc", "/foods", "/meals", "/diets", "/groceries"];

const teal = "oklch(0.62 0.09 210)";

export function NutritionNav() {
  const pathname = usePathname();
  return (
    <nav className="fixed bottom-0 inset-x-0 z-30 flex" style={{ background: "#fff", borderTop: "1px solid #eaeef0" }}>
      {TABS.map((t) => {
        const active = t.href === "/misc" ? MISC_PAGES.includes(pathname) : pathname === t.href;
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
