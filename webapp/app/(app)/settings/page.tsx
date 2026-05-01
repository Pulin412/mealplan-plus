"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { RefreshCw, LogOut, ChevronRight, Save, Download } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { logout } from "@/lib/firebase/auth";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type SyncPullResponse = components["schemas"]["SyncPullResponse"];

// Inline until types.generated updates
interface UserResponse {
  id: number;
  firebaseUid: string;
  email?: string;
  displayName?: string;
  age?: number;
  weightKg?: number;
  heightCm?: number;
  gender?: string;
  activityLevel?: string;
  targetCalories?: number;
  goalType?: string;
}

const THEME_KEY = "mealplan_theme";
const ACTIVITY_LEVELS = ["SEDENTARY", "LIGHT", "MODERATE", "ACTIVE", "VERY_ACTIVE"];
const GOAL_TYPES = ["LOSE_WEIGHT", "MAINTAIN", "GAIN_WEIGHT"];

function getStoredTheme(): "light" | "dark" {
  try { return (localStorage.getItem(THEME_KEY) as "light" | "dark") ?? "light"; }
  catch { return "light"; }
}
function applyTheme(theme: "light" | "dark") {
  document.documentElement.classList.toggle("dark", theme === "dark");
  localStorage.setItem(THEME_KEY, theme);
}

/** Mifflin-St Jeor TDEE */
function calcTDEE(age: number, weightKg: number, heightCm: number, gender: string, activityLevel: string): number {
  const bmr = gender === "FEMALE"
    ? 10 * weightKg + 6.25 * heightCm - 5 * age - 161
    : 10 * weightKg + 6.25 * heightCm - 5 * age + 5;
  const multipliers: Record<string, number> = {
    SEDENTARY: 1.2, LIGHT: 1.375, MODERATE: 1.55, ACTIVE: 1.725, VERY_ACTIVE: 1.9,
  };
  return Math.round(bmr * (multipliers[activityLevel] ?? 1.55));
}

function SettingRow({ label, subtitle, right, onClick, danger = false }: {
  label: string; subtitle?: string; right?: React.ReactNode; onClick?: () => void; danger?: boolean;
}) {
  return (
    <button onClick={onClick} disabled={!onClick}
      className={["w-full flex items-center gap-3 px-4 py-3.5 text-left transition-colors",
        onClick ? "hover:bg-bg-page" : "cursor-default"].join(" ")}>
      <div className="flex-1 min-w-0">
        <p className={["text-[14px] font-medium", danger ? "text-red-500" : "text-text-primary"].join(" ")}>{label}</p>
        {subtitle && <p className="text-[11px] text-text-muted mt-0.5">{subtitle}</p>}
      </div>
      {right ?? (onClick && <ChevronRight size={16} className="text-text-muted shrink-0" />)}
    </button>
  );
}

