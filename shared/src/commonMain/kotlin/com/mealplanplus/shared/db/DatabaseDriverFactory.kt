package com.mealplanplus.shared.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory to create platform-specific database drivers
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

/**
 * Database holder for shared access
 */
object DatabaseProvider {
    private var database: MealPlanDatabase? = null

    fun getDatabase(driverFactory: DatabaseDriverFactory): MealPlanDatabase {
        if (database == null) {
            val driver = driverFactory.createDriver()
            database = MealPlanDatabase(driver)
        }
        return database!!
    }

    fun closeDatabase() {
        database = null
    }
}
