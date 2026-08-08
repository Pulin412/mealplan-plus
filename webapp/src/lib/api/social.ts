import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

type S = components["schemas"];
export type ProfileUpdateRequest = S["ProfileUpdateRequest"];
export type HandleAvailabilityDto = S["HandleAvailabilityDto"];
export type PublicProfileDto = S["PublicProfileDto"];
export type PublicProfileSummaryDto = S["PublicProfileSummaryDto"];
export type SharedTemplateSummaryDto = S["SharedTemplateSummaryDto"];
export type SharedDietDetailDto = S["SharedDietDetailDto"];
export type SharedMealDetailDto = S["SharedMealDetailDto"];
export type SharedWorkoutDetailDto = S["SharedWorkoutDetailDto"];
export type CopyRequest = S["CopyRequest"];
export type CopyResultDto = S["CopyResultDto"];
export type UserResponse = S["UserResponse"];

export type ShareType = "DIET" | "MEAL" | "WORKOUT_TEMPLATE";

const enc = encodeURIComponent;

// ── Profile ──────────────────────────────────────────────────────────────
export function updateMyProfile(req: ProfileUpdateRequest): Promise<UserResponse> {
  return apiFetch<UserResponse>("/api/v1/users/me/profile", { method: "PUT", body: JSON.stringify(req) });
}
export function checkHandleAvailable(handle: string): Promise<HandleAvailabilityDto> {
  return apiFetch<HandleAvailabilityDto>(`/api/v1/social/handle-available?handle=${enc(handle)}`);
}

// ── Discovery ──────────────────────────────────────────────────────────────
export function searchUsers(q: string): Promise<PublicProfileSummaryDto[]> {
  return apiFetch<PublicProfileSummaryDto[]>(`/api/v1/social/users/search?q=${enc(q)}`);
}
export function getPublicProfile(handle: string): Promise<PublicProfileDto> {
  return apiFetch<PublicProfileDto>(`/api/v1/social/users/${enc(handle)}`);
}
export function listFollowers(handle: string): Promise<PublicProfileSummaryDto[]> {
  return apiFetch<PublicProfileSummaryDto[]>(`/api/v1/social/users/${enc(handle)}/followers`);
}
export function listFollowing(handle: string): Promise<PublicProfileSummaryDto[]> {
  return apiFetch<PublicProfileSummaryDto[]>(`/api/v1/social/users/${enc(handle)}/following`);
}

// ── Follow / safety ──────────────────────────────────────────────────────
export function followUser(handle: string): Promise<void> {
  return apiFetch<void>(`/api/v1/social/users/${enc(handle)}/follow`, { method: "POST" });
}
export function unfollowUser(handle: string): Promise<void> {
  return apiFetch<void>(`/api/v1/social/users/${enc(handle)}/follow`, { method: "DELETE" });
}
export function blockUser(handle: string): Promise<void> {
  return apiFetch<void>(`/api/v1/social/users/${enc(handle)}/block`, { method: "POST" });
}

// ── Shared library reads ─────────────────────────────────────────────────────
export function listSharedDiets(handle: string): Promise<SharedTemplateSummaryDto[]> {
  return apiFetch<SharedTemplateSummaryDto[]>(`/api/v1/social/users/${enc(handle)}/diets`);
}
export function listSharedMeals(handle: string): Promise<SharedTemplateSummaryDto[]> {
  return apiFetch<SharedTemplateSummaryDto[]>(`/api/v1/social/users/${enc(handle)}/meals`);
}
export function listSharedWorkouts(handle: string): Promise<SharedTemplateSummaryDto[]> {
  return apiFetch<SharedTemplateSummaryDto[]>(`/api/v1/social/users/${enc(handle)}/workouts`);
}
export function getSharedDiet(handle: string, serverId: string): Promise<SharedDietDetailDto> {
  return apiFetch<SharedDietDetailDto>(`/api/v1/social/users/${enc(handle)}/diets/${serverId}`);
}
export function getSharedMeal(handle: string, serverId: string): Promise<SharedMealDetailDto> {
  return apiFetch<SharedMealDetailDto>(`/api/v1/social/users/${enc(handle)}/meals/${serverId}`);
}
export function getSharedWorkout(handle: string, serverId: string): Promise<SharedWorkoutDetailDto> {
  return apiFetch<SharedWorkoutDetailDto>(`/api/v1/social/users/${enc(handle)}/workouts/${serverId}`);
}

// ── Copy ─────────────────────────────────────────────────────────────────
export function copyTemplate(req: CopyRequest): Promise<CopyResultDto> {
  return apiFetch<CopyResultDto>("/api/v1/social/copy", { method: "POST", body: JSON.stringify(req) });
}

// ── Share toggles (per-item, keyed by serverId) ─────────────────────────────
export function toggleDietShare(serverId: string) {
  return apiFetch<S["DietDto"]>(`/api/v1/diets/${serverId}/share`, { method: "PATCH" });
}
export function toggleMealShare(serverId: string) {
  return apiFetch<S["MealDto"]>(`/api/v1/meals/${serverId}/share`, { method: "PATCH" });
}
export function toggleWorkoutShare(serverId: string) {
  return apiFetch<S["WorkoutTemplateDto"]>(`/api/v1/workout-templates/${serverId}/share`, { method: "PATCH" });
}
