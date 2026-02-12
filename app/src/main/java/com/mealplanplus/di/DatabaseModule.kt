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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create plans table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS plans (
                    date TEXT PRIMARY KEY NOT NULL,
                    dietId INTEGER,
                    notes TEXT,
                    FOREIGN KEY(dietId) REFERENCES diets(id) ON DELETE SET NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_plans_dietId ON plans(dietId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plans_date ON plans(date)")

            // Create custom_metric_types table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_metric_types (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    minValue REAL,
                    maxValue REAL,
                    isActive INTEGER NOT NULL DEFAULT 1
                )
            """)

            // Create health_metrics table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS health_metrics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    date TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    metricType TEXT,
                    customTypeId INTEGER,
                    value REAL NOT NULL,
                    notes TEXT
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_date ON health_metrics(date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_metricType ON health_metrics(metricType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_customTypeId ON health_metrics(customTypeId)")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add barcode, isFavorite, lastUsed columns to food_items
            db.execSQL("ALTER TABLE food_items ADD COLUMN barcode TEXT")
            db.execSQL("ALTER TABLE food_items ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE food_items ADD COLUMN lastUsed INTEGER")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add isSystemFood column for bundled foods
            db.execSQL("ALTER TABLE food_items ADD COLUMN isSystemFood INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create logged_meals table for logging meals instead of individual foods
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS logged_meals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    logDate TEXT NOT NULL,
                    mealId INTEGER NOT NULL,
                    slotType TEXT NOT NULL,
                    quantity REAL NOT NULL DEFAULT 1.0,
                    timestamp INTEGER,
                    notes TEXT,
                    FOREIGN KEY(logDate) REFERENCES daily_logs(date) ON DELETE CASCADE,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_meals_logDate ON logged_meals(logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_meals_mealId ON logged_meals(mealId)")
        }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add tags column to diets
            db.execSQL("ALTER TABLE diets ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add isCompleted column to plans for tracking finished plans
            db.execSQL("ALTER TABLE plans ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Create new food_items table with per-100g macros
            db.execSQL("""
                CREATE TABLE food_items_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    brand TEXT,
                    barcode TEXT,
                    caloriesPer100 REAL NOT NULL,
                    proteinPer100 REAL NOT NULL,
                    carbsPer100 REAL NOT NULL,
                    fatPer100 REAL NOT NULL,
                    gramsPerPiece REAL,
                    gramsPerCup REAL,
                    gramsPerTbsp REAL,
                    gramsPerTsp REAL,
                    glycemicIndex INTEGER,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    lastUsed INTEGER,
                    createdAt INTEGER NOT NULL,
                    isSystemFood INTEGER NOT NULL DEFAULT 0
                )
            """)

            // 2. Migrate data - convert serving-based to per-100g
            // If servingSize was in grams, normalize to per-100g
            db.execSQL("""
                INSERT INTO food_items_new (
                    id, name, brand, barcode,
                    caloriesPer100, proteinPer100, carbsPer100, fatPer100,
                    gramsPerPiece,
                    glycemicIndex, isFavorite, lastUsed, createdAt, isSystemFood
                )
                SELECT
                    id, name, brand, barcode,
                    CASE WHEN servingSize > 0 THEN (calories / servingSize) * 100 ELSE calories END,
                    CASE WHEN servingSize > 0 THEN (protein / servingSize) * 100 ELSE protein END,
                    CASE WHEN servingSize > 0 THEN (carbs / servingSize) * 100 ELSE carbs END,
                    CASE WHEN servingSize > 0 THEN (fat / servingSize) * 100 ELSE fat END,
                    CASE WHEN servingUnit = 'piece' OR servingUnit = 'pc' THEN servingSize ELSE NULL END,
                    glycemicIndex, isFavorite, lastUsed, createdAt, isSystemFood
                FROM food_items
            """)

            // 3. Drop old table and rename new one
            db.execSQL("DROP TABLE food_items")
            db.execSQL("ALTER TABLE food_items_new RENAME TO food_items")

            // 4. Add unit column to meal_food_items
            db.execSQL("ALTER TABLE meal_food_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'GRAM'")

            // 5. Add unit column to logged_foods
            db.execSQL("ALTER TABLE logged_foods ADD COLUMN unit TEXT NOT NULL DEFAULT 'GRAM'")

            // 6. Create daily_log_slot_overrides table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_log_slot_overrides (
                    logDate TEXT NOT NULL,
                    slotType TEXT NOT NULL,
                    overrideMealId INTEGER,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(logDate, slotType),
                    FOREIGN KEY(logDate) REFERENCES daily_logs(date) ON DELETE CASCADE,
                    FOREIGN KEY(overrideMealId) REFERENCES meals(id) ON DELETE SET NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_log_slot_overrides_logDate ON daily_log_slot_overrides(logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_log_slot_overrides_overrideMealId ON daily_log_slot_overrides(overrideMealId)")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
            // Removed fallbackToDestructiveMigration() - this was destroying user data!
            // If migration fails, app will crash (better than silent data loss)
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

    @Provides
    fun providePlanDao(database: AppDatabase): PlanDao = database.planDao()

    @Provides
    fun provideHealthMetricDao(database: AppDatabase): HealthMetricDao = database.healthMetricDao()
}
