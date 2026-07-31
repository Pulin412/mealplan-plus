// MealPlan+ service worker — dependency-free runtime caching.
// - Static build assets (content-hashed): stale-while-revalidate.
// - Page navigations: network-first with an offline fallback.
// Cross-origin requests (Firebase auth, the Cloud Run API) are never touched — no
// user data or authed API responses are cached here.
const VERSION = "mp-v2";
const STATIC_CACHE = `${VERSION}-static`;
const PAGE_CACHE = `${VERSION}-pages`;

self.addEventListener("install", () => {
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((k) => !k.startsWith(VERSION)).map((k) => caches.delete(k))))
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const req = event.request;
  if (req.method !== "GET") return;

  const url = new URL(req.url);
  if (url.origin !== self.location.origin) return; // leave Firebase / API / other origins alone

  const isStatic =
    url.pathname.startsWith("/_next/static/") ||
    /\.(css|js|woff2?|png|jpe?g|svg|ico|webp|gif)$/.test(url.pathname);

  if (isStatic) {
    event.respondWith(staleWhileRevalidate(req));
  } else if (req.mode === "navigate") {
    event.respondWith(networkFirst(req));
  }
});

async function staleWhileRevalidate(req) {
  const cache = await caches.open(STATIC_CACHE);
  const cached = await cache.match(req);
  const network = fetch(req)
    .then((res) => {
      if (res && res.ok) cache.put(req, res.clone());
      return res;
    })
    .catch(() => cached);
  return cached || network;
}

async function networkFirst(req) {
  const cache = await caches.open(PAGE_CACHE);
  try {
    const res = await fetch(req);
    if (res && res.ok) cache.put(req, res.clone());
    return res;
  } catch {
    return (await cache.match(req)) || (await cache.match("/")) || Response.error();
  }
}

// ── Web Push: "you haven't logged yet" reminders ─────────────────────────────
// Payload is JSON: { title, body, url }. Sent by the backend via VAPID.
self.addEventListener("push", (event) => {
  let data = {};
  try {
    data = event.data ? event.data.json() : {};
  } catch {
    data = { body: event.data ? event.data.text() : "" };
  }
  const title = data.title || "MealPlan+";
  const options = {
    body: data.body || "Reminder",
    icon: "/icon-192.png",
    badge: "/icon-192.png",
    tag: "mp-reminder", // collapse repeats into one notification
    data: { url: data.url || "/today" },
  };
  event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", (event) => {
  event.notification.close();
  const target = (event.notification.data && event.notification.data.url) || "/today";
  event.waitUntil(
    self.clients.matchAll({ type: "window", includeUncontrolled: true }).then((clients) => {
      for (const c of clients) {
        if (c.url.includes(target) && "focus" in c) return c.focus();
      }
      for (const c of clients) {
        if ("focus" in c) {
          if ("navigate" in c) c.navigate(target);
          return c.focus();
        }
      }
      if (self.clients.openWindow) return self.clients.openWindow(target);
    })
  );
});
