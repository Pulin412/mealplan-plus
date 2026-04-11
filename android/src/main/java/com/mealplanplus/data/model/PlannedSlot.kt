package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A slot assignment for a specific day plan.
 *
 * One row per slot per day per user. This is the source of truth for
 * what is planned for each slot. When loading a day's plan, check
 * [PlannedSlot] first; fall back to [Plan.dietId]'s [DietMeal] rows
 * only for slots that have no [PlannedSlot] entry.
 *
 * - [mealId] = null  → ad-hoc slot (foods added directly via [PlannedSlotFood])
 * - [sourceDietId]   → informational only: which diet this meal came from (no FK)
 */
@Entity(
    tableName = "planned_slots",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("userId", "date"),
        Index(value = ["userId", "date", "slotType"], unique = true),
        Index("mealId")
    ]
)
data class PlannedSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long,             // epoch ms, midnight UTC — mirrors plans(userId, date)
    val slotType: String,       // DefaultMealSlot name or free-form custom string
    val mealId: Long? = null,   // null = ad-hoc foods-only slot
    val sourceDietId: Long? = null,  // informational: which diet contributed this meal
    val instructions: String? = null
)

/**
 * An individual food item added directly to a planned slot.
 *
 * Used for:
 * - Ad-hoc foods on top of a meal ([PlannedSlot.mealId] is set)
 * - Entire slot content when no meal is assigned ([PlannedSlot.mealId] = null)
 */
@Entity(
    tableName = "planned_slot_foods",
    foreignKeys = [
        ForeignKey(
            entity = PlannedSlot::class,
            parentColumns = ["id"],
            childColumns = ["plannedSlotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("plannedSlotId"), Index("foodId")]
)
data class PlannedSlotFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val plannedSlotId: Long,
    val foodId: Long,
    val quantity: Double,
    val unit: FoodUnit = FoodUnit.GRAM,
    val notes: String? = null
)

/**
 * A planned slot fully resolved with its meal's foods and any ad-hoc additions.
 */
data class PlannedSlotWithContent(
    val slot: PlannedSlot,
    val meal: MealWithFoods?,               // null when mealId is null
    val adHocFoods: List<PlannedSlotFoodWithDetails> = emptyList()
) {
    val allFoodItems: List<MealFoodItemWithDetails>
        get() = (meal?.items ?: emptyList())

    val totalCalories: Double
        get() = allFoodItems.sumOf { it.calculatedCalories } +
                adHocFoods.sumOf { it.calculatedCalories }

    val totalProtein: Double
        get() = allFoodItems.sumOf { it.calculatedProtein } +
                adHocFoods.sumOf { it.calculatedProtein }

    val totalCarbs: Double
        get() = allFoodItems.sumOf { it.calculatedCarbs } +
                adHocFoods.sumOf { it.calculatedCarbs }

    val totalFat: Double
        get() = allFoodItems.sumOf { it.calculatedFat } +
                adHocFoods.sumOf { it.calculatedFat }
}

/**
 * PlannedSlotFood with the resolved FoodItem details.
 */
data class PlannedSlotFoodWithDetails(
    val plannedSlotFood: PlannedSlotFood,
    val food: FoodItem
) {
    val quantityInGrams: Double
        get() = food.toGrams(plannedSlotFood.quantity, plannedSlotFood.unit)

    val calculatedCalories: Double
        get() = food.calculateCalories(plannedSlotFood.quantity, plannedSlotFood.unit)

    val calculatedProtein: Double
        get() = food.calculateProtein(plannedSlotFood.quantity, plannedSlotFood.unit)

    val calculatedCarbs: Double
        get() = food.calculateCarbs(plannedSlotFood.quantity, plannedSlotFood.unit)

    val calculatedFat: Double
        get() = food.calculateFat(plannedSlotFood.quantity, plannedSlotFood.unit)
}
