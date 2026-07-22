import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type WorkoutSessionDto = components["schemas"]["WorkoutSessionDto"];
export type WorkoutSetDto = components["schemas"]["WorkoutSetDto"];

/** Server-backed workout session log (read-only history for the Logs tab). */
export function listWorkoutSessions(): Promise<WorkoutSessionDto[]> {
  return apiFetch<WorkoutSessionDto[]>("/api/v1/workout-sessions");
}
