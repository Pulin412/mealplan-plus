"use client";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Home, FileText, CalendarDays, Settings, Plus, Activity, Salad, ShoppingCart, Dumbbell, Apple, ChevronRight } from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";
import { logout } from "@/lib/firebase/auth";
import { IosInstallBanner } from "@/components/IosInstallBanner";


// More sheet items (Android MiscSheet)
const moreItems = [
  { href: "/diets",     label: "Diets",     subtitle: "Browse & manage diet plans",   icon: Salad,       color: "#2E7D52", bg: "#E8F5EE" },
  { href: "/health",    label: "Health",    subtitle: "Metrics, weight & activity",   icon: Activity,    color: "#D32F2F", bg: "#FFF0F0" },
  { href: "/grocery",   label: "Grocery",   subtitle: "Your shopping lists",          icon: ShoppingCart,color: "#6A1B9A", bg: "#F3EEFF" },
  { href: "/foods",     label: "Foods",     subtitle: "Browse & add food items",      icon: Apple,       color: "#E65100", bg: "#FFF3E0" },
  { href: "/exercises", label: "Exercises", subtitle: "Exercise catalogue",           icon: Dumbbell,    color: "#1565C0", bg: "#E3F2FD" },
  { href: "/workouts",  label: "Workouts",  subtitle: "Log & view workout sessions",  icon: Dumbbell,    color: "#7B1FA2", bg: "#F3E5F5" },
  { href: "/settings",  label: "Settings",  subtitle: "Preferences & account",        icon: Settings,    color: "#555555", bg: "#F0F0F0" },
];

// Quick-add sheet items (Android QuickAddSheet)
const quickItems = [
  { href: "/log",      label: "Log Today's Meals", subtitle: "Open today's food diary",       icon: FileText,    color: "#2E7D52", bg: "#E8F5EE" },
  { href: "/workouts", label: "Log a Workout",      subtitle: "Record a workout session",      icon: Dumbbell,    color: "#7B1FA2", bg: "#F3E5F5" },
  { href: "/diets",    label: "Build a Diet",       subtitle: "Create a structured diet plan", icon: Salad,       color: "#1E4FBF", bg: "#E8EEFF" },
  { href: "/grocery",  label: "Grocery List",        subtitle: "Manage your shopping list",     icon: ShoppingCart,color: "#6A1B9A", bg: "#F3EEFF" },
  { href: "/health",   label: "Log Health Metric",   subtitle: "Weight, steps, and more",       icon: Activity,    color: "#C05200", bg: "#FFF0E6" },
];

// ── Bottom nav tab ─────────────────────────────────────────────────────────────
function BottomTab({ href, label, icon: Icon, active, onClick }: {
  href?: string; label: string; icon: React.ElementType; active: boolean; onClick?: () => void;
}) {
  const cls = cn(
    "flex flex-col items-center gap-0.5 flex-1 py-1 transition-colors",
    active ? "text-text-primary" : "text-text-muted"
  );
  if (onClick) return (
    <button className={cls} onClick={onClick}>
      <div className={cn("p-1 rounded-md", active && "bg-[#F0F0F0]")}>
        <Icon size={22} />
      </div>
      <span className="text-[11px] font-medium">{label}</span>
    </button>
  );
  return (
    <Link href={href!} className={cls}>
      <div className={cn("p-1 rounded-md", active && "bg-[#F0F0F0]")}>
        <Icon size={22} />
      </div>
      <span className="text-[11px] font-medium">{label}</span>
    </Link>
  );
}

// ── Bottom sheet ──────────────────────────────────────────────────────────────
function BottomSheet({ items, onClose, onNavigate }: {
  items: typeof moreItems;
  onClose: () => void;
  onNavigate: (href: string) => void;
}) {
  return (
    <>
      <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />
      <div className="fixed bottom-0 inset-x-0 z-50 bg-bg-card rounded-t-3xl shadow-xl animate-in slide-in-from-bottom duration-200">
        {/* drag handle */}
        <div className="flex justify-center pt-3 pb-1">
          <div className="w-9 h-1 rounded-full bg-text-muted/30" />
        </div>
        <div className="pb-10">
          {items.map((item, idx) => (
            <div key={item.href}>
              <button
                className="w-full flex items-center gap-3.5 px-5 py-3.5 hover:bg-bg-page transition-colors text-left"
                onClick={() => { onClose(); onNavigate(item.href); }}
              >
                <div className="w-[42px] h-[42px] rounded-xl flex items-center justify-center shrink-0"
                  style={{ background: item.bg }}>
                  <item.icon size={20} style={{ color: item.color }} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-[15px] font-semibold text-text-primary">{item.label}</p>
                  <p className="text-xs text-text-muted">{item.subtitle}</p>
                </div>
                <ChevronRight size={18} className="text-text-muted/50 shrink-0" />
              </button>
              {idx < items.length - 1 && (
                <div className="ml-[74px] mr-5 h-px bg-divider" />
              )}
            </div>
          ))}
        </div>
      </div>
    </>
  );
}

