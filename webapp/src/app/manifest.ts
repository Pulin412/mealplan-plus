import type { MetadataRoute } from "next";

// Served at /manifest.webmanifest — makes the webapp an installable PWA (Add to Home Screen).
export default function manifest(): MetadataRoute.Manifest {
  return {
    id: "/",
    name: "MealPlan+",
    short_name: "MealPlan+",
    description: "Offline-first meal planning & food logging",
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#f7f9fa",
    theme_color: "#2f8f9d",
    icons: [
      // PNGs are what installers actually rasterize; maskable has full-bleed padding for adaptive masks.
      { src: "/icon-192.png", sizes: "192x192", type: "image/png", purpose: "any" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/icon-maskable.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
      // Scalable extra for browsers that prefer SVG.
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
    ],
  };
}
