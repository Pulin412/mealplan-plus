package com.mealplanplus.api.domain.grocery

import com.mealplanplus.api.domain.diet.DietMealRepository
import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class GroceryService(
    private val listRepo: GroceryListRepository,
    private val itemRepo: GroceryItemRepository,
    private val tombstones: TombstoneService,
    // for from-diet generation
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val mealFoodRepo: MealFoodItemRepository,
    private val foodRepo: FoodRepository
) {
    fun list(firebaseUid: String): List<GroceryListDto> =
        listRepo.findByFirebaseUid(firebaseUid).map { it.toDto(itemRepo.findByGroceryListId(it.id)) }

    fun get(id: Long): GroceryListDto {
        val gl = listRepo.findById(id).orElseThrow()
        return gl.toDto(itemRepo.findByGroceryListId(gl.id))
    }

    @Transactional
    fun create(dto: GroceryListDto, firebaseUid: String): GroceryListDto {
        val gl = GroceryList(firebaseUid = firebaseUid, name = dto.name, dietId = dto.dietId)
            .also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = listRepo.save(gl)
        val items = dto.items.map { item ->
            itemRepo.save(GroceryItem(groceryListId = saved.id, foodId = item.foodId, name = item.name,
                quantity = item.quantity, unit = item.unit, category = item.category, done = item.done))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val gl = listRepo.findById(id).orElseThrow()
        require(gl.firebaseUid == firebaseUid) { "Forbidden" }
        itemRepo.deleteByGroceryListId(id)
        listRepo.delete(gl)
        tombstones.record(firebaseUid, "grocery_list", gl.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<GroceryListDto> =
        listRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(itemRepo.findByGroceryListId(it.id)) }

    @Transactional
    fun update(id: Long, dto: GroceryListDto, firebaseUid: String): GroceryListDto {
        val existing = listRepo.findById(id).orElseThrow()
        require(existing.firebaseUid == firebaseUid) { "Forbidden" }
        itemRepo.deleteByGroceryListId(existing.id)
        val updated = GroceryList(id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, dietId = dto.dietId)
            .also { it.serverId = existing.serverId }
        val saved = listRepo.save(updated)
        val items = dto.items.map { item ->
            itemRepo.save(GroceryItem(groceryListId = saved.id, foodId = item.foodId, name = item.name,
                quantity = item.quantity, unit = item.unit, category = item.category, done = item.done))
        }
        return saved.toDto(items)
    }

    @Transactional
    fun upsert(dto: GroceryListDto, firebaseUid: String): GroceryListDto {
        val existing = dto.serverId?.let { listRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toDto(itemRepo.findByGroceryListId(existing.id))
        itemRepo.deleteByGroceryListId(existing.id)
        val updated = GroceryList(id = existing.id, firebaseUid = existing.firebaseUid, name = dto.name, dietId = dto.dietId)
            .also { it.serverId = existing.serverId }
        val saved = listRepo.save(updated)
        val items = dto.items.map { item ->
            itemRepo.save(GroceryItem(groceryListId = saved.id, foodId = item.foodId, name = item.name,
                quantity = item.quantity, unit = item.unit, category = item.category, done = item.done))
        }
        return saved.toDto(items)
    }

    /**
     * Generate a grocery list from all foods used across every meal in a diet.
     * Quantities are aggregated per food (same foodId = sum the grams).
     */
    @Transactional
    fun createFromDiet(dietId: Long, firebaseUid: String): GroceryListDto {
        val diet = dietRepo.findById(dietId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Diet not found")
        }
        val dietMeals = dietMealRepo.findByDietId(dietId)
        // Aggregate: foodId → total grams
        val totals = mutableMapOf<Long, Double>()
        dietMeals.forEach { dm ->
            mealFoodRepo.findByMealId(dm.mealId).forEach { item ->
                val grams = if (item.unit == "GRAM") item.quantity else item.quantity * 100.0
                totals[item.foodId] = (totals[item.foodId] ?: 0.0) + grams
            }
        }
        val gl = GroceryList(firebaseUid = firebaseUid, name = "${diet.name} — Shopping List", dietId = dietId)
        val saved = listRepo.save(gl)
        val items = totals.map { (foodId, grams) ->
            val food = foodRepo.findById(foodId).orElse(null)
            itemRepo.save(GroceryItem(
                groceryListId = saved.id,
                foodId = foodId,
                name = food?.name ?: "Food #$foodId",
                quantity = grams,
                unit = "GRAM",
                category = null,
                done = false
            ))
        }
        return saved.toDto(items)
    }
}
