import type { Metadata, Viewport } from "next";
import "./globals.css";
import { AuthProvider } from "@/hooks/useAuth";
import { ServiceWorkerRegister } from "@/components/ServiceWorkerRegister";

// DM Mono for numerals (spec §3) — uncomment once @next/font is available:
// import { DM_Mono } from "next/font/google";
// const dmMono = DM_Mono({ subsets: ["latin"], weight: ["400", "500"], variable: "--font-dm-mono" });

export const metadata: Metadata = {
  title: "MealPlan+",
  description: "Offline-first meal planning & food logging",
  manifest: "/manifest.webmanifest",
  icons: {
    icon: [
      { url: "/icon.svg", type: "image/svg+xml" },
      { url: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { url: "/icon-512.png", sizes: "512x512", type: "image/png" },
    ],
    // iOS ignores SVG here — must be a PNG.
    apple: "/apple-touch-icon.png",
  },
  appleWebApp: { capable: true, title: "MealPlan+", statusBarStyle: "default" },
};

export const viewport: Viewport = {
  themeColor: "#2f8f9d",
  // Let the standalone PWA draw under the status bar / home indicator; screens pad with safe-area insets.
  viewportFit: "cover",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className="antialiased">
        <AuthProvider>{children}</AuthProvider>
        <ServiceWorkerRegister />
      </body>
    </html>
  );
}
