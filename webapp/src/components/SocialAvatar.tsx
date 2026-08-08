// Deterministic, client-rendered generated avatar (no image storage). A stable seed maps to a
// fixed hue + initials so the same user always looks the same.
const PALETTE = ["#4F46E5", "#0EA5E9", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6", "#14B8A6"];

function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}
function initials(label: string | null | undefined, fallback: string): string {
  const src = (label && label.trim()) || fallback;
  const parts = src.split(/[\s_-]+/).filter(Boolean);
  if (parts.length === 0) return "?";
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[1][0]).toUpperCase();
}

export function SocialAvatar({
  seed,
  label,
  size = 40,
}: {
  seed?: string | null;
  label?: string | null;
  size?: number;
}) {
  const s = (seed && seed.trim()) || label || "?";
  const bg = PALETTE[hash(s) % PALETTE.length];
  return (
    <div
      className="flex-none flex items-center justify-center rounded-full text-white font-bold"
      style={{ width: size, height: size, background: bg, fontSize: size * 0.38 }}
    >
      {initials(label, s)}
    </div>
  );
}
