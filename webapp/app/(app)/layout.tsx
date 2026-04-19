"use client";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { LayoutDashboard, BookOpen, CalendarDays, Activity, Salad, ShoppingCart, Settings } from "lucide-react";
import { cn } from "@/lib/utils";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/log", label: "Log", icon: BookOpen },
  { href: "/plan", label: "Plan", icon: CalendarDays },
  { href: "/health", label: "Health", icon: Activity },
  { href: "/diets", label: "Diets", icon: Salad },
  { href: "/grocery", label: "Grocery", icon: ShoppingCart },
  { href: "/settings", label: "Settings", icon: Settings },
];

function NavLink({ href, label, icon: Icon }: (typeof navItems)[0]) {
  const pathname = usePathname();
  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
  return (
    <Link
      href={href}
      className={cn(
        "flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
        active
          ? "bg-primary text-primary-foreground"
          : "text-muted-foreground hover:bg-muted hover:text-foreground"
      )}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span>{label}</span>
    </Link>
  );
}

function BottomNavLink({ href, label, icon: Icon }: (typeof navItems)[0]) {
  const pathname = usePathname();
  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
  return (
    <Link
      href={href}
      className={cn(
        "flex flex-col items-center gap-0.5 px-2 py-1 text-xs font-medium transition-colors",
        active ? "text-primary" : "text-muted-foreground"
      )}
    >
      <Icon className="h-5 w-5" />
      <span>{label}</span>
    </Link>
  );
}

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen flex">
      {/* Sidebar — desktop only */}
      <aside className="hidden md:flex flex-col w-56 border-r bg-background px-3 py-6 gap-1 shrink-0">
        <p className="px-3 pb-4 text-lg font-bold text-primary">MealPlan+</p>
        {navItems.map((item) => (
          <NavLink key={item.href} {...item} />
        ))}
      </aside>

      {/* Main content */}
      <main className="flex-1 flex flex-col min-h-screen">
        <div className="flex-1 p-4 md:p-6 pb-20 md:pb-6">{children}</div>

        {/* Bottom nav — mobile only */}
        <nav className="md:hidden fixed bottom-0 inset-x-0 border-t bg-background flex justify-around items-center h-14 px-1 z-50">
          {navItems.map((item) => (
            <BottomNavLink key={item.href} {...item} />
          ))}
        </nav>
      </main>
    </div>
  );
}
