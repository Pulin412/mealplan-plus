package com.mealplanplus.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.mealplanplus.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of import operation
 */
data class ImportResult(
    val success: Boolean,
    val dietsImported: Int = 0,
    val mealsImported: Int = 0,
    val tagsCreated: Int = 0,
    val errorMessage: String? = null,
    val skippedDiets: List<String> = emptyList()
)

/**
 * Import strategy for handling duplicates
 */
enum class ImportStrategy {
    SKIP_DUPLICATES,    // Skip items with same name
    RENAME_DUPLICATES,  // Add suffix like "Diet-1 (2)"
    OVERWRITE           // Replace existing with same name
}

/**
 * Imports diet/meal data from JSON files (seed_data.json format).
 */
@Singleton
class JsonDataImporter @Inject constructor(
    private val tagDao: TagDao,
    private val dietDao: DietDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao
) {
    private val gson = Gson()
    private val TAG = "JsonDataImporter"

    // JSON data classes matching seed_data.json format
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
        val meal_type: String?,  // REMISSION, MAINTENANCE, SOS (optional)
        val tags: List<String>?, // Alternative: explicit tag names
        val description: String?,
        val meals: Map<String, SeedMeal>
    )

    data class SeedData(
        val diets: List<SeedDiet>
    )

    /**
     * Import diets from a JSON file URI
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        userId: Long,
        strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read JSON from URI
            val json = readJsonFromUri(context, uri)
                ?: return@withContext ImportResult(false, errorMessage = "Could not read file")

            importFromJson(json, userId, strategy)
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}", e)
            ImportResult(false, errorMessage = e.message ?: "Import failed")
        }
    }

    /**
     * Import diets from JSON string
     */
    suspend fun importFromJson(
        json: String,
        userId: Long,
        strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Parse JSON
            val seedData = try {
                gson.fromJson(json, SeedData::class.java)
            } catch (e: JsonSyntaxException) {
                return@withContext ImportResult(false, errorMessage = "Invalid JSON format: ${e.message}")
            }

            if (seedData?.diets.isNullOrEmpty()) {
                return@withContext ImportResult(false, errorMessage = "No diets found in file")
            }

            Log.d(TAG, "Parsed ${seedData.diets.size} diets from JSON")

            // Build food lookup map
            val foodMap = buildFoodMap()
            if (foodMap.isEmpty()) {
                return@withContext ImportResult(false, errorMessage = "No foods in database. Seed foods first.")
            }

            // Get existing diets/tags for duplicate checking
            val existingDietNames = getExistingDietNames(userId)
            val tagMap = getOrCreateTags(userId, seedData.diets)

            var dietsImported = 0
            var mealsImported = 0
            val skippedDiets = mutableListOf<String>()

            // Import each diet
            for (seedDiet in seedData.diets) {
                val dietName = when (strategy) {
                    ImportStrategy.SKIP_DUPLICATES -> {
                        if (seedDiet.name in existingDietNames) {
                            skippedDiets.add(seedDiet.name)
                            continue
                        }
                        seedDiet.name
                    }
                    ImportStrategy.RENAME_DUPLICATES -> {
                        generateUniqueName(seedDiet.name, existingDietNames)
                    }
                    ImportStrategy.OVERWRITE -> {
                        // Delete existing diet with same name
                        deleteExistingDiet(userId, seedDiet.name)
                        seedDiet.name
                    }
                }

                try {
                    val result = createDietWithMeals(userId, seedDiet.copy(name = dietName), tagMap, foodMap)
                    if (result.first) {
                        dietsImported++
                        mealsImported += result.second
                        // Update existingDietNames for subsequent duplicates
                        (existingDietNames as? MutableSet)?.add(dietName)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to import diet ${seedDiet.name}: ${e.message}")
                    skippedDiets.add(seedDiet.name)
                }
            }

            ImportResult(
                success = true,
                dietsImported = dietsImported,
                mealsImported = mealsImported,
                tagsCreated = tagMap.size,
                skippedDiets = skippedDiets
            )
        } catch (e: Exception) {
            Log.e(TAG, "Import failed: ${e.message}", e)
            ImportResult(false, errorMessage = e.message ?: "Import failed")
        }
    }

    private fun readJsonFromUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file: ${e.message}")
            null
        }
    }

    private suspend fun buildFoodMap(): Map<String, Long> {
        val foodMap = mutableMapOf<String, Long>()
        foodDao.getAllFoodsOnce().forEach { food ->
            foodMap[food.name] = food.id
            // Also add lowercase version for case-insensitive matching
            foodMap[food.name.lowercase()] = food.id
        }
        return foodMap
    }

    private suspend fun getExistingDietNames(userId: Long): MutableSet<String> {
        return dietDao.getAllDietNamesOnce().toMutableSet()
    }

    private suspend fun getOrCreateTags(userId: Long, diets: List<SeedDiet>): Map<String, Long> {
        val tagMap = mutableMapOf<String, Long>()

        // Collect all unique tags from diets
        val allTags = mutableSetOf<String>()
        diets.forEach { diet ->
            diet.meal_type?.let { allTags.add(it.uppercase()) }
            diet.tags?.forEach { allTags.add(it.uppercase()) }
        }

        // Create tags that don't exist
        allTags.forEach { tagName ->
            val displayName = tagName.lowercase().replaceFirstChar { it.uppercase() }
            try {
                val tag = Tag(userId = userId, name = displayName)
                val tagId = tagDao.insertTag(tag)
                tagMap[tagName] = tagId
            } catch (e: Exception) {
                // Tag might already exist, try to find it
                // For now, skip - we'll handle existing tags better later
                Log.w(TAG, "Tag $displayName might already exist")
            }
        }

        return tagMap
    }

    private suspend fun deleteExistingDiet(userId: Long, name: String) {
        // Would need a query to find diet by name and userId
        // For now, skip overwrite functionality
    }

    private fun generateUniqueName(baseName: String, existingNames: Set<String>): String {
        if (baseName !in existingNames) return baseName

        var counter = 2
        while ("$baseName ($counter)" in existingNames) {
            counter++
        }
        return "$baseName ($counter)"
    }

    private fun SeedDiet.copy(name: String) = SeedDiet(name, meal_type, tags, description, meals)

    private suspend fun createDietWithMeals(
        userId: Long,
        seedDiet: SeedDiet,
        tagMap: Map<String, Long>,
        foodMap: Map<String, Long>
    ): Pair<Boolean, Int> {
        // Create diet
        val diet = Diet(
            name = seedDiet.name,
            description = seedDiet.description
        )
        val dietId = dietDao.insertDiet(diet)

        // Assign tags
        val tagIds = mutableSetOf<Long>()
        seedDiet.meal_type?.let { tagMap[it.uppercase()]?.let { id -> tagIds.add(id) } }
        seedDiet.tags?.forEach { tagMap[it.uppercase()]?.let { id -> tagIds.add(id) } }

        tagIds.forEach { tagId ->
            tagDao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = tagId))
        }

        var mealsCreated = 0

        // Create meals
        seedDiet.meals.forEach { (slotType, seedMeal) ->
            val mealId = createMealWithFoods(userId, slotType, seedMeal, foodMap)
            if (mealId != null) {
                dietDao.insertDietMeal(DietMeal(dietId = dietId, slotType = slotType, mealId = mealId))
                mealsCreated++
            }
        }

        return Pair(true, mealsCreated)
    }

    private suspend fun createMealWithFoods(
        userId: Long,
        slotType: String,
        seedMeal: SeedMeal,
        foodMap: Map<String, Long>
    ): Long? {
        val meal = Meal(name = seedMeal.name)
        val mealId = mealDao.insertMeal(meal)

        val foodItems = seedMeal.items.mapNotNull { seedItem ->
            // Try exact match first, then case-insensitive
            val foodId = foodMap[seedItem.food] ?: foodMap[seedItem.food.lowercase()]
            if (foodId == null) {
                Log.w(TAG, "Food not found: ${seedItem.food}")
                return@mapNotNull null
            }

            MealFoodItem(
                mealId = mealId,
                foodId = foodId,
                quantity = seedItem.quantity,
                unit = parseUnit(seedItem.unit)
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
            "ml" -> FoodUnit.GRAM
            else -> FoodUnit.GRAM
        }
    }
}
