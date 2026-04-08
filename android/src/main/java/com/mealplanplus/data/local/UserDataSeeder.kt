package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mealplanplus.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds user-specific data (tags, diets, meals) from seed_data.json when user signs up.
 */
@Singleton
class UserDataSeeder @Inject constructor(
    private val tagDao: TagDao,
    private val dietDao: DietDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao
) {
    private val gson = Gson()
    private val TAG = "UserDataSeeder"

    // JSON data classes
    data class SeedFoodItem(
        val food: String,
        val quantity: Double,
        val unit: String
    )

    data class SeedMeal(
        val name: String,
        val items: List<SeedFoodItem>
    )

    data class SeedDiet(
        val name: String,
        val meal_type: String,  // REMISSION, MAINTENANCE, SOS
        val description: String,
        val meals: Map<String, SeedMeal>  // BREAKFAST, LUNCH, etc.
    )

    data class SeedData(
        val diets: List<SeedDiet>
    )

    /**
     * Seed default tags, diets, and meals for a newly registered user.
     * Call this after user sign-up.
     */
    suspend fun seedUserData(context: Context, userId: Long) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting user data seed for userId=$userId")

        // 1. Create default tags
        val tagMap = createDefaultTags(userId)
        Log.d(TAG, "Created ${tagMap.size} tags")

        // 2. Load sample_data.json
        val seedData = loadSeedData(context) ?: run {
            Log.w(TAG, "Failed to load sample_data.json")
            return@withContext
        }
        Log.d(TAG, "Loaded ${seedData.diets.size} sample diets from sample_data.json")

        // 3. Build food lookup map
        val foodMap = buildFoodMap()
        Log.d(TAG, "Built food map with ${foodMap.size} entries")

        // 4. Create diets with meals
        var dietsCreated = 0
        var mealsCreated = 0

        seedData.diets.forEach { seedDiet ->
            try {
                val result = createDietWithMeals(userId, seedDiet, tagMap, foodMap)
                if (result.first) {
                    dietsCreated++
                    mealsCreated += result.second
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create diet ${seedDiet.name}: ${e.message}")
            }
        }

        Log.d(TAG, "Seeding complete: $dietsCreated diets, $mealsCreated meals")
    }

    private suspend fun createDefaultTags(userId: Long): Map<String, Long> {
        val tags = listOf("Remission", "Maintenance", "SOS")
        val tagMap = mutableMapOf<String, Long>()

        tags.forEach { tagName ->
            val tag = Tag(userId = userId, name = tagName)
            val tagId = tagDao.insertTag(tag)
            tagMap[tagName.uppercase()] = tagId  // Store as REMISSION, MAINTENANCE, SOS
        }

        return tagMap
    }

    private fun loadSeedData(context: Context): SeedData? {
        return try {
            val json = context.assets.open("data/sample_data.json")
                .bufferedReader()
                .use { it.readText() }
            gson.fromJson(json, SeedData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading sample_data.json: ${e.message}")
            null
        }
    }

    private suspend fun buildFoodMap(): Map<String, Long> {
        val foodMap = mutableMapOf<String, Long>()

        // Get all foods - they're already seeded from ingredients.json
        foodDao.getAllFoodsOnce().forEach { food ->
            foodMap[food.name] = food.id
        }

        return foodMap
    }

    /**
     * Create a diet with its meals. Returns (success, mealsCreatedCount)
     */
    private suspend fun createDietWithMeals(
        userId: Long,
        seedDiet: SeedDiet,
        tagMap: Map<String, Long>,
        foodMap: Map<String, Long>
    ): Pair<Boolean, Int> {
        // Create the diet
        val diet = Diet(
            userId = userId,
            name = seedDiet.name,
            description = seedDiet.description
        )
        val dietId = dietDao.insertDiet(diet)

        // Assign tag based on meal_type
        val tagId = tagMap[seedDiet.meal_type]
        if (tagId != null) {
            tagDao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = tagId))
        }

        var mealsCreated = 0

        // Create meals for each slot
        seedDiet.meals.forEach { (slotType, seedMeal) ->
            val mealId = createMealWithFoods(userId, slotType, seedMeal, foodMap)
            if (mealId != null) {
                // Link meal to diet
                dietDao.insertDietMeal(DietMeal(dietId = dietId, slotType = slotType, mealId = mealId))
                mealsCreated++
            }
        }

        return Pair(true, mealsCreated)
    }

    /**
     * Create a meal with its food items. Returns mealId or null on failure.
     */
    private suspend fun createMealWithFoods(
        userId: Long,
        slotType: String,
        seedMeal: SeedMeal,
        foodMap: Map<String, Long>
    ): Long? {
        val meal = Meal(
            userId = userId,
            name = seedMeal.name,
            slotType = slotType
        )
        val mealId = mealDao.insertMeal(meal)

        val foodItems = seedMeal.items.mapNotNull { seedItem ->
            val foodId = foodMap[seedItem.food]
            if (foodId == null) {
                Log.w(TAG, "Food not found: ${seedItem.food}")
                return@mapNotNull null
            }

            val unit = parseUnit(seedItem.unit)
            MealFoodItem(
                mealId = mealId,
                foodId = foodId,
                quantity = seedItem.quantity,
                unit = unit
            )
        }

        if (foodItems.isNotEmpty()) {
            mealDao.insertMealFoodItems(foodItems)
        }

        return mealId
    }

    private fun parseUnit(unitStr: String): FoodUnit {
        return when (unitStr.lowercase()) {
            "g", "gram", "grams" -> FoodUnit.GRAM
            "piece", "pieces" -> FoodUnit.PIECE
            "cup", "cups" -> FoodUnit.CUP
            "tbsp" -> FoodUnit.TBSP
            "tsp" -> FoodUnit.TSP
            "ml" -> FoodUnit.GRAM  // Treat ml as gram (close enough for liquids)
            else -> FoodUnit.GRAM
        }
    }
}
