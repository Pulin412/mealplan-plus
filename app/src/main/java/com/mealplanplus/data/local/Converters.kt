package com.mealplanplus.data.local

import androidx.room.TypeConverter
import com.mealplanplus.data.model.FoodUnit

class Converters {
    @TypeConverter
    fun fromFoodUnit(value: FoodUnit): String = value.name

    @TypeConverter
    fun toFoodUnit(value: String): FoodUnit = FoodUnit.valueOf(value)
}
