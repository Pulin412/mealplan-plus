package com.mealplanplus.api.domain.grocery

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class GroceryService(
    private val listRepo: GroceryListRepository,
    private val itemRepo: GroceryItemRepository,
    private val tombstones: TombstoneService
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
}
