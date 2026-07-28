package com.mealplanplus.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies MIGRATION_8_9 adds foods.category in place, keeps existing rows, and produces a
 * schema Room accepts (runMigrationsAndValidate throws on any mismatch). Guards against a
 * broken migration crashing the app on upgrade.
 */
@RunWith(AndroidJUnit4::class)
class FoodCategoryMigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate8To9_preservesFoods_andAddsNullCategory() {
        helper.createDatabase(dbName, 8).use { db ->
            db.execSQL(
                "INSERT INTO foods (id, name, caloriesPer100, proteinPer100, carbsPer100, fatPer100, " +
                    "unit, isFavorite, isSystemFood, verified, updatedAt, dirty) " +
                    "VALUES ('u1', 'Oats', 389, 16.9, 66.3, 6.9, 'GRAM', 0, 0, 0, 0, 1)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 9, true, MIGRATION_8_9)

        db.query("SELECT name, category FROM foods WHERE id = 'u1'").use { c ->
            assertTrue("row should survive the migration", c.moveToFirst())
            assertEquals("Oats", c.getString(0))
            assertTrue("category is null after the additive migration", c.isNull(1))
        }
    }
}
