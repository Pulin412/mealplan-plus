"use client";

import { useState } from "react";
import Link from "next/link";
import { useAuth } from "@/hooks/useAuth";

const C = {
  ink: "#14181b", muted: "#8a949b", border: "#eaeef0",
  surface: "#ffffff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b",
};

export default function ForgotPasswordPage() {
  const { sendPasswordReset } = useAuth();
  const [email, setEmail] = useState("");
  const [busy, setBusy] = useState(false);
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await sendPasswordReset(email);
      setSentTo(email.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message.replace(/^Firebase:\s*/, "") : "Something went wrong");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-5" style={{ background: C.bg }}>
      <div className="w-full max-w-[360px]">
        <div className="text-center mb-6">
          <div className="text-[22px] font-bold" style={{ color: C.teal }}>EatMyPlan</div>
          <div className="text-[13px] mt-1" style={{ color: C.muted }}>
            {sentTo ? "Check your inbox" : "Reset password"}
          </div>
        </div>

        {sentTo ? (
          <div className="rounded-[12px] p-4 flex flex-col gap-3"
            style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            <p className="text-[13px]" style={{ color: C.muted }}>
              We sent a password reset link to <span style={{ color: C.ink }}>{sentTo}</span>. Open it to set a new password.
            </p>
            <Link href="/login"
              className="rounded-[8px] py-2 text-[13px] font-semibold text-white text-center"
              style={{ background: C.teal }}>
              Back to log in
            </Link>
          </div>
        ) : (
          <form onSubmit={onSubmit}
            className="rounded-[12px] p-4 flex flex-col gap-3"
            style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            <p className="text-[13px]" style={{ color: C.muted }}>
              Enter the email for your account and we&apos;ll send you a secure link to set a new password.
            </p>
            <label className="flex flex-col gap-1">
              <span className="text-[11px] font-semibold" style={{ color: C.muted }}>Email</span>
              <input type="email" required autoComplete="email" value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="rounded-[8px] px-3 py-2 text-[13px] outline-none"
                style={{ border: `1px solid ${C.border}`, color: C.ink }} />
            </label>

            {error && <div className="text-[11.5px]" style={{ color: C.danger }}>{error}</div>}

            <button type="submit" disabled={busy || !email}
              className="rounded-[8px] py-2 text-[13px] font-semibold text-white disabled:opacity-60"
              style={{ background: C.teal }}>
              {busy ? "Please wait…" : "Send reset link"}
            </button>
          </form>
        )}

        <div className="text-center mt-4 text-[12px]" style={{ color: C.muted }}>
          <Link href="/login" className="font-semibold" style={{ color: C.teal }}>Back to log in</Link>
        </div>
      </div>
    </div>
  );
}
