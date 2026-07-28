"use client";

import { useCallback, useEffect, useState } from "react";

// First-run onboarding is a blocking flow shown before the app. We only persist whether it's
// been completed/skipped (locally, per device). Mirrors Android's OnboardingStore.
const KEY = "mp_onboarding_done";

export function useOnboarding() {
  const [done, setDone] = useState(false);
  const [hydrated, setHydrated] = useState(false);

  useEffect(() => {
    try { setDone(localStorage.getItem(KEY) === "1"); } catch { /* ignore */ }
    setHydrated(true);
  }, []);

  const markDone = useCallback(() => {
    try { localStorage.setItem(KEY, "1"); } catch { /* ignore */ }
    setDone(true);
  }, []);

  const reset = useCallback(() => {
    try { localStorage.removeItem(KEY); } catch { /* ignore */ }
    setDone(false);
  }, []);

  return { done, hydrated, markDone, reset };
}
