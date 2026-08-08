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

    /** foodId → stable serverId, so meal items carry a UUID the clients can resolve. */
    private fun foodServerIds(items: List<MealFoodItem>): Map<Long, UUID> =
        foodRepo.findAllById(items.map { it.foodId }.toSet()).associate { it.id to it.serverId }

    fun list(firebaseUid: String, favoritesOnly: Boolean = false): List<MealDto> {
        val meals = mealRepo.findByFirebaseUid(firebaseUid)
            .let { if (favoritesOnly) it.filter { m -> m.isFavorite } else it }
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        val foodSids = foodServerIds(itemsByMealId.values.flatten())
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList(), foodSids) }
    }

    fun get(id: Long, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.findByMealId(meal.id).let { return meal.toDto(it, foodServerIds(it)) }
    }

    @Transactional
    fun create(dto: MealDto, firebaseUid: String): MealDto {
        val meal = Meal(firebaseUid = firebaseUid, name = dto.name, isFavorite = dto.isFavorite ?: false,
            slots = dto.slots ?: emptyList())
            .also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        val saved = mealRepo.save(meal)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items, foodServerIds(items))
    }

    @Transactional
    fun update(id: Long, dto: MealDto, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        itemRepo.deleteByMealId(id)
        val updated = Meal(id = meal.id, firebaseUid = meal.firebaseUid,
            name = dto.name, isFavorite = meal.isFavorite, slots = dto.slots ?: meal.slots)
            .also { it.serverId = meal.serverId }
        val saved = mealRepo.save(updated)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items, foodServerIds(items))
    }

    @Transactional
    fun toggleFavorite(id: Long, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        meal.isFavorite = !meal.isFavorite
        itemRepo.findByMealId(meal.id).let { return mealRepo.save(meal).toDto(it, foodServerIds(it)) }
    }

    @Transactional
    fun toggleShare(id: Long, firebaseUid: String): MealDto {
        val meal = mealRepo.findById(id).orElseThrow()
        if (meal.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        meal.isShared = !meal.isShared
        itemRepo.findByMealId(meal.id).let { return mealRepo.save(meal).toDto(it, foodServerIds(it)) }
    }

    /** Author-scoped shared reads for the social layer. */
    fun sharedMealsOf(authorUid: String): List<Meal> =
        mealRepo.findByFirebaseUid(authorUid).filter { it.isShared }

    fun sharedMealDto(authorUid: String, serverId: UUID): MealDto? {
        val meal = mealRepo.findByServerId(serverId) ?: return null
        if (meal.firebaseUid != authorUid || !meal.isShared) return null
        itemRepo.findByMealId(meal.id).let { return meal.toDto(it, foodServerIds(it)) }
    }

    /** Meals referenced by a shared diet (viewable as part of it, share flag not required). */
    fun dtosByServerIds(authorUid: String, serverIds: Collection<UUID>): List<MealDto> {
        if (serverIds.isEmpty()) return emptyList()
        val meals = mealRepo.findByServerIdIn(serverIds).filter { it.firebaseUid == authorUid }
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        val foodSids = foodServerIds(itemsByMealId.values.flatten())
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList(), foodSids) }
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

    /** Sync-push delete: remove by stable serverId and record a tombstone. No-op if absent/foreign. */
    @Transactional
    fun deleteByServerId(serverId: UUID, firebaseUid: String) {
        val meal = mealRepo.findByServerId(serverId) ?: return
        if (meal.firebaseUid != firebaseUid) return
        itemRepo.deleteByMealId(meal.id)
        mealRepo.delete(meal)
        tombstones.record(firebaseUid, "meal", meal.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<MealDto> {
        val meals = mealRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
        if (meals.isEmpty()) return emptyList()
        val itemsByMealId = itemRepo.findByMealIdIn(meals.map { it.id }).groupBy { it.mealId }
        val foodSids = foodServerIds(itemsByMealId.values.flatten())
        return meals.map { it.toDto(itemsByMealId[it.id] ?: emptyList(), foodSids) }
    }

    @Transactional
    fun upsert(dto: MealDto, firebaseUid: String): MealDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existingByServerId = serverId?.let { mealRepo.findByServerId(it) }
        val existing = existingByServerId ?: mealRepo.findByFirebaseUidAndName(firebaseUid, dto.name)
        if (existing == null) return create(dto, firebaseUid)
        if (existingByServerId == null && dto.serverId != null) existing.serverId = UUID.fromString(dto.serverId.toString())
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt))
            itemRepo.findByMealId(existing.id).let { return existing.toDto(it, foodServerIds(it)) }
        itemRepo.deleteByMealId(existing.id)
        val updated = Meal(id = existing.id, firebaseUid = existing.firebaseUid,
            // Honour the client's favourite flag (sync-push is the only path clients use to toggle it).
            name = dto.name, isFavorite = dto.isFavorite ?: existing.isFavorite, slots = dto.slots ?: existing.slots)
            .also { it.serverId = existing.serverId }
        val saved = mealRepo.save(updated)
        val items = (dto.items ?: emptyList()).map { item ->
            itemRepo.save(MealFoodItem(mealId = saved.id, foodId = resolveFoodId(item),
                quantity = item.quantity, unit = item.unit.value, notes = item.notes))
        }
        return saved.toDto(items, foodServerIds(items))
    }
}

fun MealFoodItem.toDto(foodServerIds: Map<Long, UUID>) = MealFoodItemDto(
    id           = id,
    mealId       = mealId,
    foodId       = foodId,
    foodServerId = foodServerIds[foodId],   // so clients can resolve the food by its stable UUID
    quantity     = quantity,
    unit         = FoodUnit.forValue(unit),
    notes        = notes
)

fun Meal.toDto(items: List<MealFoodItem>, foodServerIds: Map<Long, UUID>) = MealDto(
    id          = id,
    serverId    = serverId,
    firebaseUid = firebaseUid,
    name        = name,
    slots       = slots,
    items       = items.map { it.toDto(foodServerIds) },
    isFavorite  = isFavorite,
    isShared    = isShared,
    updatedAt   = updatedAt
)
