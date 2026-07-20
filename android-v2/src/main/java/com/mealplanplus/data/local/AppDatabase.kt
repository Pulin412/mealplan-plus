package com.mealplanplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.Meal

@Database(
    entities  = [Food::class, Meal::class],
    version   = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
}
