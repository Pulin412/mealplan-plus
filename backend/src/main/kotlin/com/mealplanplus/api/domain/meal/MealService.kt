package com.mealplanplus.api.domain.meal

import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class MealService(
    private val mealRepo: MealRepository,
    private val itemRepo: MealFoodItemRepository,
    private val foodRepo: FoodRepository,
    private val tombstones: TombstoneService
) {

    private fun resolveFoodId(dto: MealFoodItemDto): Long {
        if (dto.foodServerId != null) {
            val food = runCatching { foodRepo.findByServerId(UUID.fromString(dto.foodServerId.toString())) }.getOrNull()
            if (food != null) return food.id
        }
        return dto.foodId ?: 0L
    }

    fun list(firebaseUid: String, favoritesOnly: Boolean = false): List<MealDto> {
        val meals = mealRepo.findByFirebaseUid(firebaseUid)
            .let { if (favoritesOnly) it.filter { m -> m.isFavorite } else it }
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList()) }
    }

    fun get(id: Long, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        return meal.toDto(itemRepo.findByMealId(meal.id))
    }

    @Transactional
    fun create(dto: MealDto, firebaseUid: String): MealDto {
        val meal = Meal(firebaseUid = firebaseUid, name = dto.name, isFavorite = dto.isFavorite ?: false)
            .also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        val saved = mealRepo.save(meal)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun update(id: Long, dto: MealDto, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.deleteByMealId(id)
        val updated = Meal(id = meal.id, firebaseUid = meal.firebaseUid,
            name = dto.name, isFavorite = meal.isFavorite)
            .also { it.serverId = meal.serverId }
        val saved = mealRepo.save(updated)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun toggleFavorite(id: Long, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        meal.isFavorite = !meal.isFavorite
        return mealRepo.save(meal).toDto(itemRepo.findByMealId(meal.id))
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.deleteByMealId(id)
        mealRepo.delete(meal)
        tombstones.record(firebaseUid, "meal", meal.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<MealDto> {
        val meals = mealRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList()) }
    }

    @Transactional
    fun upsert(dto: MealDto, firebaseUid: String): MealDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existingByServerId = serverId?.let { mealRepo.findByServerId(it) }
        val existing = existingByServerId ?: mealRepo.findByFirebaseUidAndName(firebaseUid, dto.name)
        if (existing == null) return create(dto, firebaseUid)
        if (existingByServerId == null && dto.serverId != null) existing.serverId = UUID.fromString(dto.serverId.toString())
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt))
            return existing.toDto(itemRepo.findByMealId(existing.id))
        itemRepo.deleteByMealId(existing.id)
        val updated = Meal(id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, isFavorite = existing.isFavorite)
            .also { it.serverId = existing.serverId }
        val saved = mealRepo.save(updated)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items)
    }
}

fun MealFoodItem.toDto() = MealFoodItemDto(
    id          = id,
    mealId      = mealId,
    foodId      = foodId,
    quantity    = quantity,
    unit        = FoodUnit.forValue(unit),
    notes       = notes
)

fun Meal.toDto(items: List<MealFoodItem>) = MealDto(
    id          = id,
    serverId    = serverId,
    firebaseUid = firebaseUid,
    name        = name,
    items       = items.map { it.toDto() },
    isFavorite  = isFavorite,
    updatedAt   = updatedAt
)
