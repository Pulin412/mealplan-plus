package com.mealplanplus.api.domain.log

import com.mealplanplus.api.generated.api.DailyLogsApi
import com.mealplanplus.api.generated.model.DailyLogDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class LogController(private val service: DailyLogService) : DailyLogsApi {

    override fun listDailyLogs(): ResponseEntity<List<DailyLogDto>> =
        ResponseEntity.ok(service.list(currentUid()))

    override fun getDailyLog(id: Long): ResponseEntity<DailyLogDto> =
        ResponseEntity.ok(service.get(id))

    override fun createDailyLog(dailyLogDto: DailyLogDto): ResponseEntity<DailyLogDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.create(dailyLogDto, currentUid()))

    override fun updateDailyLog(id: Long, dailyLogDto: DailyLogDto): ResponseEntity<DailyLogDto> =
        ResponseEntity.ok(service.update(id, dailyLogDto, currentUid()))

    override fun deleteDailyLog(id: Long): ResponseEntity<Unit> {
        service.delete(id, currentUid()); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
