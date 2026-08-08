"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { AuthGuard } from "@/components/auth/AuthGuard";
import { SocialAvatar } from "@/components/SocialAvatar";
import { getMe } from "@/lib/api/user";
import { checkHandleAvailable, updateMyProfile } from "@/lib/api/social";

const C = { ink: "#14181b", muted: "#8a949b", muted2: "#9aa4aa", border: "#eaeef0", surface: "#fff", bg: "#f7f9fa", teal: "oklch(0.62 0.09 210)", danger: "#b23b3b" };

type Avail = "idle" | "checking" | "ok" | "taken" | "invalid";

function EditInner() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [handle, setHandle] = useState("");
  const [bio, setBio] = useState("");
  const [avatarSeed, setAvatarSeed] = useState("");
  const [searchable, setSearchable] = useState(true);
  const [displayName, setDisplayName] = useState<string | null>(null);
  const [avail, setAvail] = useState<Avail>("idle");
  const [saving, setSaving] = useState(false);
  const [savedOk, setSavedOk] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const debounce = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    getMe().then((u) => {
      setHandle(u.handle ?? "");
      setBio(u.bio ?? "");
      setAvatarSeed(u.avatarSeed ?? u.handle ?? "");
      setSearchable(u.isSearchable ?? true);
      setDisplayName(u.displayName ?? null);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  function onHandle(v: string) {
    const h = v.toLowerCase().replace(/[^a-z0-9_]/g, "").slice(0, 20);
    setHandle(h); setSavedOk(false);
    setAvail(h ? "checking" : "idle");
    clearTimeout(debounce.current);
    if (!h) return;
    debounce.current = setTimeout(async () => {
      try {
        const r = await checkHandleAvailable(h);
        setAvail(!r.valid ? "invalid" : r.available ? "ok" : "taken");
      } catch { setAvail("idle"); }
    }, 400);
  }

  async function save() {
    setSaving(true); setError(null); setSavedOk(false);
    try {
      await updateMyProfile({
        handle: handle || null,
        bio: bio || null,
        avatarSeed: avatarSeed || null,
        isSearchable: searchable,
      });
      setSavedOk(true);
    } catch (e) {
      const msg = String((e as Error).message ?? "");
      setError(msg.startsWith("409") ? "That handle is already taken" : msg.startsWith("400") ? "Handle must be 3–20 chars: a–z, 0–9, _" : "Couldn't save");
    } finally { setSaving(false); }
  }

  const hint = avail === "ok" ? [`@${handle} is available`, C.teal] : avail === "taken" ? [`@${handle} is taken`, C.danger]
    : avail === "invalid" ? ["3–20 chars: a–z, 0–9, _", C.danger] : avail === "checking" ? ["Checking…", C.muted]
    : ["Your unique @handle so others can find you", C.muted];

  return (
    <div className="min-h-screen" style={{ background: C.bg }}>
      <div className="flex items-center gap-2 px-3 py-3">
        <button onClick={() => router.back()} style={{ fontSize: 22, color: C.ink }}>‹</button>
        <h1 className="text-[17px] font-bold" style={{ color: C.ink }}>Public profile</h1>
      </div>
      {loading ? <div className="p-8 text-center" style={{ color: C.muted }}>Loading…</div> : (
        <div className="px-4 pb-24 max-w-md mx-auto">
          <div className="flex flex-col items-center py-3">
            <SocialAvatar seed={avatarSeed} label={displayName ?? handle} size={84} />
            <button onClick={() => setAvatarSeed(Math.random().toString(36).slice(2, 10))}
              className="mt-2 text-[13px] font-semibold" style={{ color: C.teal }}>🔀 Shuffle avatar</button>
          </div>

          <label className="block text-[12px] font-semibold mb-1" style={{ color: C.muted }}>Handle</label>
          <div className="flex items-center rounded-[10px] px-3 py-2" style={{ background: C.surface, border: `1px solid ${C.border}` }}>
            <span style={{ color: C.muted }}>@</span>
            <input value={handle} onChange={(e) => onHandle(e.target.value)} placeholder="yourhandle"
              className="flex-1 outline-none bg-transparent text-[14px] ml-0.5" style={{ color: C.ink }} />
          </div>
          <div className="text-[11.5px] mt-1 mb-3" style={{ color: hint[1] as string }}>{hint[0]}</div>

          <label className="block text-[12px] font-semibold mb-1" style={{ color: C.muted }}>Bio</label>
          <textarea value={bio} onChange={(e) => setBio(e.target.value.slice(0, 300))} rows={3}
            className="w-full rounded-[10px] px-3 py-2 outline-none text-[14px]" style={{ background: C.surface, border: `1px solid ${C.border}`, color: C.ink }} />
          <div className="text-right text-[11px] mt-0.5" style={{ color: C.muted }}>{bio.length}/300</div>

          <div className="flex items-center justify-between mt-3 mb-5">
            <div>
              <div className="text-[14px] font-semibold" style={{ color: C.ink }}>Show me in search</div>
              <div className="text-[11.5px]" style={{ color: C.muted }}>Let others discover you by handle or name</div>
            </div>
            <div onClick={() => { setSearchable((v) => !v); setSavedOk(false); }}
              style={{ width: 44, height: 26, borderRadius: 13, background: searchable ? C.teal : "#d7dde0", position: "relative", cursor: "pointer" }}>
              <div style={{ position: "absolute", top: 3, left: searchable ? 21 : 3, width: 20, height: 20, borderRadius: 10, background: "#fff", transition: "left .15s" }} />
            </div>
          </div>

          {error && <div className="text-[13px] mb-2" style={{ color: C.danger }}>{error}</div>}
          {savedOk && <div className="text-[13px] mb-2 font-semibold" style={{ color: C.teal }}>Saved ✓</div>}
          <button onClick={save} disabled={saving || avail === "taken" || avail === "invalid"}
            className="w-full rounded-[11px] py-3 font-bold text-[14px]"
            style={{ background: C.teal, color: "#fff", opacity: saving || avail === "taken" || avail === "invalid" ? 0.6 : 1 }}>
            {saving ? "Saving…" : "Save"}
          </button>
        </div>
      )}
    </div>
  );
}

export default function Page() {
  return <AuthGuard><EditInner /></AuthGuard>;
}
