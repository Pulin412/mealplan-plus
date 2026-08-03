package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.TagsApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.model.CreateTagRequest
import com.mealplanplus.data.generated.model.TagDto
import com.mealplanplus.data.generated.model.TagEntityType
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
    private val tagsApi: TagsApi,
) {
    suspend fun list(): List<WorkoutTemplateDto> =
        runCatching { api.listWorkoutTemplates(null).body().orEmpty() }.getOrDefault(emptyList())

    suspend fun create(name: String, exercises: List<TemplateExerciseDto>, tagIds: List<Long> = emptyList()): Result<WorkoutTemplateDto> = runCatching {
        api.createWorkoutTemplate(WorkoutTemplateDto(name = name.trim(), exercises = reindex(exercises), tagIds = tagIds)).body()!!
    }

    suspend fun update(id: Long, name: String, exercises: List<TemplateExerciseDto>, tagIds: List<Long> = emptyList()): Result<WorkoutTemplateDto> = runCatching {
        api.updateWorkoutTemplate(id, WorkoutTemplateDto(name = name.trim(), id = id, exercises = reindex(exercises), tagIds = tagIds)).body()!!
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        api.deleteWorkoutTemplate(id); Unit
    }

    /** WORKOUT tags the user can pick from (system + own). Empty on failure/offline. */
    suspend fun listTags(): List<TagDto> =
        runCatching { tagsApi.listTags(TagEntityType.WORKOUT).body().orEmpty() }.getOrDefault(emptyList())

    /** Create (or reuse) a WORKOUT tag by name; returns it with its server id, or null. */
    suspend fun createTag(name: String, color: String? = null): TagDto? =
        runCatching {
            tagsApi.createTag(CreateTagRequest(name = name.trim(), entityType = TagEntityType.WORKOUT, color = color)).body()
        }.getOrNull()

    /** Stamp orderIndex from list position so the server preserves the builder's ordering. */
    private fun reindex(exercises: List<TemplateExerciseDto>): List<TemplateExerciseDto> =
        exercises.mapIndexed { i, e -> e.copy(orderIndex = i) }
}
