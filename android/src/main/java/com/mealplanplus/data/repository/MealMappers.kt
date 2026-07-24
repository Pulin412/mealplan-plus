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

/**
 * Grams for [quantity] of a food measured in [unit]. GRAM/ML are taken as-is (density ≈ 1);
 * count-based units multiply by the food's matching gramsPer* factor (fallback 1.0).
 */
fun Food?.gramsFor(quantity: Double, unit: String): Double = when (unit) {
    "PIECE" -> quantity * (this?.gramsPerPiece ?: 1.0)
    "CUP"   -> quantity * (this?.gramsPerCup ?: 1.0)
    "TBSP"  -> quantity * (this?.gramsPerTbsp ?: 1.0)
    "TSP"   -> quantity * (this?.gramsPerTsp ?: 1.0)
    else    -> quantity   // GRAM, ML
}

/** Short display label for a unit, e.g. GRAM -> "g", PIECE -> "pcs". */
fun unitLabel(unit: String): String = when (unit) {
    "ML"    -> "ml"
    "PIECE" -> "pcs"
    "CUP"   -> "cup"
    "TBSP"  -> "tbsp"
    "TSP"   -> "tsp"
    else    -> "g"
}

/** Selectable food units, in display order. */
val FOOD_UNITS = listOf("GRAM", "ML", "PIECE", "CUP", "TBSP", "TSP")

/** Count-based units need a grams-per-unit factor to compute calories; GRAM/ML don't. */
fun isCountUnit(unit: String): Boolean = unit in setOf("PIECE", "CUP", "TBSP", "TSP")

/** Sensible default quantity when a food is first added to a meal, per unit. */
fun defaultQtyFor(unit: String): Double = if (isCountUnit(unit)) 1.0 else 100.0

/** Grams that one unit of this food weighs (for the totals math); 1.0 for GRAM/ML. */
fun Food.gramsPerUnit(): Double = when (unit) {
    "PIECE" -> gramsPerPiece ?: 1.0
    "CUP"   -> gramsPerCup ?: 1.0
    "TBSP"  -> gramsPerTbsp ?: 1.0
    "TSP"   -> gramsPerTsp ?: 1.0
    else    -> 1.0
}

/** Resolve a meal's item foodIds against the local food cache and compute totals. */
fun Meal.resolve(foodsById: Map<String, Food>): MealUi {
    val resolved = items.map { item ->
        val food = foodsById[item.foodServerId]
        val grams = food.gramsFor(item.quantity, item.unit)
        val factor = grams / 100.0
        val kcal = ((food?.caloriesPer100 ?: 0.0) * factor).roundToInt()
        MealItemUi(
            name  = food?.name ?: "Unknown food",
            grams = grams,
            kcal  = kcal,
            meta  = "${item.quantity.trimNum()} ${unitLabel(item.unit)} · $kcal kcal",
        )
    }
    var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
    items.forEach { item ->
        val food = foodsById[item.foodServerId] ?: return@forEach
        val factor = food.gramsFor(item.quantity, item.unit) / 100.0
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
