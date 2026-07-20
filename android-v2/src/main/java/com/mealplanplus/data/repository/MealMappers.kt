package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.generated.model.MealDto
import com.mealplanplus.data.generated.model.MealFoodItemDto
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealItem
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

// ── DTO ↔ entity ─────────────────────────────────────────────────────────────
fun MealDto.toEntity(dirty: Boolean = false): Meal = Meal(
    id         = (serverId ?: UUID.randomUUID()).toString(),
    name       = name,
    slots      = slots ?: emptyList(),
    items      = (items ?: emptyList()).map { it.toItem() },
    isFavorite = isFavorite ?: false,
    updatedAt  = updatedAt?.toEpochMilli() ?: System.currentTimeMillis(),
    dirty      = dirty,
    deletedAt  = null,
)

fun Meal.toDto(): MealDto = MealDto(
    name       = name,
    serverId   = UUID.fromString(id),
    slots      = slots,
    items      = items.map { it.toDto() },
    isFavorite = isFavorite,
    updatedAt  = Instant.ofEpochMilli(updatedAt),
)

private fun MealFoodItemDto.toItem(): MealItem = MealItem(
    foodServerId = (foodServerId ?: UUID.randomUUID()).toString(),
    quantity     = quantity,
    unit         = unit.value,
    notes        = notes,
)

private fun MealItem.toDto(): MealFoodItemDto = MealFoodItemDto(
    foodId       = 0L,                      // server resolves by foodServerId
    quantity     = quantity,
    unit         = FoodUnit.decode(unit) ?: FoodUnit.GRAM,
    foodServerId = UUID.fromString(foodServerId),
    notes        = notes,
)

// ── Resolved display model (meal + its foods' names & macros) ────────────────
data class MealUi(
    val meal: Meal,
    val items: List<MealItemUi>,
    val totalKcal: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val itemsSummary: String,
)

data class MealItemUi(
    val name: String,
    val grams: Double,
    val kcal: Int,
    val meta: String,
)

/** Resolve a meal's item foodIds against the local food cache and compute totals. */
fun Meal.resolve(foodsById: Map<String, Food>): MealUi {
    val resolved = items.map { item ->
        val food = foodsById[item.foodServerId]
        val grams = item.quantity           // New Meal enters grams (unit = GRAM)
        val factor = grams / 100.0
        val kcal = ((food?.caloriesPer100 ?: 0.0) * factor).roundToInt()
        MealItemUi(
            name  = food?.name ?: "Unknown food",
            grams = grams,
            kcal  = kcal,
            meta  = "${grams.trimNum()}g · $kcal kcal",
        )
    }
    var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
    items.forEach { item ->
        val food = foodsById[item.foodServerId] ?: return@forEach
        val factor = item.quantity / 100.0
        kcal += food.caloriesPer100 * factor
        p += food.proteinPer100 * factor
        c += food.carbsPer100 * factor
        f += food.fatPer100 * factor
    }
    val names = resolved.map { it.name }
    val summary = when {
        items.isEmpty() -> "No items"
        else -> "${items.size} item${if (items.size == 1) "" else "s"} · ${names.joinToString(", ")}"
    }
    return MealUi(this, resolved, kcal.roundToInt(), p, c, f, summary)
}

private fun Double.trimNum(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
