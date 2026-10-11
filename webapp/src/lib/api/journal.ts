import { apiFetch } from "./client";
import type { components } from "@/lib/api/types.generated";

export type JournalEntryDto = components["schemas"]["JournalEntryDto"];

/** Diary notes in a date range (one entry per date that has a note). */
export function listJournal(from: string, to: string): Promise<JournalEntryDto[]> {
  return apiFetch<JournalEntryDto[]>(`/api/v1/journal?from=${from}&to=${to}`);
}

/** Create or replace the note for a date. */
export function upsertJournal(date: string, note: string): Promise<JournalEntryDto> {
  return apiFetch<JournalEntryDto>(`/api/v1/journal/${date}`, { method: "PUT", body: JSON.stringify({ date, note }) });
}

/** Delete the note for a date. */
export function deleteJournal(date: string): Promise<void> {
  return apiFetch<void>(`/api/v1/journal/${date}`, { method: "DELETE" });
}
