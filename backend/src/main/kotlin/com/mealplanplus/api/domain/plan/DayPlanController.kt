package com.mealplanplus.api.domain.plan

import com.mealplanplus.api.generated.api.PlansApi
import com.mealplanplus.api.generated.model.DayPlanDto
import com.mealplanplus.api.generated.model.PlannedWorkoutDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class DayPlanController(private val service: DayPlanService) : PlansApi {

    override fun listPlans(from: LocalDate?, to: LocalDate?): ResponseEntity<List<DayPlanDto>> =
        ResponseEntity.ok(service.list(currentUid(), from, to))

    override fun getPlan(date: LocalDate): ResponseEntity<DayPlanDto> {
        val plan = service.get(currentUid(), date)
        return if (plan != null) ResponseEntity.ok(plan) else ResponseEntity.notFound().build()
    }

    override fun upsertPlan(date: LocalDate, dayPlanDto: DayPlanDto): ResponseEntity<DayPlanDto> =
        ResponseEntity.ok(service.upsert(currentUid(), date, dayPlanDto))

    override fun deletePlan(date: LocalDate): ResponseEntity<Unit> {
        service.delete(currentUid(), date); return ResponseEntity.noContent().build()
    }

    override fun addPlannedWorkout(date: LocalDate, plannedWorkoutDto: PlannedWorkoutDto): ResponseEntity<DayPlanDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.addWorkout(currentUid(), date, plannedWorkoutDto))

    override fun removePlannedWorkout(date: LocalDate, workoutId: Long): ResponseEntity<Unit> {
        service.removeWorkout(currentUid(), date, workoutId); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
