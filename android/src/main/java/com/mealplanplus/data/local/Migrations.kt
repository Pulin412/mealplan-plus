package com.mealplanplus.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v8 → v9: add the nullable `foods.category` column in place. Explicit (not destructive) so all
 * local data — including foods/meals/diets created offline and not yet synced to the server — is
 * preserved across the app upgrade. Matches Room's expected v9 column: `category` TEXT, nullable.
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `foods` ADD COLUMN `category` TEXT")
    }
}

/**
 * v9 → v10: add the `cached_response` table — a generic read-through cache for server-computed
 * read-models (Home/Today dashboard, Plan, Groceries …) so those screens paint instantly from
 * disk on open instead of blocking on a Cloud Run cold start. Additive: no existing table or row
 * is touched. Column shape matches Room's generated schema for [com.mealplanplus.data.model.CachedResponse].
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `cached_response` (" +
                "`key` TEXT NOT NULL, `payload` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`key`))"
        )
    }
}

/**
 * v10 → v11: free-text notes on meals and diets (parity with the webapp + backend). Additive and
 * nullable so all local data — including offline, not-yet-synced meals/diets — is preserved. Meals
 * get a new `notes` column; diets reuse the server's existing `description` field for their note.
 * Column shapes match Room's generated schema for [com.mealplanplus.data.model.Meal] / `Diet`.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `meals` ADD COLUMN `notes` TEXT")
        db.execSQL("ALTER TABLE `diets` ADD COLUMN `description` TEXT")
    }
}
