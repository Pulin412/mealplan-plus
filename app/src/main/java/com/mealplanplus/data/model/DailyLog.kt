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
 * Per-slot override for a day's diet
 * Allows changing individual meal slots without affecting entire day
 */
@Entity(
    tableName = "daily_log_slot_overrides",
    primaryKeys = ["userId", "logDate", "slotType"],
    foreignKeys = [
        ForeignKey(
            entity = DailyLog::class,
            parentColumns = ["userId", "date"],
            childColumns = ["userId", "logDate"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["overrideMealId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId", "logDate"), Index("overrideMealId")]
)
data class DailyLogSlotOverride(
    val userId: Long,
    val logDate: String,
    val slotType: String,
    val overrideMealId: Long?,  // null = skip this slot
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
    val plannedDiet: Diet? = null,
    val slotOverrides: List<DailyLogSlotOverride> = emptyList()
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

    fun hasOverrideForSlot(slotType: String): Boolean =
        slotOverrides.any { it.slotType == slotType }

    fun getOverrideForSlot(slotType: String): DailyLogSlotOverride? =
        slotOverrides.find { it.slotType == slotType }
}

/**
 * Logged meal - when user logs an entire meal for a slot
 */
@Entity(
    tableName = "logged_meals",
    foreignKeys = [
        ForeignKey(
            entity = DailyLog::class,
            parentColumns = ["userId", "date"],
            childColumns = ["userId", "logDate"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId", "logDate"), Index("mealId")]
)
data class LoggedMeal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val logDate: String,
    val mealId: Long,
    val slotType: String,
    val quantity: Double = 1.0,  // Multiplier for the meal
    val timestamp: Long? = null,
    val notes: String? = null
)

/**
 * Logged meal with full details
 */
data class LoggedMealWithDetails(
    val loggedMeal: LoggedMeal,
    val meal: Meal,
    val foods: List<MealFoodItemWithDetails>
) {
    val totalCalories: Double
        get() = foods.sumOf { it.calculatedCalories } * loggedMeal.quantity
    val totalProtein: Double
        get() = foods.sumOf { it.calculatedProtein } * loggedMeal.quantity
    val totalCarbs: Double
        get() = foods.sumOf { it.calculatedCarbs } * loggedMeal.quantity
    val totalFat: Double
        get() = foods.sumOf { it.calculatedFat } * loggedMeal.quantity
}

/**
 * Full daily log with meals
 */
data class DailyLogWithMeals(
    val log: DailyLog,
    val meals: List<LoggedMealWithDetails>,
    val plannedDiet: Diet? = null,
    val slotOverrides: List<DailyLogSlotOverride> = emptyList()
) {
    val totalCalories: Double
        get() = meals.sumOf { it.totalCalories }
    val totalProtein: Double
        get() = meals.sumOf { it.totalProtein }
    val totalCarbs: Double
        get() = meals.sumOf { it.totalCarbs }
    val totalFat: Double
        get() = meals.sumOf { it.totalFat }

    fun mealsForSlot(slotType: String): List<LoggedMealWithDetails> =
        meals.filter { it.loggedMeal.slotType == slotType }

    fun hasOverrideForSlot(slotType: String): Boolean =
        slotOverrides.any { it.slotType == slotType }

    fun getOverrideForSlot(slotType: String): DailyLogSlotOverride? =
        slotOverrides.find { it.slotType == slotType }
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
