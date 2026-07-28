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
  icons: { icon: "/icon.svg", apple: "/icon.svg" },
  appleWebApp: { capable: true, title: "MealPlan+", statusBarStyle: "default" },
};

export const viewport: Viewport = {
  themeColor: "#2f8f9d",
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
