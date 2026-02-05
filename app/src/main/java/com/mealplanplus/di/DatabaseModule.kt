package com.mealplanplus.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mealplanplus.data.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create custom_meal_slots
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_meal_slots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    `order` INTEGER NOT NULL DEFAULT 99
                )
            """)

            // Create meals
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    slotType TEXT NOT NULL,
                    customSlotId INTEGER,
                    createdAt INTEGER NOT NULL
                )
            """)

            // Create meal_food_items
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS meal_food_items (
                    mealId INTEGER NOT NULL,
                    foodId INTEGER NOT NULL,
                    quantity REAL NOT NULL,
                    notes TEXT,
                    PRIMARY KEY(mealId, foodId),
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE CASCADE,
                    FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_food_items_mealId ON meal_food_items(mealId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meal_food_items_foodId ON meal_food_items(foodId)")

            // Create diets
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS diets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    createdAt INTEGER NOT NULL
                )
            """)

            // Create diet_meals
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS diet_meals (
                    dietId INTEGER NOT NULL,
                    slotType TEXT NOT NULL,
                    mealId INTEGER,
                    PRIMARY KEY(dietId, slotType),
                    FOREIGN KEY(dietId) REFERENCES diets(id) ON DELETE CASCADE,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE SET NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diet_meals_dietId ON diet_meals(dietId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diet_meals_mealId ON diet_meals(mealId)")

            // Create daily_logs
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_logs (
                    date TEXT PRIMARY KEY NOT NULL,
                    plannedDietId INTEGER,
                    notes TEXT,
                    createdAt INTEGER NOT NULL
                )
            """)

            // Create logged_foods
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS logged_foods (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    logDate TEXT NOT NULL,
                    foodId INTEGER NOT NULL,
                    quantity REAL NOT NULL,
                    slotType TEXT NOT NULL,
                    timestamp INTEGER,
                    notes TEXT,
                    FOREIGN KEY(logDate) REFERENCES daily_logs(date) ON DELETE CASCADE,
                    FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_foods_logDate ON logged_foods(logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_foods_foodId ON logged_foods(foodId)")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "mealplan_database"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideFoodDao(database: AppDatabase): FoodDao = database.foodDao()

    @Provides
    fun provideMealDao(database: AppDatabase): MealDao = database.mealDao()

    @Provides
    fun provideDietDao(database: AppDatabase): DietDao = database.dietDao()

    @Provides
    fun provideDailyLogDao(database: AppDatabase): DailyLogDao = database.dailyLogDao()
}
