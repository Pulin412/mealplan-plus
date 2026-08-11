import { toast } from "sonner";

/**
 * Stable error codes the backend sends in the {@link ApiError} envelope (see
 * `docs/openapi.yaml#ApiError`), plus two client-only codes for failures the server never sees.
 */
export type ApiErrorCode =
  | "VALIDATION"
  | "BAD_REQUEST"
  | "UNAUTHORIZED"
  | "FORBIDDEN"
  | "NOT_FOUND"
  | "CONFLICT"
  | "INTERNAL"
  | "SERVICE_UNAVAILABLE"
  | "OFFLINE" // client-only: the request never reached the server
  | "UNKNOWN"; // client-only: an unrecognized / unparseable failure

/** A typed API failure. Replaces the old `throw new Error("500: <text>")` in {@link apiFetch}. */
export class ApiError extends Error {
  readonly code: ApiErrorCode;
  readonly status: number; // 0 when the request never reached the server
  readonly requestId?: string;
  readonly serverMessage?: string; // the backend's `message`, kept as a fallback only

  constructor(args: {
    code: ApiErrorCode;
    status: number;
    requestId?: string;
    serverMessage?: string;
  }) {
    super(args.serverMessage ?? args.code);
    this.name = "ApiError";
    this.code = args.code;
    this.status = args.status;
    this.requestId = args.requestId;
    this.serverMessage = args.serverMessage;
  }
}

/** Friendly, human copy per code. The single source of user-facing wording on the web client. */
const CODE_MESSAGES: Record<ApiErrorCode, string> = {
  VALIDATION: "Please check the highlighted fields and try again.",
  BAD_REQUEST: "That request wasn't valid. Please check your input and try again.",
  UNAUTHORIZED: "Your session expired. Please sign in again.",
  FORBIDDEN: "You don't have access to this.",
  NOT_FOUND: "We couldn't find what you were looking for.",
  CONFLICT: "That conflicts with something that already exists.",
  INTERNAL: "Something went wrong on our end. Please try again.",
  SERVICE_UNAVAILABLE: "The service is busy right now. Please try again in a moment.",
  OFFLINE: "You appear to be offline. Check your connection and try again.",
  UNKNOWN: "Something went wrong. Please try again.",
};

/** Map any thrown value to a single user-facing line (a request id is appended for server faults). */
export function friendlyMessage(err: unknown): string {
  if (err instanceof ApiError) {
    const base = CODE_MESSAGES[err.code] ?? CODE_MESSAGES.UNKNOWN;
    // Only surface the trace id on server-side faults — it's what support needs to find the incident.
    const showRef = err.status >= 500 || err.code === "INTERNAL";
    return showRef && err.requestId ? `${base} (Ref: ${err.requestId})` : base;
  }
  if (err instanceof Error && err.message) return err.message;
  return CODE_MESSAGES.UNKNOWN;
}

/** Show a failure to the user as a toast. Use in `catch` blocks instead of swallowing the error. */
export function toastApiError(err: unknown): void {
  toast.error(friendlyMessage(err));
}

/** Derive a code from a bare HTTP status when the body isn't a structured {@link ApiError}. */
export function codeForStatus(status: number): ApiErrorCode {
  switch (status) {
    case 400:
      return "BAD_REQUEST";
    case 401:
      return "UNAUTHORIZED";
    case 403:
      return "FORBIDDEN";
    case 404:
      return "NOT_FOUND";
    case 409:
      return "CONFLICT";
    case 503:
      return "SERVICE_UNAVAILABLE";
    default:
      return status >= 500 ? "INTERNAL" : "UNKNOWN";
  }
}
