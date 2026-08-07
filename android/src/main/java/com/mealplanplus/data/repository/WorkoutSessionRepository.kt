package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.WorkoutSessionsApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.WorkoutSessionDto
import com.mealplanplus.data.generated.model.WorkoutSetDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed workout session log. Sessions live outside the offline sync contract, so these are
 * direct REST calls (needs a backend). Powers the Logs tab (read) and the Session Runner (start →
 * update-as-you-log → finish). One session per (workout, day) — re-finishing upserts the same log.
 */
@Singleton
class WorkoutSessionRepository @Inject constructor(
    private val api: WorkoutSessionsApi,
    private val templatesApi: WorkoutTemplatesApi,
) {
    /** Completed + in-progress sessions, most-recent first. Empty on failure/offline. */
    suspend fun list(): List<WorkoutSessionDto> =
        runCatching { api.listWorkoutSessions().body().orEmpty() }
            .getOrDefault(emptyList())
            .sortedByDescending { it.date }

    /** Sessions on a single day (used to resume/inspect today's workout). */
    suspend fun listForDate(date: LocalDate): List<WorkoutSessionDto> =
        runCatching { api.listWorkoutSessions(date, date).body().orEmpty() }.getOrDefault(emptyList())

    /** Start a new in-progress session from a template (server pre-populates sets from the targets). */
    suspend fun start(templateId: Long): Result<WorkoutSessionDto> = runCatching {
        templatesApi.startWorkoutFromTemplate(templateId).body()!!
    }

    /** Create an ad-hoc in-progress session (e.g. a single random exercise logged from Home). */
    suspend fun create(name: String, date: LocalDate, sets: List<WorkoutSetDto>): Result<WorkoutSessionDto> = runCatching {
        api.createWorkoutSession(WorkoutSessionDto(name = name, date = date, isCompleted = false, sets = sets)).body()!!
    }

    /** Persist the current session (sets as logged so far). Enables resume after navigating away. */
    suspend fun update(session: WorkoutSessionDto): Result<WorkoutSessionDto> = runCatching {
        api.updateWorkoutSession(session.id!!, session).body()!!
    }

    /** Mark the session complete → it becomes the day's log. */
    suspend fun finish(id: Long): Result<WorkoutSessionDto> = runCatching {
        api.finishWorkoutSession(id).body()!!
    }

    /** Delete a logged session. Returns true on success. */
    suspend fun delete(id: Long): Boolean =
        runCatching { api.deleteWorkoutSession(id).isSuccessful }.getOrDefault(false)

    /**
     * Sets for "Last time" / Copy last. [workoutName] scopes it to the most recent completed session
     * of the same workout (the last time you did *this* workout); null falls back to any workout.
     */
    suspend fun lastForExercise(exerciseId: Long, workoutName: String? = null): List<WorkoutSetDto> =
        runCatching { api.lastSetsForExercise(exerciseId, workoutName).body()?.sets.orEmpty() }.getOrDefault(emptyList())
}
