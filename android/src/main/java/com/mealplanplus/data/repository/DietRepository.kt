package com.mealplanplus.data.repository

import com.mealplanplus.data.local.dao.DietDao
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietEntry
import com.mealplanplus.data.model.DietTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only diets. Reads combine the diets table with the meal + food caches so each
 * entry resolves to a real name + macros (see [Diet.resolve]). Writes are optimistic +
 * marked dirty for [com.mealplanplus.data.sync.SyncManager].
 */
@Singleton
class DietRepository @Inject constructor(
    private val dietDao: DietDao,
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
) {
    fun getDiets(): Flow<List<DietUi>> =
        combine(dietDao.getAllDiets(), mealDao.getAllMeals(), foodDao.getAllFoods()) { diets, meals, foods ->
            val foodsById = foods.associateBy { it.id }
            val mealsById = meals.associate { it.id to it.resolve(foodsById) }
            diets.map { it.resolve(mealsById, foodsById) }
        }

    suspend fun create(name: String, entries: List<DietEntry>, tags: List<DietTag> = emptyList(), targetCalories: Double? = null, description: String? = null) {
        dietDao.upsert(Diet(name = name.trim(), description = description?.trim()?.ifBlank { null }, entries = entries, tags = tags, targetCalories = targetCalories, dirty = true))
    }

    /** Overwrite an existing diet's name/notes/entries/tags (keeps id + favourite); marks dirty. */
    suspend fun update(existing: Diet, name: String, entries: List<DietEntry>, tags: List<DietTag>, description: String? = null) {
        dietDao.upsert(existing.copy(
            name = name.trim(), description = description?.trim()?.ifBlank { null }, entries = entries, tags = tags,
            updatedAt = System.currentTimeMillis(), dirty = true,
        ))
    }

    suspend fun toggleFavorite(diet: Diet) {
        dietDao.upsert(
            diet.copy(isFavorite = !diet.isFavorite, updatedAt = System.currentTimeMillis(), dirty = true)
        )
    }

    suspend fun delete(diet: Diet) {
        val now = System.currentTimeMillis()
        dietDao.upsert(diet.copy(deletedAt = now, updatedAt = now, dirty = true))
    }
}
