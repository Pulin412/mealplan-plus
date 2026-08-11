package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.generated.model.DietDto
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.DietFoodItemDto
import com.mealplanplus.api.generated.model.DietMealDto
import com.mealplanplus.api.generated.model.TagDto
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class DietService(
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val dietFoodItemRepo: DietFoodItemRepository,
    private val tagRepo: TagRepository,
    private val entityTagRepo: EntityTagRepository,
    private val mealRepo: MealRepository,
    private val foodRepo: FoodRepository,
    private val tombstones: TombstoneService,
    private val notificationService: com.mealplanplus.api.domain.social.NotificationService,
) {
    // ── serverId ↔ id resolution so offline clients (UUID identity) work ─────────
    /** UUID a client sent → local meal id (falls back to the numeric mealId). */
    private fun resolveMealId(dto: DietMealDto): Long {
        if (dto.mealServerId != null) {
            val meal = runCatching { mealRepo.findByServerId(UUID.fromString(dto.mealServerId.toString())) }.getOrNull()
            if (meal != null) return meal.id
        }
        return dto.mealId ?: 0L
    }

    private fun resolveFoodId(dto: DietFoodItemDto): Long {
        if (dto.foodServerId != null) {
            val food = runCatching { foodRepo.findByServerId(UUID.fromString(dto.foodServerId.toString())) }.getOrNull()
            if (food != null) return food.id
        }
        return dto.foodId ?: 0L
    }

    private fun mealServerIds(meals: List<DietMeal>): Map<Long, UUID> =
        mealRepo.findAllById(meals.map { it.mealId }.toSet()).associate { it.id to it.serverId }

    private fun foodServerIds(items: List<DietFoodItem>): Map<Long, UUID> =
        foodRepo.findAllById(items.map { it.foodId }.toSet()).associate { it.id to it.serverId }

    private fun tagsForDiet(dietId: Long): List<Tag> {
        val tagIds = entityTagRepo
            .findByEntityTypeAndEntityId(TagEntityType.DIET, dietId)
            .map { it.tagId }
        return if (tagIds.isEmpty()) emptyList() else tagRepo.findAllById(tagIds).toList()
    }

    private fun Diet.toFullDto(): DietDto {
        val meals = dietMealRepo.findByDietId(id)
        val foods = dietFoodItemRepo.findByDietId(id)
        return toDto(meals, foods, tagsForDiet(id), mealServerIds(meals), foodServerIds(foods))
    }

    private fun batchToDtos(diets: List<Diet>): List<DietDto> {
        if (diets.isEmpty()) return emptyList()
        val ids = diets.map { it.id }
        val mealsByDietId    = dietMealRepo.findByDietIdIn(ids).groupBy { it.dietId }
        val foodsByDietId    = dietFoodItemRepo.findByDietIdIn(ids).groupBy { it.dietId }
        val entityTags       = entityTagRepo.findByEntityTypeAndEntityIdIn(TagEntityType.DIET, ids)
        val tagIds           = entityTags.map { it.tagId }.toSet()
        val tagsById         = if (tagIds.isEmpty()) emptyMap()
                               else tagRepo.findAllById(tagIds).associateBy { it.id }
        val entityTagsByDiet = entityTags.groupBy { it.entityId }
        val mealSids = mealServerIds(mealsByDietId.values.flatten())
        val foodSids = foodServerIds(foodsByDietId.values.flatten())
        return diets.map { diet ->
            val tags = (entityTagsByDiet[diet.id] ?: emptyList()).mapNotNull { tagsById[it.tagId] }
            diet.toDto(meals     = mealsByDietId[diet.id] ?: emptyList(),
                       foodItems = foodsByDietId[diet.id] ?: emptyList(),
                       tags      = tags,
                       mealServerIds = mealSids,
                       foodServerIds = foodSids)
        }
    }

    private fun saveMealsAndItems(dietId: Long, dto: DietDto) {
        (dto.meals ?: emptyList()).forEach { m ->
            dietMealRepo.save(DietMeal(dietId = dietId, mealId = resolveMealId(m),
                dayOfWeek = m.dayOfWeek ?: 0, slot = m.slot ?: "", instructions = m.instructions))
        }
        (dto.foodItems ?: emptyList()).forEach { f ->
            dietFoodItemRepo.save(DietFoodItem(dietId = dietId, foodId = resolveFoodId(f),
                slot = f.slot ?: "", quantity = f.quantity ?: 1.0, unit = f.unit.value))
        }
        (dto.tagIds ?: emptyList()).forEach { tagId ->
            entityTagRepo.save(EntityTag(tagId = tagId, entityType = TagEntityType.DIET, entityId = dietId))
        }
    }

    private fun clearDietChildren(dietId: Long) {
        dietMealRepo.deleteByDietId(dietId)
        dietFoodItemRepo.deleteByDietId(dietId)
        entityTagRepo.deleteByEntityTypeAndEntityId(TagEntityType.DIET, dietId)
    }

    fun list(firebaseUid: String, favoritesOnly: Boolean = false): List<DietDto> {
        val diets = dietRepo.findByFirebaseUid(firebaseUid)
            .let { if (favoritesOnly) it.filter { d -> d.isFavorite } else it }
        return batchToDtos(diets)
    }

    fun get(id: Long, firebaseUid: String): DietDto {
        val diet = dietRepo.findById(id).orElseThrow()
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        return diet.toFullDto()
    }

    @Transactional
    fun create(dto: DietDto, firebaseUid: String): DietDto {
        val diet = Diet(
            firebaseUid    = firebaseUid,
            name           = dto.name,
            description    = dto.description,
            targetCalories = dto.targetCalories,
            targetProtein  = dto.targetProtein,
            targetCarbs    = dto.targetCarbs,
            targetFat      = dto.targetFat,
            isFavorite     = dto.isFavorite ?: false
        ).also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        val saved = dietRepo.save(diet)
        saveMealsAndItems(saved.id, dto)
        return saved.toFullDto()
    }

    @Transactional
    fun update(id: Long, dto: DietDto, firebaseUid: String): DietDto {
        val diet = dietRepo.findById(id).orElseThrow()
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        clearDietChildren(id)
        val updated = Diet(
            id = diet.id, firebaseUid = diet.firebaseUid,
            name = dto.name, description = dto.description,
            targetCalories = dto.targetCalories, targetProtein = dto.targetProtein,
            targetCarbs = dto.targetCarbs, targetFat = dto.targetFat,
            isFavorite = diet.isFavorite
        ).also { it.serverId = diet.serverId }
        val saved = dietRepo.save(updated)
        saveMealsAndItems(saved.id, dto)
        return saved.toFullDto()
    }

    @Transactional
    fun toggleFavorite(id: Long, firebaseUid: String): DietDto {
        val diet = dietRepo.findById(id).orElseThrow()
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        diet.isFavorite = !diet.isFavorite
        return dietRepo.save(diet).toFullDto()
    }

    @Transactional
    fun toggleShare(serverId: UUID, firebaseUid: String): DietDto {
        val diet = dietRepo.findByServerId(serverId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        val wasShared = diet.isShared
        diet.isShared = !diet.isShared
        val saved = dietRepo.save(diet)
        if (!wasShared && saved.isShared) {
            notificationService.notifyShare(
                firebaseUid, com.mealplanplus.api.domain.social.NotificationSubjectKind.DIET,
                saved.serverId, saved.name,
            )
        }
        return saved.toFullDto()
    }

    /** Author-scoped shared reads for the social layer. */
    fun sharedDietsOf(authorUid: String): List<Diet> =
        dietRepo.findByFirebaseUid(authorUid).filter { it.isShared }

    fun sharedDietDto(authorUid: String, serverId: UUID): DietDto? {
        val diet = dietRepo.findByServerId(serverId) ?: return null
        if (diet.firebaseUid != authorUid || !diet.isShared) return null
        return diet.toFullDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val diet = dietRepo.findById(id).orElseThrow()
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        clearDietChildren(id)
        dietRepo.delete(diet)
        tombstones.record(firebaseUid, "diet", diet.serverId)
    }

    /** Sync-push delete: remove by stable serverId and record a tombstone. No-op if absent/foreign. */
    @Transactional
    fun deleteByServerId(serverId: UUID, firebaseUid: String) {
        val diet = dietRepo.findByServerId(serverId) ?: return
        if (diet.firebaseUid != firebaseUid) return
        clearDietChildren(diet.id)
        dietRepo.delete(diet)
        tombstones.record(firebaseUid, "diet", diet.serverId)
    }

    @Transactional
    fun duplicate(id: Long, firebaseUid: String): DietDto {
        val original  = dietRepo.findById(id).orElseThrow()
        val meals     = dietMealRepo.findByDietId(original.id)
        val foodItems = dietFoodItemRepo.findByDietId(original.id)
        val tagIds    = entityTagRepo.findByEntityTypeAndEntityId(TagEntityType.DIET, original.id).map { it.tagId }
        val copy = Diet(
            firebaseUid    = firebaseUid,
            name           = "${original.name} (copy)",
            description    = original.description,
            targetCalories = original.targetCalories,
            targetProtein  = original.targetProtein,
            targetCarbs    = original.targetCarbs,
            targetFat      = original.targetFat
        )
        val saved = dietRepo.save(copy)
        meals.forEach { m ->
            dietMealRepo.save(DietMeal(dietId = saved.id, mealId = m.mealId,
                dayOfWeek = m.dayOfWeek, slot = m.slot, instructions = m.instructions))
        }
        foodItems.forEach { f ->
            dietFoodItemRepo.save(DietFoodItem(dietId = saved.id, foodId = f.foodId,
                slot = f.slot, quantity = f.quantity, unit = f.unit))
        }
        tagIds.forEach { tagId ->
            entityTagRepo.save(EntityTag(tagId = tagId, entityType = TagEntityType.DIET, entityId = saved.id))
        }
        return saved.toFullDto()
    }

    // ── Tags ─────────────────────────────────────────────────────────────────

    fun listTags(firebaseUid: String, entityType: TagEntityType? = null): List<TagDto> {
        val systemTags = if (entityType != null)
            tagRepo.findByEntityType(entityType).filter { it.firebaseUid == null }
        else
            tagRepo.findAll().filter { it.firebaseUid == null }
        val userTags = if (entityType != null)
            tagRepo.findByFirebaseUidAndEntityType(firebaseUid, entityType)
        else
            tagRepo.findByFirebaseUid(firebaseUid)
        return (systemTags + userTags).map { it.toDto() }
    }

    @Transactional
    fun createTag(name: String, color: String?, firebaseUid: String, entityType: TagEntityType): TagDto {
        val existing = tagRepo.findByFirebaseUidAndName(firebaseUid, name)
        if (existing != null) return existing.toDto()
        return tagRepo.save(Tag(firebaseUid = firebaseUid, name = name, color = color,
            entityType = entityType)).toDto()
    }

    @Transactional
    fun deleteTag(id: Long, firebaseUid: String) {
        val tag = tagRepo.findById(id).orElseThrow()
        if (tag.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        entityTagRepo.deleteByTagId(id)
        tagRepo.delete(tag)
    }

    fun since(firebaseUid: String, since: Instant): List<DietDto> =
        batchToDtos(dietRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since))

    @Transactional
    fun upsert(dto: DietDto, firebaseUid: String): DietDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existingByServerId = serverId?.let { dietRepo.findByServerId(it) }
        val existing = existingByServerId ?: dietRepo.findByFirebaseUidAndName(firebaseUid, dto.name)
        if (existing == null) return create(dto, firebaseUid)
        if (existingByServerId == null && dto.serverId != null) existing.serverId = UUID.fromString(dto.serverId.toString())
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt)) return existing.toFullDto()
        clearDietChildren(existing.id)
        val updated = Diet(
            id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, description = dto.description,
            targetCalories = dto.targetCalories, targetProtein = dto.targetProtein,
            targetCarbs = dto.targetCarbs, targetFat = dto.targetFat,
            // Honour the client's favourite flag (sync-push is the only path clients use to toggle it);
            // fall back to the stored value when the payload omits it.
            isFavorite = dto.isFavorite ?: existing.isFavorite
        ).also { it.serverId = existing.serverId }
        val saved = dietRepo.save(updated)
        saveMealsAndItems(saved.id, dto)
        return saved.toFullDto()
    }
}

