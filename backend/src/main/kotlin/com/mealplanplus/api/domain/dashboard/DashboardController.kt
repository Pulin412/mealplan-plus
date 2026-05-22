package com.mealplanplus.api.domain.dashboard

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard")
class DashboardController(private val service: DashboardService) {

    @GetMapping
    fun get(
        auth: Authentication,
        @RequestParam(required = false) date: String?
    ): DashboardDto {
        val today = if (date != null) runCatching { LocalDate.parse(date) }.getOrNull() else null
        return service.get(auth.name, today)
    }
}
