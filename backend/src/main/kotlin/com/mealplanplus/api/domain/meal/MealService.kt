package com.mealplanplus.api.domain.meal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class MealService(
    private val mealRepo: MealRepository,
    private val itemRepo: MealFoodItemRepository
) {
    fun list(firebaseUid: String): List<MealDto> =
        mealRepo.findByFirebaseUid(firebaseUid).map { it.toDto(itemRepo.findByMealId(it.id)) }

    fun get(id: Long): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        return meal.toDto(itemRepo.findByMealId(meal.id))
    }

    @Transactional
    fun create(dto: MealDto, firebaseUid: String): MealDto {
        val meal = Meal(firebaseUid = firebaseUid, name = dto.name, slot = dto.slot)
            .also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = mealRepo.save(meal)
        val items = dto.items.map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = item.foodId,
                quantity = item.quantity, unit = item.unit, notes = item.notes))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val meal = mealRepo.findById(id).orElseThrow()
        require(meal.firebaseUid == firebaseUid) { "Forbidden" }
        itemRepo.deleteByMealId(id)
        mealRepo.delete(meal)
    }

    fun since(firebaseUid: String, since: Instant): List<MealDto> =
        mealRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(itemRepo.findByMealId(it.id)) }

    @Transactional
    fun upsert(dto: MealDto, firebaseUid: String): MealDto {
        val existing = dto.serverId?.let { mealRepo.findByServerId(it) }
        return if (existing == null || (dto.updatedAt ?: Instant.EPOCH) >= existing.updatedAt)
            create(dto, firebaseUid)
        else existing.toDto(itemRepo.findByMealId(existing.id))
    }
}
