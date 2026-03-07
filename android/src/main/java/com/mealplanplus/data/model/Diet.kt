package com.mealplanplus.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A diet template - combines meals for a full day
 */
@Entity(
    tableName = "diets",
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
data class Diet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val isSystemDiet: Boolean = false,
    // Sync columns (v19)
    val serverId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
)

/**
 * Junction table: Diet contains meals for each slot
 */
@Entity(
    tableName = "diet_meals",
    primaryKeys = ["dietId", "slotType"],
    foreignKeys = [
        ForeignKey(
            entity = Diet::class,
            parentColumns = ["id"],
            childColumns = ["dietId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dietId"), Index("mealId")]
)
data class DietMeal(
    val dietId: Long,
    val slotType: String,  // DefaultMealSlot name
    val mealId: Long?,  // Can be null if no meal assigned
    val instructions: String? = null
)

/**
 * Diet with all its meals - for display
 */
data class DietWithMeals(
    val diet: Diet,
    val meals: Map<String, MealWithFoods?>,  // slotType -> meal
    val instructions: Map<String, String?> = emptyMap()  // slotType -> prep instructions
) {
    val totalCalories: Double
        get() = meals.values.filterNotNull().sumOf { it.totalCalories }
    val totalProtein: Double
        get() = meals.values.filterNotNull().sumOf { it.totalProtein }
    val totalCarbs: Double
        get() = meals.values.filterNotNull().sumOf { it.totalCarbs }
    val totalFat: Double
        get() = meals.values.filterNotNull().sumOf { it.totalFat }
}

/**
 * Diet summary for list display (single JOIN query result)
 */
data class DietSummary(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val mealCount: Int,
    val totalCalories: Int
) {
    fun toDiet() = Diet(id, userId, name, description, createdAt, isSystemDiet = false)
}

/**
 * Diet full summary with all macros (single JOIN query result)
 */
data class DietFullSummary(
    val id: Long,
    val userId: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val mealCount: Int,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int
) {
    fun toDiet() = Diet(id, userId, name, description, createdAt, isSystemDiet = false)
}
