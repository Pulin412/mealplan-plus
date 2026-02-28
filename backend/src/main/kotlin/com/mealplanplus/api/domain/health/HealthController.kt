package com.mealplanplus.api.domain.health

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/health-metrics")
@Tag(name = "Health Metrics")
class HealthController(private val service: HealthMetricService) {

    @GetMapping fun list(auth: Authentication) = service.list(auth.name)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: HealthMetricDto, auth: Authentication) = service.create(dto, auth.name)
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) = service.delete(id, auth.name)

    @GetMapping("/custom-types") fun listCustomTypes(auth: Authentication) = service.listCustomTypes(auth.name)
    @PostMapping("/custom-types") @ResponseStatus(HttpStatus.CREATED)
    fun createCustomType(@RequestBody dto: CustomMetricTypeDto, auth: Authentication) =
        service.createCustomType(dto, auth.name)
}
