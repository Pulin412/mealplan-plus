package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.generated.model.DietDto
import com.mealplanplus.api.generated.model.DietFoodItemDto
import com.mealplanplus.api.generated.model.DietMealDto
import com.mealplanplus.api.generated.model.TagDto
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
    private val tombstones: TombstoneService
) {
    private fun tagsForDiet(dietId: Long): List<Tag> {
        val tagIds = entityTagRepo
            .findByEntityTypeAndEntityId(TagEntityType.DIET, dietId)
            .map { it.tagId }
        return if (tagIds.isEmpty()) emptyList() else tagRepo.findAllById(tagIds).toList()
    }

    private fun Diet.toFullDto(): DietDto = toDto(
        meals     = dietMealRepo.findByDietId(id),
        foodItems = dietFoodItemRepo.findByDietId(id),
        tags      = tagsForDiet(id)
    )

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
        return diets.map { diet ->
            val tags = (entityTagsByDiet[diet.id] ?: emptyList()).mapNotNull { tagsById[it.tagId] }
            diet.toDto(meals     = mealsByDietId[diet.id] ?: emptyList(),
                       foodItems = foodsByDietId[diet.id] ?: emptyList(),
                       tags      = tags)
        }
    }

    private fun saveMealsAndItems(dietId: Long, dto: DietDto) {
        (dto.meals ?: emptyList()).forEach { m ->
            dietMealRepo.save(DietMeal(dietId = dietId, mealId = m.mealId ?: 0L,
                dayOfWeek = m.dayOfWeek ?: 0, slot = m.slot ?: "", instructions = m.instructions))
        }
        (dto.foodItems ?: emptyList()).forEach { f ->
            dietFoodItemRepo.save(DietFoodItem(dietId = dietId, foodId = f.foodId ?: 0L,
                slot = f.slot ?: "", quantity = f.quantity ?: 1.0, unit = f.unit ?: "GRAM"))
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
    fun delete(id: Long, firebaseUid: String) {
        val diet = dietRepo.findById(id).orElseThrow()
        if (diet.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        clearDietChildren(id)
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
            isFavorite = existing.isFavorite
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

fun DietMeal.toDto() = DietMealDto(id = id, dietId = dietId, mealId = mealId,
    dayOfWeek = dayOfWeek, slot = slot, instructions = instructions)

fun DietFoodItem.toDto() = DietFoodItemDto(id = id, dietId = dietId, foodId = foodId,
    slot = slot, quantity = quantity, unit = unit)

fun Diet.toDto(meals: List<DietMeal>, foodItems: List<DietFoodItem>, tags: List<Tag>) = DietDto(
    id             = id,
    serverId       = serverId?.toString(),
    firebaseUid    = firebaseUid,
    name           = name,
    description    = description,
    targetCalories = targetCalories,
    targetProtein  = targetProtein,
    targetCarbs    = targetCarbs,
    targetFat      = targetFat,
    meals          = meals.map { it.toDto() },
    foodItems      = foodItems.map { it.toDto() },
    tagIds         = tags.map { it.id },
    tags           = tags.map { it.toDto() },
    isFavorite     = isFavorite,
    updatedAt      = updatedAt
)
