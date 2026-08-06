"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { hasSeenTour, startTour } from "@/lib/tour";

/** Mounted on Today (inside AuthGuard, so onboarding is already done). On first visit it kicks off
 *  the guided tour once the nav has painted. Renders nothing. */
export function TourAutostart() {
  const router = useRouter();
  useEffect(() => {
    if (hasSeenTour()) return;
    const t = setTimeout(() => { void startTour(router); }, 500);
    return () => clearTimeout(t);
  }, [router]);
  return null;
}
