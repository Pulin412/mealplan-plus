"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { StytchProvider, IdentityProvider } from "@stytch/nextjs";
import { getStytchClient } from "@/lib/auth/stytch";
import { useAuth } from "@/hooks/useAuth";

/**
 * OAuth authorize page for the MCP connector (Stytch Connected Apps authorization URL points here).
 *
 * Stytch redirects the client (e.g. Claude) here with the OAuth query params. We bridge our existing
 * Firebase session to Stytch: if the user isn't signed in we bounce to Firebase login (preserving the
 * OAuth params via ?next=), then hand Stytch the Firebase ID token as a Trusted Auth Token. The Stytch
 * <IdentityProvider> reads the OAuth params from the URL, renders the consent screen, and completes the
 * flow back to the client with an access token that our MCP resource server validates.
 */
const TOKEN_PROFILE_ID = process.env.NEXT_PUBLIC_STYTCH_TOKEN_PROFILE_ID as string | undefined;

// Scopes a Stytch connected app can actually be granted. Clients (e.g. an over-eager DCR registration)
// may request scopes like `full_access` that aren't grantable to third-party apps, which fails the
// authorize with "invalid scope". We normalize the request down to the supported set before consent.
const ALLOWED_SCOPES = new Set(["openid", "offline_access", "profile", "email"]);

export default function AuthorizePage() {
  const { user, loading } = useAuth();
  const router = useRouter();
  const [idToken, setIdToken] = useState<string | null>(null);

  // Strip unsupported scopes from the OAuth request before <IdentityProvider> reads them from the URL.
  useEffect(() => {
    const url = new URL(window.location.href);
    const scope = url.searchParams.get("scope");
    if (!scope) return;
    const kept = scope.split(/\s+/).filter((s) => ALLOWED_SCOPES.has(s));
    const normalized = (kept.length ? kept : ["openid"]).join(" ");
    if (normalized !== scope) {
      url.searchParams.set("scope", normalized);
      window.history.replaceState(null, "", url.toString());
    }
  }, []);

  useEffect(() => {
    if (loading) return;
    if (!user) {
      const next = window.location.pathname + window.location.search;
      router.replace(`/login?next=${encodeURIComponent(next)}`);
      return;
    }
    user.getIdToken().then(setIdToken).catch(() => setIdToken(null));
  }, [user, loading, router]);

  const authTokenParams = useMemo(
    () => (idToken && TOKEN_PROFILE_ID ? { trustedAuthToken: idToken, tokenProfileID: TOKEN_PROFILE_ID } : undefined),
    [idToken],
  );

  if (loading || !user || !idToken) {
    return <Centered>Signing you in…</Centered>;
  }
  if (!TOKEN_PROFILE_ID) {
    return <Centered>Connector auth is not configured (missing NEXT_PUBLIC_STYTCH_TOKEN_PROFILE_ID).</Centered>;
  }

  return (
    <StytchProvider stytch={getStytchClient()}>
      <IdentityProvider authTokenParams={authTokenParams} />
    </StytchProvider>
  );
}

function Centered({ children }: { children: React.ReactNode }) {
  return (
    <main className="min-h-screen flex items-center justify-center px-5 text-center"
      style={{ background: "#f7f9fa", color: "#8a949b", fontSize: 13 }}>
      {children}
    </main>
  );
}
