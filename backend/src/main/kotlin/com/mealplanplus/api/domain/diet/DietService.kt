package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DietService(
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val tagRepo: TagRepository,
    private val crossRefRepo: DietTagCrossRefRepository,
    private val tombstones: TombstoneService
) {
    private fun Diet.toFullDto() = toDto(
        dietMealRepo.findByDietId(id),
        crossRefRepo.findByDietId(id).map { it.tagId }
    )

    fun list(firebaseUid: String): List<DietDto> =
        dietRepo.findByFirebaseUid(firebaseUid).map { it.toFullDto() }

    fun get(id: Long): DietDto = dietRepo.findById(id).orElseThrow().toFullDto()

    @Transactional
    fun create(dto: DietDto, firebaseUid: String): DietDto {
        val diet = Diet(
            firebaseUid = firebaseUid, name = dto.name, description = dto.description,
            targetCalories = dto.targetCalories, targetProtein = dto.targetProtein,
            targetCarbs = dto.targetCarbs, targetFat = dto.targetFat
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = dietRepo.save(diet)
        dto.meals.forEach { m ->
            dietMealRepo.save(DietMeal(dietId = saved.id, mealId = m.mealId,
                dayOfWeek = m.dayOfWeek, slot = m.slot, instructions = m.instructions))
        }
        dto.tagIds.forEach { tagId -> crossRefRepo.save(DietTagCrossRef(dietId = saved.id, tagId = tagId)) }
        return saved.toFullDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val diet = dietRepo.findById(id).orElseThrow()
        require(diet.firebaseUid == firebaseUid) { "Forbidden" }
        dietMealRepo.deleteByDietId(id)
        crossRefRepo.deleteByDietId(id)
        dietRepo.delete(diet)
        tombstones.record(firebaseUid, "diet", diet.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<DietDto> =
        dietRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since).map { it.toFullDto() }

    @Transactional
    fun upsert(dto: DietDto, firebaseUid: String): DietDto {
        val existing = dto.serverId?.let { dietRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toFullDto()
        dietMealRepo.deleteByDietId(existing.id)
        crossRefRepo.deleteByDietId(existing.id)
        val updated = Diet(
            id = existing.id, firebaseUid = existing.firebaseUid, name = dto.name,
            description = dto.description, targetCalories = dto.targetCalories,
            targetProtein = dto.targetProtein, targetCarbs = dto.targetCarbs, targetFat = dto.targetFat
        ).also { it.serverId = existing.serverId }
        val saved = dietRepo.save(updated)
        dto.meals.forEach { m ->
            dietMealRepo.save(DietMeal(dietId = saved.id, mealId = m.mealId,
                dayOfWeek = m.dayOfWeek, slot = m.slot, instructions = m.instructions))
        }
        dto.tagIds.forEach { tagId -> crossRefRepo.save(DietTagCrossRef(dietId = saved.id, tagId = tagId)) }
        return saved.toFullDto()
    }
}
