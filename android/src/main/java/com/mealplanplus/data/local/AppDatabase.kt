package com.mealplanplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mealplanplus.data.local.dao.CachedResponseDao
import com.mealplanplus.data.local.dao.DietDao
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.model.CachedResponse
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Food
import com.mealplanplus.data.model.Meal

@Database(
    entities  = [Food::class, Meal::class, Diet::class, CachedResponse::class],
    version   = 11,  // v11: meals.notes + diets.description — notes UI via MIGRATION_10_11 (additive).
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun dietDao(): DietDao
    abstract fun cachedResponseDao(): CachedResponseDao
}
