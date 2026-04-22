import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        // Page & surface
        "bg-page":         "var(--bg-page)",
        "bg-card":         "var(--bg-card)",
        divider:           "var(--divider)",
        outline:           "var(--outline)",

        // Text
        "text-primary":    "var(--text-primary)",
        "text-secondary":  "var(--text-secondary)",
        "text-muted":      "var(--text-muted)",
        "text-placeholder":"var(--text-placeholder)",
        "text-destructive":"var(--text-destructive)",

        // Brand
        green: {
          DEFAULT: "var(--green)",
          light:   "var(--green-light)",
        },

        // Macros
        "macro-protein":   "var(--macro-protein)",
        "macro-carbs":     "var(--macro-carbs)",
        "macro-fat":       "var(--macro-fat)",
        "macro-cal":       "var(--macro-cal)",

        // Slots
        "slot-breakfast":  "var(--slot-breakfast)",
        "slot-lunch":      "var(--slot-lunch)",
        "slot-dinner":     "var(--slot-dinner)",
        "slot-snack":      "var(--slot-snack)",

        // shadcn aliases (for ui/ components)
        background:  "var(--background)",
        foreground:  "var(--foreground)",
        card: {
          DEFAULT:    "var(--card)",
          foreground: "var(--card-foreground)",
        },
        primary: {
          DEFAULT:    "var(--primary)",
          foreground: "var(--primary-foreground)",
        },
        secondary: {
          DEFAULT:    "var(--secondary)",
          foreground: "var(--secondary-foreground)",
        },
        muted: {
          DEFAULT:    "var(--muted)",
          foreground: "var(--muted-foreground)",
        },
        accent: {
          DEFAULT:    "var(--accent)",
          foreground: "var(--accent-foreground)",
        },
        destructive: {
          DEFAULT:    "var(--destructive)",
          foreground: "var(--destructive-foreground)",
        },
        border: "var(--border)",
        input:  "var(--input)",
        ring:   "var(--ring)",
      },
      borderRadius: {
        xs:  "4px",
        sm:  "8px",
        md:  "12px",
        lg:  "16px",
        xl:  "24px",
        // keep shadcn defaults
        DEFAULT: "var(--radius)",
      },
      fontSize: {
        "2xs": ["11px", { lineHeight: "16px", letterSpacing: "0.5px" }],
      },
    },
  },
  plugins: [],
};
export default config;
