package com.mealplanplus.api.domain.meal

import java.time.Instant
import java.util.UUID

data class MealFoodItemDto(
    val id: Long = 0,
    val mealId: Long = 0,
    val foodId: Long = 0,
    val quantity: Double = 0.0,
    val unit: String = "GRAM",
    val notes: String? = null
)

data class MealDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val slot: String = "Lunch",
    val items: List<MealFoodItemDto> = emptyList(),
    val updatedAt: Instant? = null
)

fun MealFoodItem.toDto() = MealFoodItemDto(id, mealId, foodId, quantity, unit, notes)
fun Meal.toDto(items: List<MealFoodItem>) =
    MealDto(id, serverId, firebaseUid, name, slot, items.map { it.toDto() }, updatedAt)
