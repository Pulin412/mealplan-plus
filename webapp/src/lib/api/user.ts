import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type UserResponse = components["schemas"]["UserResponse"];
export type UserUpdateRequest = components["schemas"]["UserUpdateRequest"];

export function getMe(): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/v1/users/me");
}

/** Partial update — send only the changed fields. */
export function updateMe(patch: UserUpdateRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/v1/users/me", { method: "PUT", body: JSON.stringify(patch) });
}
