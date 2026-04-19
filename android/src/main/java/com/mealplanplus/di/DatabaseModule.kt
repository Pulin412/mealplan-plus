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

    // Migration 14->15: Drop dead tables (logged_meals, daily_log_slot_overrides, custom_meal_slots)
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `logged_meals`")
            db.execSQL("DROP TABLE IF EXISTS `daily_log_slot_overrides`")
            db.execSQL("DROP TABLE IF EXISTS `custom_meal_slots`")
        }
    }

    // Migration 17->18: Add body metrics + goal fields to users
    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE users ADD COLUMN weightKg REAL DEFAULT NULL")
            db.execSQL("ALTER TABLE users ADD COLUMN heightCm REAL DEFAULT NULL")
            db.execSQL("ALTER TABLE users ADD COLUMN gender TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE users ADD COLUMN activityLevel TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE users ADD COLUMN targetCalories INTEGER DEFAULT NULL")
            db.execSQL("ALTER TABLE users ADD COLUMN goalType TEXT DEFAULT NULL")
        }
    }

    // Migration 16->17: Add instructions to diet_meals
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE diet_meals ADD COLUMN instructions TEXT DEFAULT NULL")
        }
    }

    // Migration 15->16: Add subType+secondaryValue to health_metrics; category to grocery_items; migrate FASTING_SUGAR->BLOOD_GLUCOSE
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE health_metrics ADD COLUMN subType TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE health_metrics ADD COLUMN secondaryValue REAL DEFAULT NULL")
            db.execSQL("UPDATE health_metrics SET metricType = 'BLOOD_GLUCOSE' WHERE metricType = 'FASTING_SUGAR'")
            db.execSQL("UPDATE health_metrics SET metricType = 'BLOOD_GLUCOSE' WHERE metricType = 'HBA1C'")
            db.execSQL("UPDATE health_metrics SET subType = 'FASTING' WHERE metricType = 'BLOOD_GLUCOSE' AND subType IS NULL")
            db.execSQL("ALTER TABLE grocery_items ADD COLUMN category TEXT DEFAULT NULL")
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

    // Migration 18->19: Add sync columns (serverId, updatedAt, syncedAt)
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // food_items
            db.execSQL("ALTER TABLE food_items ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE food_items ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE food_items ADD COLUMN syncedAt INTEGER")
            // meals
            db.execSQL("ALTER TABLE meals ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE meals ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE meals ADD COLUMN syncedAt INTEGER")
            // diets
            db.execSQL("ALTER TABLE diets ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE diets ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE diets ADD COLUMN syncedAt INTEGER")
            // health_metrics
            db.execSQL("ALTER TABLE health_metrics ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE health_metrics ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE health_metrics ADD COLUMN syncedAt INTEGER")
            // grocery_lists (updatedAt already exists)
            db.execSQL("ALTER TABLE grocery_lists ADD COLUMN serverId TEXT")
            db.execSQL("ALTER TABLE grocery_lists ADD COLUMN syncedAt INTEGER")
        }
    }

    // Migration 19->20: add preferredUnit to food_items, create custom_meal_slots table
    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE food_items ADD COLUMN preferredUnit TEXT")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_meal_slots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    date TEXT NOT NULL,
                    name TEXT NOT NULL,
                    slotOrder INTEGER NOT NULL DEFAULT 99
                )
            """.trimIndent())
        }
    }

    // Migration 21->22: fix food_items that had per-serving values stored as per-100g,
    // and populate gramsPerPiece / gramsPerCup so unit conversions are accurate.
    private val MIGRATION_24_25 = object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Fix: migration 23→24 used wrong food names that didn't match the DB.
            // This migration uses the exact names from common_foods.json to populate GI.
            // GI scale: Low ≤55 (green), Medium 56–69 (amber), High ≥70 (red)
            // Grains
            db.execSQL("UPDATE food_items SET glycemicIndex = 64 WHERE name = 'White Rice (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Brown Rice (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Basmati Rice (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 62 WHERE name = 'Chapati/Roti' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 71 WHERE name = 'Whole Wheat Bread' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 73 WHERE name = 'White Bread' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Oats (dry)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Oats' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 53 WHERE name = 'Quinoa (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 49 WHERE name = 'Pasta (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 49 WHERE name = 'Pasta Penne' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 49 WHERE name = 'Spaghetti' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 71 WHERE name = 'Naan' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 62 WHERE name = 'Paratha' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Poha (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Poha Raw' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Upma' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 69 WHERE name = 'Idli' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Dosa (plain)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Puri' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'BB Toast' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Wheat Flour' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Gram Flour' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 85 WHERE name = 'Maida' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Large Tortilla' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 68 WHERE name = 'Ragi Flour' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Jowar Flour' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 81 WHERE name = 'Corn Flakes' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 54 WHERE name = 'Sourdough Bread' AND isSystemFood = 1")
            // Proteins
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Chicken Breast (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Chicken Thigh (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Boneless Chicken' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Chicken Curry' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Egg (whole, boiled)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Egg Whole' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Egg White' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Egg Omelette' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Paneer' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Tofu' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Fish (rohu, cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Salmon (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Tuna (canned)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Prawns (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Mutton (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Beef (lean, cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Pork (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 18 WHERE name = 'Soya Chunks' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Whey Isolate' AND isSystemFood = 1")
            // Legumes
            db.execSQL("UPDATE food_items SET glycemicIndex = 29 WHERE name = 'Dal (Toor/Arhar)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 31 WHERE name = 'Dal (Moong)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Dal (Chana)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 29 WHERE name = 'Dal (Masoor)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 38 WHERE name = 'Dal (Urad)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 29 WHERE name = 'Rajma (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 29 WHERE name = 'Boiled Rajma' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 28 WHERE name = 'Chole/Chickpeas (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 28 WHERE name = 'Chickpea' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Black Chickpea' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Black Beans (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 29 WHERE name = 'Lentils (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 14 WHERE name = 'Peanuts (raw)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Soybean (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 38 WHERE name = 'Yellow Moong Dal' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 38 WHERE name = 'Green Moong' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Chana Dal' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 33 WHERE name = 'Chickpea Raw' AND isSystemFood = 1")
            // Dairy
            db.execSQL("UPDATE food_items SET glycemicIndex = 27 WHERE name = 'Milk (whole)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 27 WHERE name = 'Milk (skimmed)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 27 WHERE name = 'Milk (toned)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 27 WHERE name = 'Milk' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Curd/Yogurt' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Dahi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 11 WHERE name = 'Greek Yogurt' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Buttermilk/Chaas' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Lassi (sweet)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Cheese (cheddar)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Cheese Slice' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Cottage Cheese' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Butter' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Ghee' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Fresh Cream' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Fresh Cream Cooking' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 25 WHERE name = 'Almond Milk' AND isSystemFood = 1")
            // Fruits
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Apple' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 36 WHERE name = 'Red Apple' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 38 WHERE name = 'Green Apple' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 52 WHERE name = 'Banana' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 43 WHERE name = 'Orange' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 51 WHERE name = 'Mango' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Papaya' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 72 WHERE name = 'Watermelon' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 46 WHERE name = 'Grapes' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 53 WHERE name = 'Pomegranate' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 24 WHERE name = 'Guava' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 59 WHERE name = 'Pineapple' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Strawberries' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 53 WHERE name = 'Blueberries' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Chikoo/Sapota' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Litchi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 100 WHERE name = 'Dates (dried)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 100 WHERE name = 'Black Dates' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 64 WHERE name = 'Raisins' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 64 WHERE name = 'Raisin' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 38 WHERE name = 'Pear' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 53 WHERE name = 'Kiwi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Avocado' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 45 WHERE name = 'Cranberry' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 32 WHERE name = 'Raspberry' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 39 WHERE name = 'Plum' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Kharbooja' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Cantaloupe' AND isSystemFood = 1")
            // Vegetables
            db.execSQL("UPDATE food_items SET glycemicIndex = 78 WHERE name = 'Potato (boiled)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 44 WHERE name = 'Sweet Potato (boiled)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Onion' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Tomato' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Carrot' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Cucumber' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Spinach (cooked)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Cabbage' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Cauliflower' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Broccoli' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Green Beans' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'French Beans' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Peas (green)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Matar' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Capsicum/Bell Pepper' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Red Bell Pepper' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Yellow Bell Pepper' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Brinjal/Eggplant' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 20 WHERE name = 'Bhindi/Okra' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 20 WHERE name = 'Bhindi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Lauki/Bottle Gourd' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Lauki' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Palak Paneer' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Aloo Gobi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Mixed Vegetable Curry' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Mushroom' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 52 WHERE name = 'Corn (sweet)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Zucchini' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Lettuce' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 61 WHERE name = 'Beetroot' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Black Olives' AND isSystemFood = 1")
            // Nuts & Seeds
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Almonds' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 22 WHERE name = 'Cashews' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Walnuts' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Pistachios' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Pista' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Flax Seeds' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Chia Seeds' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Sunflower Seeds' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 35 WHERE name = 'Roasted Melon Seeds' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 14 WHERE name = 'Peanut Butter' AND isSystemFood = 1")
            // Oils
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Olive Oil' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Coconut Oil' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Mustard Oil' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Sunflower Oil' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0 WHERE name = 'Desi Ghee' AND isSystemFood = 1")
            // Snacks
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Samosa' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Pakora/Bhajiya' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Vada Pav' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Pav Bhaji' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Dhokla' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Kachori' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Chips/Crisps' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Biscuits (plain)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Namkeen/Mixture' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Oreo Biscuits' AND isSystemFood = 1")
            // Sweeteners
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Sugar' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 84 WHERE name = 'Jaggery/Gur' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 61 WHERE name = 'Honey' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 54 WHERE name = 'Maple Syrup' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 5  WHERE name = 'Cinnamon Powder' AND isSystemFood = 1")
            // Beverages
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Tea (with milk, sugar)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Coffee (with milk, sugar)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Black Tea' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Black Coffee' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 0  WHERE name = 'Green Tea' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Coconut Water' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Orange Juice' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Mango Shake' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Banana Shake' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 40 WHERE name = 'Nimbu Pani/Lemonade' AND isSystemFood = 1")
            // Dishes
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Biryani (chicken)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Biryani (veg)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Pulao' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Khichdi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Fried Rice' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Noodles (veg)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Pizza (1 slice)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Burger (veg)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Burger (chicken)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Sandwich (veg)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Wrap/Roll' AND isSystemFood = 1")
            // Sides
            db.execSQL("UPDATE food_items SET glycemicIndex = 30 WHERE name = 'Raita' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Pickle (mango)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Papad (roasted)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 75 WHERE name = 'Papad (fried)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Chutney (green)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Chutney (tamarind)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 10 WHERE name = 'Salad (mixed)' AND isSystemFood = 1")
            // Desserts
            db.execSQL("UPDATE food_items SET glycemicIndex = 70 WHERE name = 'Gulab Jamun' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Rasgulla' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 80 WHERE name = 'Jalebi' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Kheer' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 65 WHERE name = 'Halwa (sooji)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Ladoo (besan)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 57 WHERE name = 'Ice Cream (vanilla)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 23 WHERE name = 'Chocolate (dark)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Cake (chocolate)' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Brownie' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 60 WHERE name = 'Chocolate Ice Cream' AND isSystemFood = 1")
            // Condiments
            db.execSQL("UPDATE food_items SET glycemicIndex = 55 WHERE name = 'Hot & Sweet Chilli Sauce' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 50 WHERE name = 'Tomato Ketchup' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 45 WHERE name = 'Pizza Pasta Sauce' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 20 WHERE name = 'Soy Sauce' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Veggie Mayo' AND isSystemFood = 1")
            db.execSQL("UPDATE food_items SET glycemicIndex = 15 WHERE name = 'Mayo' AND isSystemFood = 1")
        }
    }

    private val MIGRATION_23_24 = object : Migration(23, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No-op: the GI updates in this migration used wrong food names.
            // Corrected in MIGRATION_24_25.
        }
    }

    private val MIGRATION_22_23 = object : Migration(22, 23) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE diets ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Egg Whole: stored 78 kcal/egg → correct 155 kcal/100g, gramsPerPiece=50
            db.execSQL("UPDATE food_items SET caloriesPer100=155, proteinPer100=12.6, carbsPer100=1.1, fatPer100=10.6, gramsPerPiece=50 WHERE name='Egg Whole' AND isSystemFood=1")
            // Egg White: stored 17 kcal/white → correct 52 kcal/100g, gramsPerPiece=33
            db.execSQL("UPDATE food_items SET caloriesPer100=52, proteinPer100=10.9, carbsPer100=0.7, fatPer100=0.2, gramsPerPiece=33 WHERE name='Egg White' AND isSystemFood=1")
            // BB Toast: stored 75 kcal/slice → correct 260 kcal/100g, gramsPerPiece=30
            db.execSQL("UPDATE food_items SET caloriesPer100=260, proteinPer100=8.7, carbsPer100=47.0, fatPer100=3.3, gramsPerPiece=30 WHERE name='BB Toast' AND isSystemFood=1")
            // Large Tortilla: stored 144 kcal/tortilla → correct 220 kcal/100g, gramsPerPiece=65
            db.execSQL("UPDATE food_items SET caloriesPer100=220, proteinPer100=6.8, carbsPer100=39.0, fatPer100=5.5, gramsPerPiece=65 WHERE name='Large Tortilla' AND isSystemFood=1")
            // Cheese Slice: stored 113 kcal/slice → correct 370 kcal/100g, gramsPerPiece=28
            db.execSQL("UPDATE food_items SET caloriesPer100=370, proteinPer100=23.0, carbsPer100=1.3, fatPer100=31.0, gramsPerPiece=28 WHERE name='Cheese Slice' AND isSystemFood=1")
            // Black Coffee: stored 2 kcal/cup → correct 1 kcal/100ml, gramsPerCup=240
            db.execSQL("UPDATE food_items SET caloriesPer100=1, proteinPer100=0.1, carbsPer100=0.0, fatPer100=0.0, gramsPerCup=240 WHERE name='Black Coffee' AND isSystemFood=1")
            // Oreo Biscuits: stored 160 kcal/serving → correct 473 kcal/100g, gramsPerPiece=11
            db.execSQL("UPDATE food_items SET caloriesPer100=473, proteinPer100=4.6, carbsPer100=67.0, fatPer100=24.0, gramsPerPiece=11 WHERE name='Oreo Biscuits' AND isSystemFood=1")
        }
    }

    // Migration 20->21: add food_tag_cross_refs junction table
    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS food_tag_cross_refs (
                    foodId INTEGER NOT NULL,
                    tagId INTEGER NOT NULL,
                    PRIMARY KEY(foodId, tagId),
                    FOREIGN KEY(foodId) REFERENCES food_items(id) ON DELETE CASCADE,
                    FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_food_tag_cross_refs_foodId ON food_tag_cross_refs(foodId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_food_tag_cross_refs_tagId ON food_tag_cross_refs(tagId)")
        }
    }

    /**
     * Migrate date columns from TEXT (ISO-8601 'YYYY-MM-DD') to INTEGER (epoch milliseconds).
     *
     * Tables affected: daily_logs, logged_foods, plans, grocery_lists,
     *                  health_metrics, custom_meal_slots
     *
     * Conversion: (julianday(col) - julianday('1970-01-01')) * 86400000
     * This yields midnight UTC epoch ms for any 'YYYY-MM-DD' string, matching
     * the DateUtils.LocalDate.toEpochMs() semantics used throughout the app.
     *
     * SQLite does not support ALTER COLUMN, so each affected table is recreated
     * in the standard "new → copy → drop → rename" pattern.
     * PRAGMA foreign_keys is disabled for the duration.
     */
    private val MIGRATION_25_26 = object : Migration(25, 26) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // ── daily_logs ──────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE daily_logs_new (
                    `userId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `plannedDietId` INTEGER,
                    `notes` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`userId`, `date`),
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO daily_logs_new
                SELECT userId,
                       CAST((julianday(`date`) - julianday('1970-01-01')) * 86400000 AS INTEGER),
                       plannedDietId, notes, createdAt
                FROM daily_logs
            """.trimIndent())
            db.execSQL("DROP TABLE daily_logs")
            db.execSQL("ALTER TABLE daily_logs_new RENAME TO daily_logs")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_logs_userId` ON `daily_logs` (`userId`)")

            // ── logged_foods (child of daily_logs) ──────────────────────────
            db.execSQL("""
                CREATE TABLE logged_foods_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `logDate` INTEGER NOT NULL,
                    `foodId` INTEGER NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unit` TEXT NOT NULL,
                    `slotType` TEXT NOT NULL,
                    `timestamp` INTEGER,
                    `notes` TEXT,
                    FOREIGN KEY(`userId`, `logDate`) REFERENCES `daily_logs`(`userId`, `date`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO logged_foods_new
                SELECT id, userId,
                       CAST((julianday(logDate) - julianday('1970-01-01')) * 86400000 AS INTEGER),
                       foodId, quantity, unit, slotType, timestamp, notes
                FROM logged_foods
            """.trimIndent())
            db.execSQL("DROP TABLE logged_foods")
            db.execSQL("ALTER TABLE logged_foods_new RENAME TO logged_foods")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_foods_userId_logDate` ON `logged_foods` (`userId`, `logDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_logged_foods_foodId` ON `logged_foods` (`foodId`)")

            // ── plans ────────────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE plans_new (
                    `userId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `dietId` INTEGER,
                    `notes` TEXT,
                    `isCompleted` INTEGER NOT NULL,
                    PRIMARY KEY(`userId`, `date`),
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`dietId`) REFERENCES `diets`(`id`)
                        ON UPDATE NO ACTION ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO plans_new
                SELECT userId,
                       CAST((julianday(`date`) - julianday('1970-01-01')) * 86400000 AS INTEGER),
                       dietId, notes, isCompleted
                FROM plans
            """.trimIndent())
            db.execSQL("DROP TABLE plans")
            db.execSQL("ALTER TABLE plans_new RENAME TO plans")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_userId` ON `plans` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_plans_dietId` ON `plans` (`dietId`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_plans_userId_date` ON `plans` (`userId`, `date`)")

            // ── grocery_lists (nullable startDate / endDate) ─────────────────
            db.execSQL("""
                CREATE TABLE grocery_lists_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `startDate` INTEGER,
                    `endDate` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `serverId` TEXT,
                    `syncedAt` INTEGER,
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO grocery_lists_new
                SELECT id, userId, name,
                       CASE WHEN startDate IS NULL THEN NULL
                            ELSE CAST((julianday(startDate) - julianday('1970-01-01')) * 86400000 AS INTEGER)
                       END,
                       CASE WHEN endDate IS NULL THEN NULL
                            ELSE CAST((julianday(endDate) - julianday('1970-01-01')) * 86400000 AS INTEGER)
                       END,
                       createdAt, updatedAt, serverId, syncedAt
                FROM grocery_lists
            """.trimIndent())
            db.execSQL("DROP TABLE grocery_lists")
            db.execSQL("ALTER TABLE grocery_lists_new RENAME TO grocery_lists")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_grocery_lists_userId` ON `grocery_lists` (`userId`)")

            // ── health_metrics ───────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE health_metrics_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `metricType` TEXT,
                    `customTypeId` INTEGER,
                    `value` REAL NOT NULL,
                    `secondaryValue` REAL,
                    `subType` TEXT,
                    `notes` TEXT,
                    `serverId` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO health_metrics_new
                SELECT id, userId,
                       CAST((julianday(`date`) - julianday('1970-01-01')) * 86400000 AS INTEGER),
                       timestamp, metricType, customTypeId, value, secondaryValue,
                       subType, notes, serverId, updatedAt, syncedAt
                FROM health_metrics
            """.trimIndent())
            db.execSQL("DROP TABLE health_metrics")
            db.execSQL("ALTER TABLE health_metrics_new RENAME TO health_metrics")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_metrics_userId` ON `health_metrics` (`userId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_metrics_date` ON `health_metrics` (`date`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_metrics_metricType` ON `health_metrics` (`metricType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_metrics_customTypeId` ON `health_metrics` (`customTypeId`)")

            // ── custom_meal_slots ────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE custom_meal_slots_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `name` TEXT NOT NULL,
                    `slotOrder` INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO custom_meal_slots_new
                SELECT id, userId,
                       CAST((julianday(`date`) - julianday('1970-01-01')) * 86400000 AS INTEGER),
                       name, slotOrder
                FROM custom_meal_slots
            """.trimIndent())
            db.execSQL("DROP TABLE custom_meal_slots")
            db.execSQL("ALTER TABLE custom_meal_slots_new RENAME TO custom_meal_slots")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    /**
     * v26 → v27: New clean-sheet schema changes.
     *
     * Structural:
     * - meals: drop userId, slotType, customSlotId; add isSystem
     * - diets: drop userId; rename isSystemDiet → isSystem
     * - diet_meals → diet_slots (ALTER TABLE RENAME)
     * - DROP custom_meal_slots (0 rows; table no longer needed)
     *
     * Additive:
     * - CREATE planned_slots  (per-slot day planning)
     * - CREATE planned_slot_foods  (ad-hoc foods on a slot)
     *
     * Data safety: all existing plans, daily_logs, logged_foods, meal_food_items
     * are untouched. Streak is unaffected.
     */
    private val MIGRATION_26_27 = object : Migration(26, 27) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // ── 1. Recreate meals — drop userId, slotType, customSlotId; add isSystem ──
            db.execSQL("""
                CREATE TABLE meals_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `isSystem` INTEGER NOT NULL,
                    `serverId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO meals_new (id, name, description, isSystem, serverId, createdAt, updatedAt, syncedAt)
                SELECT id, name, description, 0, serverId, createdAt, updatedAt, syncedAt FROM meals
            """.trimIndent())
            db.execSQL("DROP TABLE meals")
            db.execSQL("ALTER TABLE meals_new RENAME TO meals")

            // ── 2. Recreate diets — drop userId; rename isSystemDiet → isSystem ───────
            db.execSQL("""
                CREATE TABLE diets_new (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `isSystem` INTEGER NOT NULL DEFAULT 0,
                    `serverId` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isFavourite` INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO diets_new (id, name, description, createdAt, isSystem, serverId, updatedAt, syncedAt, isFavourite)
                SELECT id, name, description, createdAt, isSystemDiet, serverId, updatedAt, syncedAt, isFavourite FROM diets
            """.trimIndent())
            db.execSQL("DROP TABLE diets")
            db.execSQL("ALTER TABLE diets_new RENAME TO diets")

            // ── 3. Rename diet_meals → diet_slots ────────────────────────────────────
            db.execSQL("ALTER TABLE diet_meals RENAME TO diet_slots")
            db.execSQL("DROP INDEX IF EXISTS index_diet_meals_dietId")
            db.execSQL("DROP INDEX IF EXISTS index_diet_meals_mealId")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diet_slots_dietId` ON `diet_slots` (`dietId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diet_slots_mealId` ON `diet_slots` (`mealId`)")

            // ── 4. Create planned_slots ───────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `planned_slots` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `userId` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `slotType` TEXT NOT NULL,
                    `mealId` INTEGER,
                    `sourceDietId` INTEGER,
                    `instructions` TEXT,
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`mealId`) REFERENCES `meals`(`id`) ON DELETE SET NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_slots_userId_date` ON `planned_slots` (`userId`, `date`)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_planned_slots_userId_date_slotType` ON `planned_slots` (`userId`, `date`, `slotType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_slots_mealId` ON `planned_slots` (`mealId`)")

            // ── 5. Create planned_slot_foods ──────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `planned_slot_foods` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `plannedSlotId` INTEGER NOT NULL,
                    `foodId` INTEGER NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unit` TEXT NOT NULL,
                    `notes` TEXT,
                    FOREIGN KEY(`plannedSlotId`) REFERENCES `planned_slots`(`id`) ON DELETE CASCADE,
                    FOREIGN KEY(`foodId`) REFERENCES `food_items`(`id`) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_slot_foods_plannedSlotId` ON `planned_slot_foods` (`plannedSlotId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_slot_foods_foodId` ON `planned_slot_foods` (`foodId`)")

            // ── 6. Drop custom_meal_slots (always 0 rows, table no longer needed) ────
            db.execSQL("DROP TABLE IF EXISTS custom_meal_slots")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    /**
     * v28: Deduplicate diets and meals that accumulated due to missing idempotency
     * guards in UserDataSeeder and JsonDataImporter.
     *
     * Strategy:
     *  1. Re-point diet_slots / planned_slots to the canonical (min-id) meal per name,
     *     so FK references survive meal deduplication.
     *  2. Delete duplicate meals (keep lowest id per name).
     *     → meal_food_items cascade automatically.
     *  3. Delete duplicate diets (keep lowest id per name).
     *     → diet_tags and diet_slots cascade automatically.
     *
     * No schema change — only data cleanup.
     * planned_slots.sourceDietId is informational-only (no FK) so no cleanup needed there.
     */
    private val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Step 1a: re-point diet_slots.mealId to canonical meal
            database.execSQL("""
                UPDATE diet_slots
                SET mealId = (
                    SELECT MIN(m2.id) FROM meals m2
                    WHERE m2.name = (SELECT m1.name FROM meals m1 WHERE m1.id = diet_slots.mealId)
                )
                WHERE mealId IS NOT NULL
            """.trimIndent())

            // Step 1b: re-point planned_slots.mealId to canonical meal (if any data)
            database.execSQL("""
                UPDATE planned_slots
                SET mealId = (
                    SELECT MIN(m2.id) FROM meals m2
                    WHERE m2.name = (SELECT m1.name FROM meals m1 WHERE m1.id = planned_slots.mealId)
                )
                WHERE mealId IS NOT NULL
            """.trimIndent())

            // Step 2: delete duplicate meals, keep min(id) per name
            // meal_food_items deleted via ON DELETE CASCADE
            database.execSQL("""
                DELETE FROM meals
                WHERE id NOT IN (SELECT MIN(id) FROM meals GROUP BY name)
            """.trimIndent())

            // Step 3: delete duplicate diets, keep min(id) per name
            // diet_tags and diet_slots deleted via ON DELETE CASCADE
            database.execSQL("""
                DELETE FROM diets
                WHERE id NOT IN (SELECT MIN(id) FROM diets GROUP BY name)
            """.trimIndent())

            // Note: planned_slots.sourceDietId is informational-only (no FK),
            // so no cleanup is needed for planned_slots after diet deduplication.
            // planned_slots.mealId already re-pointed to canonical meals in step 1b,
            // and ON DELETE SET_NULL on that FK handles any remaining orphans.
        }
    }

    /**
     * v29: Deduplicate food_items that accumulated via multiple search/import runs.
     *
     * For compound-PK tables (meal_food_items, food_tag_cross_refs) the strategy is:
     *  a. DELETE entries where the canonical food already covers the same row (would PK-conflict).
     *  b. UPDATE remaining entries to the canonical (min-id) food.
     * Simple-PK tables (logged_foods, planned_slot_foods, grocery_items) just need UPDATE.
     * Finally, delete non-canonical food_items rows.
     * All child-table cascades are no-ops by the time the food DELETE fires.
     *
     * No schema change — data cleanup only.
     */
    private val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // ── meal_food_items (PK: mealId + foodId) ──────────────────────────
            // a) drop entries that would PK-conflict after re-pointing to canonical food
            database.execSQL("""
                DELETE FROM meal_food_items
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
                  AND EXISTS (
                      SELECT 1 FROM meal_food_items mfi2
                      WHERE mfi2.mealId = meal_food_items.mealId
                        AND mfi2.foodId = (
                            SELECT MIN(f2.id) FROM food_items f2
                            WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = meal_food_items.foodId)
                        )
                  )
            """.trimIndent())
            // b) re-point the rest
            database.execSQL("""
                UPDATE meal_food_items
                SET foodId = (
                    SELECT MIN(f2.id) FROM food_items f2
                    WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = meal_food_items.foodId)
                )
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())

            // ── food_tag_cross_refs (PK: foodId + tagId) ───────────────────────
            database.execSQL("""
                DELETE FROM food_tag_cross_refs
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
                  AND EXISTS (
                      SELECT 1 FROM food_tag_cross_refs ftcr2
                      WHERE ftcr2.tagId = food_tag_cross_refs.tagId
                        AND ftcr2.foodId = (
                            SELECT MIN(f2.id) FROM food_items f2
                            WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = food_tag_cross_refs.foodId)
                        )
                  )
            """.trimIndent())
            database.execSQL("""
                UPDATE food_tag_cross_refs
                SET foodId = (
                    SELECT MIN(f2.id) FROM food_items f2
                    WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = food_tag_cross_refs.foodId)
                )
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())

            // ── logged_foods ───────────────────────────────────────────────────
            database.execSQL("""
                UPDATE logged_foods
                SET foodId = (
                    SELECT MIN(f2.id) FROM food_items f2
                    WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = logged_foods.foodId)
                )
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())

            // ── planned_slot_foods ─────────────────────────────────────────────
            database.execSQL("""
                UPDATE planned_slot_foods
                SET foodId = (
                    SELECT MIN(f2.id) FROM food_items f2
                    WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = planned_slot_foods.foodId)
                )
                WHERE foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())

            // ── grocery_items (nullable foodId) ───────────────────────────────
            database.execSQL("""
                UPDATE grocery_items
                SET foodId = (
                    SELECT MIN(f2.id) FROM food_items f2
                    WHERE f2.name = (SELECT f1.name FROM food_items f1 WHERE f1.id = grocery_items.foodId)
                )
                WHERE foodId IS NOT NULL
                  AND foodId NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())

            // ── Delete duplicate food_items (keep min id per name) ────────────
            database.execSQL("""
                DELETE FROM food_items
                WHERE id NOT IN (SELECT MIN(id) FROM food_items GROUP BY name)
            """.trimIndent())
        }
    }

    /**
     * v30: Clear all meals and diet_slots wiped by the incorrect name-based deduplication in
     * MIGRATION_27_28. Many diets legitimately share a meal name (e.g. "Mix Fruit Bowl") but
     * have completely different ingredients. The v28 migration collapsed them all into one row,
     * making every diet show the wrong meals.
     *
     * This migration wipes the bad data. MealSlotReseeder runs at next app launch to
     * re-create one correct meal row per diet slot from seed_data.json.
     *
     * No schema change — data cleanup only.
     */
    private val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Delete in FK-safe order (children before parents)
            database.execSQL("DELETE FROM meal_food_items")
            database.execSQL("DELETE FROM diet_slots")
            database.execSQL("DELETE FROM meals")
        }
    }

    private val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Scope meals and diets to individual users.
            // NULL = system / built-in record visible to everyone.
            // Existing rows (seeded system data) get NULL by default → still visible to all users.
            db.execSQL("ALTER TABLE meals ADD COLUMN userId INTEGER DEFAULT NULL REFERENCES users(id) ON DELETE CASCADE")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_meals_userId ON meals(userId)")
            db.execSQL("ALTER TABLE diets ADD COLUMN userId INTEGER DEFAULT NULL REFERENCES users(id) ON DELETE CASCADE")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_diets_userId ON diets(userId)")
        }
    }

    /**
     * MIGRATION_31_32: Fix the `userId` column default on `meals` and `diets`.
     *
     * The v30→v31 migration used ALTER TABLE ADD COLUMN without an explicit DEFAULT NULL
     * clause, so SQLite stored the default as undefined. Room validates the schema against
     * the entity annotation (@ColumnInfo defaultValue="NULL") and crashes.
     *
     * SQLite does not support ALTER COLUMN, so we recreate both tables using the standard
     * "create-copy-drop-rename" approach.
     */
    private val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = OFF")

            // --- meals ---
            db.execSQL("""
                CREATE TABLE `meals_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `isSystem` INTEGER NOT NULL,
                    `userId` INTEGER DEFAULT NULL,
                    `serverId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `meals_new`
                SELECT `id`, `name`, `description`, `isSystem`, `userId`, `serverId`, `createdAt`, `updatedAt`, `syncedAt`
                FROM `meals`
            """.trimIndent())
            db.execSQL("DROP TABLE `meals`")
            db.execSQL("ALTER TABLE `meals_new` RENAME TO `meals`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_meals_userId` ON `meals`(`userId`)")

            // --- diets ---
            db.execSQL("""
                CREATE TABLE `diets_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `description` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `isSystem` INTEGER NOT NULL DEFAULT 0,
                    `userId` INTEGER DEFAULT NULL,
                    `serverId` TEXT,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `isFavourite` INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("""
                INSERT INTO `diets_new`
                SELECT `id`, `name`, `description`, `createdAt`, `isSystem`, `userId`, `serverId`, `updatedAt`, `syncedAt`, `isFavourite`
                FROM `diets`
            """.trimIndent())
            db.execSQL("DROP TABLE `diets`")
            db.execSQL("ALTER TABLE `diets_new` RENAME TO `diets`")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_diets_userId` ON `diets`(`userId`)")

            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    private val MIGRATION_32_33 = object : Migration(32, 33) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    muscleGroup TEXT,
                    equipment TEXT,
                    description TEXT,
                    isSystem INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    durationMinutes INTEGER,
                    notes TEXT,
                    isCompleted INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    serverId TEXT,
                    syncedAt INTEGER
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_sets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    setNumber INTEGER NOT NULL,
                    reps INTEGER,
                    weightKg REAL,
                    durationSeconds INTEGER,
                    distanceMeters REAL,
                    notes TEXT,
                    FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_sessionId` ON workout_sets(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sets_exerciseId` ON workout_sets(exerciseId)")
        }
    }

    private val MIGRATION_33_34 = object : Migration(33, 34) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add videoLink + userId to exercises (new custom exercise support)
            db.execSQL("ALTER TABLE exercises ADD COLUMN videoLink TEXT DEFAULT NULL")
            db.execSQL("ALTER TABLE exercises ADD COLUMN userId TEXT DEFAULT NULL")

            // Workout templates (reusable named plans like "Chest Day")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_templates (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL DEFAULT 'STRENGTH',
                    notes TEXT,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())

            // Exercises per template with targets
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_template_exercises (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    templateId INTEGER NOT NULL,
                    exerciseId INTEGER NOT NULL,
                    orderIndex INTEGER NOT NULL DEFAULT 0,
                    targetSets INTEGER,
                    targetReps INTEGER,
                    targetWeightKg REAL,
                    targetDurationSeconds INTEGER,
                    notes TEXT,
                    FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_templateId` ON workout_template_exercises(templateId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_template_exercises_exerciseId` ON workout_template_exercises(exerciseId)")

            // Planned workouts per date (like day_plans for meals)
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS planned_workouts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    templateId INTEGER NOT NULL,
                    FOREIGN KEY(templateId) REFERENCES workout_templates(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_workouts_templateId` ON planned_workouts(templateId)")
        }
    }

    private val MIGRATION_34_35 = object : Migration(34, 35) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Per-set pyramid data for workout template exercises
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS workout_template_sets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    templateExerciseId INTEGER NOT NULL,
                    setIndex INTEGER NOT NULL DEFAULT 0,
                    reps INTEGER,
                    weightKg REAL,
                    FOREIGN KEY(templateExerciseId) REFERENCES workout_template_exercises(id)
                        ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_workout_template_sets_templateExerciseId ON workout_template_sets(templateExerciseId)")
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
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35)
            // Removed fallbackToDestructiveMigration() — this was destroying user data!
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
    fun providePlannedSlotDao(database: AppDatabase): PlannedSlotDao = database.plannedSlotDao()

    @Provides
    fun provideHealthMetricDao(database: AppDatabase): HealthMetricDao = database.healthMetricDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideGroceryDao(database: AppDatabase): GroceryDao = database.groceryDao()

    @Provides
    fun provideExerciseDao(database: AppDatabase): ExerciseDao = database.exerciseDao()

    @Provides
    fun provideWorkoutSessionDao(database: AppDatabase): WorkoutSessionDao = database.workoutSessionDao()

    @Provides
    fun provideWorkoutSetDao(database: AppDatabase): WorkoutSetDao = database.workoutSetDao()

    @Provides
    fun provideWorkoutTemplateDao(database: AppDatabase): WorkoutTemplateDao = database.workoutTemplateDao()

    @Provides
    fun providePlannedWorkoutDao(database: AppDatabase): PlannedWorkoutDao = database.plannedWorkoutDao()
}
