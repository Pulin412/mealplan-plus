package com.mealplanplus.api.domain.health

import com.mealplanplus.api.generated.api.HealthMetricsApi
import com.mealplanplus.api.generated.model.CustomMetricTypeDto
import com.mealplanplus.api.generated.model.HealthMetricDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class HealthController(private val service: HealthMetricService) : HealthMetricsApi {

    override fun listHealthMetrics(type: String?, from: LocalDate?, to: LocalDate?): ResponseEntity<List<HealthMetricDto>> =
        ResponseEntity.ok(service.list(currentUid(), type, from, to))

    override fun createHealthMetric(healthMetricDto: HealthMetricDto): ResponseEntity<HealthMetricDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(healthMetricDto, currentUid()))

    override fun deleteHealthMetric(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    override fun listCustomMetricTypes(): ResponseEntity<List<CustomMetricTypeDto>> =
        ResponseEntity.ok(service.listCustomTypes(currentUid()))

    override fun createCustomMetricType(customMetricTypeDto: CustomMetricTypeDto): ResponseEntity<CustomMetricTypeDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createCustomType(customMetricTypeDto, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
