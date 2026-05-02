"use client";

import { useEffect, useState } from "react";

/**
 * Shows a one-time "Add to Home Screen" nudge on iOS Safari.
 *
 * Conditions for showing:
 *   - Running in iOS Safari (navigator.standalone is defined but false)
 *   - NOT already installed as a PWA (navigator.standalone === false)
 *   - User hasn't dismissed it before (localStorage flag)
 */
export function IosInstallBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const isIos =
      /iphone|ipad|ipod/i.test(navigator.userAgent) &&
      !(navigator as unknown as { standalone?: boolean }).standalone;
    const dismissed = localStorage.getItem("ios-install-dismissed") === "1";
    if (isIos && !dismissed) setVisible(true);
  }, []);

  if (!visible) return null;

  return (
    <div className="fixed bottom-0 left-0 right-0 z-50 p-4 pb-safe">
      <div className="bg-white dark:bg-neutral-900 border border-neutral-200 dark:border-neutral-700 rounded-2xl shadow-xl p-4 flex items-start gap-3">
        {/* App icon */}
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/api/icons/60"
          alt="MealPlan+"
          className="w-12 h-12 rounded-xl flex-shrink-0"
        />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-semibold text-neutral-900 dark:text-white">
            Install MealPlan+
          </p>
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-0.5 leading-relaxed">
            Tap{" "}
            <span className="inline-flex items-center gap-0.5 font-medium text-neutral-700 dark:text-neutral-300">
              Share
              <svg
                className="w-3.5 h-3.5 inline"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth={2}
              >
                <path d="M4 12v8a2 2 0 002 2h12a2 2 0 002-2v-8M16 6l-4-4-4 4M12 2v13" />
              </svg>
            </span>{" "}
            then{" "}
            <span className="font-medium text-neutral-700 dark:text-neutral-300">
              &ldquo;Add to Home Screen&rdquo;
            </span>{" "}
            for the full app experience.
          </p>
        </div>
        <button
          onClick={() => {
            localStorage.setItem("ios-install-dismissed", "1");
            setVisible(false);
          }}
          className="flex-shrink-0 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-200 p-1 -mr-1 -mt-1"
          aria-label="Dismiss"
        >
          <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
            <path d="M18 6L6 18M6 6l12 12" />
          </svg>
        </button>
      </div>
    </div>
  );
}
