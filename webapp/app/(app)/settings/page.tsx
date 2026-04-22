"use client";
export const dynamic = "force-dynamic";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { Moon, Sun, RefreshCw } from "lucide-react";
import { useAuth } from "@/context/AuthContext";
import { api } from "@/lib/api/client";
import { logout } from "@/lib/firebase/auth";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import type { components } from "@/lib/api/types.generated";

type UserResponse = components["schemas"]["UserResponse"];
type SyncPullResponse = components["schemas"]["SyncPullResponse"];

const THEME_KEY = "mealplan_theme";

function getStoredTheme(): "light" | "dark" {
  try {
    return (localStorage.getItem(THEME_KEY) as "light" | "dark") ?? "light";
  } catch {
    return "light";
  }
}

function applyTheme(theme: "light" | "dark") {
  document.documentElement.classList.toggle("dark", theme === "dark");
  localStorage.setItem(THEME_KEY, theme);
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

  useEffect(() => {
    // Restore theme on mount
    const stored = getStoredTheme();
    setTheme(stored);
    applyTheme(stored);
  }, []);

  useEffect(() => {
    if (!user) return;
    api.get<UserResponse>("/api/v1/users/me")
      .then(setProfile)
      .catch((e: unknown) => setError(e instanceof Error ? e.message : "Failed to load profile"))
      .finally(() => setLoading(false));
  }, [user]);

  const toggleTheme = () => {
    const next = theme === "light" ? "dark" : "light";
    setTheme(next);
    applyTheme(next);
  };

  const handleSignOut = async () => {
    setSigningOut(true);
    try {
      await logout();
      router.replace("/login");
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Sign out failed");
      setSigningOut(false);
    }
  };

  const handleSync = async () => {
    setSyncing(true);
    setSyncResult(null);
    try {
      const result = await api.get<SyncPullResponse>("/api/v1/sync/pull?since=1970-01-01T00:00:00Z");
      const total =
        result.foods.length +
        result.meals.length +
        result.diets.length +
        result.healthMetrics.length +
        result.groceryLists.length +
        result.dailyLogs.length;
      setSyncResult(`Pulled ${total} records from server (as of ${new Date(result.serverTime).toLocaleTimeString()})`);
    } catch (e: unknown) {
      setSyncResult(`Sync failed: ${e instanceof Error ? e.message : "unknown error"}`);
    } finally {
      setSyncing(false);
    }
  };

  return (
    <div className="space-y-6 max-w-md">
      <h1 className="text-2xl font-bold">Settings</h1>

      {error && (
        <p className="text-sm text-destructive bg-destructive/10 rounded-md px-3 py-2">{error}</p>
      )}

      {/* Profile */}
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Account</CardTitle></CardHeader>
        <CardContent className="space-y-2">
          {loading ? (
            <>
              <Skeleton className="h-5 w-48" />
              <Skeleton className="h-4 w-32" />
            </>
          ) : (
            <>
              <p className="font-semibold">{profile?.displayName ?? user?.displayName ?? "—"}</p>
              <p className="text-sm text-muted-foreground">{profile?.email ?? user?.email ?? "—"}</p>
              {profile?.id && <p className="text-xs text-muted-foreground">User ID: {profile.id}</p>}
            </>
          )}
          <Separator className="my-3" />
          <Button
            variant="destructive"
            size="sm"
            onClick={handleSignOut}
            disabled={signingOut}
          >
            {signingOut ? "Signing out…" : "Sign out"}
          </Button>
        </CardContent>
      </Card>

      {/* Appearance */}
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Appearance</CardTitle></CardHeader>
        <CardContent>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Theme</p>
              <p className="text-xs text-muted-foreground">{theme === "dark" ? "Dark mode" : "Light mode"}</p>
            </div>
            <Button variant="outline" size="sm" onClick={toggleTheme}>
              {theme === "dark" ? <Sun className="h-4 w-4 mr-1" /> : <Moon className="h-4 w-4 mr-1" />}
              {theme === "dark" ? "Light" : "Dark"}
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Data / Sync */}
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">Data</CardTitle></CardHeader>
        <CardContent className="space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm font-medium">Sync now</p>
              <p className="text-xs text-muted-foreground">Pull all records from the server</p>
            </div>
            <Button variant="outline" size="sm" onClick={handleSync} disabled={syncing}>
              <RefreshCw className={["h-4 w-4 mr-1", syncing ? "animate-spin" : ""].join(" ")} />
              {syncing ? "Syncing…" : "Sync"}
            </Button>
          </div>
          {syncResult && (
            <p className="text-xs text-muted-foreground bg-muted rounded px-2 py-1.5">{syncResult}</p>
          )}
        </CardContent>
      </Card>

      {/* App info */}
      <Card>
        <CardHeader className="pb-2"><CardTitle className="text-sm font-medium">About</CardTitle></CardHeader>
        <CardContent className="space-y-1 text-sm text-muted-foreground">
          <p>MealPlan+</p>
          <p>Version 0.1.0</p>
          <p>Phase 3 — Web App (Next.js 14 PWA)</p>
        </CardContent>
      </Card>
    </div>
  );
}
