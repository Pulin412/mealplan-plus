package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.util.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// For common_foods.json
data class BundledFood(
    val name: String,
    val category: String,
    val caloriesPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,
    val glycemicIndex: Int? = null
)

@Singleton
class DatabaseSeeder @Inject constructor(
    private val foodDao: FoodDao
) {
    private val gson = Gson()
    private val TAG = "DatabaseSeeder"

    companion object {
        /**
         * Bump this whenever common_foods.json changes (new foods, corrected macros/GI, etc.).
         * On startup the app compares this against the value stored in DataStore; if they differ,
         * all system foods are deleted and re-inserted from the asset. User-created foods are
         * never touched because they have isSystemFood = false.
         */
        const val SYSTEM_FOODS_VERSION = 4

        private val SYSTEM_FOODS_VERSION_KEY = intPreferencesKey("system_foods_version")
    }

    /**
     * Seeds (or re-seeds) system foods from common_foods.json if the stored version
     * does not match [SYSTEM_FOODS_VERSION]. Call once at app startup.
     *
     * Safe upsert strategy — never deletes system food rows:
     *  - Existing system foods are updated in-place (ID preserved → FK refs in
     *    meal_food_items / logged_foods survive).
     *  - New foods not yet in the DB are inserted.
     *  - Foods removed from the JSON are left untouched (still referenced by user data).
     */
    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val storedVersion = context.dataStore.data.first()[SYSTEM_FOODS_VERSION_KEY] ?: 0

        if (storedVersion == SYSTEM_FOODS_VERSION) {
            Log.d(TAG, "System foods are up-to-date (v$SYSTEM_FOODS_VERSION), skipping re-seed")
            return@withContext
        }

        Log.d(TAG, "Re-seeding system foods: stored v$storedVersion → current v$SYSTEM_FOODS_VERSION")
        try {
            val jsonString = context.assets.open("common_foods.json")
                .bufferedReader()
                .use { it.readText() }

            val listType = object : TypeToken<List<BundledFood>>() {}.type
            val bundledFoods: List<BundledFood> = gson.fromJson(jsonString, listType)

            // Build a name→id map from existing system foods so we can preserve IDs
            val existingByName = foodDao.getAllSystemFoods().associateBy { it.name }

            val toUpsert = mutableListOf<FoodItem>()
            val toInsert = mutableListOf<FoodItem>()

            for (food in bundledFoods) {
                val existing = existingByName[food.name]
                val item = FoodItem(
                    id = existing?.id ?: 0,   // existing ID → update in-place; 0 → auto-increment
                    name = food.name,
                    brand = food.category,
                    caloriesPer100 = food.caloriesPer100,
                    proteinPer100 = food.proteinPer100,
                    carbsPer100 = food.carbsPer100,
                    fatPer100 = food.fatPer100,
                    gramsPerPiece = food.gramsPerPiece,
                    gramsPerCup = food.gramsPerCup,
                    gramsPerTbsp = food.gramsPerTbsp,
                    gramsPerTsp = food.gramsPerTsp,
                    glycemicIndex = food.glycemicIndex,
                    isSystemFood = true
                )
                if (existing != null) toUpsert.add(item) else toInsert.add(item)
            }

            // UPDATE existing rows in-place via SQL UPDATE (IDs preserved, FK refs survive).
            // Never use INSERT OR REPLACE here — that does DELETE+INSERT and fires ON DELETE CASCADE.
            if (toUpsert.isNotEmpty()) foodDao.updateAll(toUpsert)
            // INSERT brand-new foods
            if (toInsert.isNotEmpty()) foodDao.insertAll(toInsert)

            context.dataStore.edit { prefs ->
                prefs[SYSTEM_FOODS_VERSION_KEY] = SYSTEM_FOODS_VERSION
            }

            Log.d(TAG, "System foods re-seeded: ${toUpsert.size} updated, ${toInsert.size} new (v$SYSTEM_FOODS_VERSION)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-seed system foods", e)
        }
    }
}
