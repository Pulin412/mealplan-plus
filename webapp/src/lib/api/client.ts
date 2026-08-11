import { getFirebaseAuth } from "@/lib/auth/firebase";
import { ApiError, codeForStatus } from "@/lib/api/errors";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "https://mealplan-api-rfo22lhanq-ez.a.run.app";

async function getToken(): Promise<string> {
  const user = getFirebaseAuth().currentUser;
  if (!user) throw new ApiError({ code: "UNAUTHORIZED", status: 401 });
  return user.getIdToken();
}

export async function apiFetch<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = await getToken();

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
        ...options.headers,
      },
    });
  } catch {
    // fetch() rejects only on network failure (offline, DNS, CORS) — the request never landed.
    throw new ApiError({ code: "OFFLINE", status: 0 });
  }

  if (!res.ok) {
    // The backend returns a structured ApiError envelope; parse it so we get a stable code +
    // requestId instead of a raw "500: <text>" string. Fall back to the status if the body
    // isn't our envelope (e.g. an infra-level 502 from Cloud Run before the app is reached).
    const headerRequestId = res.headers.get("X-Request-Id") ?? undefined;
    const raw = await res.text().catch(() => "");
    let code = codeForStatus(res.status);
    let requestId = headerRequestId;
    let serverMessage: string | undefined;
    try {
      const body = raw ? JSON.parse(raw) : null;
      if (body && typeof body.code === "string") code = body.code;
      if (body && typeof body.requestId === "string") requestId = body.requestId;
      if (body && typeof body.message === "string") serverMessage = body.message;
    } catch {
      /* non-JSON body — keep the status-derived code */
    }
    throw new ApiError({ code, status: res.status, requestId, serverMessage });
  }

  if (res.status === 204) return undefined as T;
  // Some endpoints (e.g. 201 Created with no body) return an empty response — don't choke on JSON.parse.
  const text = await res.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
