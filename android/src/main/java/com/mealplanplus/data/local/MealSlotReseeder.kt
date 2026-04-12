package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guards and triggers a one-time re-seed of meal rows and diet_slot rows after
 * MIGRATION_29_30 cleared them due to incorrect name-based deduplication.
 *
 * Run this at app startup (before any UI loads meals).
 * It is a no-op once meals exist in the database.
 */
@Singleton
class MealSlotReseeder @Inject constructor(
    private val jsonDataImporter: JsonDataImporter,
    private val mealDao: MealDao
) {
    private val TAG = "MealSlotReseeder"

    suspend fun reseedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val mealCount = mealDao.getMealCount()
        if (mealCount > 0) {
            Log.d(TAG, "Meals already present ($mealCount), skipping reseed")
            return@withContext
        }
        Log.d(TAG, "No meals found — reseeding from seed_data.json")
        val created = jsonDataImporter.reseedMealsForExistingDiets(context)
        Log.d(TAG, "Reseed complete: $created meal slots created")
    }
}
