package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val foodDao: FoodDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getMealsByUser(): Flow<List<Meal>> = mealDao.getMealsByUser(getCurrentUserId())

    fun getMealsByUserAndSlot(slotType: String): Flow<List<Meal>> =
        mealDao.getMealsByUserAndSlot(getCurrentUserId(), slotType)

    suspend fun getMealById(id: Long): Meal? = mealDao.getMealById(id)

    suspend fun getMealWithFoods(mealId: Long): MealWithFoods? {
        val meal = mealDao.getMealById(mealId) ?: return null
        val mealFoodItems = mealDao.getMealFoodItems(mealId)
        val items = mealFoodItems.mapNotNull { mfi ->
            foodDao.getFoodById(mfi.foodId)?.let { food ->
                MealFoodItemWithDetails(mfi, food)
            }
        }
        return MealWithFoods(meal, items)
    }

    suspend fun insertMeal(meal: Meal): Long {
        val mealWithUserId = meal.copy(userId = getCurrentUserId())
        return mealDao.insertMeal(mealWithUserId)
    }

    suspend fun updateMeal(meal: Meal) = mealDao.updateMeal(meal)

    suspend fun deleteMeal(meal: Meal) = mealDao.deleteMeal(meal)

    suspend fun addFoodToMeal(mealId: Long, foodId: Long, quantity: Double, unit: FoodUnit = FoodUnit.GRAM) {
        mealDao.insertMealFoodItem(MealFoodItem(mealId, foodId, quantity, unit))
    }

    suspend fun removeFoodFromMeal(mealId: Long, foodId: Long) {
        mealDao.removeFoodFromMeal(mealId, foodId)
    }

    suspend fun updateMealFoods(mealId: Long, items: List<MealFoodItem>) {
        mealDao.clearMealFoodItems(mealId)
        mealDao.insertMealFoodItems(items)
    }

    suspend fun getMealFoodItems(mealId: Long): List<MealFoodItem> {
        return mealDao.getMealFoodItems(mealId)
    }

    suspend fun updateMealFoodItem(item: MealFoodItem) {
        mealDao.insertMealFoodItem(item) // Uses REPLACE strategy
    }
}
