import { ImageResponse } from "next/og";

export const size = { width: 180, height: 180 };
export const contentType = "image/png";

export default function AppleIcon() {
  return new ImageResponse(
    (
      <div
        style={{
          width: 180,
          height: 180,
          background: "linear-gradient(135deg, #1a7a4a 0%, #22c55e 100%)",
          borderRadius: 40,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          position: "relative",
        }}
      >
        <span
          style={{
            color: "#ffffff",
            fontSize: 96,
            fontWeight: 800,
            fontFamily: "sans-serif",
            lineHeight: 1,
            letterSpacing: "-2px",
          }}
        >
          M
        </span>
        <span
          style={{
            color: "#ffffff",
            fontSize: 44,
            fontWeight: 700,
            position: "absolute",
            top: 22,
            right: 18,
            fontFamily: "sans-serif",
            lineHeight: 1,
          }}
        >
          +
        </span>
      </div>
    ),
    { ...size }
  );
}
