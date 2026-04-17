package com.mealplanplus.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.mealplanplus.data.model.DietMeal
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealFoodItem
import com.mealplanplus.util.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class RestoreResult(
    val mealsImported: Int,
    val foodItemsLinked: Int,
    val dietSlotsLinked: Int,
    val skipped: Int
)

/**
 * One-time importer that reads the v26 JSON backup bundled in assets/user_backup.json
 * and inserts all missing user-created meals + diet-slot assignments into the v30 schema.
 *
 * Safe to call multiple times — a DataStore flag prevents re-import after first success.
 * Food IDs are resolved by name (not raw ID) to handle the v28→v29 deduplication change.
 */
@Singleton
class BackupRestoreManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
) {
    companion object {
        private val KEY_BACKUP_RESTORED = booleanPreferencesKey("user_backup_v26_restored")
    }

    suspend fun isAlreadyRestored(): Boolean =
        context.dataStore.data.first()[KEY_BACKUP_RESTORED] == true

    /**
     * Performs the full restore. Returns null if backup was already applied.
     */
    suspend fun restoreFromBackup(): RestoreResult? {
        if (isAlreadyRestored()) return null

        val json = context.assets.open("user_backup.json").bufferedReader().readText()
        val root = JSONObject(json)

        // ── Step 1: build lookup maps for current DB state ───────────────────
        val currentFoods = foodDao.getAllFoodsOnce()
        val foodNameToId: Map<String, Long> = currentFoods.associate { it.name to it.id }

        val currentMeals = mealDao.getAllMealsOnce()
        val existingMealNames: Set<String> = currentMeals.map { it.name }.toSet()

        val currentDiets = dietDao.getAllDietsOnce()
        val dietNameToId: Map<String, Long> = currentDiets.associate { it.name to it.id }

        // ── Step 2: parse backup food_items for id→name reverse lookup ───────
        val backupFoodIdToName = mutableMapOf<Long, String>()
        val foodsArray = root.getJSONArray("food_items")
        for (i in 0 until foodsArray.length()) {
            val o = foodsArray.getJSONObject(i)
            backupFoodIdToName[o.getLong("id")] = o.getString("name")
        }

        // ── Step 3: parse backup diets for id→name lookup ────────────────────
        val backupDietIdToName = mutableMapOf<Long, String>()
        val dietsArray = root.getJSONArray("diets")
        for (i in 0 until dietsArray.length()) {
            val o = dietsArray.getJSONObject(i)
            backupDietIdToName[o.getLong("id")] = o.getString("name")
        }

        // ── Step 4: import meals ──────────────────────────────────────────────
        // oldId → newId mapping (includes meals that already exist by name)
        val oldMealIdToNewId = mutableMapOf<Long, Long>()
        var mealsImported = 0
        var skipped = 0

        val mealsArray = root.getJSONArray("meals")
        for (i in 0 until mealsArray.length()) {
            val o = mealsArray.getJSONObject(i)
            val oldId = o.getLong("id")
            val name = o.getString("name")
            val description = if (o.isNull("description")) null else o.getString("description")
            val createdAt = o.getLong("createdAt")

            if (name in existingMealNames) {
                // Map to existing meal so we can still wire food items
                val existingId = currentMeals.first { it.name == name }.id
                oldMealIdToNewId[oldId] = existingId
                skipped++
                continue
            }

            val newId = mealDao.insertMeal(
                Meal(
                    name = name,
                    description = description,
                    isSystem = false,
                    createdAt = createdAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
            oldMealIdToNewId[oldId] = newId
            mealsImported++
        }

        // ── Step 5: import meal_food_items ────────────────────────────────────
        var foodItemsLinked = 0
        val mfiArray = root.getJSONArray("meal_food_items")
        for (i in 0 until mfiArray.length()) {
            val o = mfiArray.getJSONObject(i)
            val oldMealId = o.getLong("mealId")
            val oldFoodId = o.getLong("foodId")
            val quantity = o.getDouble("quantity")
            val unitStr = if (o.has("unit") && !o.isNull("unit")) o.getString("unit") else "GRAM"
            val notes = if (o.has("notes") && !o.isNull("notes")) o.getString("notes") else null

            val newMealId = oldMealIdToNewId[oldMealId] ?: continue
            val foodName = backupFoodIdToName[oldFoodId] ?: continue
            val currentFoodId = foodNameToId[foodName] ?: continue

            val unit = runCatching { FoodUnit.valueOf(unitStr) }.getOrDefault(FoodUnit.GRAM)

            mealDao.insertMealFoodItem(
                MealFoodItem(
                    mealId = newMealId,
                    foodId = currentFoodId,
                    quantity = quantity,
                    unit = unit,
                    notes = notes
                )
            )
            foodItemsLinked++
        }

        // ── Step 6: import diet_meals → diet_slots ────────────────────────────
        var dietSlotsLinked = 0
        val dietMealsArray = root.getJSONArray("diet_meals")
        for (i in 0 until dietMealsArray.length()) {
            val o = dietMealsArray.getJSONObject(i)
            val oldDietId = o.getLong("dietId")
            val slotType = o.getString("slotType")
            val oldMealId = if (o.isNull("mealId")) null else o.getLong("mealId")

            val dietName = backupDietIdToName[oldDietId] ?: continue
            val currentDietId = dietNameToId[dietName] ?: continue
            val newMealId = oldMealId?.let { oldMealIdToNewId[it] }

            // Only insert if slot not already filled (INSERT OR IGNORE semantics)
            val existing = dietDao.getDietMeal(currentDietId, slotType)
            if (existing == null) {
                dietDao.insertDietMeal(
                    DietMeal(
                        dietId = currentDietId,
                        slotType = slotType,
                        mealId = newMealId,
                        instructions = null
                    )
                )
                dietSlotsLinked++
            }
        }

        // ── Step 7: mark as done ──────────────────────────────────────────────
        context.dataStore.edit { it[KEY_BACKUP_RESTORED] = true }

        return RestoreResult(
            mealsImported = mealsImported,
            foodItemsLinked = foodItemsLinked,
            dietSlotsLinked = dietSlotsLinked,
            skipped = skipped
        )
    }
}
