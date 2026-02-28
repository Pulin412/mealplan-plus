package com.mealplanplus.api.domain.grocery

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface GroceryListRepository : JpaRepository<GroceryList, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<GroceryList>
    fun findByServerId(serverId: UUID): GroceryList?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<GroceryList>
}

interface GroceryItemRepository : JpaRepository<GroceryItem, Long> {
    fun findByGroceryListId(groceryListId: Long): List<GroceryItem>
    fun deleteByGroceryListId(groceryListId: Long)
}
