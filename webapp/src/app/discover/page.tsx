"use client";

import { useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { ProfileRow } from "@/components/social/ProfileRow";
import { searchUsers, type PublicProfileSummaryDto } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)" };

function DiscoverInner() {
  const router = useRouter();
  const [q, setQ] = useState("");
  const [results, setResults] = useState<PublicProfileSummaryDto[]>([]);
  const [loading, setLoading] = useState(false);
  const debounce = useRef<ReturnType<typeof setTimeout>>();

  function onQuery(v: string) {
    setQ(v);
    clearTimeout(debounce.current);
    if (!v.trim()) { setResults([]); setLoading(false); return; }
    setLoading(true);
    debounce.current = setTimeout(async () => {
      try { setResults(await searchUsers(v.trim())); } finally { setLoading(false); }
    }, 350);
  }

  return (
    <div className="min-h-screen" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <h1 className="text-[17px] font-bold" style={{ color: C.ink }}>Discover people</h1>
      </div>
      <div className="px-4 max-w-md mx-auto">
        <input value={q} onChange={(e) => onQuery(e.target.value)} placeholder="Search by handle or name"
          className="w-full rounded-[10px] px-3 py-2.5 outline-none text-[14px]" style={{ background: C.surface, border: `1px solid ${C.border}`, color: C.ink }} />
        <div className="mt-3">
          {loading ? <div className="py-8 text-center" style={{ color: C.muted }}>Searching…</div>
            : q.trim() && results.length === 0 ? <div className="py-8 text-center" style={{ color: C.muted }}>No users found</div>
            : results.map((u) => <ProfileRow key={u.handle} u={u} />)}
        </div>
      </div>
    </div>
  );
}

export default function Page() {
  return <AuthGuard><DiscoverInner /></AuthGuard>;
}
