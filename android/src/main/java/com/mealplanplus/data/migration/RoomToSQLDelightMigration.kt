package com.mealplanplus.data.migration

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.mealplanplus.shared.db.MealPlanDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Migrates data from Room database to SQLDelight database.
 * This runs once on first launch after the KMP update.
 */
class RoomToSQLDelightMigration(
    private val context: Context,
    private val sqlDelightDb: MealPlanDatabase
) {
    companion object {
        private const val TAG = "RoomMigration"
        private const val ROOM_DB_NAME = "mealplan_database"
        private const val MIGRATION_COMPLETE_KEY = "room_to_sqldelight_migrated"
    }

    private val prefs by lazy {
        context.getSharedPreferences("migration_prefs", Context.MODE_PRIVATE)
    }

    suspend fun migrateIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        if (prefs.getBoolean(MIGRATION_COMPLETE_KEY, false)) {
            Log.d(TAG, "Migration already completed")
            return@withContext true
        }

        val roomDbFile = context.getDatabasePath(ROOM_DB_NAME)
        if (!roomDbFile.exists()) {
            Log.d(TAG, "No Room database found, nothing to migrate")
            markMigrationComplete()
            return@withContext true
        }

        try {
            Log.i(TAG, "Starting Room to SQLDelight migration...")
            migrateData(roomDbFile)
            markMigrationComplete()
            Log.i(TAG, "Migration completed successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
            false
        }
    }

    private suspend fun migrateData(roomDbFile: File) {
        val roomDb = SQLiteDatabase.openDatabase(
            roomDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        try {
            migrateUsers(roomDb)
            migrateFoodItems(roomDb)
            migrateMeals(roomDb)
            migrateMealFoodItems(roomDb)
            migrateDiets(roomDb)
            migrateDietMeals(roomDb)
            migrateTags(roomDb)
            migratePlans(roomDb)
            migrateDailyLogs(roomDb)
            migrateLoggedMeals(roomDb)
            migrateLoggedFoods(roomDb)
            migrateHealthMetrics(roomDb)
            migrateGroceryLists(roomDb)
            migrateGroceryItems(roomDb)
        } finally {
            roomDb.close()
        }
    }

    private suspend fun migrateUsers(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM users", null)
        cursor.use {
            while (it.moveToNext()) {
                val email = it.getString(it.getColumnIndexOrThrow("email"))
                val passwordHash = it.getStringOrNull(it.safeGetColumnIndex("passwordHash")) ?: ""
                val displayName = it.getStringOrNull(it.safeGetColumnIndex("displayName"))
                    ?: it.getStringOrNull(it.safeGetColumnIndex("name"))
                val photoUrl = it.getStringOrNull(it.safeGetColumnIndex("photoUrl"))
                val age = it.getLongOrNull(it.safeGetColumnIndex("age"))
                val contact = it.getStringOrNull(it.safeGetColumnIndex("contact"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))
                val updatedAt = it.getLongOrNull(it.safeGetColumnIndex("updatedAt")) ?: createdAt
                val weightKg = it.getDoubleOrNull(it.safeGetColumnIndex("weightKg"))
                val heightCm = it.getDoubleOrNull(it.safeGetColumnIndex("heightCm"))
                val gender = it.getStringOrNull(it.safeGetColumnIndex("gender"))
                val activityLevel = it.getStringOrNull(it.safeGetColumnIndex("activityLevel"))
                val targetCalories = it.getLongOrNull(it.safeGetColumnIndex("targetCalories"))
                val goalType = it.getStringOrNull(it.safeGetColumnIndex("goalType"))

                sqlDelightDb.userQueries.insert(
                    email, passwordHash, displayName, photoUrl, age, contact,
                    weightKg, heightCm, gender, activityLevel, targetCalories, goalType,
                    createdAt, updatedAt
                )
            }
        }
        Log.d(TAG, "Migrated users")
    }

    private suspend fun migrateFoodItems(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM food_items", null)
        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val brand = it.getStringOrNull(it.safeGetColumnIndex("brand"))
                val barcode = it.getStringOrNull(it.safeGetColumnIndex("barcode"))
                val caloriesPer100 = it.getDouble(it.getColumnIndexOrThrow("caloriesPer100"))
                val proteinPer100 = it.getDouble(it.getColumnIndexOrThrow("proteinPer100"))
                val carbsPer100 = it.getDouble(it.getColumnIndexOrThrow("carbsPer100"))
                val fatPer100 = it.getDouble(it.getColumnIndexOrThrow("fatPer100"))
                val gramsPerPiece = it.getDoubleOrNull(it.safeGetColumnIndex("gramsPerPiece"))
                val gramsPerCup = it.getDoubleOrNull(it.safeGetColumnIndex("gramsPerCup"))
                val gramsPerTbsp = it.getDoubleOrNull(it.safeGetColumnIndex("gramsPerTbsp"))
                val gramsPerTsp = it.getDoubleOrNull(it.safeGetColumnIndex("gramsPerTsp"))
                val glycemicIndex = it.getLongOrNull(it.safeGetColumnIndex("glycemicIndex"))
                val isFavorite = it.getLongOrNull(it.safeGetColumnIndex("isFavorite")) ?: 0L
                val lastUsed = it.getLongOrNull(it.safeGetColumnIndex("lastUsed"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))
                val isSystemFood = it.getLongOrNull(it.safeGetColumnIndex("isSystemFood")) ?: 0L

                sqlDelightDb.foodItemQueries.insert(
                    name, brand, barcode, caloriesPer100, proteinPer100, carbsPer100, fatPer100,
                    gramsPerPiece, gramsPerCup, gramsPerTbsp, gramsPerTsp, glycemicIndex,
                    isFavorite, lastUsed, createdAt, isSystemFood
                )
            }
        }
        Log.d(TAG, "Migrated food items")
    }

    private suspend fun migrateMeals(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM meals", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val description = it.getStringOrNull(it.safeGetColumnIndex("description"))
                val slotType = it.getStringOrNull(it.safeGetColumnIndex("slotType"))
                    ?: it.getStringOrNull(it.safeGetColumnIndex("defaultSlot"))
                    ?: "BREAKFAST"
                val customSlotId = it.getLongOrNull(it.safeGetColumnIndex("customSlotId"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))

                sqlDelightDb.mealQueries.insertMeal(
                    userId, name, description, slotType, customSlotId, createdAt
                )
            }
        }
        Log.d(TAG, "Migrated meals")
    }

    private suspend fun migrateMealFoodItems(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM meal_food_items", null)
        cursor.use {
            while (it.moveToNext()) {
                val mealId = it.getLong(it.getColumnIndexOrThrow("mealId"))
                val foodId = it.getLongOrNull(it.safeGetColumnIndex("foodId"))
                    ?: it.getLong(it.getColumnIndexOrThrow("foodItemId"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("quantity"))
                val unit = it.getString(it.getColumnIndexOrThrow("unit"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))

                sqlDelightDb.mealQueries.insertMealFoodItem(
                    mealId, foodId, quantity, unit, notes
                )
            }
        }
        Log.d(TAG, "Migrated meal food items")
    }

    private suspend fun migrateDiets(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM diets", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val description = it.getStringOrNull(it.safeGetColumnIndex("description"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))
                val isSystemDiet = it.getLongOrNull(it.safeGetColumnIndex("isSystemDiet")) ?: 0L

                sqlDelightDb.dietQueries.insertDiet(
                    userId, name, description, createdAt, isSystemDiet
                )
            }
        }
        Log.d(TAG, "Migrated diets")
    }

    private suspend fun migrateDietMeals(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM diet_meals", null)
        cursor.use {
            while (it.moveToNext()) {
                val dietId = it.getLong(it.getColumnIndexOrThrow("dietId"))
                val slotType = it.getStringOrNull(it.safeGetColumnIndex("slotType"))
                    ?: it.getString(it.getColumnIndexOrThrow("mealSlot"))
                val mealId = it.getLongOrNull(it.safeGetColumnIndex("mealId"))
                val instructions = it.getStringOrNull(it.safeGetColumnIndex("instructions"))

                sqlDelightDb.dietQueries.insertDietMeal(
                    dietId, slotType, mealId, instructions
                )
            }
        }
        Log.d(TAG, "Migrated diet meals")
    }

    private suspend fun migrateTags(roomDb: SQLiteDatabase) {
        // Check if tags table exists
        val tablesCursor = roomDb.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='tags'", null
        )
        val tagsTableExists = tablesCursor.use { it.count > 0 }

        if (!tagsTableExists) {
            Log.d(TAG, "Tags table does not exist, skipping")
            return
        }

        val cursor = roomDb.rawQuery("SELECT * FROM tags", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val color = it.getStringOrNull(it.safeGetColumnIndex("color"))
                val createdAt = it.getLongOrNull(it.safeGetColumnIndex("createdAt"))
                    ?: System.currentTimeMillis()

                sqlDelightDb.dietQueries.insertTag(
                    userId, name, color, createdAt
                )
            }
        }

        // Migrate diet_tag cross refs if exists
        val crossRefCursor = roomDb.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='diet_tag_cross_ref'", null
        )
        val crossRefExists = crossRefCursor.use { it.count > 0 }

        if (crossRefExists) {
            val dietTagCursor = roomDb.rawQuery("SELECT * FROM diet_tag_cross_ref", null)
            dietTagCursor.use {
                while (it.moveToNext()) {
                    val dietId = it.getLong(it.getColumnIndexOrThrow("dietId"))
                    val tagId = it.getLong(it.getColumnIndexOrThrow("tagId"))

                    sqlDelightDb.dietQueries.insertDietTag(dietId, tagId)
                }
            }
        }
        Log.d(TAG, "Migrated tags")
    }

    private suspend fun migratePlans(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM plans", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val date = it.getStringOrNull(it.safeGetColumnIndex("date"))
                    ?: it.getString(it.getColumnIndexOrThrow("startDate"))
                val dietId = it.getLongOrNull(it.safeGetColumnIndex("dietId"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))
                val isCompleted = it.getLongOrNull(it.safeGetColumnIndex("isCompleted"))
                    ?: it.getLongOrNull(it.safeGetColumnIndex("isActive"))
                    ?: 0L

                sqlDelightDb.planQueries.insertPlan(
                    userId, date, dietId, notes, isCompleted
                )
            }
        }
        Log.d(TAG, "Migrated plans")
    }

    private suspend fun migrateDailyLogs(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM daily_logs", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val date = it.getString(it.getColumnIndexOrThrow("date"))
                val plannedDietId = it.getLongOrNull(it.safeGetColumnIndex("plannedDietId"))
                    ?: it.getLongOrNull(it.safeGetColumnIndex("dietId"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))

                sqlDelightDb.dailyLogQueries.insertDailyLog(
                    userId, date, plannedDietId, notes, createdAt
                )
            }
        }
        Log.d(TAG, "Migrated daily logs")
    }

    private suspend fun migrateLoggedMeals(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM logged_meals", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLongOrNull(it.safeGetColumnIndex("userId"))
                val logDate = it.getStringOrNull(it.safeGetColumnIndex("logDate"))
                val mealId = it.getLong(it.getColumnIndexOrThrow("mealId"))
                val slotType = it.getStringOrNull(it.safeGetColumnIndex("slotType"))
                    ?: it.getString(it.getColumnIndexOrThrow("mealSlot"))
                val quantity = it.getDoubleOrNull(it.safeGetColumnIndex("quantity")) ?: 1.0
                val timestamp = it.getLongOrNull(it.safeGetColumnIndex("timestamp"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))

                // If we have userId and logDate, use them; otherwise try to get from dailyLogId
                val finalUserId = userId ?: run {
                    val dailyLogId = it.getLongOrNull(it.safeGetColumnIndex("dailyLogId"))
                    if (dailyLogId != null) {
                        // Try to lookup userId from daily_logs - for now use 1 as default
                        1L
                    } else {
                        1L
                    }
                }
                val finalLogDate = logDate ?: run {
                    // Try to derive from dailyLogId or use today
                    java.time.LocalDate.now().toString()
                }

                sqlDelightDb.dailyLogQueries.insertLoggedMeal(
                    finalUserId, finalLogDate, mealId, slotType, quantity, timestamp, notes
                )
            }
        }
        Log.d(TAG, "Migrated logged meals")
    }

    private suspend fun migrateLoggedFoods(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM logged_foods", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLongOrNull(it.safeGetColumnIndex("userId"))
                val logDate = it.getStringOrNull(it.safeGetColumnIndex("logDate"))
                val foodId = it.getLongOrNull(it.safeGetColumnIndex("foodId"))
                    ?: it.getLong(it.getColumnIndexOrThrow("foodItemId"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("quantity"))
                val unit = it.getString(it.getColumnIndexOrThrow("unit"))
                val slotType = it.getStringOrNull(it.safeGetColumnIndex("slotType"))
                    ?: it.getString(it.getColumnIndexOrThrow("mealSlot"))
                val timestamp = it.getLongOrNull(it.safeGetColumnIndex("timestamp"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))

                val finalUserId = userId ?: 1L
                val finalLogDate = logDate ?: java.time.LocalDate.now().toString()

                sqlDelightDb.dailyLogQueries.insertLoggedFood(
                    finalUserId, finalLogDate, foodId, quantity, unit, slotType, timestamp, notes
                )
            }
        }
        Log.d(TAG, "Migrated logged foods")
    }

    private suspend fun migrateHealthMetrics(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM health_metrics", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val date = it.getString(it.getColumnIndexOrThrow("date"))
                val timestamp = it.getLongOrNull(it.safeGetColumnIndex("timestamp"))
                    ?: it.getLong(it.getColumnIndexOrThrow("createdAt"))
                val metricType = it.getStringOrNull(it.safeGetColumnIndex("metricType"))
                val customTypeId = it.getLongOrNull(it.safeGetColumnIndex("customTypeId"))
                val value = it.getDouble(it.getColumnIndexOrThrow("value"))
                val secondaryValue = it.getDoubleOrNull(it.safeGetColumnIndex("secondaryValue"))
                val subType = it.getStringOrNull(it.safeGetColumnIndex("subType"))
                val notes = it.getStringOrNull(it.safeGetColumnIndex("notes"))

                sqlDelightDb.healthMetricQueries.insertHealthMetric(
                    userId, date, timestamp, metricType, customTypeId, value,
                    secondaryValue, subType, notes
                )
            }
        }
        Log.d(TAG, "Migrated health metrics")
    }

    private suspend fun migrateGroceryLists(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM grocery_lists", null)
        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getLong(it.getColumnIndexOrThrow("userId"))
                val name = it.getString(it.getColumnIndexOrThrow("name"))
                val startDate = it.getStringOrNull(it.safeGetColumnIndex("startDate"))
                val endDate = it.getStringOrNull(it.safeGetColumnIndex("endDate"))
                val createdAt = it.getLong(it.getColumnIndexOrThrow("createdAt"))
                val updatedAt = it.getLongOrNull(it.safeGetColumnIndex("updatedAt")) ?: createdAt

                sqlDelightDb.groceryListQueries.insertGroceryList(
                    userId, name, startDate, endDate, createdAt, updatedAt
                )
            }
        }
        Log.d(TAG, "Migrated grocery lists")
    }

    private suspend fun migrateGroceryItems(roomDb: SQLiteDatabase) {
        val cursor = roomDb.rawQuery("SELECT * FROM grocery_items", null)
        cursor.use {
            while (it.moveToNext()) {
                val listId = it.getLongOrNull(it.safeGetColumnIndex("listId"))
                    ?: it.getLong(it.getColumnIndexOrThrow("groceryListId"))
                val foodId = it.getLongOrNull(it.safeGetColumnIndex("foodId"))
                    ?: it.getLongOrNull(it.safeGetColumnIndex("foodItemId"))
                val customName = it.getStringOrNull(it.safeGetColumnIndex("customName"))
                    ?: it.getStringOrNull(it.safeGetColumnIndex("name"))
                val quantity = it.getDouble(it.getColumnIndexOrThrow("quantity"))
                val unit = it.getString(it.getColumnIndexOrThrow("unit"))
                val isChecked = it.getLongOrNull(it.safeGetColumnIndex("isChecked")) ?: 0L
                val sortOrder = it.getLongOrNull(it.safeGetColumnIndex("sortOrder")) ?: 0L
                val category = it.getStringOrNull(it.safeGetColumnIndex("category"))

                sqlDelightDb.groceryListQueries.insertGroceryItem(
                    listId, foodId, customName, quantity, unit, isChecked, sortOrder, category
                )
            }
        }
        Log.d(TAG, "Migrated grocery items")
    }

    private fun markMigrationComplete() {
        prefs.edit().putBoolean(MIGRATION_COMPLETE_KEY, true).apply()
    }

    // Helper extension functions with safe column index handling
    private fun android.database.Cursor.safeGetColumnIndex(name: String): Int {
        val index = getColumnIndex(name)
        return if (index >= 0) index else -1
    }

    private fun android.database.Cursor.getStringOrNull(index: Int): String? =
        if (index < 0 || isNull(index)) null else getString(index)

    private fun android.database.Cursor.getDoubleOrNull(index: Int): Double? =
        if (index < 0 || isNull(index)) null else getDouble(index)

    private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
        if (index < 0 || isNull(index)) null else getLong(index)
}
