import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type FeedbackRequestDto = components["schemas"]["FeedbackRequestDto"];
export type FeedbackDto = components["schemas"]["FeedbackDto"];

/** Submit in-app feedback. Attaches the client version/platform so it can be triaged by build. */
export function submitFeedback(message: string, appVersion: string): Promise<FeedbackDto> {
  const body: FeedbackRequestDto = { message, appVersion, platform: "web" };
  return apiFetch<FeedbackDto>("/api/v1/feedback", { method: "POST", body: JSON.stringify(body) });
}
