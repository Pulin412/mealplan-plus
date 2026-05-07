package com.mealplanplus.api.domain.diet

import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

data class CreateTagRequest(val name: String, val color: String? = null)

@RestController
@RequestMapping("/api/v1/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags")
class TagController(private val service: DietService) {

    @GetMapping
    fun list(auth: Authentication) = service.listTags(auth.name)

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: CreateTagRequest, auth: Authentication) =
        service.createTag(req.name, req.color, auth.name)

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) =
        service.deleteTag(id, auth.name)
}
