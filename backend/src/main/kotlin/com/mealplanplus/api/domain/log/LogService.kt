package com.mealplanplus.api.domain.log

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class DailyLogService(
    private val logRepo: DailyLogRepository,
    private val foodRepo: LoggedFoodRepository
) {
    fun list(firebaseUid: String): List<DailyLogDto> =
        logRepo.findByFirebaseUid(firebaseUid).map { it.toDto(foodRepo.findByDailyLogId(it.id)) }

    fun get(id: Long): DailyLogDto {
        val log = logRepo.findById(id).orElseThrow()
        return log.toDto(foodRepo.findByDailyLogId(log.id))
    }

    @Transactional
    fun create(dto: DailyLogDto, firebaseUid: String): DailyLogDto {
        val log = DailyLog(
            firebaseUid = firebaseUid,
            date = dto.date ?: LocalDate.now(),
            notes = dto.notes
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = logRepo.save(log)
        val foods = dto.loggedFoods.map { f ->
            foodRepo.save(LoggedFood(dailyLogId = saved.id, foodId = f.foodId,
                mealSlot = f.mealSlot, quantity = f.quantity, unit = f.unit))
        }
        return saved.toDto(foods)
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val log = logRepo.findById(id).orElseThrow()
        require(log.firebaseUid == firebaseUid) { "Forbidden" }
        foodRepo.deleteByDailyLogId(id)
        logRepo.delete(log)
    }

    fun since(firebaseUid: String, since: Instant): List<DailyLogDto> =
        logRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(foodRepo.findByDailyLogId(it.id)) }

    @Transactional
    fun upsert(dto: DailyLogDto, firebaseUid: String): DailyLogDto {
        val existing = dto.serverId?.let { logRepo.findByServerId(it) }
        return if (existing == null || (dto.updatedAt ?: Instant.EPOCH) >= existing.updatedAt)
            create(dto, firebaseUid)
        else existing.toDto(foodRepo.findByDailyLogId(existing.id))
    }
}
