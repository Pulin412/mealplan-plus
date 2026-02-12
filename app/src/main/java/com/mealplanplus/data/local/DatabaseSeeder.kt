package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietMeal
import com.mealplanplus.data.model.DietTag
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealFoodItem
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
    val fat: Double
)

data class SeedFoodWrapper(
    val foods: List<SeedFood>
)

// For seed_data.json
data class SeedDiet(
    val name: String,
    val meal_type: String,
    val description: String?,
    val meals: Map<String, SeedMeal>
)

data class SeedMeal(
    val name: String,
    val items: List<SeedFoodItem>
)

data class SeedFoodItem(
    val food: String,
    val quantity: Double,
    val unit: String
)

data class SeedDataWrapper(
    val diets: List<SeedDiet>
)

@Singleton
class DatabaseSeeder @Inject constructor(
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietDao: DietDao
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

        // 1. Seed foods from ingredients.json
        val foodMap = seedFoodsFromIngredients(context)
        Log.d(TAG, "Seeded ${foodMap.size} foods")

        // 2. Seed diets from seed_data.json
        val dietCount = seedDietsFromSeedData(context, foodMap)
        Log.d(TAG, "Seeded $dietCount diets")
    }

    /**
     * Clear all data and seed from ingredients.json + seed_data.json
     * WARNING: This deletes all user data! Use only for dev/testing.
     */
    suspend fun clearAndSeedFromFiles(context: Context) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting clearAndSeedFromFiles - WARNING: deleting all data!")

        // 1. Clear all existing data (in order due to foreign keys)
        clearAllData()

        // 2. Seed foods from ingredients.json
        val foodMap = seedFoodsFromIngredients(context)
        Log.d(TAG, "Seeded ${foodMap.size} foods")

        // 3. Seed diets from seed_data.json
        val dietCount = seedDietsFromSeedData(context, foodMap)
        Log.d(TAG, "Seeded $dietCount diets")
    }

    private suspend fun clearAllData() {
        Log.d(TAG, "Clearing all data")
        // Delete in order due to foreign keys
        dietDao.deleteAllDietMeals()
        dietDao.deleteAllDiets()
        mealDao.deleteAllMealFoodItems()
        mealDao.deleteAllMeals()
        foodDao.deleteAllFoods()
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
                isSystemFood = true
            )
            val id = foodDao.insertFood(foodItem)
            foodMap[sf.name] = id
        }

        return foodMap
    }

    private suspend fun seedDietsFromSeedData(context: Context, foodMap: Map<String, Long>): Int {
        val json = context.assets.open("data/seed_data.json")
            .bufferedReader()
            .use { it.readText() }

        val wrapper = gson.fromJson(json, SeedDataWrapper::class.java)
        var missingFoods = mutableSetOf<String>()

        wrapper.diets.forEach { sd ->
            // Determine tags based on meal_type and special cases
            val tags = buildDietTags(sd.name, sd.meal_type)

            // Create diet
            val dietId = dietDao.insertDiet(Diet(
                name = sd.name,
                description = sd.description,
                tags = tags
            ))

            // Create meals for each slot
            sd.meals.forEach { (slotType, seedMeal) ->
                // Create meal
                val mealId = mealDao.insertMeal(Meal(
                    name = seedMeal.name,
                    slotType = slotType
                ))

                // Add food items to meal
                val mealFoodItems = seedMeal.items.mapNotNull { item ->
                    val foodId = foodMap[item.food]
                    if (foodId == null) {
                        missingFoods.add(item.food)
                        return@mapNotNull null
                    }
                    MealFoodItem(
                        mealId = mealId,
                        foodId = foodId,
                        quantity = item.quantity,
                        unit = parseUnit(item.unit)
                    )
                }
                mealDao.insertMealFoodItems(mealFoodItems)

                // Link meal to diet
                dietDao.insertDietMeal(DietMeal(
                    dietId = dietId,
                    slotType = slotType,
                    mealId = mealId
                ))
            }
        }

        if (missingFoods.isNotEmpty()) {
            Log.w(TAG, "Missing foods in ingredients.json: $missingFoods")
        }

        return wrapper.diets.size
    }

    private fun parseUnit(unit: String): FoodUnit {
        return when(unit.lowercase()) {
            "g" -> FoodUnit.GRAM
            "ml" -> FoodUnit.ML
            "piece" -> FoodUnit.PIECE
            "cup" -> FoodUnit.CUP
            "tbsp" -> FoodUnit.TBSP
            "tsp" -> FoodUnit.TSP
            "slice" -> FoodUnit.SLICE
            "scoop" -> FoodUnit.SCOOP
            else -> FoodUnit.GRAM
        }
    }

    private fun buildDietTags(name: String, mealType: String): String {
        val tags = mutableListOf<DietTag>()

        // Diet-M20 and Diet-M21 are both SOS and MAINTENANCE
        if (name == "Diet-M20" || name == "Diet-M21") {
            tags.add(DietTag.MAINTENANCE)
            tags.add(DietTag.SOS)
        } else {
            // Use meal_type from seed data
            when (mealType.uppercase()) {
                "REMISSION" -> tags.add(DietTag.REMISSION)
                "MAINTENANCE" -> tags.add(DietTag.MAINTENANCE)
                "SOS" -> tags.add(DietTag.SOS)
            }
        }

        return tags.joinToString(",") { it.name }
    }
}
