package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "foods")
data class Food(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverId: String? = null,
    val name: String,
    val brand: String? = null,
    val servingLabel: String? = null,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null,
    val isFavorite: Boolean = false,
    val isSystemFood: Boolean = false,
    val verified: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
