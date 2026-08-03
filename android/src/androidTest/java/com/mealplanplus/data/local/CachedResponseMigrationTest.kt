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
 * Verifies MIGRATION_9_10 adds the `cached_response` table with the shape Room expects
 * (runMigrationsAndValidate throws on any schema mismatch) and preserves existing data. Guards
 * against a broken migration crashing the app on upgrade.
 */
@RunWith(AndroidJUnit4::class)
class CachedResponseMigrationTest {
    private val dbName = "migration-test-9-10.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrate9To10_addsCacheTable_andPreservesFoods() {
        helper.createDatabase(dbName, 9).use { db ->
            db.execSQL(
                "INSERT INTO foods (id, name, caloriesPer100, proteinPer100, carbsPer100, fatPer100, " +
                    "unit, isFavorite, isSystemFood, verified, updatedAt, dirty, category) " +
                    "VALUES ('u1', 'Oats', 389, 16.9, 66.3, 6.9, 'GRAM', 0, 0, 0, 0, 1, NULL)"
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 10, true, MIGRATION_9_10)

        // Existing data survives.
        db.query("SELECT name FROM foods WHERE id = 'u1'").use { c ->
            assertTrue("food row should survive the migration", c.moveToFirst())
            assertEquals("Oats", c.getString(0))
        }

        // New table exists and is usable as a keyed cache.
        db.execSQL("INSERT INTO cached_response (`key`, payload, fetchedAt) VALUES ('u1|dashboard|d', '{}', 1)")
        db.query("SELECT payload, fetchedAt FROM cached_response WHERE `key` = 'u1|dashboard|d'").use { c ->
            assertTrue("cached_response row should be readable", c.moveToFirst())
            assertEquals("{}", c.getString(0))
            assertEquals(1L, c.getLong(1))
        }
        // Primary-key replace semantics: re-inserting the same key does not create a duplicate.
        db.execSQL("INSERT OR REPLACE INTO cached_response (`key`, payload, fetchedAt) VALUES ('u1|dashboard|d', '{\"n\":1}', 2)")
        db.query("SELECT COUNT(*) FROM cached_response").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }
}
