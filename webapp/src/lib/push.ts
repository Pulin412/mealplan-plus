// Web Push (VAPID) client helpers for the PWA. Enabling stores a browser push subscription on the
// backend; a scheduled server job then sends "you haven't logged yet" reminders — the only way to
// reach a closed PWA (the web has no on-device scheduler, unlike Android's AlarmManager).

import { apiFetch } from "@/lib/api/client";

const VAPID_PUBLIC_KEY = process.env.NEXT_PUBLIC_VAPID_PUBLIC_KEY ?? "";

export type EnableResult =
  | { ok: true }
  | { ok: false; reason: "unsupported" | "no-key" | "denied" | "ios-not-installed" | "error"; message?: string };

/** Push requires a service worker, the Push API, and the Notification API. */
export function pushSupported(): boolean {
  return (
    typeof window !== "undefined" &&
    "serviceWorker" in navigator &&
    "PushManager" in window &&
    "Notification" in window
  );
}

export function isIos(): boolean {
  if (typeof navigator === "undefined") return false;
  return /iphone|ipad|ipod/i.test(navigator.userAgent) ||
    // iPadOS reports as Mac; detect touch to disambiguate.
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

/** iOS only delivers Web Push when the PWA is installed to the Home Screen (standalone). */
export function isStandalone(): boolean {
  if (typeof window === "undefined") return false;
  return (
    window.matchMedia("(display-mode: standalone)").matches ||
    (navigator as unknown as { standalone?: boolean }).standalone === true
  );
}

function urlBase64ToUint8Array(base64: string): Uint8Array {
  const padding = "=".repeat((4 - (base64.length % 4)) % 4);
  const b64 = (base64 + padding).replace(/-/g, "+").replace(/_/g, "/");
  const raw = atob(b64);
  const arr = new Uint8Array(raw.length);
  for (let i = 0; i < raw.length; i++) arr[i] = raw.charCodeAt(i);
  return arr;
}

export async function currentSubscription(): Promise<PushSubscription | null> {
  if (!pushSupported()) return null;
  const reg = await navigator.serviceWorker.getRegistration();
  return reg ? reg.pushManager.getSubscription() : null;
}

export async function remindersEnabled(): Promise<boolean> {
  if (typeof Notification === "undefined" || Notification.permission !== "granted") return false;
  return (await currentSubscription()) != null;
}

export async function enableReminders(): Promise<EnableResult> {
  if (!pushSupported()) return { ok: false, reason: "unsupported" };
  if (isIos() && !isStandalone()) return { ok: false, reason: "ios-not-installed" };
  if (!VAPID_PUBLIC_KEY) return { ok: false, reason: "no-key" };
  try {
    const permission = await Notification.requestPermission();
    if (permission !== "granted") return { ok: false, reason: "denied" };

    const reg = await navigator.serviceWorker.ready;
    const sub = await reg.pushManager.subscribe({
      userVisibleOnly: true,
      // Cast: lib.dom types applicationServerKey as BufferSource; our Uint8Array satisfies it.
      applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY) as BufferSource,
    });

    const json = sub.toJSON();
    await apiFetch("/api/v1/push/subscriptions", {
      method: "POST",
      body: JSON.stringify({
        endpoint: sub.endpoint,
        keys: { p256dh: json.keys?.p256dh ?? "", auth: json.keys?.auth ?? "" },
        userAgent: navigator.userAgent,
      }),
    });
    return { ok: true };
  } catch (e) {
    return { ok: false, reason: "error", message: e instanceof Error ? e.message : String(e) };
  }
}

export async function disableReminders(): Promise<void> {
  const sub = await currentSubscription();
  if (!sub) return;
  try {
    await apiFetch("/api/v1/push/subscriptions", {
      method: "DELETE",
      body: JSON.stringify({ endpoint: sub.endpoint }),
    });
  } finally {
    await sub.unsubscribe();
  }
}
