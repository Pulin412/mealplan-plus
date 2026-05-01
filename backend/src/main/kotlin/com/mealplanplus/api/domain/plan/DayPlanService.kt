package com.mealplanplus.api.domain.plan

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class DayPlanService(
    private val repo: DayPlanRepository,
    private val tombstones: TombstoneService
) {
    fun list(firebaseUid: String): List<DayPlanDto> =
        repo.findByFirebaseUid(firebaseUid).map { it.toDto() }

    fun get(firebaseUid: String, date: LocalDate): DayPlanDto? =
        repo.findByFirebaseUidAndDate(firebaseUid, date)?.toDto()

    /** Create or replace the plan for a given date (upsert by date). */
    @Transactional
    fun upsert(firebaseUid: String, date: LocalDate, dto: DayPlanDto): DayPlanDto {
        val existing = repo.findByFirebaseUidAndDate(firebaseUid, date)
        val plan = if (existing != null) {
            // Replace in-place — keep server identity
            DayPlan(id = existing.id, firebaseUid = firebaseUid, date = date, dietId = dto.dietId)
                .also { it.serverId = existing.serverId }
        } else {
            DayPlan(firebaseUid = firebaseUid, date = date, dietId = dto.dietId)
                .also { if (dto.serverId != null) it.serverId = dto.serverId }
        }
        return repo.save(plan).toDto()
    }

    @Transactional
    fun delete(firebaseUid: String, date: LocalDate) {
        val existing = repo.findByFirebaseUidAndDate(firebaseUid, date) ?: return
        tombstones.record(firebaseUid, "day_plan", existing.serverId)
        repo.delete(existing)
    }

    fun since(firebaseUid: String, since: Instant): List<DayPlanDto> =
        repo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since).map { it.toDto() }
}
