package com.mealplanplus.api.domain.grocery

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*

@Entity
@Table(name = "grocery_lists")
class GroceryList(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val dietId: Long? = null
) : SyncableEntity()

@Entity
@Table(name = "grocery_items")
class GroceryItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val groceryListId: Long = 0,
    val foodId: Long? = null,
    val name: String = "",
    val quantity: Double = 1.0,
    val unit: String = "GRAM",
    val category: String? = null,
    val done: Boolean = false
)
