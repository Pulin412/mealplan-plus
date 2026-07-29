"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useOnboarding } from "@/hooks/useOnboarding";
import { OnboardingFlow } from "@/components/onboarding/OnboardingFlow";
import { getMe, updateMe } from "@/lib/api/user";

/** Wraps authenticated pages: redirects to /login when signed out, and blocks the app behind
 *  first-run onboarding (no Today / bottom nav) until it's completed or skipped.
 *
 *  Onboarding completion is authoritative on the server (users.onboarding_completed_at), so it
 *  only shows once per ACCOUNT — not once per device. The local flag is an offline cache: when it
 *  isn't set we ask the server (getMe) before deciding, to avoid flashing onboarding on a device
 *  where the account already onboarded. */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const { done, hydrated, markDone } = useOnboarding();
  const router = useRouter();
  const [serverChecked, setServerChecked] = useState(false);

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  // When the local flag isn't set, confirm against the server before showing onboarding.
  useEffect(() => {
    if (!user || !hydrated || done || serverChecked) return;
    let cancelled = false;
    getMe()
      .then((me) => { if (!cancelled && me.onboardingCompletedAt) markDone(); })
      .catch(() => { /* offline / cold start — fall back to showing onboarding */ })
      .finally(() => { if (!cancelled) setServerChecked(true); });
    return () => { cancelled = true; };
  }, [user, hydrated, done, serverChecked, markDone]);

  // Finishing/skipping onboarding: persist to the server (best-effort) then cache locally.
  const completeOnboarding = () => {
    void updateMe({ onboardingCompleted: true }).catch(() => { /* non-fatal; local flag still set */ });
    markDone();
  };

  if (loading || !user || !hydrated) return <Loading />;
  if (!done && !serverChecked) return <Loading />;
  if (!done) return <OnboardingFlow onDone={completeOnboarding} />;

  return <>{children}</>;
}

function Loading() {
  return (
    <div className="min-h-screen flex items-center justify-center text-[13px]" style={{ color: "#8a949b" }}>
      Loading…
    </div>
  );
}
