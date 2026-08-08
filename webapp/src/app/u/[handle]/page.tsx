"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { SocialAvatar } from "@/components/SocialAvatar";
import {
  followUser, unfollowUser, getPublicProfile, listSharedDiets, listSharedMeals, listSharedWorkouts,
  type PublicProfileDto, type SharedTemplateSummaryDto,
} from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)" };
type Tab = "DIET" | "MEAL" | "WORKOUT_TEMPLATE";
const TAB_PATH: Record<Tab, string> = { DIET: "diets", MEAL: "meals", WORKOUT_TEMPLATE: "workouts" };

function ProfileInner() {
  const router = useRouter();
  const handle = String(useParams().handle);
  const [p, setP] = useState<PublicProfileDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>("DIET");
  const [items, setItems] = useState<Record<Tab, SharedTemplateSummaryDto[]>>({ DIET: [], MEAL: [], WORKOUT_TEMPLATE: [] });
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    setLoading(true); setError(null);
    try {
      const prof = await getPublicProfile(handle);
      setP(prof);
      if (prof.isFollowedByMe || prof.isMe) {
        const [d, m, w] = await Promise.all([listSharedDiets(handle), listSharedMeals(handle), listSharedWorkouts(handle)]);
        setItems({ DIET: d, MEAL: m, WORKOUT_TEMPLATE: w });
      }
    } catch (e) {
      setError(String((e as Error).message).startsWith("403") ? "Unavailable" : "Couldn't load profile");
    } finally { setLoading(false); }
  }, [handle]);

  useEffect(() => { void load(); }, [load]);

  async function toggleFollow() {
    if (!p) return;
    setBusy(true);
    try {
      if (p.isFollowedByMe) await unfollowUser(handle); else await followUser(handle);
      await load();
    } finally { setBusy(false); }
  }

  const tabItems = items[tab];

  return (
    <div className="min-h-screen" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <h1 className="text-[17px] font-bold" style={{ color: C.ink }}>@{handle}</h1>
      </div>
      {loading ? <div className="p-8 text-center" style={{ color: C.muted }}>Loading…</div>
        : error || !p ? <div className="p-8 text-center" style={{ color: C.muted }}>{error ?? "Unavailable"}</div>
        : (
        <div className="max-w-md mx-auto px-4">
          <div className="flex items-center gap-3.5 py-2">
            <SocialAvatar seed={p.avatarSeed} label={p.displayName ?? p.handle} size={64} />
            <div className="flex-1 min-w-0">
              {p.displayName && <div className="text-[16px] font-bold" style={{ color: C.ink }}>{p.displayName}</div>}
              <div className="text-[12.5px]" style={{ color: C.muted }}>
                <Link href={`/u/${handle}/followers`}>{p.followerCount} followers</Link>
                {"  ·  "}
                <Link href={`/u/${handle}/following`}>{p.followingCount} following</Link>
              </div>
            </div>
          </div>
          {p.bio && <div className="text-[13.5px] mb-3" style={{ color: C.ink }}>{p.bio}</div>}

          {!p.isMe && (
            <button onClick={toggleFollow} disabled={busy}
              className="w-full rounded-[11px] py-2.5 font-bold text-[14px] mb-3"
              style={p.isFollowedByMe ? { background: C.surface, border: `1px solid ${C.border}`, color: C.ink } : { background: C.teal, color: "#fff" }}>
              {p.isFollowedByMe ? "Following" : "Follow"}
            </button>
          )}

          {!p.isFollowedByMe && !p.isMe ? (
            <div className="py-16 text-center text-[14px]" style={{ color: C.muted }}>Follow to see their library</div>
          ) : (
            <>
              <div className="flex gap-2 mb-3">
                {(["DIET", "MEAL", "WORKOUT_TEMPLATE"] as Tab[]).map((t) => (
                  <button key={t} onClick={() => setTab(t)}
                    className="rounded-full px-3.5 py-1.5 text-[13px] font-semibold"
                    style={tab === t ? { background: C.teal, color: "#fff" } : { background: C.surface, border: `1px solid ${C.border}`, color: C.ink }}>
                    {t === "DIET" ? "Diets" : t === "MEAL" ? "Meals" : "Workouts"}
                  </button>
                ))}
              </div>
              {tabItems.length === 0 ? <div className="py-10 text-center text-[13px]" style={{ color: C.muted }}>Nothing shared yet</div>
                : tabItems.map((it) => (
                  <Link key={it.serverId} href={`/u/${handle}/${TAB_PATH[tab]}/${it.serverId}`}
                    className="flex items-center justify-between rounded-[12px] px-3.5 py-3 mb-2" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
                    <div className="min-w-0">
                      <div className="text-[15px] font-semibold truncate" style={{ color: C.ink }}>{it.name}</div>
                      {it.subtitle && <div className="text-[12px]" style={{ color: C.muted }}>{it.subtitle}</div>}
                    </div>
                    <span style={{ color: C.muted }}>›</span>
                  </Link>
                ))}
            </>
          )}
        </div>
      )}
    </div>
  );
}

export default function Page() {
  return <AuthGuard><ProfileInner /></AuthGuard>;
}
