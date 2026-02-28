package com.mealplanplus.api.domain.grocery

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/grocery-lists")
@Tag(name = "Grocery Lists")
class GroceryController(private val service: GroceryService) {

    @GetMapping fun list(auth: Authentication) = service.list(auth.name)
    @GetMapping("/{id}") fun get(@PathVariable id: Long, auth: Authentication) = service.get(id)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: GroceryListDto, auth: Authentication) = service.create(dto, auth.name)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) = service.delete(id, auth.name)
}
