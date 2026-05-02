import type { Metadata, Viewport } from "next";
import localFont from "next/font/local";
import "./globals.css";
import { AuthProvider } from "@/context/AuthContext";

const geistSans = localFont({
  src: "./fonts/GeistVF.woff",
  variable: "--font-geist-sans",
  weight: "100 900",
});
const geistMono = localFont({
  src: "./fonts/GeistMonoVF.woff",
  variable: "--font-geist-mono",
  weight: "100 900",
});

export const metadata: Metadata = {
  title: "MealPlan+",
  description: "Offline-first meal planning and food logging",
  appleWebApp: {
    capable: true,
    title: "MealPlan+",
    statusBarStyle: "black-translucent",
  },
};

// viewport-fit=cover lets the app extend under the iPhone notch / Dynamic Island.
// themeColor here drives the browser chrome colour on Android Chrome.
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
  themeColor: "#1a7a4a",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <head>
        <link rel="manifest" href="/manifest.json" />
        {/* iOS PWA — icon shown on Home Screen after "Add to Home Screen" */}
        <link rel="apple-touch-icon" href="/api/icons/192" />
        <link rel="apple-touch-icon" sizes="152x152" href="/api/icons/152" />
        <link rel="apple-touch-icon" sizes="180x180" href="/api/icons/180" />
        <link rel="apple-touch-icon" sizes="167x167" href="/api/icons/167" />
      </head>
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
