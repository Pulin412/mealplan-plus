package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.TemplateExerciseDto
import com.mealplanplus.data.generated.model.WorkoutTemplateDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed workout templates (name + ordered exercises with a sets×reps target). Not in the
 * offline sync contract, so these are direct REST calls (needs a backend up), like [ExerciseRepository].
 */
@Singleton
class WorkoutRepository @Inject constructor(
    private val api: WorkoutTemplatesApi,
) {
    suspend fun list(): List<WorkoutTemplateDto> =
        runCatching { api.listWorkoutTemplates().body().orEmpty() }.getOrDefault(emptyList())

    suspend fun create(name: String, exercises: List<TemplateExerciseDto>): Result<WorkoutTemplateDto> = runCatching {
        api.createWorkoutTemplate(WorkoutTemplateDto(name = name.trim(), exercises = reindex(exercises))).body()!!
    }

    suspend fun update(id: Long, name: String, exercises: List<TemplateExerciseDto>): Result<WorkoutTemplateDto> = runCatching {
        api.updateWorkoutTemplate(id, WorkoutTemplateDto(name = name.trim(), id = id, exercises = reindex(exercises))).body()!!
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        api.deleteWorkoutTemplate(id); Unit
    }

    /** Stamp orderIndex from list position so the server preserves the builder's ordering. */
    private fun reindex(exercises: List<TemplateExerciseDto>): List<TemplateExerciseDto> =
        exercises.mapIndexed { i, e -> e.copy(orderIndex = i) }
}
