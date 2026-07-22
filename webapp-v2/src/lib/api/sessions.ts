import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type WorkoutSessionDto = components["schemas"]["WorkoutSessionDto"];
export type WorkoutSetDto = components["schemas"]["WorkoutSetDto"];
export type LastSetsDto = components["schemas"]["LastSetsDto"];

/** All sessions (read-only history for the Logs tab), most-recent first up to the caller. */
export function listWorkoutSessions(): Promise<WorkoutSessionDto[]> {
  return apiFetch<WorkoutSessionDto[]>("/api/v1/workout-sessions");
}

/** Sessions on a single day (resume/inspect today's workout). */
export function listSessionsForDate(date: string): Promise<WorkoutSessionDto[]> {
  return apiFetch<WorkoutSessionDto[]>(`/api/v1/workout-sessions?from=${date}&to=${date}`);
}

/** Start a session from a template (server pre-populates sets from the targets). */
export function startWorkout(templateId: number): Promise<WorkoutSessionDto> {
  return apiFetch<WorkoutSessionDto>(`/api/v1/workout-templates/${templateId}/start`, { method: "POST" });
}

/** Create an ad-hoc in-progress session (e.g. a single random exercise). */
export function createSession(name: string, date: string, sets: WorkoutSetDto[]): Promise<WorkoutSessionDto> {
  return apiFetch<WorkoutSessionDto>("/api/v1/workout-sessions", {
    method: "POST",
    body: JSON.stringify({ name, date, isCompleted: false, sets }),
  });
}

/** Persist current sets to the in-progress session (auto-save while logging). */
export function updateSession(id: number, dto: WorkoutSessionDto): Promise<WorkoutSessionDto> {
  return apiFetch<WorkoutSessionDto>(`/api/v1/workout-sessions/${id}`, { method: "PUT", body: JSON.stringify(dto) });
}

/** Mark the session complete → it becomes the day's log. */
export function finishSession(id: number): Promise<WorkoutSessionDto> {
  return apiFetch<WorkoutSessionDto>(`/api/v1/workout-sessions/${id}/finish`, { method: "POST" });
}

/** Sets from the most recent completed session containing this exercise ("Last time" / Copy last). */
export function lastForExercise(exerciseId: number): Promise<WorkoutSetDto[]> {
  return apiFetch<LastSetsDto>(`/api/v1/workout-sessions/last-for-exercise/${exerciseId}`)
    .then((r) => r.sets ?? [])
    .catch(() => []);
}
