import Link from "next/link";
import { SocialAvatar } from "@/components/SocialAvatar";
import type { PublicProfileSummaryDto } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", border: "#eaeef0" };

export function ProfileRow({ u }: { u: PublicProfileSummaryDto }) {
  return (
    <Link href={`/u/${u.handle}`} className="flex items-center gap-3 py-2.5" style={{ borderBottom: `1px solid ${C.border}` }}>
      <SocialAvatar seed={u.avatarSeed} label={u.displayName ?? u.handle} size={44} />
      <div className="flex-1 min-w-0">
        <div className="text-[15px] font-semibold truncate" style={{ color: C.ink }}>@{u.handle}</div>
        {u.displayName && <div className="text-[12.5px] truncate" style={{ color: C.muted }}>{u.displayName}</div>}
      </div>
      {u.isFollowedByMe && <span className="text-[12px]" style={{ color: C.muted }}>Following</span>}
    </Link>
  );
}
