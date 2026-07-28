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
