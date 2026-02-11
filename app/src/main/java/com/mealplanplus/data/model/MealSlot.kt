package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Predefined meal slots - matches seed_data.json slots
 */
enum class DefaultMealSlot(val displayName: String, val order: Int) {
    EARLY_MORNING("Early Morning", 0),
    BREAKFAST("Breakfast", 1),
    NOON("Noon", 2),
    MID_MORNING("Mid Morning", 3),
    LUNCH("Lunch", 4),
    PRE_WORKOUT("Pre-Workout", 5),
    EVENING("Evening", 6),
    EVENING_SNACK("Evening Snack", 7),
    POST_WORKOUT("Post-Workout", 8),
    DINNER("Dinner", 9),
    POST_DINNER("Post Dinner", 10);

    companion object {
        fun fromString(value: String): DefaultMealSlot? =
            entries.find { it.name == value }
    }
}

/**
 * Custom meal slots created by user
 */
@Entity(tableName = "custom_meal_slots")
data class CustomMealSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val order: Int = 99  // Custom slots appear after defaults
)

/**
 * Unified slot reference - can be default or custom
 */
data class MealSlotRef(
    val isDefault: Boolean,
    val defaultSlot: DefaultMealSlot? = null,
    val customSlotId: Long? = null
) {
    val displayName: String
        get() = if (isDefault) defaultSlot?.displayName ?: "" else "Custom"

    companion object {
        fun fromDefault(slot: DefaultMealSlot) = MealSlotRef(true, slot, null)
        fun fromCustom(slotId: Long) = MealSlotRef(false, null, slotId)
    }
}