export default function SettingsPage() {
  const { user } = useAuth();
  const router = useRouter();
  const [profile, setProfile] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [theme, setTheme] = useState<"light" | "dark">("light");
  const [signingOut, setSigningOut] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [syncResult, setSyncResult] = useState<string | null>(null);

  // Profile edit
  const [editingProfile, setEditingProfile] = useState(false);
  const [formName, setFormName] = useState("");
  const [formAge, setFormAge] = useState("");
  const [formWeight, setFormWeight] = useState("");
  const [formHeight, setFormHeight] = useState("");
  const [formGender, setFormGender] = useState("MALE");
  const [formActivity, setFormActivity] = useState("MODERATE");
  const [formGoal, setFormGoal] = useState("MAINTAIN");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const stored = getStoredTheme();
    setTheme(stored); applyTheme(stored);
  }, []);

  useEffect(() => {
    if (!user) return;
    api.get<UserResponse>("/api/v1/users/me")
      .then((p) => {
        setProfile(p);
        setFormName(p.displayName ?? "");
        setFormAge(p.age?.toString() ?? "");
        setFormWeight(p.weightKg?.toString() ?? "");
        setFormHeight(p.heightCm?.toString() ?? "");
        setFormGender(p.gender ?? "MALE");
        setFormActivity(p.activityLevel ?? "MODERATE");
        setFormGoal(p.goalType ?? "MAINTAIN");
      })
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load profile"))
      .finally(() => setLoading(false));
  }, [user]);

  const toggleTheme = () => { const next = theme === "light" ? "dark" : "light"; setTheme(next); applyTheme(next); };

  const handleSignOut = async () => {
    setSigningOut(true);
    try { await logout(); router.replace("/login"); }
    catch (e: unknown) { setError(e instanceof Error ? e.message : "Sign out failed"); setSigningOut(false); }
  };

  const handleSync = async () => {
    setSyncing(true); setSyncResult(null);
    try {
      const result = await api.get<SyncPullResponse>("/api/v1/sync/pull?since=1970-01-01T00:00:00Z");
      const total = result.foods.length + result.meals.length + result.diets.length +
        result.healthMetrics.length + result.groceryLists.length + result.dailyLogs.length;
      setSyncResult(`Pulled ${total} records · ${new Date(result.serverTime).toLocaleTimeString()}`);
    } catch (e: unknown) {
      setSyncResult(`Sync failed: ${e instanceof Error ? e.message : "unknown error"}`);
    } finally { setSyncing(false); }
  };

  const handleExport = async () => {
    try {
      const data = await api.get<SyncPullResponse>("/api/v1/sync/pull?since=1970-01-01T00:00:00Z");
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `mealplan-export-${new Date().toISOString().split("T")[0]}.json`;
      a.click();
      URL.revokeObjectURL(url);
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Export failed"); }
  };

  const handleSaveProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await api.put<UserResponse>("/api/v1/users/me", {
        displayName: formName || undefined,
        age: formAge ? parseInt(formAge) : undefined,
        weightKg: formWeight ? parseFloat(formWeight) : undefined,
        heightCm: formHeight ? parseFloat(formHeight) : undefined,
        gender: formGender,
        activityLevel: formActivity,
        goalType: formGoal,
      });
      setProfile(updated);
      setEditingProfile(false);
    } catch (e: unknown) { setError(e instanceof Error ? e.message : "Failed to save"); }
    finally { setSaving(false); }
  };

  const age = formAge ? parseInt(formAge) : profile?.age;
  const weightKg = formWeight ? parseFloat(formWeight) : profile?.weightKg;
  const heightCm = formHeight ? parseFloat(formHeight) : profile?.heightCm;
  const tdee = age && weightKg && heightCm
    ? calcTDEE(age, weightKg, heightCm, formGender, formActivity)
    : null;

  const displayName = profile?.displayName ?? user?.displayName ?? user?.email?.split("@")[0] ?? "You";
  const initial = displayName[0]?.toUpperCase() ?? "?";

  return (
    <div className="space-y-4">
      <h1 className="text-[22px] font-semibold text-text-primary pt-1">Settings</h1>

      {error && <div className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-xl px-3 py-2">{error}</div>}

      {/* ── Profile ── */}
      <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
        <div className="flex items-center gap-4 px-4 py-4 border-b border-divider">
          <div className="w-12 h-12 rounded-full bg-text-primary flex items-center justify-center shrink-0">
            <span className="text-[18px] font-bold text-bg-card">{initial}</span>
          </div>
          <div className="flex-1 min-w-0">
            {loading ? (
              <><Skeleton className="h-5 w-36 mb-1" /><Skeleton className="h-4 w-48" /></>
            ) : (
              <>
                <p className="text-[15px] font-semibold text-text-primary truncate">{displayName}</p>
                <p className="text-[12px] text-text-muted truncate">{profile?.email ?? user?.email ?? "—"}</p>
              </>
            )}
          </div>
          <button
            onClick={() => setEditingProfile((v) => !v)}
            className="shrink-0 text-[12px] font-medium text-text-muted hover:text-text-primary border border-divider rounded-lg px-2.5 py-1"
          >
            {editingProfile ? "Cancel" : "Edit"}
          </button>
        </div>

        {editingProfile && (
          <form onSubmit={handleSaveProfile} className="px-4 py-3 space-y-3 border-b border-divider">
            <div className="grid grid-cols-2 gap-3">
              <div className="col-span-2">
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Display name</p>
                <input value={formName} onChange={(e) => setFormName(e.target.value)} placeholder="Your name"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Age</p>
                <input type="number" value={formAge} onChange={(e) => setFormAge(e.target.value)} placeholder="e.g. 30" min={1} max={120}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Gender</p>
                <select value={formGender} onChange={(e) => setFormGender(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary">
                  <option value="MALE">Male</option>
                  <option value="FEMALE">Female</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Weight (kg)</p>
                <input type="number" step="0.1" value={formWeight} onChange={(e) => setFormWeight(e.target.value)} placeholder="e.g. 75"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Height (cm)</p>
                <input type="number" value={formHeight} onChange={(e) => setFormHeight(e.target.value)} placeholder="e.g. 175"
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary" />
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Activity level</p>
                <select value={formActivity} onChange={(e) => setFormActivity(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary">
                  {ACTIVITY_LEVELS.map((l) => <option key={l} value={l}>{l.charAt(0) + l.slice(1).toLowerCase().replace("_", " ")}</option>)}
                </select>
              </div>
              <div>
                <p className="text-[10px] font-bold text-text-muted uppercase mb-1">Goal</p>
                <select value={formGoal} onChange={(e) => setFormGoal(e.target.value)}
                  className="w-full rounded-lg border border-divider bg-bg-page px-3 py-2 text-sm text-text-primary outline-none focus:border-text-primary">
                  {GOAL_TYPES.map((g) => <option key={g} value={g}>{g.replace("_", " ").toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase())}</option>)}
                </select>
              </div>
            </div>

            {/* TDEE preview */}
            {tdee && (
              <div className="bg-green-light rounded-lg px-3 py-2 flex items-center justify-between">
                <p className="text-[11px] text-text-muted">Estimated TDEE (Mifflin-St Jeor)</p>
                <p className="text-[13px] font-bold text-green">{tdee} kcal/day</p>
              </div>
            )}

            <button type="submit" disabled={saving}
              className="w-full rounded-xl bg-text-primary py-2.5 text-sm font-semibold text-bg-card disabled:opacity-50 flex items-center justify-center gap-2">
              <Save size={14} /> {saving ? "Saving…" : "Save profile"}
            </button>
          </form>
        )}

        {/* Stats preview */}
        {!editingProfile && !loading && profile && (
          <div className="px-4 py-3 grid grid-cols-3 gap-3 border-b border-divider">
            {[
              { label: "Age", val: profile.age ? `${profile.age}y` : "—" },
              { label: "Weight", val: profile.weightKg ? `${profile.weightKg}kg` : "—" },
              { label: "Goal", val: profile.goalType ? profile.goalType.replace("_", " ").toLowerCase() : "—" },
            ].map(({ label, val }) => (
              <div key={label} className="text-center">
                <p className="text-[13px] font-semibold text-text-primary">{val}</p>
                <p className="text-[10px] text-text-muted">{label}</p>
              </div>
            ))}
          </div>
        )}

        <SettingRow
          label={signingOut ? "Signing out…" : "Sign out"}
          subtitle="Log out of your account"
          danger
          onClick={handleSignOut}
          right={<LogOut size={16} className="text-red-400 shrink-0" />}
        />
      </div>

      {/* ── Appearance ── */}
      <div>
        <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Appearance</p>
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden">
          <div className="flex items-center justify-between px-4 py-3.5">
            <div>
              <p className="text-[14px] font-medium text-text-primary">Theme</p>
              <p className="text-[11px] text-text-muted mt-0.5">{theme === "dark" ? "Dark mode on" : "Light mode on"}</p>
            </div>
            <button onClick={toggleTheme}
              className={["relative w-12 h-6 rounded-full transition-colors",
                theme === "dark" ? "bg-text-primary" : "bg-bg-page border border-divider"].join(" ")}>
              <span className={["absolute top-0.5 w-5 h-5 rounded-full bg-bg-card shadow transition-transform",
                theme === "dark" ? "translate-x-6" : "translate-x-0.5"].join(" ")} />
            </button>
          </div>
        </div>
      </div>

      {/* ── Data & Sync ── */}
      <div>
        <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">Data</p>
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
          <div className="flex items-center justify-between px-4 py-3.5">
            <div>
              <p className="text-[14px] font-medium text-text-primary">Sync now</p>
              <p className="text-[11px] text-text-muted mt-0.5">Pull all records from the server</p>
            </div>
            <button onClick={handleSync} disabled={syncing}
              className="flex items-center gap-1.5 rounded-xl bg-bg-page border border-divider px-3 py-1.5 text-sm font-medium text-text-secondary disabled:opacity-50">
              <RefreshCw size={13} className={syncing ? "animate-spin" : ""} />
              {syncing ? "Syncing…" : "Sync"}
            </button>
          </div>
          {syncResult && (
            <div className="px-4 py-2">
              <p className="text-[11px] text-text-muted">{syncResult}</p>
            </div>
          )}
          <div className="flex items-center justify-between px-4 py-3.5">
            <div>
              <p className="text-[14px] font-medium text-text-primary">Export data</p>
              <p className="text-[11px] text-text-muted mt-0.5">Download all your data as JSON</p>
            </div>
            <button onClick={handleExport}
              className="flex items-center gap-1.5 rounded-xl bg-bg-page border border-divider px-3 py-1.5 text-sm font-medium text-text-secondary">
              <Download size={13} />
              Export
            </button>
          </div>
        </div>
      </div>

      {/* ── About ── */}
      <div>
        <p className="text-[10px] font-extrabold tracking-widest text-text-muted uppercase mb-2">About</p>
        <div className="bg-bg-card rounded-xl border border-divider overflow-hidden divide-y divide-divider">
          {[
            { label: "App",     val: "MealPlan+" },
            { label: "Version", val: "0.1.0" },
            { label: "Phase",   val: "3a · Web Parity" },
          ].map(({ label, val }) => (
            <div key={label} className="flex items-center justify-between px-4 py-3">
              <p className="text-[13px] text-text-muted">{label}</p>
              <p className="text-[13px] font-medium text-text-primary">{val}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
