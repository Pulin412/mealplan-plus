package com.mealplanplus.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mealplanplus.data.model.*
import com.mealplanplus.util.dataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-time importer that restores all user data from assets/backup_data.json
 * (exported from the v26 database before the schema redesign).
 *
 * - Runs only once, guarded by the BACKUP_IMPORTED DataStore flag.
 * - Imports all 3 user accounts, deduplicating app-global meals/diets by name.
 * - Remaps every old ID to new Room auto-generated IDs so FK integrity is preserved.
 * - System foods (isSystemFood=true) are matched by name against the current DB
 *   rather than re-inserted, so existing FK refs in meal_food_items stay intact.
 */
@Singleton
class BackupDataImporter @Inject constructor(
    private val userDao: UserDao,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val tagDao: TagDao,
    private val dailyLogDao: DailyLogDao,
    private val healthMetricDao: HealthMetricDao,
    private val planDao: PlanDao,
    private val groceryDao: GroceryDao
) {
    private val TAG = "BackupDataImporter"

    // v5: clean up orphan null-userId meals/diets left by the v3 import before re-inserting.
    private val BACKUP_IMPORTED_KEY = booleanPreferencesKey("backup_data_imported_v5")

    suspend fun importIfNeeded(context: Context) {
        val prefs = context.dataStore.data.first()
        if (prefs[BACKUP_IMPORTED_KEY] == true) {
            Log.d(TAG, "Backup already imported, skipping.")
            return
        }

        Log.i(TAG, "Starting one-time backup import…")
        try {
            val json = context.assets.open("backup_data.json").bufferedReader().readText()
            val root = JSONObject(json)

            // ------------------------------------------------------------------
            // 1. Build name → current-DB-id map for system foods (already seeded)
            // ------------------------------------------------------------------
            val systemFoodNameToId = mutableMapOf<String, Long>()
            foodDao.getAllFoodsOnce().forEach { food ->
                if (food.isSystemFood) systemFoodNameToId[food.name] = food.id
            }
            Log.d(TAG, "Loaded ${systemFoodNameToId.size} system food names from DB")

            // ------------------------------------------------------------------
            // 2. Import users — map backupUserId → new Room userId
            // ------------------------------------------------------------------
            val userIdMap = mutableMapOf<Long, Long>()
            val usersArr = root.getJSONArray("users")
            for (i in 0 until usersArr.length()) {
                val u = usersArr.getJSONObject(i)
                val backupId = u.getLong("id")
                val email = u.getString("email")
                val existing = userDao.getUserByEmail(email)
                if (existing != null) {
                    userIdMap[backupId] = existing.id
                    Log.d(TAG, "User $email already exists with id=${existing.id}")
                } else {
                    val user = User(
                        email = email,
                        displayName = u.optString("displayName", null),
                        photoUrl = u.optString("photoUrl", null),
                        age = u.optIntOrNull("age"),
                        weightKg = u.optDoubleOrNull("weightKg"),
                        heightCm = u.optDoubleOrNull("heightCm"),
                        gender = u.optString("gender", null),
                        activityLevel = u.optString("activityLevel", null),
                        targetCalories = u.optIntOrNull("targetCalories"),
                        goalType = u.optString("goalType", null),
                        createdAt = u.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = u.optLong("updatedAt", System.currentTimeMillis())
                    )
                    val newId = userDao.insertUser(user)
                    userIdMap[backupId] = newId
                    Log.d(TAG, "Inserted user $email → id=$newId")
                }
            }

            // ------------------------------------------------------------------
            // 3. Import user-created food items (isSystemFood=false)
            //    Matched by name if already present, otherwise inserted.
            // ------------------------------------------------------------------
            val foodIdMap = mutableMapOf<Long, Long>()
            // Seed system food name→id map first (covers backup food IDs 1-106)
            val allFoodsArr = root.getJSONArray("food_items")
            for (i in 0 until allFoodsArr.length()) {
                val f = allFoodsArr.getJSONObject(i)
                val backupId = f.getLong("id")
                if (f.getInt("isSystemFood") == 1) {
                    // Look up current ID by name
                    val name = f.getString("name")
                    val currentId = systemFoodNameToId[name]
                    if (currentId != null) {
                        foodIdMap[backupId] = currentId
                    } else {
                        Log.w(TAG, "System food '$name' not found in DB — FK refs to it will be skipped")
                    }
                } else {
                    // User-created food — insert if not already present
                    val name = f.getString("name")
                    val existingFood = foodDao.getFoodByExactName(name)
                    if (existingFood != null) {
                        foodIdMap[backupId] = existingFood.id
                        Log.d(TAG, "User food '$name' already exists → id=${existingFood.id}")
                    } else {
                        val food = FoodItem(
                            name = name,
                            brand = f.optString("brand", null),
                            barcode = f.optString("barcode", null),
                            caloriesPer100 = f.optDouble("caloriesPer100", 0.0),
                            proteinPer100 = f.optDouble("proteinPer100", 0.0),
                            carbsPer100 = f.optDouble("carbsPer100", 0.0),
                            fatPer100 = f.optDouble("fatPer100", 0.0),
                            gramsPerPiece = f.optDoubleOrNull("gramsPerPiece"),
                            gramsPerCup = f.optDoubleOrNull("gramsPerCup"),
                            gramsPerTbsp = f.optDoubleOrNull("gramsPerTbsp"),
                            gramsPerTsp = f.optDoubleOrNull("gramsPerTsp"),
                            glycemicIndex = f.optIntOrNull("glycemicIndex"),
                            preferredUnit = f.optString("preferredUnit", null),
                            isFavorite = f.getInt("isFavorite") == 1,
                            lastUsed = f.optLongOrNull("lastUsed"),
                            createdAt = f.optLong("createdAt", System.currentTimeMillis()),
                            isSystemFood = false
                        )
                        val newId = foodDao.insertFood(food)
                        foodIdMap[backupId] = newId
                        Log.d(TAG, "Inserted user food '$name' → id=$newId")
                    }
                }
            }

            // ------------------------------------------------------------------
            // 4. Import tags (user-owned) — map old tagId → new tagId per user
            // ------------------------------------------------------------------
            val tagIdMap = mutableMapOf<Long, Long>() // old tag id → new tag id
            val tagsArr = root.getJSONArray("tags")
            for (i in 0 until tagsArr.length()) {
                val t = tagsArr.getJSONObject(i)
                val backupTagId = t.getLong("id")
                val backupUserId = t.getLong("userId")
                val newUserId = userIdMap[backupUserId] ?: continue
                val name = t.getString("name")
                val tag = Tag(
                    userId = newUserId,
                    name = name,
                    color = t.optString("color", null),
                    createdAt = t.optLong("createdAt", System.currentTimeMillis())
                )
                val newTagId = try {
                    tagDao.insertTag(tag)
                } catch (e: Exception) {
                    // Unique constraint (userId, name) — tag already exists, look it up
                    tagDao.getTagsByUser(newUserId).first().find { it.name == name }?.id ?: run {
                        Log.w(TAG, "Could not insert or find tag '$name' for user $newUserId"); -1L
                    }
                }
                if (newTagId > 0) tagIdMap[backupTagId] = newTagId
            }
            Log.d(TAG, "Imported ${tagIdMap.size} tags")

            // ------------------------------------------------------------------
            // 4b. Clear any previously imported meals/diets before re-inserting.
            //     Also clears orphan null-userId records left by earlier bad imports.
            //     CASCADE on the FK deletes meal_food_items and diet_slots too.
            // ------------------------------------------------------------------
            mealDao.deleteOrphanUserMeals()   // removes isSystem=0, userId IS NULL leftovers
            dietDao.deleteOrphanUserDiets()
            for (newUserId in userIdMap.values) {
                mealDao.deleteAllMealsForUser(newUserId)
                dietDao.deleteAllDietsForUser(newUserId)
            }
            Log.d(TAG, "Cleared previous meals/diets (orphans + ${userIdMap.size} users)")

            // ------------------------------------------------------------------
            // 5. Import meals — now user-scoped (no name deduplication needed).
            //    Each user gets their own meal records with their own userId.
            //    Build old-meal-id → new-meal-id map per user.
            // ------------------------------------------------------------------
            val mealIdMap = mutableMapOf<Long, Long>() // old backup meal id → new Room meal id
            val mfiByMealId = root.getJSONArray("meal_food_items").toObjectList()
                .groupBy { it.getLong("mealId") }

            val allMeals = root.getJSONArray("meals").toObjectList()
            for (m in allMeals) {
                val oldId = m.getLong("id")
                // Determine which user owns this meal from its diet_meals reference
                // (all meals in the backup were user-owned, not system).
                // We resolve the owning user when building diet slots (step 7).
                // For now insert with userId=null; we'll patch it per-user below.
                val newId = mealDao.insertMeal(
                    Meal(
                        name = m.getString("name"),
                        description = m.optString("description", null),
                        isSystem = false,
                        userId = null, // patched in step 5b
                        createdAt = m.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                mealIdMap[oldId] = newId
            }
            Log.d(TAG, "Inserted ${mealIdMap.size} meals (one per backup record)")

            // 5b. Build backup-userId → set of old meal IDs owned by that user,
            //     then patch userId on the inserted meals.
            val backupDietMeals = root.getJSONArray("diet_meals").toObjectList()
            val backupDiets = root.getJSONArray("diets").toObjectList()
            val backupDietIdToBackupUserId = mutableMapOf<Long, Long>()
            // The backup has one diet set per user; map each backup diet id to its user
            // by looking at the users array — diets[0..37] → user 1, [38..75] → user 2, etc.
            // Simpler: group diet ids by user using plans/daily_logs which carry userId.
            val backupPlansByUser = root.getJSONArray("plans").toObjectList()
                .groupBy { it.getLong("userId") }
            for ((backupUserId, plans) in backupPlansByUser) {
                val dietIdsForUser = plans.mapNotNull { it.optLong("dietId", -1L).takeIf { it > 0 } }.toSet()
                dietIdsForUser.forEach { backupDietIdToBackupUserId[it] = backupUserId }
            }
            // Also cover diets not in plans — assign by ordering (each user has same count)
            val sortedDietIds = backupDiets.map { it.getLong("id") }.sorted()
            val userIds = userIdMap.keys.sorted()
            val dietsPerUser = sortedDietIds.size / userIds.size.coerceAtLeast(1)
            sortedDietIds.forEachIndexed { idx, dietId ->
                if (!backupDietIdToBackupUserId.containsKey(dietId)) {
                    val userIndex = (idx / dietsPerUser).coerceAtMost(userIds.size - 1)
                    backupDietIdToBackupUserId[dietId] = userIds[userIndex]
                }
            }
            // Map each backup meal id → owning backup user id via diet_meals
            val backupMealIdToBackupUserId = mutableMapOf<Long, Long>()
            for (dm in backupDietMeals) {
                val backupDietId = dm.getLong("dietId")
                val backupMealId = dm.optLong("mealId", -1L).takeIf { it > 0 } ?: continue
                val backupUserId = backupDietIdToBackupUserId[backupDietId] ?: continue
                backupMealIdToBackupUserId[backupMealId] = backupUserId
            }
            // Patch userId on each inserted meal
            for ((oldMealId, newMealId) in mealIdMap) {
                val backupUserId = backupMealIdToBackupUserId[oldMealId] ?: continue
                val newUserId = userIdMap[backupUserId] ?: continue
                val meal = mealDao.getMealById(newMealId) ?: continue
                mealDao.updateMeal(meal.copy(userId = newUserId))
            }
            Log.d(TAG, "Patched userId on meals")

            // ------------------------------------------------------------------
            // 6. Import meal_food_items — straight mapping, no deduplication.
            // ------------------------------------------------------------------
            var mfiSkipped = 0
            val batchMfi = mutableListOf<MealFoodItem>()
            for ((oldMealId, items) in mfiByMealId) {
                val newMealId = mealIdMap[oldMealId]
                if (newMealId == null) { mfiSkipped += items.size; continue }
                for (fi in items) {
                    val newFoodId = foodIdMap[fi.getLong("foodId")]
                    if (newFoodId == null) { mfiSkipped++; continue }
                    val unit = fi.optString("unit", "GRAM").let { str ->
                        runCatching { FoodUnit.valueOf(str) }.getOrDefault(FoodUnit.GRAM)
                    }
                    batchMfi.add(MealFoodItem(
                        mealId = newMealId,
                        foodId = newFoodId,
                        quantity = fi.getDouble("quantity"),
                        unit = unit,
                        notes = fi.optString("notes", null)
                    ))
                }
            }
            mealDao.insertMealFoodItems(batchMfi)
            Log.d(TAG, "Inserted ${batchMfi.size} meal_food_items (skipped $mfiSkipped unmapped)")

            // ------------------------------------------------------------------
            // 7. Import diets — now user-scoped (no name deduplication).
            //    Each user gets their own diet records with their own userId.
            // ------------------------------------------------------------------
            val dietIdMap = mutableMapOf<Long, Long>()
            for (d in backupDiets) {
                val oldId = d.getLong("id")
                val backupUserId = backupDietIdToBackupUserId[oldId]
                val newUserId = backupUserId?.let { userIdMap[it] }
                val newId = dietDao.insertDiet(
                    Diet(
                        name = d.getString("name"),
                        description = d.optString("description", null),
                        isSystem = false,
                        userId = newUserId,
                        isFavourite = d.getInt("isFavourite") == 1,
                        createdAt = d.optLong("createdAt", System.currentTimeMillis())
                    )
                )
                dietIdMap[oldId] = newId
            }
            Log.d(TAG, "Inserted ${dietIdMap.size} diets (one per backup record)")

            // ------------------------------------------------------------------
            // 8. Import diet_meals → diet_slots using remapped diet + meal IDs
            // ------------------------------------------------------------------
            var dmSkipped = 0
            for (dm in backupDietMeals) {
                val oldDietId = dm.getLong("dietId")
                val oldMealId = dm.optLong("mealId", -1L)
                val newDietId = dietIdMap[oldDietId]
                if (newDietId == null) { dmSkipped++; continue }
                val newMealId = if (oldMealId > 0) mealIdMap[oldMealId] else null
                dietDao.insertDietMeal(DietMeal(
                    dietId = newDietId,
                    slotType = dm.getString("slotType"),
                    mealId = newMealId,
                    instructions = dm.optString("instructions", null)
                ))
            }
            Log.d(TAG, "Inserted ${backupDietMeals.size - dmSkipped} diet_slots (skipped $dmSkipped unmapped)")

            // ------------------------------------------------------------------
            // 9. Import diet_tags
            // ------------------------------------------------------------------
            var dtSkipped = 0
            val dietTagsArr = root.getJSONArray("diet_tags")
            val seenDietTags = mutableSetOf<Pair<Long, Long>>()
            for (i in 0 until dietTagsArr.length()) {
                val dt = dietTagsArr.getJSONObject(i)
                val newDietId = dietIdMap[dt.getLong("dietId")]
                val newTagId = tagIdMap[dt.getLong("tagId")]
                if (newDietId == null || newTagId == null) { dtSkipped++; continue }
                val key = Pair(newDietId, newTagId)
                if (key in seenDietTags) continue
                seenDietTags.add(key)
                tagDao.insertDietTag(DietTagCrossRef(newDietId, newTagId))
            }
            Log.d(TAG, "Inserted ${seenDietTags.size} diet_tags (skipped $dtSkipped unmapped)")

            // ------------------------------------------------------------------
            // 10. Import user-specific data for each user
            //     plans, daily_logs, logged_foods, health_metrics, grocery_lists
            // ------------------------------------------------------------------
            importUserSpecificData(root, userIdMap, dietIdMap, foodIdMap)

            // ------------------------------------------------------------------
            // Mark done
            // ------------------------------------------------------------------
            context.dataStore.edit { it[BACKUP_IMPORTED_KEY] = true }
            Log.i(TAG, "Backup import completed successfully ✓")

        } catch (e: Exception) {
            Log.e(TAG, "Backup import failed", e)
            // Do NOT set the flag — will retry on next launch
        }
    }

    private suspend fun importUserSpecificData(
        root: JSONObject,
        userIdMap: Map<Long, Long>,
        dietIdMap: Map<Long, Long>,
        foodIdMap: Map<Long, Long>
    ) {
        // Plans
        var plansInserted = 0
        val plansArr = root.getJSONArray("plans")
        for (i in 0 until plansArr.length()) {
            val p = plansArr.getJSONObject(i)
            val newUserId = userIdMap[p.getLong("userId")] ?: continue
            val oldDietId = p.optLong("dietId", -1L)
            val newDietId = if (oldDietId > 0) dietIdMap[oldDietId] else null
            planDao.upsertPlan(Plan(
                userId = newUserId,
                date = p.getLong("date"),
                dietId = newDietId,
                notes = p.optString("notes", null),
                isCompleted = p.getInt("isCompleted") == 1
            ))
            plansInserted++
        }
        Log.d(TAG, "Inserted $plansInserted plans")

        // Daily logs
        var logsInserted = 0
        val dailyLogsArr = root.getJSONArray("daily_logs")
        for (i in 0 until dailyLogsArr.length()) {
            val l = dailyLogsArr.getJSONObject(i)
            val newUserId = userIdMap[l.getLong("userId")] ?: continue
            val oldDietId = l.optLong("plannedDietId", -1L)
            val newDietId = if (oldDietId > 0) dietIdMap[oldDietId] else null
            dailyLogDao.insertLog(DailyLog(
                userId = newUserId,
                date = l.getLong("date"),
                plannedDietId = newDietId,
                notes = l.optString("notes", null),
                createdAt = l.optLong("createdAt", System.currentTimeMillis())
            ))
            logsInserted++
        }
        Log.d(TAG, "Inserted $logsInserted daily_logs")

        // Logged foods
        var lfInserted = 0
        var lfSkipped = 0
        val loggedFoodsArr = root.getJSONArray("logged_foods")
        val batchLf = mutableListOf<LoggedFood>()
        for (i in 0 until loggedFoodsArr.length()) {
            val lf = loggedFoodsArr.getJSONObject(i)
            val newUserId = userIdMap[lf.getLong("userId")]
            val newFoodId = foodIdMap[lf.getLong("foodId")]
            if (newUserId == null || newFoodId == null) { lfSkipped++; continue }
            val unit = lf.optString("unit", "GRAM").let { str ->
                runCatching { FoodUnit.valueOf(str) }.getOrDefault(FoodUnit.GRAM)
            }
            batchLf.add(LoggedFood(
                userId = newUserId,
                logDate = lf.getLong("logDate"),
                foodId = newFoodId,
                quantity = lf.getDouble("quantity"),
                unit = unit,
                slotType = lf.getString("slotType"),
                timestamp = lf.optLongOrNull("timestamp"),
                notes = lf.optString("notes", null)
            ))
            lfInserted++
        }
        dailyLogDao.insertLoggedFoods(batchLf)
        Log.d(TAG, "Inserted $lfInserted logged_foods (skipped $lfSkipped unmapped)")

        // Health metrics
        var hmInserted = 0
        val healthMetricsArr = root.getJSONArray("health_metrics")
        for (i in 0 until healthMetricsArr.length()) {
            val hm = healthMetricsArr.getJSONObject(i)
            val newUserId = userIdMap[hm.getLong("userId")] ?: continue
            healthMetricDao.insertMetric(HealthMetric(
                userId = newUserId,
                date = hm.getLong("date"),
                timestamp = hm.optLong("timestamp", System.currentTimeMillis()),
                metricType = hm.optString("metricType", null),
                customTypeId = hm.optLongOrNull("customTypeId"),
                value = hm.getDouble("value"),
                secondaryValue = hm.optDoubleOrNull("secondaryValue"),
                subType = hm.optString("subType", null),
                notes = hm.optString("notes", null)
            ))
            hmInserted++
        }
        Log.d(TAG, "Inserted $hmInserted health_metrics")

        // Grocery lists + items
        val groceryListIdMap = mutableMapOf<Long, Long>()
        val groceryListsArr = root.getJSONArray("grocery_lists")
        for (i in 0 until groceryListsArr.length()) {
            val gl = groceryListsArr.getJSONObject(i)
            val newUserId = userIdMap[gl.getLong("userId")] ?: continue
            val newListId = groceryDao.insertList(GroceryList(
                userId = newUserId,
                name = gl.getString("name"),
                startDate = gl.optLongOrNull("startDate"),
                endDate = gl.optLongOrNull("endDate"),
                createdAt = gl.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = gl.optLong("updatedAt", System.currentTimeMillis())
            ))
            groceryListIdMap[gl.getLong("id")] = newListId
        }

        val groceryItemsArr = root.getJSONArray("grocery_items")
        val batchGi = mutableListOf<GroceryItem>()
        var giSkipped = 0
        for (i in 0 until groceryItemsArr.length()) {
            val gi = groceryItemsArr.getJSONObject(i)
            val newListId = groceryListIdMap[gi.getLong("listId")]
            if (newListId == null) { giSkipped++; continue }
            val oldFoodId = gi.optLong("foodId", -1L)
            val newFoodId = if (oldFoodId > 0) foodIdMap[oldFoodId] else null
            val unit = gi.optString("unit", "GRAM").let { str ->
                runCatching { FoodUnit.valueOf(str) }.getOrDefault(FoodUnit.GRAM)
            }
            batchGi.add(GroceryItem(
                listId = newListId,
                foodId = newFoodId,
                customName = gi.optString("customName", null),
                quantity = gi.getDouble("quantity"),
                unit = unit,
                isChecked = gi.getInt("isChecked") == 1,
                sortOrder = gi.optInt("sortOrder", i),
                category = gi.optString("category", null)
            ))
        }
        groceryDao.insertItems(batchGi)
        Log.d(TAG, "Inserted ${batchGi.size} grocery_items (skipped $giSkipped unmapped)")
    }

    // --- JSONObject extension helpers ---

    private fun JSONObject.optString(key: String, default: String?): String? =
        if (isNull(key)) default else optString(key)

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (isNull(key) || !has(key)) null else optInt(key)

    private fun JSONObject.optDoubleOrNull(key: String): Double? =
        if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private fun JSONObject.optLongOrNull(key: String): Long? =
        if (isNull(key) || !has(key)) null else optLong(key)

    private fun JSONArray.toObjectList(): List<JSONObject> =
        (0 until length()).map { getJSONObject(it) }
}
