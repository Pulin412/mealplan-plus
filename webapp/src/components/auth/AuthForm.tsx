"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";

const C = {
  ink: "#14181b", muted: "#8a949b", border: "#eaeef0",
  surface: "#ffffff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b",
};

type Mode = "login" | "register";

const COPY = {
  login:    { title: "Welcome back", cta: "Sign in",       alt: "Need an account?", altHref: "/register", altLabel: "Create one" },
  register: { title: "Create account", cta: "Create account", alt: "Already have an account?", altHref: "/login", altLabel: "Sign in" },
};

export function AuthForm({ mode }: { mode: Mode }) {
  const { signInEmail, signUpEmail, signInGoogle } = useAuth();
  const router = useRouter();
  const copy = COPY[mode];

  const [email, setEmail]       = useState("");
  const [password, setPassword] = useState("");
  const [busy, setBusy]         = useState(false);
  const [error, setError]       = useState<string | null>(null);

  const submit = async (fn: () => Promise<void>) => {
    setBusy(true);
    setError(null);
    try {
      await fn();
      router.replace("/today");
    } catch (e) {
      setError(e instanceof Error ? e.message.replace(/^Firebase:\s*/, "") : "Something went wrong");
    } finally {
      setBusy(false);
    }
  };

  const onEmailSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    void submit(() => (mode === "login" ? signInEmail(email, password) : signUpEmail(email, password)));
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-5" style={{ background: C.bg }}>
      <div className="w-full max-w-[360px]">
        <div className="text-center mb-6">
          <div className="text-[22px] font-bold" style={{ color: C.teal }}>MealPlan+</div>
          <div className="text-[13px] mt-1" style={{ color: C.muted }}>{copy.title}</div>
        </div>

        <form onSubmit={onEmailSubmit}
          className="rounded-[12px] p-4 flex flex-col gap-3"
          style={{ background: C.surface, border: `1px solid ${C.border}` }}>
          <label className="flex flex-col gap-1">
            <span className="text-[11px] font-semibold" style={{ color: C.muted }}>Email</span>
            <input type="email" required autoComplete="email" value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="rounded-[8px] px-3 py-2 text-[13px] outline-none"
              style={{ border: `1px solid ${C.border}`, color: C.ink }} />
          </label>
          <label className="flex flex-col gap-1">
            <span className="text-[11px] font-semibold" style={{ color: C.muted }}>Password</span>
            <input type="password" required minLength={6}
              autoComplete={mode === "login" ? "current-password" : "new-password"} value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="rounded-[8px] px-3 py-2 text-[13px] outline-none"
              style={{ border: `1px solid ${C.border}`, color: C.ink }} />
          </label>

          {error && <div className="text-[11.5px]" style={{ color: C.danger }}>{error}</div>}

          <button type="submit" disabled={busy}
            className="rounded-[8px] py-2 text-[13px] font-semibold text-white disabled:opacity-60"
            style={{ background: C.teal }}>
            {busy ? "Please wait…" : copy.cta}
          </button>

          <div className="flex items-center gap-2 my-1">
            <span className="flex-1 h-px" style={{ background: C.border }} />
            <span className="text-[10.5px]" style={{ color: C.muted }}>or</span>
            <span className="flex-1 h-px" style={{ background: C.border }} />
          </div>

          <button type="button" disabled={busy} onClick={() => void submit(signInGoogle)}
            className="rounded-[8px] py-2 text-[13px] font-semibold disabled:opacity-60"
            style={{ border: `1px solid ${C.border}`, color: C.ink, background: C.surface }}>
            Continue with Google
          </button>
        </form>

        <div className="text-center mt-4 text-[12px]" style={{ color: C.muted }}>
          {copy.alt}{" "}
          <Link href={copy.altHref} className="font-semibold" style={{ color: C.teal }}>{copy.altLabel}</Link>
        </div>
      </div>
    </div>
  );
}
