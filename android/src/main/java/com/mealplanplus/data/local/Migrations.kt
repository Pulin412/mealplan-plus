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
