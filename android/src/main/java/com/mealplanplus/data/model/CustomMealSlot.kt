package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_meal_slots")
data class CustomMealSlot(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val date: Long,  // Epoch ms — per-date slots
    val name: String,
    val slotOrder: Int = 99
)
