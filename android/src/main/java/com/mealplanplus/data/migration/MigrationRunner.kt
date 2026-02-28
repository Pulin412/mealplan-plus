package com.mealplanplus.data.migration

import android.content.Context
import android.util.Log
import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.db.DatabaseDriverFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Handles running migrations at app startup.
 * Call from Application.onCreate() or MainActivity.
 */
object MigrationRunner {
    private const val TAG = "MigrationRunner"

    private var migrationComplete = false
    private var migrationInProgress = false

    /**
     * Run data migration from Room to SQLDelight.
     * Safe to call multiple times - will only run once.
     */
    suspend fun runMigration(context: Context): Boolean {
        if (migrationComplete) {
            return true
        }

        if (migrationInProgress) {
            Log.d(TAG, "Migration already in progress")
            return false
        }

        migrationInProgress = true

        return withContext(Dispatchers.IO) {
            try {
                val driverFactory = DatabaseDriverFactory(context)
                val database = MealPlanDatabase(driverFactory.createDriver())

                val migration = RoomToSQLDelightMigration(context, database)
                val success = migration.migrateIfNeeded()

                if (success) {
                    migrationComplete = true
                    deleteOldRoomDatabase(context)
                }

                success
            } catch (e: Exception) {
                Log.e(TAG, "Migration runner failed", e)
                false
            } finally {
                migrationInProgress = false
            }
        }
    }

    /**
     * Run migration in background - fire and forget.
     * Good for calling from Application.onCreate()
     */
    fun runMigrationAsync(context: Context, scope: CoroutineScope) {
        scope.launch {
            runMigration(context)
        }
    }

    /**
     * Delete the old Room database after successful migration.
     */
    private fun deleteOldRoomDatabase(context: Context) {
        try {
            val dbName = "mealplan_database"
            val dbFile = context.getDatabasePath(dbName)

            if (dbFile.exists()) {
                // Delete main db file
                dbFile.delete()
                // Delete -wal and -shm files
                context.getDatabasePath("$dbName-wal").delete()
                context.getDatabasePath("$dbName-shm").delete()
                context.getDatabasePath("$dbName-journal").delete()

                Log.i(TAG, "Old Room database deleted")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete old Room database", e)
        }
    }
}
