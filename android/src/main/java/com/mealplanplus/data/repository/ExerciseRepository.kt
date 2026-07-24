package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.ExercisesApi
import com.mealplanplus.data.generated.api.TagsApi
import com.mealplanplus.data.generated.model.CreateTagRequest
import com.mealplanplus.data.generated.model.ExerciseDto
import com.mealplanplus.data.generated.model.TagDto
import com.mealplanplus.data.generated.model.TagEntityType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-backed exercise library. Unlike the nutrition entities, the exercise/workout domain
 * is not in the offline sync contract, so these are direct REST calls (needs a backend up) —
 * mirroring the Plan screen. Exercise *tags* are the normalized EXERCISE-type tags.
 */
@Singleton
class ExerciseRepository @Inject constructor(
    private val api: ExercisesApi,
    private val tagsApi: TagsApi,
) {
    suspend fun list(): List<ExerciseDto> =
        runCatching { api.listExercises().body().orEmpty() }.getOrDefault(emptyList())

    suspend fun create(name: String, description: String?, tagIds: List<Long>): Result<ExerciseDto> = runCatching {
        api.createExercise(ExerciseDto(name = name.trim(), description = description, tagIds = tagIds)).body()!!
    }

    suspend fun update(id: Long, name: String, description: String?, tagIds: List<Long>): Result<ExerciseDto> = runCatching {
        api.updateExercise(id, ExerciseDto(name = name.trim(), id = id, description = description, tagIds = tagIds)).body()!!
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        api.deleteExercise(id); Unit
    }

    /** EXERCISE tags the user can pick from (system + own). Empty on failure/offline. */
    suspend fun listTags(): List<TagDto> =
        runCatching { tagsApi.listTags(TagEntityType.EXERCISE).body().orEmpty() }.getOrDefault(emptyList())

    /** Create (or reuse) an EXERCISE tag by name; returns it with its server id, or null. */
    suspend fun createTag(name: String, color: String? = null): TagDto? =
        runCatching {
            tagsApi.createTag(CreateTagRequest(name = name.trim(), entityType = TagEntityType.EXERCISE, color = color)).body()
        }.getOrNull()
}
