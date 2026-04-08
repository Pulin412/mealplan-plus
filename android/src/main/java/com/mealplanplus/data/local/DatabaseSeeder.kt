package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.FoodItem
import kotlinx.coroutines.Dispatchers
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

// For ingredients.json
data class SeedFood(
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null
)

data class SeedFoodWrapper(
    val foods: List<SeedFood>
)


@Singleton
class DatabaseSeeder @Inject constructor(
    private val foodDao: FoodDao
) {
    private val gson = Gson()
    private val TAG = "DatabaseSeeder"

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val systemFoodCount = foodDao.getSystemFoodCount()
        if (systemFoodCount > 0) {
            return@withContext // Already seeded
        }

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

            foodDao.insertAll(foodItems)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Seed from ingredients.json + seed_data.json ONLY if database is empty
     */
    suspend fun seedFromFilesIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val systemFoodCount = foodDao.getSystemFoodCount()
        if (systemFoodCount > 0) {
            Log.d(TAG, "Database already seeded, skipping")
            return@withContext
        }

        Log.d(TAG, "First run - seeding database from files")

        // Seed foods from ingredients.json (shared across all users)
        val foodMap = seedFoodsFromIngredients(context)
        Log.d(TAG, "Seeded ${foodMap.size} foods")

        // NOTE: Diets and meals are now user-specific, so we don't seed them here.
        // Users create their own diets with custom tags after registration.
    }

    /**
     * Clear all foods and reseed from ingredients.json
     * WARNING: This deletes all food data! Use only for dev/testing.
     */
    suspend fun clearAndSeedFoods(context: Context) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting clearAndSeedFoods - WARNING: deleting food data!")

        foodDao.deleteAllFoods()

        val foodMap = seedFoodsFromIngredients(context)
        Log.d(TAG, "Seeded ${foodMap.size} foods")
    }

    private suspend fun seedFoodsFromIngredients(context: Context): Map<String, Long> {
        val json = context.assets.open("data/ingredients.json")
            .bufferedReader()
            .use { it.readText() }

        val wrapper = gson.fromJson(json, SeedFoodWrapper::class.java)
        val foodMap = mutableMapOf<String, Long>()

        wrapper.foods.forEach { sf ->
            val foodItem = FoodItem(
                name = sf.name,
                caloriesPer100 = sf.calories,
                proteinPer100 = sf.protein,
                carbsPer100 = sf.carbs,
                fatPer100 = sf.fat,
                gramsPerPiece = sf.gramsPerPiece,
                gramsPerCup = sf.gramsPerCup,
                gramsPerTbsp = sf.gramsPerTbsp,
                gramsPerTsp = sf.gramsPerTsp,
                isSystemFood = true
            )
            val id = foodDao.insertFood(foodItem)
            foodMap[sf.name] = id
        }

        return foodMap
    }

}
