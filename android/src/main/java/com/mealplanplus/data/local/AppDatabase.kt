package com.mealplanplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mealplanplus.data.model.*

@Database(
    entities = [
        FoodItem::class,
        Meal::class,
        MealFoodItem::class,
        Diet::class,
        DietMeal::class,
        DailyLog::class,
        LoggedFood::class,
        Plan::class,
        HealthMetric::class,
        CustomMetricType::class,
        User::class,
        Tag::class,
        DietTagCrossRef::class,
        FoodTagCrossRef::class,
        GroceryList::class,
        GroceryItem::class,
        CustomMealSlot::class
    ],
    version = 22,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealDao(): MealDao
    abstract fun dietDao(): DietDao
    abstract fun dailyLogDao(): DailyLogDao
    abstract fun planDao(): PlanDao
    abstract fun healthMetricDao(): HealthMetricDao
    abstract fun userDao(): UserDao
    abstract fun tagDao(): TagDao
    abstract fun groceryDao(): GroceryDao
    abstract fun customMealSlotDao(): CustomMealSlotDao
}
