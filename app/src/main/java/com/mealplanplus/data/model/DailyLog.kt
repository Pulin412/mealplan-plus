package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily food log - what user actually ate
 */
@Entity(tableName = "daily_logs")
data class DailyLog(
    @PrimaryKey
    val date: String,  // Format: yyyy-MM-dd
    val plannedDietId: Long? = null,  // Optional reference to planned diet
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Individual logged food item
 */
@Entity(
    tableName = "logged_foods",
    foreignKeys = [
        ForeignKey(
            entity = DailyLog::class,
            parentColumns = ["date"],
            childColumns = ["logDate"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("logDate"), Index("foodId")]
)
data class LoggedFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val logDate: String,  // References DailyLog.date
    val foodId: Long,
    val quantity: Double,
    val slotType: String,  // Which meal slot
    val timestamp: Long? = null,  // Optional exact time
    val notes: String? = null
)

/**
 * Logged food with food details
 */
data class LoggedFoodWithDetails(
    val loggedFood: LoggedFood,
    val food: FoodItem
) {
    val calculatedCalories: Double
        get() = food.calories * loggedFood.quantity
    val calculatedProtein: Double
        get() = food.protein * loggedFood.quantity
    val calculatedCarbs: Double
        get() = food.carbs * loggedFood.quantity
    val calculatedFat: Double
        get() = food.fat * loggedFood.quantity
}

/**
 * Full daily log with all foods
 */
data class DailyLogWithFoods(
    val log: DailyLog,
    val foods: List<LoggedFoodWithDetails>,
    val plannedDiet: Diet? = null
) {
    val totalCalories: Double
        get() = foods.sumOf { it.calculatedCalories }
    val totalProtein: Double
        get() = foods.sumOf { it.calculatedProtein }
    val totalCarbs: Double
        get() = foods.sumOf { it.calculatedCarbs }
    val totalFat: Double
        get() = foods.sumOf { it.calculatedFat }

    fun foodsForSlot(slotType: String): List<LoggedFoodWithDetails> =
        foods.filter { it.loggedFood.slotType == slotType }
}
