package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Diet type tags
 */
enum class DietTag(val displayName: String) {
    REMISSION("Remission"),
    MAINTENANCE("Maintenance"),
    SOS("SOS"),
    CUSTOM("Custom")
}

/**
 * A diet template - combines meals for a full day
 */
@Entity(tableName = "diets")
data class Diet(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val tags: String = "",  // Comma-separated tags: "REMISSION,SOS"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getTagList(): List<DietTag> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").mapNotNull { tag ->
            try { DietTag.valueOf(tag.trim()) } catch (e: Exception) { null }
        }
    }

    fun hasTag(tag: DietTag): Boolean = getTagList().contains(tag)
}

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
    val mealId: Long?  // Can be null if no meal assigned
)

/**
 * Diet with all its meals - for display
 */
data class DietWithMeals(
    val diet: Diet,
    val meals: Map<String, MealWithFoods?>  // slotType -> meal
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
    val name: String,
    val description: String?,
    val tags: String,
    val createdAt: Long,
    val mealCount: Int,
    val totalCalories: Int
) {
    fun getTagList(): List<DietTag> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").mapNotNull { tag ->
            try { DietTag.valueOf(tag.trim()) } catch (e: Exception) { null }
        }
    }

    fun toDiet() = Diet(id, name, description, tags, createdAt)
}

/**
 * Diet full summary with all macros (single JOIN query result)
 */
data class DietFullSummary(
    val id: Long,
    val name: String,
    val description: String?,
    val tags: String,
    val createdAt: Long,
    val mealCount: Int,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int
) {
    fun getTagList(): List<DietTag> {
        if (tags.isBlank()) return emptyList()
        return tags.split(",").mapNotNull { tag ->
            try { DietTag.valueOf(tag.trim()) } catch (e: Exception) { null }
        }
    }

    fun toDiet() = Diet(id, name, description, tags, createdAt)
}
