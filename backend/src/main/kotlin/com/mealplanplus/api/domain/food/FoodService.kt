package com.mealplanplus.api.domain.food

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class FoodService(private val repo: FoodRepository, private val tombstones: TombstoneService) {

    fun list(firebaseUid: String): List<FoodDto> =
        repo.findByFirebaseUidOrIsSystemFoodTrue(firebaseUid).map { it.toDto() }

    fun get(id: Long): FoodDto = repo.findById(id).orElseThrow().toDto()

    @Transactional
    fun create(dto: FoodDto, firebaseUid: String): FoodDto {
        val food = Food(
            firebaseUid = firebaseUid, name = dto.name, brand = dto.brand,
            barcode = dto.barcode, caloriesPer100 = dto.caloriesPer100,
            proteinPer100 = dto.proteinPer100, carbsPer100 = dto.carbsPer100,
            fatPer100 = dto.fatPer100, gramsPerPiece = dto.gramsPerPiece,
            gramsPerCup = dto.gramsPerCup, gramsPerTbsp = dto.gramsPerTbsp,
            gramsPerTsp = dto.gramsPerTsp, glycemicIndex = dto.glycemicIndex
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        return repo.save(food).toDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val food = repo.findById(id).orElseThrow()
        require(food.firebaseUid == firebaseUid) { "Forbidden" }
        repo.delete(food)
        tombstones.record(firebaseUid, "food", food.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<FoodDto> =
        (repo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since) +
         repo.findByIsSystemFoodTrueAndUpdatedAtAfter(since)).map { it.toDto() }

    @Transactional
    fun upsert(dto: FoodDto, firebaseUid: String): FoodDto {
        val existing = dto.serverId?.let { repo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toDto()
        val updated = Food(
            id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, brand = dto.brand, barcode = dto.barcode,
            caloriesPer100 = dto.caloriesPer100, proteinPer100 = dto.proteinPer100,
            carbsPer100 = dto.carbsPer100, fatPer100 = dto.fatPer100,
            gramsPerPiece = dto.gramsPerPiece, gramsPerCup = dto.gramsPerCup,
            gramsPerTbsp = dto.gramsPerTbsp, gramsPerTsp = dto.gramsPerTsp,
            glycemicIndex = dto.glycemicIndex, isSystemFood = existing.isSystemFood
        ).also { it.serverId = existing.serverId }
        return repo.save(updated).toDto()
    }
}