// ── Sidebar nav link (desktop) ─────────────────────────────────────────────────
function SideNavLink({ href, label, icon: Icon, color, bg }: {
  href: string; label: string; icon: React.ElementType; color: string; bg: string;
}) {
  const pathname = usePathname();
  const active = pathname === href || (href !== "/dashboard" && pathname.startsWith(href));
  return (
    <Link
      href={href}
      className={cn(
        "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
        active ? "bg-green-light text-green" : "text-text-secondary hover:bg-bg-page"
      )}
    >
      <div className="w-8 h-8 rounded-lg flex items-center justify-center shrink-0"
        style={{ background: active ? "#E8F5EE" : bg }}>
        <Icon size={16} style={{ color: active ? "#2E7D52" : color }} />
      </div>
      {label}
    </Link>
  );
}

const sideNav = [
  { href: "/dashboard", label: "Home",      icon: Home,         color: "#2E7D52", bg: "#E8F5EE" },
  { href: "/log",       label: "Log",       icon: FileText,     color: "#F59E0B", bg: "#FFF8E6" },
  { href: "/plan",      label: "Plan",      icon: CalendarDays, color: "#7C3AED", bg: "#F3EEFF" },
  { href: "/health",    label: "Health",    icon: Activity,     color: "#D32F2F", bg: "#FFF0F0" },
  { href: "/diets",     label: "Diets",     icon: Salad,        color: "#2E7D52", bg: "#E8F5EE" },
  { href: "/foods",     label: "Foods",     icon: Apple,        color: "#E65100", bg: "#FFF3E0" },
  { href: "/exercises", label: "Exercises", icon: Dumbbell,     color: "#1565C0", bg: "#E3F2FD" },
  { href: "/workouts",  label: "Workouts",  icon: Dumbbell,     color: "#7B1FA2", bg: "#F3E5F5" },
  { href: "/grocery",   label: "Grocery",   icon: ShoppingCart, color: "#6A1B9A", bg: "#F3EEFF" },
  { href: "/settings",  label: "Settings",  icon: Settings,     color: "#555555", bg: "#F0F0F0" },
];

// ── Layout ────────────────────────────────────────────────────────────────────
export default function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const [sheet, setSheet] = useState<"more" | "quick" | null>(null);

  const handleSignOut = async () => {
    await logout();
    router.replace("/login");
  };

  const isActive = (href: string) =>
    pathname === href || (href !== "/dashboard" && pathname.startsWith(href));

  return (
    <div className="min-h-screen flex bg-bg-page">

      {/* ── Sidebar (desktop only) ── */}
      <aside className="hidden md:flex flex-col w-60 border-r border-divider bg-bg-card px-3 py-5 gap-1 shrink-0">
        <p className="px-3 pb-5 text-[22px] font-semibold text-green">MealPlan+</p>
        {sideNav.map((item) => <SideNavLink key={item.href} {...item} />)}
        <div className="flex-1" />
        <button
          onClick={handleSignOut}
          className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium text-text-muted hover:bg-bg-page transition-colors"
        >
          <div className="w-8 h-8 rounded-lg bg-[#F0F0F0] flex items-center justify-center">
            <Settings size={16} className="text-text-muted" />
          </div>
          Sign out
        </button>
      </aside>

      {/* ── Main content ── */}
      <main className="flex-1 flex flex-col min-h-screen overflow-hidden">
        <div className="flex-1 px-4 pt-5 pb-24 md:p-6 md:pb-6 max-w-xl mx-auto w-full">
          {children}
        </div>

        {/* ── Bottom nav (mobile only) ── */}
        <nav className="md:hidden fixed bottom-0 inset-x-0 bg-bg-card border-t border-divider z-30"
          style={{ paddingBottom: "env(safe-area-inset-bottom)" }}>
          <div className="flex items-center h-16 px-2">
            {/* Home */}
            <BottomTab href="/dashboard" label="Home" icon={Home} active={isActive("/dashboard")} />
            {/* Log */}
            <BottomTab href="/log" label="Log" icon={FileText} active={isActive("/log")} />

            {/* Centre FAB */}
            <div className="flex-1 flex justify-center pb-2">
              <button
                className="w-12 h-12 rounded-full bg-text-primary flex items-center justify-center shadow-sm active:scale-95 transition-transform"
                onClick={() => setSheet("quick")}
              >
                <Plus size={24} className="text-bg-card" />
              </button>
            </div>

            {/* Plan */}
            <BottomTab href="/plan" label="Plan" icon={CalendarDays} active={isActive("/plan")} />
            {/* More */}
            <BottomTab label="More" icon={Settings} active={false} onClick={() => setSheet("more")} />
          </div>
        </nav>
      </main>

      {/* ── Sheets ── */}
      {sheet === "more" && (
        <BottomSheet
          items={moreItems}
          onClose={() => setSheet(null)}
          onNavigate={(href) => router.push(href)}
        />
      )}
      {sheet === "quick" && (
        <BottomSheet
          items={quickItems}
          onClose={() => setSheet(null)}
          onNavigate={(href) => router.push(href)}
        />
      )}

      {/* iOS Safari install nudge — shown once, dismissed to localStorage */}
      <IosInstallBanner />
    </div>
  );
}
