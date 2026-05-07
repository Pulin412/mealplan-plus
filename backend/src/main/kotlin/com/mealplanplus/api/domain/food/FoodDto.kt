package com.mealplanplus.api.domain.food

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.time.Instant
import java.util.UUID

data class FoodDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    @field:NotBlank(message = "Food name must not be blank")
    val name: String = "",
    val brand: String? = null,
    val barcode: String? = null,
    @field:PositiveOrZero(message = "caloriesPer100 must be >= 0")
    val caloriesPer100: Double = 0.0,
    @field:PositiveOrZero(message = "proteinPer100 must be >= 0")
    val proteinPer100: Double = 0.0,
    @field:PositiveOrZero(message = "carbsPer100 must be >= 0")
    val carbsPer100: Double = 0.0,
    @field:PositiveOrZero(message = "fatPer100 must be >= 0")
    val fatPer100: Double = 0.0,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null,
    val isSystemFood: Boolean = false,
    val isFavorite: Boolean = false,
    val updatedAt: Instant? = null
)

fun Food.toDto() = FoodDto(id, serverId, name, brand, barcode, caloriesPer100,
    proteinPer100, carbsPer100, fatPer100, gramsPerPiece, gramsPerCup,
    gramsPerTbsp, gramsPerTsp, glycemicIndex, isSystemFood, isFavorite, updatedAt)
