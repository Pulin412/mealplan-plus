package com.mealplanplus.api.domain.food

import java.time.Instant
import java.util.UUID

data class FoodDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val name: String = "",
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100: Double = 0.0,
    val proteinPer100: Double = 0.0,
    val carbsPer100: Double = 0.0,
    val fatPer100: Double = 0.0,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null,
    val isSystemFood: Boolean = false,
    val updatedAt: Instant? = null
)

fun Food.toDto() = FoodDto(id, serverId, name, brand, barcode, caloriesPer100,
    proteinPer100, carbsPer100, fatPer100, gramsPerPiece, gramsPerCup,
    gramsPerTbsp, gramsPerTsp, glycemicIndex, isSystemFood, updatedAt)
