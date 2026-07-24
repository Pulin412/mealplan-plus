import { getFirebaseAuth } from "@/lib/auth/firebase";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://mealplan-api-rfo22lhanq-ez.a.run.app";

async function getToken(): Promise<string> {
  const user = getFirebaseAuth().currentUser;
  if (!user) throw new Error("Not authenticated");
  return user.getIdToken();
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = await getToken();
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...options.headers,
    },
  });
  if (!res.ok) {
    const text = await res.text().catch(() => res.statusText);
    throw new Error(`${res.status}: ${text}`);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}
