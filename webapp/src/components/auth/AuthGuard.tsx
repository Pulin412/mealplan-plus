"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { useOnboarding } from "@/hooks/useOnboarding";
import { OnboardingFlow } from "@/components/onboarding/OnboardingFlow";

/** Wraps authenticated pages: redirects to /login when signed out, and blocks the app behind
 *  first-run onboarding (no Today / bottom nav) until it's completed or skipped. */
export function AuthGuard({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const { done, hydrated, markDone } = useOnboarding();
  const router = useRouter();

  useEffect(() => {
    if (!loading && !user) router.replace("/login");
  }, [loading, user, router]);

  if (loading || !user || !hydrated) {
    return (
      <div className="min-h-screen flex items-center justify-center text-[13px]" style={{ color: "#8a949b" }}>
        Loading…
      </div>
    );
  }

  if (!done) return <OnboardingFlow onDone={markDone} />;

  return <>{children}</>;
}
