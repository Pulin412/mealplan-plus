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
