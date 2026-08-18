"use client";

import { createStytchUIClient } from "@stytch/nextjs";

// Lazily created on the client only (the Stytch UI client needs `window`). Used solely by the OAuth
// /authorize page to bridge our Firebase session to Stytch Connected Apps for the MCP connector flow.
let _client: ReturnType<typeof createStytchUIClient> | null = null;

export function getStytchClient() {
  if (!_client) {
    _client = createStytchUIClient(process.env.NEXT_PUBLIC_STYTCH_PUBLIC_TOKEN!);
  }
  return _client;
}
