import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type ExerciseDto = components["schemas"]["ExerciseDto"];

/** Server-backed exercise library (system + user-created). Not in the offline sync contract. */
export function listExercises(): Promise<ExerciseDto[]> {
  return apiFetch<ExerciseDto[]>("/api/v1/exercises");
}

export function createExercise(name: string, description: string | null, type: string, tagIds: number[]): Promise<ExerciseDto> {
  return apiFetch<ExerciseDto>("/api/v1/exercises", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), description, type, tagIds }),
  });
}

export function updateExercise(id: number, name: string, description: string | null, type: string, tagIds: number[]): Promise<ExerciseDto> {
  return apiFetch<ExerciseDto>(`/api/v1/exercises/${id}`, {
    method: "PUT",
    body: JSON.stringify({ id, name: name.trim(), description, type, tagIds }),
  });
}

export function deleteExercise(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/exercises/${id}`, { method: "DELETE" });
}
