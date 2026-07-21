package com.mealplanplus.data.repository

import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only meals. Reads combine the meals table with the food cache so each meal's items
 * resolve to real names + macros (see [Meal.resolve]). Writes are optimistic + marked dirty
 * for [com.mealplanplus.data.sync.SyncManager].
 */
@Singleton
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
) {
    fun getMeals(): Flow<List<MealUi>> =
        combine(mealDao.getAllMeals(), foodDao.getAllFoods()) { meals, foods ->
            val byId = foods.associateBy { it.id }
            meals.map { it.resolve(byId) }
        }

    suspend fun create(name: String, slots: List<String>, items: List<MealItem>) {
        mealDao.upsert(Meal(name = name.trim(), slots = slots, items = items, dirty = true))
    }

    /** Overwrite an existing meal's name/slots/items (keeps id + favourite); marks dirty. */
    suspend fun update(existing: Meal, name: String, slots: List<String>, items: List<MealItem>) {
        mealDao.upsert(existing.copy(
            name = name.trim(), slots = slots, items = items,
            updatedAt = System.currentTimeMillis(), dirty = true,
        ))
    }

    suspend fun toggleFavorite(meal: Meal) {
        mealDao.upsert(
            meal.copy(isFavorite = !meal.isFavorite, updatedAt = System.currentTimeMillis(), dirty = true)
        )
    }

    suspend fun delete(meal: Meal) {
        val now = System.currentTimeMillis()
        mealDao.upsert(meal.copy(deletedAt = now, updatedAt = now, dirty = true))
    }
}
