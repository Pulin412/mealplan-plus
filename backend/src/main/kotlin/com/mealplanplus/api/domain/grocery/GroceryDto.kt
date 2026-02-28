package com.mealplanplus.api.domain.grocery

import java.time.Instant
import java.util.UUID

data class GroceryItemDto(
    val id: Long = 0,
    val groceryListId: Long = 0,
    val foodId: Long? = null,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "GRAM",
    val category: String? = null,
    val done: Boolean = false
)

data class GroceryListDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val dietId: Long? = null,
    val items: List<GroceryItemDto> = emptyList(),
    val updatedAt: Instant? = null
)

fun GroceryItem.toDto() = GroceryItemDto(id, groceryListId, foodId, name, quantity, unit, category, done)
fun GroceryList.toDto(items: List<GroceryItem>) =
    GroceryListDto(id, serverId, firebaseUid, name, dietId, items.map { it.toDto() }, updatedAt)
