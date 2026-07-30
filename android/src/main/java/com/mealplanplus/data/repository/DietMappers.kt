package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.model.DietDto
import com.mealplanplus.data.generated.model.DietFoodItemDto
import com.mealplanplus.data.generated.model.DietMealDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietEntry
import com.mealplanplus.data.model.DietEntryKind
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.model.Food
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

// ── DTO ↔ entity ─────────────────────────────────────────────────────────────
fun DietDto.toEntity(dirty: Boolean = false): Diet {
    val mealEntries = (meals ?: emptyList()).mapNotNull { m ->
        m.mealServerId?.let { DietEntry(DietEntryKind.MEAL, it.toString(), m.slot, 1.0) }
    }
    val foodEntries = (foodItems ?: emptyList()).mapNotNull { f ->
        f.foodServerId?.let { DietEntry(DietEntryKind.FOOD, it.toString(), f.slot, f.quantity ?: 1.0, f.unit.value) }
    }
    return Diet(
        id             = (serverId ?: UUID.randomUUID()).toString(),
        name           = name,
        entries        = mealEntries + foodEntries,
        tags           = (tags ?: emptyList()).map { DietTag(it.id, it.name, it.color) },
        targetCalories = targetCalories,
        isFavorite     = isFavorite ?: false,
        updatedAt      = updatedAt?.toEpochMilli() ?: System.currentTimeMillis(),
        dirty          = dirty,
        deletedAt      = null,
    )
}

fun Diet.toDto(): DietDto = DietDto(
    name           = name,
    serverId       = UUID.fromString(id),
    targetCalories = targetCalories,
    isFavorite     = isFavorite,
    tagIds         = tags.map { it.id },
    updatedAt      = Instant.ofEpochMilli(updatedAt),
    meals = entries.filter { it.kind == DietEntryKind.MEAL }.map { e ->
        DietMealDto(mealId = 0L, dayOfWeek = 0, slot = e.slot, mealServerId = UUID.fromString(e.refServerId))
    },
    foodItems = entries.filter { it.kind == DietEntryKind.FOOD }.map { e ->
        DietFoodItemDto(
            foodId = 0L, slot = e.slot, quantity = e.quantity,
            unit = FoodUnit.decode(e.unit) ?: FoodUnit.GRAM,
            foodServerId = UUID.fromString(e.refServerId),
        )
    },
)

// ── Resolved display model (diet + its meals'/foods' names & macros) ──────────
data class DietUi(
    val diet: Diet,
    val slots: List<DietSlotUi>,
    val totalKcal: Int,
    val totalProtein: Double,
    val totalCarbs: Double,
    val totalFat: Double,
    val entryCount: Int,
    val summary: String,
)

data class DietSlotUi(val slot: String, val entries: List<DietEntryUi>, val kcal: Int)

data class DietEntryUi(
    val kind: DietEntryKind,
    val name: String,
    val kcal: Int,
    val meta: String,
    val mealFoods: List<MealFoodLine> = emptyList(),   // foods inside a MEAL entry — shown when expanded
)

data class MealFoodLine(val name: String, val meta: String)

private class Macros { var kcal = 0.0; var p = 0.0; var c = 0.0; var f = 0.0 }

/** Resolve a diet's entries against the meal + food caches, grouped by slot. */
fun Diet.resolve(mealsById: Map<String, MealUi>, foodsById: Map<String, Food>): DietUi {
    val total = Macros()
    val uiByEntry = entries.map { e ->
        val m = Macros()
        val ui = when (e.kind) {
            DietEntryKind.MEAL -> {
                val meal = mealsById[e.refServerId]
                val q = e.quantity.takeIf { it > 0 } ?: 1.0
                m.kcal = (meal?.totalKcal ?: 0) * q
                m.p = (meal?.totalProtein ?: 0.0) * q
                m.c = (meal?.totalCarbs ?: 0.0) * q
                m.f = (meal?.totalFat ?: 0.0) * q
                DietEntryUi(e.kind, meal?.meal?.name ?: "Unknown meal", m.kcal.roundToInt(),
                    "meal · ${m.kcal.roundToInt()} kcal",
                    mealFoods = meal?.items?.map { MealFoodLine(it.name, it.meta) } ?: emptyList())
            }
            DietEntryKind.FOOD -> {
                val food = foodsById[e.refServerId]
                val grams = food.gramsFor(e.quantity, e.unit)
                val factor = grams / 100.0
                m.kcal = (food?.caloriesPer100 ?: 0.0) * factor
                m.p = (food?.proteinPer100 ?: 0.0) * factor
                m.c = (food?.carbsPer100 ?: 0.0) * factor
                m.f = (food?.fatPer100 ?: 0.0) * factor
                DietEntryUi(e.kind, food?.name ?: "Unknown food", m.kcal.roundToInt(),
                    "${e.quantity.trim1()} ${unitLabel(e.unit)} · ${m.kcal.roundToInt()} kcal")
            }
        }
        total.kcal += m.kcal; total.p += m.p; total.c += m.c; total.f += m.f
        e.slot to ui
    }

    val slots = DIET_SLOT_ORDER(entries.map { it.slot }).map { slot ->
        val es = uiByEntry.filter { it.first == slot }.map { it.second }
        DietSlotUi(slot, es, es.sumOf { it.kcal })
    }
    val names = uiByEntry.map { it.second.name }
    val summary = when {
        entries.isEmpty() -> "No items"
        else -> "${entries.size} item${if (entries.size == 1) "" else "s"} · ${names.joinToString(", ")}"
    }
    return DietUi(this, slots, total.kcal.roundToInt(), total.p, total.c, total.f, entries.size, summary)
}

/** Canonical slot order for the slots present, unknowns appended in first-seen order. */
private val CANONICAL_SLOTS = listOf(
    "Early Breakfast", "Breakfast", "Noon", "Pre-Lunch", "Post-Lunch", "Evening",
    "Pre-workout", "Post-workout", "Pre-dinner", "Dinner", "Post Dinner",
)

private fun DIET_SLOT_ORDER(present: List<String>): List<String> {
    val distinct = present.distinct()
    val known = CANONICAL_SLOTS.filter { it in distinct }
    val unknown = distinct.filter { it !in CANONICAL_SLOTS }
    return known + unknown
}

private fun Double.trim1(): String =
    if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
