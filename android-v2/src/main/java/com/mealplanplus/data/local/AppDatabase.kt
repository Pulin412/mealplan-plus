package com.mealplanplus.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.model.Food

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `foods` (
                `id`             INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `serverId`       TEXT,
                `name`           TEXT NOT NULL,
                `brand`          TEXT,
                `servingLabel`   TEXT,
                `caloriesPer100` REAL NOT NULL,
                `proteinPer100`  REAL NOT NULL,
                `carbsPer100`    REAL NOT NULL,
                `fatPer100`      REAL NOT NULL,
                `gramsPerPiece`  REAL,
                `gramsPerCup`    REAL,
                `gramsPerTbsp`   REAL,
                `gramsPerTsp`    REAL,
                `glycemicIndex`  INTEGER,
                `isFavorite`     INTEGER NOT NULL DEFAULT 0,
                `isSystemFood`   INTEGER NOT NULL DEFAULT 0,
                `verified`       INTEGER NOT NULL DEFAULT 0,
                `updatedAt`      INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

@Database(
    entities  = [Food::class],
    version   = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
}
