package com.mealplanplus.api.domain.food

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID

@Service
class FoodService(
    private val repo: FoodRepository,
    private val prefRepo: FoodUserPrefRepository,
    private val tombstones: TombstoneService
) {

    /** Build a set of food IDs the user has favorited in food_user_prefs (system foods). */
    private fun systemFavIds(firebaseUid: String): Set<Long> =
        prefRepo.findByFirebaseUid(firebaseUid)
            .filter { it.isFavorite }
            .map { it.foodId }
            .toSet()

    /** Populate isFavorite correctly per channel:
     *  - System foods: look up food_user_prefs so it's per-user.
     *  - User-owned foods: use foods.is_favorite (already per-user via firebase_uid). */
    private fun Food.toDtoWithPrefs(favIds: Set<Long>): FoodDto =
        toDto().copy(isFavorite = if (isSystemFood) id in favIds else isFavorite)

    fun list(firebaseUid: String): List<FoodDto> {
        val foods = repo.findByFirebaseUidOrIsSystemFoodTrue(firebaseUid)
        val favIds = systemFavIds(firebaseUid)
        return foods.map { it.toDtoWithPrefs(favIds) }
    }

    fun get(id: Long, firebaseUid: String): FoodDto {
        val food = repo.findById(id).orElseThrow()
        val favIds = if (food.isSystemFood) systemFavIds(firebaseUid) else emptySet()
        return food.toDtoWithPrefs(favIds)
    }

    @Transactional
    fun create(dto: FoodDto, firebaseUid: String): FoodDto {
        val food = Food(
            firebaseUid = firebaseUid, name = dto.name, brand = dto.brand,
            barcode = dto.barcode, caloriesPer100 = dto.caloriesPer100,
            proteinPer100 = dto.proteinPer100, carbsPer100 = dto.carbsPer100,
            fatPer100 = dto.fatPer100, gramsPerPiece = dto.gramsPerPiece,
            gramsPerCup = dto.gramsPerCup, gramsPerTbsp = dto.gramsPerTbsp,
            gramsPerTsp = dto.gramsPerTsp, glycemicIndex = dto.glycemicIndex,
            isFavorite = dto.isFavorite
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        return repo.save(food).toDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val food = repo.findById(id).orElseThrow()
        if (food.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        repo.delete(food)
        tombstones.record(firebaseUid, "food", food.serverId)
    }

    @Transactional
    fun toggleFavorite(id: Long, firebaseUid: String): FoodDto {
        val food = repo.findById(id).orElseThrow()
        if (food.firebaseUid != firebaseUid && !food.isSystemFood)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")

        return if (food.isSystemFood) {
            // System foods: per-user pref stored in food_user_prefs
            val existing = prefRepo.findByFirebaseUidAndFoodId(firebaseUid, id)
            val newVal = !(existing?.isFavorite ?: false)
            if (existing == null) {
                prefRepo.save(FoodUserPref(firebaseUid = firebaseUid, foodId = id, isFavorite = newVal))
            } else {
                existing.isFavorite = newVal
                prefRepo.save(existing)
            }
            food.toDto().copy(isFavorite = newVal)
        } else {
            // User-owned food: use foods.is_favorite directly (already per-user)
            food.isFavorite = !food.isFavorite
            repo.save(food).toDto()
        }
    }

    fun since(firebaseUid: String, since: Instant): List<FoodDto> {
        val foods = repo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since) +
                    repo.findByIsSystemFoodTrueAndUpdatedAtAfter(since)
        val favIds = systemFavIds(firebaseUid)
        return foods.map { it.toDtoWithPrefs(favIds) }
    }

    @Transactional
    fun upsert(dto: FoodDto, firebaseUid: String): FoodDto {
        val existing = dto.serverId?.let { repo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) {
            val favIds = if (existing.isSystemFood) systemFavIds(firebaseUid) else emptySet()
            return existing.toDtoWithPrefs(favIds)
        }
        val updated = Food(
            id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, brand = dto.brand, barcode = dto.barcode,
            caloriesPer100 = dto.caloriesPer100, proteinPer100 = dto.proteinPer100,
            carbsPer100 = dto.carbsPer100, fatPer100 = dto.fatPer100,
            gramsPerPiece = dto.gramsPerPiece, gramsPerCup = dto.gramsPerCup,
            gramsPerTbsp = dto.gramsPerTbsp, gramsPerTsp = dto.gramsPerTsp,
            glycemicIndex = dto.glycemicIndex, isSystemFood = existing.isSystemFood,
            isFavorite = if (existing.isSystemFood) existing.isFavorite else dto.isFavorite
        ).also { it.serverId = existing.serverId }
        val saved = repo.save(updated)
        val favIds = if (saved.isSystemFood) systemFavIds(firebaseUid) else emptySet()
        return saved.toDtoWithPrefs(favIds)
    }
}
