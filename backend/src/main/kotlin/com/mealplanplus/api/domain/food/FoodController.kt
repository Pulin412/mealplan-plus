package com.mealplanplus.api.domain.food

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/foods")
@Tag(name = "Foods")
class FoodController(private val service: FoodService) {

    @GetMapping fun list(auth: Authentication) = service.list(auth.name)

    @GetMapping("/search")
    fun search(
        @RequestParam(defaultValue = "") q: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int,
        auth: Authentication
    ) = service.search(q, auth.name, PageRequest.of(page, size.coerceIn(1, 100), Sort.by("name")))

    @GetMapping("/{id}") fun get(@PathVariable id: Long, auth: Authentication) = service.get(id, auth.name)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody dto: FoodDto, auth: Authentication) = service.create(dto, auth.name)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) = service.delete(id, auth.name)

    @PatchMapping("/{id}/favorite")
    fun toggleFavorite(@PathVariable id: Long, auth: Authentication) =
        service.toggleFavorite(id, auth.name)
}
