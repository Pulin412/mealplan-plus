package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.model.Food
import java.time.Instant
import java.util.UUID

/** Server DTO → local entity (pull). The UUID `serverId` becomes the Room primary key. */
fun FoodDto.toEntity(dirty: Boolean = false): Food = Food(
    id             = (serverId ?: UUID.randomUUID()).toString(),
    name           = name,
    brand          = brand,
    category       = category,
    servingLabel   = null,
    caloriesPer100 = caloriesPer100,
    proteinPer100  = proteinPer100,
    carbsPer100    = carbsPer100,
    fatPer100      = fatPer100,
    unit           = (unit ?: FoodUnit.GRAM).value,
    gramsPerPiece  = gramsPerPiece,
    gramsPerCup    = gramsPerCup,
    gramsPerTbsp   = gramsPerTbsp,
    gramsPerTsp    = gramsPerTsp,
    glycemicIndex  = glycemicIndex,
    isFavorite     = isFavorite ?: false,
    isSystemFood   = isSystemFood ?: false,
    verified       = verified ?: false,
    updatedAt      = updatedAt?.toEpochMilli() ?: System.currentTimeMillis(),
    dirty          = dirty,
    deletedAt      = null,
)

/** Local entity → server DTO (push). Carries the client UUID as `serverId`. */
fun Food.toDto(): FoodDto = FoodDto(
    name           = name,
    caloriesPer100 = caloriesPer100,
    proteinPer100  = proteinPer100,
    carbsPer100    = carbsPer100,
    fatPer100      = fatPer100,
    serverId       = UUID.fromString(id),
    brand          = brand,
    category       = category,
    unit           = FoodUnit.decode(unit) ?: FoodUnit.GRAM,
    gramsPerPiece  = gramsPerPiece,
    gramsPerCup    = gramsPerCup,
    gramsPerTbsp   = gramsPerTbsp,
    gramsPerTsp    = gramsPerTsp,
    glycemicIndex  = glycemicIndex,
    isSystemFood   = isSystemFood,
    isFavorite     = isFavorite,
    verified       = verified,
    updatedAt      = Instant.ofEpochMilli(updatedAt),
)
