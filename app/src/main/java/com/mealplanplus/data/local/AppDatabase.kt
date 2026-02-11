package com.mealplanplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mealplanplus.data.model.*

@Database(
    entities = [
        FoodItem::class,
        CustomMealSlot::class,
        Meal::class,
        MealFoodItem::class,
        Diet::class,
        DietMeal::class,
        DailyLog::class,
        DailyLogSlotOverride::class,
        LoggedFood::class,
        LoggedMeal::class,
        Plan::class,
        HealthMetric::class,
        CustomMetricType::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun dietDao(): DietDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun planDao(): PlanDao
    abstract fun healthMetricDao(): HealthMetricDao
}
