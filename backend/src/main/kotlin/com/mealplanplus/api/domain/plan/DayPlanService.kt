package com.mealplanplus.api.domain.plan

import com.mealplanplus.api.generated.model.DayPlanDto
import com.mealplanplus.api.generated.model.PlannedWorkoutDto
import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class DayPlanService(
    private val repo: DayPlanRepository,
    private val plannedWorkoutRepo: PlannedWorkoutRepository,
    private val tombstones: TombstoneService
) {
    private fun DayPlan.toFullDto() = toDto(plannedWorkoutRepo.findByDayPlanId(id))

    private fun batchToDtos(plans: List<DayPlan>): List<DayPlanDto> {
        if (plans.isEmpty()) return emptyList()
        val workoutsByPlanId = plannedWorkoutRepo.findByDayPlanIdIn(plans.map { it.id })
            .groupBy { it.dayPlanId }
        return plans.map { it.toDto(workoutsByPlanId[it.id] ?: emptyList()) }
    }

    fun list(firebaseUid: String, from: LocalDate? = null, to: LocalDate? = null): List<DayPlanDto> {
        val plans = if (from != null && to != null)
            repo.findByFirebaseUidAndDateBetween(firebaseUid, from, to)
        else
            repo.findByFirebaseUid(firebaseUid)
        return batchToDtos(plans)
    }

    fun get(firebaseUid: String, date: LocalDate): DayPlanDto? =
        repo.findByFirebaseUidAndDate(firebaseUid, date)?.toFullDto()

    @Transactional
    fun upsert(firebaseUid: String, date: LocalDate, dto: DayPlanDto): DayPlanDto {
        val existing = repo.findByFirebaseUidAndDate(firebaseUid, date)
        val plan = if (existing != null) {
            plannedWorkoutRepo.deleteByDayPlanId(existing.id)
            DayPlan(id = existing.id, firebaseUid = firebaseUid, date = date, dietId = dto.dietId)
                .also { it.serverId = existing.serverId }
        } else {
            DayPlan(firebaseUid = firebaseUid, date = date, dietId = dto.dietId)
                .also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        }
        val saved = repo.save(plan)
        (dto.plannedWorkouts ?: emptyList()).forEach { w ->
            plannedWorkoutRepo.save(PlannedWorkout(dayPlanId = saved.id,
                workoutTemplateId = w.workoutTemplateId, activityName = w.activityName ?: ""))
        }
        return saved.toFullDto()
    }

    @Transactional
    fun addWorkout(firebaseUid: String, date: LocalDate, dto: PlannedWorkoutDto): DayPlanDto {
        val plan = repo.findByFirebaseUidAndDate(firebaseUid, date)
            ?: repo.save(DayPlan(firebaseUid = firebaseUid, date = date))
        plannedWorkoutRepo.save(PlannedWorkout(dayPlanId = plan.id,
            workoutTemplateId = dto.workoutTemplateId, activityName = dto.activityName ?: ""))
        return plan.toFullDto()
    }

    @Transactional
    fun removeWorkout(firebaseUid: String, date: LocalDate, workoutId: Long) {
        val plan = repo.findByFirebaseUidAndDate(firebaseUid, date)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No plan for $date")
        val workout = plannedWorkoutRepo.findById(workoutId).orElseThrow()
        if (workout.dayPlanId != plan.id)
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not on this day's plan")
        plannedWorkoutRepo.delete(workout)
    }

    @Transactional
    fun delete(firebaseUid: String, date: LocalDate) {
        val existing = repo.findByFirebaseUidAndDate(firebaseUid, date) ?: return
        plannedWorkoutRepo.deleteByDayPlanId(existing.id)
        tombstones.record(firebaseUid, "day_plan", existing.serverId)
        repo.delete(existing)
    }

    fun since(firebaseUid: String, since: Instant): List<DayPlanDto> =
        batchToDtos(repo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since))
}

fun PlannedWorkout.toDto() = PlannedWorkoutDto(
    id                = id,
    workoutTemplateId = workoutTemplateId,
    activityName      = activityName
)

fun DayPlan.toDto(workouts: List<PlannedWorkout> = emptyList()) = DayPlanDto(
    id              = id,
    serverId        = serverId?.toString(),
    firebaseUid     = firebaseUid,
    date            = date.toString(),
    dietId          = dietId,
    plannedWorkouts = workouts.map { it.toDto() },
    updatedAt       = updatedAt
)
