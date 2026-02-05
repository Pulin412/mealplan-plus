package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A meal template - collection of food items for a specific slot
 */
@Entity(tableName = "meals")
data class Meal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val slotType: String,  // DefaultMealSlot name or "CUSTOM"
    val customSlotId: Long? = null,  // If slotType is CUSTOM
    val createdAt: Long = System.currentTimeMillis()
) {
    val isDefaultSlot: Boolean
        get() = slotType != "CUSTOM"

    val defaultSlot: DefaultMealSlot?
        get() = if (isDefaultSlot) DefaultMealSlot.valueOf(slotType) else null
}

/**
 * Junction table: Meal contains multiple food items with quantities
 */
@Entity(
    tableName = "meal_food_items",
    primaryKeys = ["mealId", "foodId"],
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId"), Index("foodId")]
)
data class MealFoodItem(
    val mealId: Long,
    val foodId: Long,
    val quantity: Double,  // Number of servings
    val notes: String? = null
)

/**
 * Meal with its food items - for display
 */
data class MealWithFoods(
    val meal: Meal,
    val items: List<MealFoodItemWithDetails>
) {
    val totalCalories: Double
        get() = items.sumOf { it.calculatedCalories }
    val totalProtein: Double
        get() = items.sumOf { it.calculatedProtein }
    val totalCarbs: Double
        get() = items.sumOf { it.calculatedCarbs }
    val totalFat: Double
        get() = items.sumOf { it.calculatedFat }
}

/**
 * Food item in meal with calculated macros
 */
data class MealFoodItemWithDetails(
    val mealFoodItem: MealFoodItem,
    val food: FoodItem
) {
    val calculatedCalories: Double
        get() = food.calories * mealFoodItem.quantity
    val calculatedProtein: Double
        get() = food.protein * mealFoodItem.quantity
    val calculatedCarbs: Double
        get() = food.carbs * mealFoodItem.quantity
    val calculatedFat: Double
        get() = food.fat * mealFoodItem.quantity
}
