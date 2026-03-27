package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    // Macros per 100g/100ml
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    // Unit conversion helpers (optional - for piece/cup/tbsp conversions)
    val gramsPerPiece: Double? = null,   // e.g., egg = 50g, toast = 30g
    val gramsPerCup: Double? = null,     // e.g., rice = 185g, milk = 240g
    val gramsPerTbsp: Double? = null,    // e.g., oil = 14g, honey = 21g
    val gramsPerTsp: Double? = null,     // e.g., sugar = 4g
    val glycemicIndex: Int? = null,      // Optional GI value (0-100)
    val preferredUnit: String? = null,
    val isFavorite: Boolean = false,
    val lastUsed: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isSystemFood: Boolean = false,   // Bundled foods from common_foods.json
    // Sync columns (v19)
    val serverId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncedAt: Long? = null
) {
    /**
     * Convert quantity in given unit to grams
     */
    fun toGrams(quantity: Double, unit: FoodUnit): Double {
        return when (unit) {
            FoodUnit.GRAM -> quantity
            FoodUnit.ML -> quantity  // 1:1 for water-like liquids
            FoodUnit.SERVING -> quantity * 100.0  // 1 serving = 100g (servingSize is always 100g)
            FoodUnit.PIECE -> quantity * (gramsPerPiece ?: 100.0)
            FoodUnit.SLICE -> quantity * (gramsPerPiece ?: 30.0)
            FoodUnit.SCOOP -> quantity * (gramsPerPiece ?: 30.0)
            FoodUnit.CUP -> quantity * (gramsPerCup ?: 240.0)
            FoodUnit.TBSP -> quantity * (gramsPerTbsp ?: 15.0)
            FoodUnit.TSP -> quantity * (gramsPerTsp ?: 5.0)
        }
    }

    fun calculateCalories(quantity: Double, unit: FoodUnit): Double =
        (toGrams(quantity, unit) / 100.0) * caloriesPer100

    fun calculateProtein(quantity: Double, unit: FoodUnit): Double =
        (toGrams(quantity, unit) / 100.0) * proteinPer100

    fun calculateCarbs(quantity: Double, unit: FoodUnit): Double =
        (toGrams(quantity, unit) / 100.0) * carbsPer100

    fun calculateFat(quantity: Double, unit: FoodUnit): Double =
        (toGrams(quantity, unit) / 100.0) * fatPer100

    // Backward compatibility properties (per 100g values for UI display)
    val calories: Double get() = caloriesPer100
    val protein: Double get() = proteinPer100
    val carbs: Double get() = carbsPer100
    val fat: Double get() = fatPer100
    val servingSize: Double get() = 100.0
    val servingUnit: String get() = "g"
}
