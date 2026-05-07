// Clears all caches and unregisters this service worker.
// Replaces the old Serwist worker that was caching stale JS chunks.
self.addEventListener("install", () => self.skipWaiting());
self.addEventListener("activate", async () => {
  const keys = await caches.keys();
  await Promise.all(keys.map((k) => caches.delete(k)));
  await self.registration.unregister();
  self.clients.matchAll().then((clients) => clients.forEach((c) => c.navigate(c.url)));
});
