import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type TagDto = components["schemas"]["TagDto"];

export function listDietTags(): Promise<TagDto[]> {
  return apiFetch<TagDto[]>("/api/v1/tags?entityType=DIET");
}

export function createDietTag(name: string): Promise<TagDto> {
  return apiFetch<TagDto>("/api/v1/tags", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), entityType: "DIET" }),
  });
}

/** EXERCISE-type tags (muscle groups / movement patterns) the user can assign to exercises. */
export function listExerciseTags(): Promise<TagDto[]> {
  return apiFetch<TagDto[]>("/api/v1/tags?entityType=EXERCISE");
}

export function createExerciseTag(name: string): Promise<TagDto> {
  return apiFetch<TagDto>("/api/v1/tags", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), entityType: "EXERCISE" }),
  });
}

/** WORKOUT-type tags (e.g. Push/Pull/Legs, Beginner/Advanced) assignable to workout templates. */
export function listWorkoutTags(): Promise<TagDto[]> {
  return apiFetch<TagDto[]>("/api/v1/tags?entityType=WORKOUT");
}

export function createWorkoutTag(name: string): Promise<TagDto> {
  return apiFetch<TagDto>("/api/v1/tags", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), entityType: "WORKOUT" }),
  });
}
