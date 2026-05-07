package com.mealplanplus.api.domain.log

import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate

@Service
class DailyLogService(
    private val logRepo: DailyLogRepository,
    private val foodRepo: LoggedFoodRepository,
    private val tombstones: TombstoneService
) {
    fun list(firebaseUid: String): List<DailyLogDto> {
        val logs = logRepo.findByFirebaseUid(firebaseUid)
        if (logs.isEmpty()) return emptyList()
        val foodsByLogId = foodRepo.findByDailyLogIdIn(logs.map { it.id }).groupBy { it.dailyLogId }
        return logs.map { it.toDto(foodsByLogId[it.id] ?: emptyList()) }
    }

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
        if (log.firebaseUid != firebaseUid) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        foodRepo.deleteByDailyLogId(id)
        logRepo.delete(log)
        tombstones.record(firebaseUid, "daily_log", log.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<DailyLogDto> =
        logRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(foodRepo.findByDailyLogId(it.id)) }

    @Transactional
    fun update(id: Long, dto: DailyLogDto, firebaseUid: String): DailyLogDto {
        val existing = logRepo.findById(id).orElseThrow()
        if (existing.firebaseUid != firebaseUid) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        foodRepo.deleteByDailyLogId(existing.id)
        val updated = DailyLog(id = existing.id, firebaseUid = existing.firebaseUid,
            date = dto.date ?: existing.date, notes = dto.notes)
            .also { it.serverId = existing.serverId }
        val saved = logRepo.save(updated)
        val foods = dto.loggedFoods.map { f ->
            foodRepo.save(LoggedFood(dailyLogId = saved.id, foodId = f.foodId,
                mealSlot = f.mealSlot, quantity = f.quantity, unit = f.unit))
        }
        return saved.toDto(foods)
    }

    @Transactional
    fun upsert(dto: DailyLogDto, firebaseUid: String): DailyLogDto {
        val existing = dto.serverId?.let { logRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt)) return existing.toDto(foodRepo.findByDailyLogId(existing.id))
        foodRepo.deleteByDailyLogId(existing.id)
        val updated = DailyLog(id = existing.id, firebaseUid = existing.firebaseUid,
            date = dto.date ?: existing.date, notes = dto.notes)
            .also { it.serverId = existing.serverId }
        val saved = logRepo.save(updated)
        val foods = dto.loggedFoods.map { f ->
            foodRepo.save(LoggedFood(dailyLogId = saved.id, foodId = f.foodId,
                mealSlot = f.mealSlot, quantity = f.quantity, unit = f.unit))
        }
        return saved.toDto(foods)
    }
}
