import { driver, type Driver } from "driver.js";
import "driver.js/dist/driver.css";

/** Minimal shape we need from Next's router (so this stays easy to call/test). */
type RouterLike = { push: (href: string) => void };

const KEY = "mp_tour_seen";

/** Local, per-device flag — cosmetic, mirrors Android's TourStore (a fresh browser re-shows it). */
export function hasSeenTour(): boolean {
  try { return localStorage.getItem(KEY) === "1"; } catch { return true; }
}
export function markTourSeen(): void {
  try { localStorage.setItem(KEY, "1"); } catch { /* ignore */ }
}
export function resetTour(): void {
  try { localStorage.removeItem(KEY); } catch { /* ignore */ }
}

/** Resolve once the selector exists in the DOM (or after [timeout] ms), so we can wait out route
 *  changes before the spotlight anchors to a freshly-rendered element. */
function waitForEl(selector: string, timeout = 3000): Promise<Element | null> {
  return new Promise((resolve) => {
    const start = Date.now();
    const tick = () => {
      const el = document.querySelector(selector);
      if (el) return resolve(el);
      if (Date.now() - start > timeout) return resolve(null);
      requestAnimationFrame(tick);
    };
    tick();
  });
}

// Mirrors the Android tour: the 5 bottom-nav tabs, then drill into the More library, then a
// closing card. Nav tabs are on every page; the misc-* targets only exist on /misc.
function buildSteps() {
  const top = { side: "top" as const, align: "center" as const };
  const drill = { side: "bottom" as const, align: "start" as const };
  return [
    { element: '[data-tour="nav-today"]', popover: { title: "Today", description: "Your daily hub — log meals by slot and watch your calorie ring.", ...top } },
    { element: '[data-tour="nav-plan"]', popover: { title: "Plan", description: "Plan diets across your week and auto-build a grocery list.", ...top } },
    { element: '[data-tour="nav-exercises"]', popover: { title: "Exercises", description: "Workouts, sessions and your exercise library.", ...top } },
    { element: '[data-tour="nav-health"]', popover: { title: "Health", description: "Track glucose, weight & blood pressure with trends.", ...top } },
    { element: '[data-tour="nav-misc"]', popover: { title: "More", description: "Foods, Meals, Diets & Groceries all live under here.", ...top } },
    { element: '[data-tour="misc-foods"]', popover: { title: "Foods", description: "Your ingredient library — add foods here.", ...drill } },
    { element: '[data-tour="misc-meals"]', popover: { title: "Meals", description: "Combine foods into reusable meals.", ...drill } },
    { element: '[data-tour="misc-diets"]', popover: { title: "Diets", description: "Build day-plan diets from your meals, then schedule them in Plan.", ...drill } },
    { element: '[data-tour="misc-groceries"]', popover: { title: "Groceries", description: "Auto-generate a shopping list from your planned diets.", side: "top" as const, align: "start" as const } },
    { popover: { title: "You're all set!", description: "Replay this tour anytime from Settings." } },
  ];
}

// After the "More" nav step (index 4) the tour jumps to the /misc page for the drill-in steps.
const MISC_ENTRY_INDEX = 4;

/** Start the guided spotlight tour. Navigates to Today first (where the nav is), then drives
 *  Driver.js; between the nav map and the drill-in it routes to /misc and waits for it to render. */
export async function startTour(router: RouterLike): Promise<void> {
  markTourSeen(); // one-shot: this run counts as "seen" so it won't auto-open again
  if (typeof window !== "undefined" && window.location.pathname !== "/today") {
    router.push("/today");
  }
  await waitForEl('[data-tour="nav-today"]');

  const d: Driver = driver({
    showProgress: true,
    showButtons: ["next", "close"],
    popoverClass: "mp-tour",
    steps: buildSteps(),
    onNextClick: async () => {
      if (d.getActiveIndex() === MISC_ENTRY_INDEX) {
        router.push("/misc");
        await waitForEl('[data-tour="misc-foods"]');
      }
      d.moveNext();
    },
    onCloseClick: () => { d.destroy(); },
  });
  d.drive();
}
