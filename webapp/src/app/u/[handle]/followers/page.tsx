"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { ProfileRow } from "@/components/social/ProfileRow";
import { listFollowers, type PublicProfileSummaryDto } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", bg: "#f7f9fa" };

function Inner() {
  const router = useRouter();
  const handle = String(useParams().handle);
  const [list, setList] = useState<PublicProfileSummaryDto[] | null>(null);
  useEffect(() => { listFollowers(handle).then(setList).catch(() => setList([])); }, [handle]);
  return (
    <div className="min-h-screen" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <h1 className="text-[17px] font-bold" style={{ color: C.ink }}>Followers</h1>
      </div>
      <div className="max-w-md mx-auto px-4">
        {list === null ? <div className="py-8 text-center" style={{ color: C.muted }}>Loading…</div>
          : list.length === 0 ? <div className="py-8 text-center" style={{ color: C.muted }}>No followers yet</div>
          : list.map((u) => <ProfileRow key={u.handle} u={u} />)}
      </div>
    </div>
  );
}

export default function Page() {
  return <AuthGuard><Inner /></AuthGuard>;
}
