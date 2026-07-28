package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.generated.api.TagsApi
import com.mealplanplus.api.generated.model.CreateTagRequest
import com.mealplanplus.api.generated.model.TagDto
import com.mealplanplus.api.generated.model.TagEntityType as GeneratedTagEntityType
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class TagController(private val service: DietService) : TagsApi {

    override fun listTags(entityType: GeneratedTagEntityType?): ResponseEntity<List<TagDto>> =
        ResponseEntity.ok(service.listTags(currentUid(), entityType?.let { TagEntityType.valueOf(it.value) }))

    override fun createTag(createTagRequest: CreateTagRequest): ResponseEntity<TagDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(
            service.createTag(createTagRequest.name, createTagRequest.color, currentUid(),
                TagEntityType.valueOf(createTagRequest.entityType.value))
        )

    override fun deleteTag(id: Long): ResponseEntity<Unit> {
        service.deleteTag(id, currentUid()); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
