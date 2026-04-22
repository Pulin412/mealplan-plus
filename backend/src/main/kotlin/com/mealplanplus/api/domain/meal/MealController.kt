package com.mealplanplus.api.domain.meal

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/meals")
@Tag(name = "Meals")
class MealController(private val service: MealService) {

    @GetMapping fun list(auth: Authentication) = service.list(auth.name)
    @GetMapping("/{id}") fun get(@PathVariable id: Long, auth: Authentication) = service.get(id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: MealDto, auth: Authentication) = service.create(dto, auth.name)
    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @RequestBody dto: MealDto, auth: Authentication) = service.update(id, dto, auth.name)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) = service.delete(id, auth.name)
}
