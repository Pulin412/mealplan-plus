// Lightweight visibility lock for journal notes (webapp). The notes themselves aren't encrypted
// (they're plaintext on the server) — this is a casual-privacy gate: a PIN hashed in localStorage,
// checked to reveal notes for the session. Android uses real BiometricPrompt instead.

const PIN_KEY = "mp_journal_pin";

async function sha256(text: string): Promise<string> {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(text));
  return Array.from(new Uint8Array(buf)).map((b) => b.toString(16).padStart(2, "0")).join("");
}

export function hasJournalPin(): boolean {
  if (typeof window === "undefined") return false;
  return !!localStorage.getItem(PIN_KEY);
}

export async function setJournalPin(pin: string): Promise<void> {
  localStorage.setItem(PIN_KEY, await sha256(pin));
}

export async function verifyJournalPin(pin: string): Promise<boolean> {
  const stored = localStorage.getItem(PIN_KEY);
  return stored != null && stored === (await sha256(pin));
}
