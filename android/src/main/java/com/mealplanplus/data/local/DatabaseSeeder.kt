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
        const val SYSTEM_FOODS_VERSION = 3

        private val SYSTEM_FOODS_VERSION_KEY = intPreferencesKey("system_foods_version")
    }

    /**
     * Seeds (or re-seeds) system foods from common_foods.json if the stored version
     * does not match [SYSTEM_FOODS_VERSION]. Call once at app startup.
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

            val foodItems = bundledFoods.map { food ->
                FoodItem(
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
            }

            // Delete only system foods so user-created foods are preserved
            foodDao.deleteSystemFoods()
            foodDao.insertAll(foodItems)

            context.dataStore.edit { prefs ->
                prefs[SYSTEM_FOODS_VERSION_KEY] = SYSTEM_FOODS_VERSION
            }

            Log.d(TAG, "System foods re-seeded: ${foodItems.size} items (v$SYSTEM_FOODS_VERSION)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-seed system foods", e)
        }
    }
}
