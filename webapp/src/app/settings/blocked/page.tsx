"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { SocialAvatar } from "@/components/SocialAvatar";
import { listBlocks, unblockUser, type PublicProfileSummaryDto } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", muted3: "#5b666e", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)" };

function BlockedInner() {
  const router = useRouter();
  const [rows, setRows] = useState<PublicProfileSummaryDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try { setRows(await listBlocks()); }
    catch { setError("Couldn't load your blocked accounts. Check your connection and try again."); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { void load(); }, [load]);

  async function unblock(handle: string) {
    if (busy) return;
    setBusy(handle);
    try {
      await unblockUser(handle);
      setRows((r) => r.filter((x) => x.handle !== handle));
    } catch {
      alert("Couldn't unblock — please try again.");
    } finally { setBusy(null); }
  }

  return (
    <div className="min-h-dvh" style={{ background: C.bg }}>
      <div style={{ display: "flex", alignItems: "center", padding: "10px 16px 8px 6px" }}>
        <button onClick={() => router.back()} style={{ fontSize: 20, color: C.ink, padding: "0 8px", cursor: "pointer" }}>‹</button>
        <span style={{ font: "700 19px system-ui", color: C.ink }}>Blocked accounts</span>
      </div>
      <div className="max-w-md mx-auto px-4">
        {loading ? <div className="p-8 text-center text-[13px]" style={{ color: C.muted }}>Loading…</div>
          : error ? <div className="p-8 text-center text-[13px]" style={{ color: C.muted }}>{error}</div>
          : rows.length === 0 ? <div className="py-16 text-center text-[13.5px]" style={{ color: C.muted }}>You haven&apos;t blocked anyone.</div>
          : rows.map((u) => (
            <div key={u.handle} className="flex items-center gap-3 rounded-[12px] px-3.5 py-3 mb-2" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
              <SocialAvatar seed={u.avatarSeed} label={u.displayName ?? u.handle} size={40} />
              <div className="flex-1 min-w-0">
                {u.displayName && <div className="text-[14px] font-semibold truncate" style={{ color: C.ink }}>{u.displayName}</div>}
                <div className="text-[12.5px]" style={{ color: C.muted }}>@{u.handle}</div>
              </div>
              <button onClick={() => unblock(u.handle)} disabled={busy === u.handle}
                className="rounded-full px-3.5 py-1.5 text-[12.5px] font-semibold"
                style={{ background: C.surface, border: `1px solid ${C.border}`, color: C.ink, opacity: busy === u.handle ? 0.6 : 1 }}>
                {busy === u.handle ? "…" : "Unblock"}
              </button>
            </div>
          ))}
      </div>
    </div>
  );
}

export default function BlockedPage() {
  return <AuthGuard><BlockedInner /></AuthGuard>;
}
