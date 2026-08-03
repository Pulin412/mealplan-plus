import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type WorkoutTemplateDto = components["schemas"]["WorkoutTemplateDto"];
export type TemplateExerciseDto = components["schemas"]["TemplateExerciseDto"];
export type TemplateSetDto = components["schemas"]["TemplateSetDto"];

/** Stamp orderIndex from list position so the server preserves the builder's ordering. */
function reindex(exercises: TemplateExerciseDto[]): TemplateExerciseDto[] {
  return exercises.map((e, i) => ({ ...e, orderIndex: i }));
}

/** Server-backed workout templates (name + ordered exercises with per-set targets). */
export function listWorkouts(): Promise<WorkoutTemplateDto[]> {
  return apiFetch<WorkoutTemplateDto[]>("/api/v1/workout-templates");
}

export function getWorkout(id: number): Promise<WorkoutTemplateDto> {
  return apiFetch<WorkoutTemplateDto>(`/api/v1/workout-templates/${id}`);
}

export function createWorkout(name: string, exercises: TemplateExerciseDto[], tagIds: number[] = []): Promise<WorkoutTemplateDto> {
  return apiFetch<WorkoutTemplateDto>("/api/v1/workout-templates", {
    method: "POST",
    body: JSON.stringify({ name: name.trim(), exercises: reindex(exercises), tagIds }),
  });
}

export function updateWorkout(id: number, name: string, exercises: TemplateExerciseDto[], tagIds: number[] = []): Promise<WorkoutTemplateDto> {
  return apiFetch<WorkoutTemplateDto>(`/api/v1/workout-templates/${id}`, {
    method: "PUT",
    body: JSON.stringify({ id, name: name.trim(), exercises: reindex(exercises), tagIds }),
  });
}

export function deleteWorkout(id: number): Promise<void> {
  return apiFetch<void>(`/api/v1/workout-templates/${id}`, { method: "DELETE" });
}
