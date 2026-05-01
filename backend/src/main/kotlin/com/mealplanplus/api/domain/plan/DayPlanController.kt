package com.mealplanplus.api.domain.plan

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Plans")
class DayPlanController(private val service: DayPlanService) {

    /** List all plans for the authenticated user. */
    @GetMapping
    fun list(auth: Authentication): List<DayPlanDto> =
        service.list(auth.name)

    /** Get the plan for a specific date (yyyy-MM-dd). */
    @GetMapping("/{date}")
    fun get(@PathVariable date: String, auth: Authentication): DayPlanDto? =
        service.get(auth.name, LocalDate.parse(date))

    /**
     * Assign (or reassign) a diet to a date.
     * Body: `{ "dietId": 42 }` — other fields ignored.
     */
    @PutMapping("/{date}")
    fun upsert(
        @PathVariable date: String,
        @RequestBody dto: DayPlanDto,
        auth: Authentication
    ): DayPlanDto = service.upsert(auth.name, LocalDate.parse(date), dto)

    /** Remove the diet assignment for a date. */
    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable date: String, auth: Authentication) =
        service.delete(auth.name, LocalDate.parse(date))
}
