package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.generated.api.DashboardApi
import com.mealplanplus.api.generated.model.DashboardDto
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class DashboardController(private val service: DashboardService) : DashboardApi {

    override fun getDashboard(date: LocalDate?): ResponseEntity<DashboardDto> =
        ResponseEntity.ok(service.get(currentUid(), date))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
