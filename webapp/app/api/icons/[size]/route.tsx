import { ImageResponse } from "next/og";
import { NextRequest } from "next/server";

export const runtime = "edge";

export async function GET(
  _req: NextRequest,
  { params }: { params: { size: string } }
) {
  const dim = Math.min(Math.max(parseInt(params.size, 10) || 192, 16), 512);
  const radius = Math.round(dim * 0.22);
  const fontSize = Math.round(dim * 0.38);
  const plusSize = Math.round(dim * 0.22);
  const plusOffset = Math.round(dim * 0.02);

  return new ImageResponse(
    (
      <div
        style={{
          width: dim,
          height: dim,
          background: "linear-gradient(135deg, #1a7a4a 0%, #22c55e 100%)",
          borderRadius: radius,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          position: "relative",
        }}
      >
        {/* M letter */}
        <span
          style={{
            color: "#ffffff",
            fontSize,
            fontWeight: 800,
            letterSpacing: "-2px",
            lineHeight: 1,
            fontFamily: "sans-serif",
          }}
        >
          M
        </span>
        {/* + superscript */}
        <span
          style={{
            color: "#ffffff",
            fontSize: plusSize,
            fontWeight: 700,
            position: "absolute",
            top: Math.round(dim * 0.12),
            right: Math.round(dim * 0.1) + plusOffset,
            lineHeight: 1,
            fontFamily: "sans-serif",
          }}
        >
          +
        </span>
      </div>
    ),
    { width: dim, height: dim }
  );
}
