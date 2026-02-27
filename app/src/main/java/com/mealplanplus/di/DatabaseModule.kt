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

    private val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create users table for local authentication
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    email TEXT NOT NULL,
                    passwordHash TEXT NOT NULL,
                    displayName TEXT,
                    photoUrl TEXT,
                    age INTEGER,
                    contact TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_email ON users(email)")
        }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()

            // 1. Create tags table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS tags (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_userId_name ON tags(userId, name)")

            // 2. Create diet_tags junction table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS diet_tags (
                    dietId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(dietId, tagId),
                    FOREIGN KEY(dietId) REFERENCES diets(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diet_tags_dietId ON diet_tags(dietId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diet_tags_tagId ON diet_tags(tagId)")

            // 3. Recreate diets with userId (duplicate for all users)
            db.execSQL("""
                CREATE TABLE diets_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diets_new_userId ON diets_new(userId)")

            // Insert diets duplicated for each user
            db.execSQL("""
                INSERT INTO diets_new (userId, name, description, createdAt)
                SELECT u.id, d.name, d.description, d.createdAt
                FROM diets d, users u
            """)

            // Create tags from old diets.tags column for each user
            // Parse comma-separated tags and insert unique ones
            db.execSQL("""
                INSERT OR IGNORE INTO tags (userId, name, createdAt)
                SELECT DISTINCT u.id, trim(tag), $currentTime
                FROM diets d, users u,
                     (WITH RECURSIVE split(dietId, tag, rest) AS (
                         SELECT id, '', tags || ',' FROM diets WHERE tags != ''
                         UNION ALL
                         SELECT dietId, substr(rest, 0, instr(rest, ',')), substr(rest, instr(rest, ',')+1)
                         FROM split WHERE rest != ''
                     )
                     SELECT dietId, tag FROM split WHERE tag != '')
                WHERE d.id = dietId
            """)

            // Link diets to tags in diet_tags junction
            // This is complex - we need to match old diet tags to new diet IDs per user
            // For simplicity, we'll handle this in app code on first run

            // 4. Recreate meals with userId
            db.execSQL("""
                CREATE TABLE meals_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    description TEXT,
                    slotType TEXT NOT NULL,
                    customSlotId INTEGER,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meals_new_userId ON meals_new(userId)")

            db.execSQL("""
                INSERT INTO meals_new (userId, name, description, slotType, customSlotId, createdAt)
                SELECT u.id, m.name, m.description, m.slotType, m.customSlotId, m.createdAt
                FROM meals m, users u
            """)

            // 5. Recreate custom_meal_slots with userId
            db.execSQL("""
                CREATE TABLE custom_meal_slots_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    `order` INTEGER NOT NULL DEFAULT 99,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_meal_slots_new_userId ON custom_meal_slots_new(userId)")

            db.execSQL("""
                INSERT INTO custom_meal_slots_new (userId, name, `order`)
                SELECT u.id, c.name, c.`order`
                FROM custom_meal_slots c, users u
            """)

            // 6. Recreate daily_logs with userId and composite PK
            db.execSQL("""
                CREATE TABLE daily_logs_new (
                    userId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    plannedDietId INTEGER,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, date),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_logs_new_userId ON daily_logs_new(userId)")

            db.execSQL("""
                INSERT INTO daily_logs_new (userId, date, plannedDietId, notes, createdAt)
                SELECT u.id, dl.date, dl.plannedDietId, dl.notes, dl.createdAt
                FROM daily_logs dl, users u
            """)

            // 7. Copy data from old child tables before dropping
            // Store in temp tables to migrate after parent renames
            db.execSQL("""
                CREATE TABLE temp_daily_log_slot_overrides AS
                SELECT u.id as userId, dlo.logDate, dlo.slotType, dlo.overrideMealId, dlo.notes, dlo.createdAt
                FROM daily_log_slot_overrides dlo, users u
            """)

            db.execSQL("""
                CREATE TABLE temp_logged_foods AS
                SELECT u.id as userId, lf.logDate, lf.foodId, lf.quantity, lf.unit, lf.slotType, lf.timestamp, lf.notes
                FROM logged_foods lf, users u
            """)

            db.execSQL("""
                CREATE TABLE temp_logged_meals AS
                SELECT u.id as userId, lm.logDate, lm.mealId, lm.slotType, lm.quantity, lm.timestamp, lm.notes
                FROM logged_meals lm, users u
            """)

            db.execSQL("""
                CREATE TABLE temp_plans AS
                SELECT u.id as userId, p.date, p.dietId, p.notes, p.isCompleted
                FROM plans p, users u
            """)

            // 8. Recreate health_metrics with userId
            db.execSQL("""
                CREATE TABLE health_metrics_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    metricType TEXT,
                    customTypeId INTEGER,
                    value REAL NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_new_userId ON health_metrics_new(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_new_date ON health_metrics_new(date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_new_metricType ON health_metrics_new(metricType)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_health_metrics_new_customTypeId ON health_metrics_new(customTypeId)")

            db.execSQL("""
                INSERT INTO health_metrics_new (userId, date, timestamp, metricType, customTypeId, value, notes)
                SELECT u.id, hm.date, hm.timestamp, hm.metricType, hm.customTypeId, hm.value, hm.notes
                FROM health_metrics hm, users u
            """)

            // 9. Recreate custom_metric_types with userId
            db.execSQL("""
                CREATE TABLE custom_metric_types_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    unit TEXT NOT NULL,
                    minValue REAL,
                    maxValue REAL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_custom_metric_types_new_userId ON custom_metric_types_new(userId)")

            db.execSQL("""
                INSERT INTO custom_metric_types_new (userId, name, unit, minValue, maxValue, isActive)
                SELECT u.id, cmt.name, cmt.unit, cmt.minValue, cmt.maxValue, cmt.isActive
                FROM custom_metric_types cmt, users u
            """)

            // 10. Drop old tables and rename parent tables FIRST
            // (Child tables need correct FK references to final table names)
            db.execSQL("DROP TABLE IF EXISTS diet_meals")
            db.execSQL("DROP TABLE IF EXISTS logged_meals")
            db.execSQL("DROP TABLE IF EXISTS logged_foods")
            db.execSQL("DROP TABLE IF EXISTS daily_log_slot_overrides")
            db.execSQL("DROP TABLE IF EXISTS daily_logs")
            db.execSQL("DROP TABLE IF EXISTS plans")
            db.execSQL("DROP TABLE IF EXISTS health_metrics")
            db.execSQL("DROP TABLE IF EXISTS custom_metric_types")
            db.execSQL("DROP TABLE IF EXISTS meals")
            db.execSQL("DROP TABLE IF EXISTS diets")
            db.execSQL("DROP TABLE IF EXISTS custom_meal_slots")

            // Rename parent tables first
            db.execSQL("ALTER TABLE diets_new RENAME TO diets")
            db.execSQL("ALTER TABLE meals_new RENAME TO meals")
            db.execSQL("ALTER TABLE custom_meal_slots_new RENAME TO custom_meal_slots")
            db.execSQL("ALTER TABLE daily_logs_new RENAME TO daily_logs")
            db.execSQL("ALTER TABLE health_metrics_new RENAME TO health_metrics")
            db.execSQL("ALTER TABLE custom_metric_types_new RENAME TO custom_metric_types")

            // 11. NOW create child tables with FKs pointing to final table names
            db.execSQL("""
                CREATE TABLE daily_log_slot_overrides (
                    userId INTEGER NOT NULL,
                    logDate TEXT NOT NULL,
                    slotType TEXT NOT NULL,
                    overrideMealId INTEGER,
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(userId, logDate, slotType),
                    FOREIGN KEY(userId, logDate) REFERENCES daily_logs(userId, date) ON DELETE CASCADE,
                    FOREIGN KEY(overrideMealId) REFERENCES meals(id) ON DELETE SET NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_log_slot_overrides_userId_logDate ON daily_log_slot_overrides(userId, logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_log_slot_overrides_overrideMealId ON daily_log_slot_overrides(overrideMealId)")

            db.execSQL("""
                INSERT INTO daily_log_slot_overrides (userId, logDate, slotType, overrideMealId, notes, createdAt)
                SELECT userId, logDate, slotType, overrideMealId, notes, createdAt FROM temp_daily_log_slot_overrides
            """)

            db.execSQL("""
                CREATE TABLE logged_foods (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    logDate TEXT NOT NULL,
                    foodId INTEGER NOT NULL,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL DEFAULT 'GRAM',
                    slotType TEXT NOT NULL,
                    timestamp INTEGER,
                    notes TEXT,
                    FOREIGN KEY(userId, logDate) REFERENCES daily_logs(userId, date) ON DELETE CASCADE,
                    FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_foods_userId_logDate ON logged_foods(userId, logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_foods_foodId ON logged_foods(foodId)")

            db.execSQL("""
                INSERT INTO logged_foods (userId, logDate, foodId, quantity, unit, slotType, timestamp, notes)
                SELECT userId, logDate, foodId, quantity, unit, slotType, timestamp, notes FROM temp_logged_foods
            """)

            db.execSQL("""
                CREATE TABLE logged_meals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    logDate TEXT NOT NULL,
                    mealId INTEGER NOT NULL,
                    slotType TEXT NOT NULL,
                    quantity REAL NOT NULL DEFAULT 1.0,
                    timestamp INTEGER,
                    notes TEXT,
                    FOREIGN KEY(userId, logDate) REFERENCES daily_logs(userId, date) ON DELETE CASCADE,
                    FOREIGN KEY(mealId) REFERENCES meals(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_meals_userId_logDate ON logged_meals(userId, logDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_logged_meals_mealId ON logged_meals(mealId)")

            db.execSQL("""
                INSERT INTO logged_meals (userId, logDate, mealId, slotType, quantity, timestamp, notes)
                SELECT userId, logDate, mealId, slotType, quantity, timestamp, notes FROM temp_logged_meals
            """)

            db.execSQL("""
                CREATE TABLE plans (
                    userId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    dietId INTEGER,
                    notes TEXT,
                    isCompleted INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(userId, date),
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY(dietId) REFERENCES diets(id) ON DELETE SET NULL
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_plans_userId ON plans(userId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_plans_dietId ON plans(dietId)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_plans_userId_date ON plans(userId, date)")

            db.execSQL("""
                INSERT INTO plans (userId, date, dietId, notes, isCompleted)
                SELECT userId, date, dietId, notes, isCompleted FROM temp_plans
            """)

            // Drop temp tables
            db.execSQL("DROP TABLE IF EXISTS temp_daily_log_slot_overrides")
            db.execSQL("DROP TABLE IF EXISTS temp_logged_foods")
            db.execSQL("DROP TABLE IF EXISTS temp_logged_meals")
            db.execSQL("DROP TABLE IF EXISTS temp_plans")

            // 12. Recreate diet_meals junction
            db.execSQL("""
                CREATE TABLE diet_meals (
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

    // Migration 11->12: Add color column to tags table
    private val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tags ADD COLUMN color TEXT DEFAULT NULL")
        }
    }

    // Migration 13->14: Add isSystemDiet column to diets
    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE diets ADD COLUMN isSystemDiet INTEGER NOT NULL DEFAULT 0")
        }
    }

    // Migration 12->13: Add grocery list tables
    private val MIGRATION_12_13 = object : Migration(12, 13) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create grocery_lists table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS grocery_lists (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    startDate TEXT,
                    endDate TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_grocery_lists_userId ON grocery_lists(userId)")

            // Create grocery_items table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS grocery_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    listId INTEGER NOT NULL,
                    foodId INTEGER,
                    customName TEXT,
                    quantity REAL NOT NULL,
                    unit TEXT NOT NULL,
                    isChecked INTEGER NOT NULL DEFAULT 0,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(listId) REFERENCES grocery_lists(id) ON DELETE CASCADE,
                    FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_grocery_items_listId ON grocery_items(listId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_grocery_items_foodId ON grocery_items(foodId)")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
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

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideGroceryDao(database: AppDatabase): GroceryDao = database.groceryDao()
}
