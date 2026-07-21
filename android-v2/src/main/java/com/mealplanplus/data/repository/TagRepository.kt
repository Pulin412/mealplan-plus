package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.TagsApi
import com.mealplanplus.data.generated.model.CreateTagRequest
import com.mealplanplus.data.generated.model.TagEntityType
import com.mealplanplus.data.model.DietTag
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Online tag directory. Tags are server-managed (normalized Tag/EntityTag), so listing and
 * creating them are network calls — the "online-only" case in the offline-first guidance.
 * A diet's *assignment* of tags still syncs offline via `DietDto.tagIds`.
 */
@Singleton
class TagRepository @Inject constructor(
    private val api: TagsApi,
) {
    /** All DIET tags the user can pick from (system + own). Empty on failure/offline. */
    suspend fun listDietTags(): List<DietTag> =
        runCatching {
            api.listTags(TagEntityType.DIET).body().orEmpty().map { DietTag(it.id, it.name, it.color) }
        }.getOrDefault(emptyList())

    /** Create (or reuse) a DIET tag by name; returns it with its server id, or null if offline. */
    suspend fun createDietTag(name: String, color: String? = null): DietTag? =
        runCatching {
            api.createTag(CreateTagRequest(name = name.trim(), entityType = TagEntityType.DIET, color = color))
                .body()?.let { DietTag(it.id, it.name, it.color) }
        }.getOrNull()
}
