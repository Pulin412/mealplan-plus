package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "food_items")
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val servingSize: Double,
    val servingUnit: String,  // g, ml, piece, cup, etc.
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val glycemicIndex: Int? = null,  // Optional GI value (0-100)
    val createdAt: Long = System.currentTimeMillis()
)
