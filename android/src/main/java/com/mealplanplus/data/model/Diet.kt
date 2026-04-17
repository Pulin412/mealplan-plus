package com.mealplanplus.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A diet template — a reusable named collection of meals assigned to slots.
 *
 * [userId] scopes the diet to a specific user.
 * NULL means a system/built-in diet visible to every user.
 * User-created diets always have a non-null userId.
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
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0")
    val isSystem: Boolean = false,
    /** NULL = system diet visible to all; non-null = owned by this user. */
    @ColumnInfo(defaultValue = "NULL")
    val userId: Long? = null,
    val serverId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val isFavourite: Boolean = false
)

/**
 * Junction table: a diet maps slot types to meals.
 *
 * Table renamed from diet_meals → diet_slots (v27) to clarify intent.
 */
@Entity(
    tableName = "diet_slots",
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
    val slotType: String,   // DefaultMealSlot name or free-form custom string
    val mealId: Long?,      // null = slot exists in template but no meal assigned yet
    val instructions: String? = null
)

/**
 * Diet with all its meals — for display.
 */
data class DietWithMeals(
    val diet: Diet,
    val meals: Map<String, MealWithFoods?>,       // slotType → meal
    val instructions: Map<String, String?> = emptyMap()  // slotType → prep instructions
) {
    val totalCalories: Double
        get() = meals.values.filterNotNull().sumOf { it.totalCalories }
    val totalProtein: Double
        get() = meals.values.filterNotNull().sumOf { it.totalProtein }
    val totalCarbs: Double
        get() = meals.values.filterNotNull().sumOf { it.totalCarbs }
    val totalFat: Double
        get() = meals.values.filterNotNull().sumOf { it.totalFat }

    /**
     * Total Glycemic Load for all meals in this diet.
     * Returns null when no food in any meal has GI data.
     */
    val totalGlycemicLoad: Double?
        get() {
            val loads = meals.values.filterNotNull().mapNotNull { it.totalGlycemicLoad }
            return if (loads.isEmpty()) null else loads.sum()
        }
}

/**
 * Diet full summary with all macros (single JOIN query result).
 */
data class DietFullSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val mealCount: Int,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val totalGlycemicLoad: Double? = null,
    val isFavourite: Boolean = false
) {
    fun toDiet() = Diet(id = id, name = name, description = description, createdAt = createdAt, isFavourite = isFavourite)
}
