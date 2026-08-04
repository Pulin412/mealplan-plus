package com.mealplanplus.api.domain.log

import com.mealplanplus.api.generated.api.LoggingApi
import com.mealplanplus.api.generated.model.AddLoggedFoodRequest
import com.mealplanplus.api.generated.model.DayCompletionDto
import com.mealplanplus.api.generated.model.LoggedFoodResponseDto
import com.mealplanplus.api.generated.model.LoggedMealSlotDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class LoggingController(private val service: LoggingService) : LoggingApi {

    override fun getLoggedSlots(date: LocalDate): ResponseEntity<List<LoggedMealSlotDto>> =
        ResponseEntity.ok(service.getSlots(currentUid(), date))

    override fun toggleMealSlot(date: LocalDate, slot: String): ResponseEntity<LoggedMealSlotDto> =
        ResponseEntity.ok(service.toggleSlot(currentUid(), date, slot))

    override fun toggleDayComplete(date: LocalDate): ResponseEntity<DayCompletionDto> =
        ResponseEntity.ok(DayCompletionDto(date, service.toggleDayComplete(currentUid(), date)))

    override fun getCompletedDays(from: LocalDate, to: LocalDate): ResponseEntity<List<LocalDate>> =
        ResponseEntity.ok(service.getCompletedDays(currentUid(), from, to))

    override fun getLoggedFoods(date: LocalDate): ResponseEntity<List<LoggedFoodResponseDto>> =
        ResponseEntity.ok(service.getFoods(currentUid(), date))

    override fun addLoggedFood(addLoggedFoodRequest: AddLoggedFoodRequest): ResponseEntity<LoggedFoodResponseDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.addFood(currentUid(), addLoggedFoodRequest))

    override fun removeLoggedFood(id: Long): ResponseEntity<Unit> {
        service.removeFood(currentUid(), id); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
