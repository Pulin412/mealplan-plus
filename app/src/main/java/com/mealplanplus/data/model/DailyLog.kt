package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Daily food log - what user actually ate
 */
@Entity(
    tableName = "daily_logs",
    primaryKeys = ["userId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class DailyLog(
    val userId: Long,
    val date: String,  // Format: yyyy-MM-dd
    val plannedDietId: Long? = null,  // Default diet for the day
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
            parentColumns = ["userId", "date"],
            childColumns = ["userId", "logDate"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId", "logDate"), Index("foodId")]
)
data class LoggedFood(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val logDate: String,  // References DailyLog.date
    val foodId: Long,
    val quantity: Double,
    val unit: FoodUnit = FoodUnit.GRAM,  // Unit of measurement
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
    val quantityInGrams: Double
        get() = food.toGrams(loggedFood.quantity, loggedFood.unit)

    val calculatedCalories: Double
        get() = food.calculateCalories(loggedFood.quantity, loggedFood.unit)

    val calculatedProtein: Double
        get() = food.calculateProtein(loggedFood.quantity, loggedFood.unit)

    val calculatedCarbs: Double
        get() = food.calculateCarbs(loggedFood.quantity, loggedFood.unit)

    val calculatedFat: Double
        get() = food.calculateFat(loggedFood.quantity, loggedFood.unit)
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

/**
 * Daily macro summary for charts
 */
data class DailyMacroSummary(
    val date: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
