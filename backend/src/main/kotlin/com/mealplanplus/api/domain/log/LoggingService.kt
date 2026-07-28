package com.mealplanplus.api.domain.log

import com.mealplanplus.api.generated.model.AddLoggedFoodRequest
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.LoggedFoodResponseDto
import com.mealplanplus.api.generated.model.LoggedMealSlotDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class LoggingService(
    private val slotRepo: LoggedMealSlotRepository,
    private val logRepo: DailyLogRepository,
    private val foodRepo: LoggedFoodRepository
) {
    // ── Slot toggle ───────────────────────────────────────────────────────────

    fun getSlots(firebaseUid: String, date: LocalDate): List<LoggedMealSlotDto> =
        slotRepo.findByFirebaseUidAndDate(firebaseUid, date).map { it.toDto() }

    fun slotsSince(firebaseUid: String, since: Instant): List<LoggedMealSlotDto> {
        val sinceDate = since.atZone(ZoneOffset.UTC).toLocalDate()
        return slotRepo.findByFirebaseUidAndDateGreaterThanEqual(firebaseUid, sinceDate).map { it.toDto() }
    }

    @Transactional
    fun upsertSlot(firebaseUid: String, dto: LoggedMealSlotDto): LoggedMealSlotDto {
        val date = dto.date
        val existing = slotRepo.findByFirebaseUidAndDateAndSlot(firebaseUid, date, dto.slot)
        val saved = if (existing != null) {
            existing.isLogged = dto.isLogged
            slotRepo.save(existing)
        } else {
            slotRepo.save(LoggedMealSlot(firebaseUid = firebaseUid, date = date,
                slot = dto.slot, isLogged = dto.isLogged))
        }
        return saved.toDto()
    }

    @Transactional
    fun toggleSlot(firebaseUid: String, date: LocalDate, slot: String): LoggedMealSlotDto {
        val existing = slotRepo.findByFirebaseUidAndDateAndSlot(firebaseUid, date, slot)
        val updated = if (existing != null) {
            existing.isLogged = !existing.isLogged
            slotRepo.save(existing)
        } else {
            slotRepo.save(LoggedMealSlot(firebaseUid = firebaseUid, date = date,
                slot = slot, isLogged = true))
        }
        return updated.toDto()
    }

    // ── Individual food entries ───────────────────────────────────────────────

    fun getFoods(firebaseUid: String, date: LocalDate): List<LoggedFoodResponseDto> {
        val log = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, date)
            ?: return emptyList()
        return foodRepo.findByDailyLogId(log.id).map { it.toResponseDto(date) }
    }

    @Transactional
    fun addFood(firebaseUid: String, req: AddLoggedFoodRequest): LoggedFoodResponseDto {
        val date = req.date
        val log = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, date)
            ?: logRepo.save(DailyLog(firebaseUid = firebaseUid, date = date))
        val food = foodRepo.save(LoggedFood(dailyLogId = log.id, foodId = req.foodId,
            mealSlot = req.mealSlot, quantity = req.quantity, unit = (req.unit ?: FoodUnit.GRAM).value))
        return food.toResponseDto(date)
    }

    @Transactional
    fun removeFood(firebaseUid: String, id: Long) {
        val food = foodRepo.findById(id).orElseThrow()
        val log  = logRepo.findById(food.dailyLogId).orElseThrow()
        if (log.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        foodRepo.delete(food)
    }
}

fun LoggedMealSlot.toDto() = LoggedMealSlotDto(
    id       = id,
    date     = date,
    slot     = slot,
    isLogged = isLogged
)

private fun LoggedFood.toResponseDto(date: LocalDate) = LoggedFoodResponseDto(
    id         = id,
    dailyLogId = dailyLogId,
    date       = date,
    foodId     = foodId,
    mealSlot   = mealSlot,
    quantity   = quantity,
    unit       = FoodUnit.forValue(unit)
)
