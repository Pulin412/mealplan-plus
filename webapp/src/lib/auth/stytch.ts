"use client";

import { createStytchUIClient } from "@stytch/nextjs";

// Lazily created on the client only (the Stytch UI client needs `window`). Used solely by the OAuth
// /authorize page to bridge our Firebase session to Stytch Connected Apps for the MCP connector flow.
let _client: ReturnType<typeof createStytchUIClient> | null = null;

export function getStytchClient() {
  if (!_client) {
    // Point the SDK at our Stytch custom auth domain so the OAuth authorization the IdentityProvider
    // completes — and its RFC 9207 `iss` — use the custom-domain issuer (consistent with the access
    // token + discovery), which strict clients like Claude require. Falls back to Stytch's default
    // domain when unset.
    const customBaseUrl = process.env.NEXT_PUBLIC_STYTCH_CUSTOM_DOMAIN;
    _client = createStytchUIClient(
      process.env.NEXT_PUBLIC_STYTCH_PUBLIC_TOKEN!,
      customBaseUrl ? { customBaseUrl } : undefined,
    );
  }
  return _client;
}