fun Tag.toDto() = TagDto(
    id         = id,
    name       = name,
    color      = color,
    entityType = com.mealplanplus.api.generated.model.TagEntityType.valueOf(entityType.name)
)

fun DietMeal.toDto(mealServerIds: Map<Long, UUID>) = DietMealDto(id = id, dietId = dietId, mealId = mealId,
    mealServerId = mealServerIds[mealId], dayOfWeek = dayOfWeek, slot = slot, instructions = instructions)

fun DietFoodItem.toDto(foodServerIds: Map<Long, UUID>) = DietFoodItemDto(id = id, dietId = dietId, foodId = foodId,
    foodServerId = foodServerIds[foodId], slot = slot, quantity = quantity, unit = FoodUnit.forValue(unit))

fun Diet.toDto(
    meals: List<DietMeal>, foodItems: List<DietFoodItem>, tags: List<Tag>,
    mealServerIds: Map<Long, UUID>, foodServerIds: Map<Long, UUID>,
) = DietDto(
    id             = id,
    serverId       = serverId,
    firebaseUid    = firebaseUid,
    name           = name,
    description    = description,
    targetCalories = targetCalories,
    targetProtein  = targetProtein,
    targetCarbs    = targetCarbs,
    targetFat      = targetFat,
    meals          = meals.map { it.toDto(mealServerIds) },
    foodItems      = foodItems.map { it.toDto(foodServerIds) },
    tagIds         = tags.map { it.id },
    tags           = tags.map { it.toDto() },
    isFavorite     = isFavorite,
    isShared       = isShared,
    imported       = copiedFromUid != null,
    updatedAt      = updatedAt
)
