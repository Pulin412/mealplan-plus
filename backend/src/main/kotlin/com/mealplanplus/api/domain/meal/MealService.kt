package com.mealplanplus.api.domain.meal

import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class MealService(
    private val mealRepo: MealRepository,
    private val itemRepo: MealFoodItemRepository,
    private val tombstones: TombstoneService
) {
    fun list(firebaseUid: String): List<MealDto> {
        val meals = mealRepo.findByFirebaseUid(firebaseUid)
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList()) }
    }

    fun get(id: Long): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        return meal.toDto(itemRepo.findByMealId(meal.id))
    }

    @Transactional
    fun create(dto: MealDto, firebaseUid: String): MealDto {
        val meal = Meal(firebaseUid = firebaseUid, name = dto.name)
            .also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = mealRepo.save(meal)
        val items = dto.items.map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = item.foodId,
                quantity = item.quantity, unit = item.unit, notes = item.notes))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun update(id: Long, dto: MealDto, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.deleteByMealId(id)
        val updated = Meal(id = meal.id, firebaseUid = meal.firebaseUid, name = dto.name)
            .also { it.serverId = meal.serverId }
        val saved = mealRepo.save(updated)
        val items = dto.items.map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = item.foodId,
                quantity = item.quantity, unit = item.unit, notes = item.notes))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.deleteByMealId(id)
        mealRepo.delete(meal)
        tombstones.record(firebaseUid, "meal", meal.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<MealDto> =
        mealRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(itemRepo.findByMealId(it.id)) }

    @Transactional
    fun upsert(dto: MealDto, firebaseUid: String): MealDto {
        val existing = dto.serverId?.let { mealRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt)) return existing.toDto(itemRepo.findByMealId(existing.id))
        itemRepo.deleteByMealId(existing.id)
        val updated = Meal(id = existing.id, firebaseUid = existing.firebaseUid, name = dto.name)
            .also { it.serverId = existing.serverId }
        val saved = mealRepo.save(updated)
        val items = dto.items.map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = item.foodId,
                quantity = item.quantity, unit = item.unit, notes = item.notes))
        }
        return saved.toDto(items)
    }
}
