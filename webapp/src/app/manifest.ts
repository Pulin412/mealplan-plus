import type { MetadataRoute } from "next";

// Served at /manifest.webmanifest — makes the webapp an installable PWA (Add to Home Screen).
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: "MealPlan+",
    short_name: "MealPlan+",
    description: "Offline-first meal planning & food logging",
    start_url: "/",
    display: "standalone",
    background_color: "#f7f9fa",
    theme_color: "#2f8f9d",
    icons: [
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "any" },
      { src: "/icon.svg", sizes: "any", type: "image/svg+xml", purpose: "maskable" },
    ],
  };
}
