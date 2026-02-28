package com.mealplanplus.api.domain.diet

import java.time.Instant
import java.util.UUID

data class DietMealDto(
    val id: Long = 0,
    val dietId: Long = 0,
    val mealId: Long = 0,
    val dayOfWeek: Int = 0,
    val slot: String = "Lunch",
    val instructions: String? = null
)

data class DietDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val name: String = "",
    val description: String? = null,
    val targetCalories: Double? = null,
    val targetProtein: Double? = null,
    val targetCarbs: Double? = null,
    val targetFat: Double? = null,
    val meals: List<DietMealDto> = emptyList(),
    val tagIds: List<Long> = emptyList(),
    val updatedAt: Instant? = null
)

fun DietMeal.toDto() = DietMealDto(id, dietId, mealId, dayOfWeek, slot, instructions)
fun Diet.toDto(meals: List<DietMeal>, tagIds: List<Long>) =
    DietDto(id, serverId, firebaseUid, name, description,
        targetCalories, targetProtein, targetCarbs, targetFat,
        meals.map { it.toDto() }, tagIds, updatedAt)
