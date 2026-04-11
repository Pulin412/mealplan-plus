package com.mealplanplus.data.repository

import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val foodDao: FoodDao
) {
    fun getAllMeals(): Flow<List<Meal>> = mealDao.getAllMeals()

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

    suspend fun insertMeal(meal: Meal): Long = mealDao.insertMeal(meal)

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

    suspend fun getMealFoodItems(mealId: Long): List<MealFoodItem> = mealDao.getMealFoodItems(mealId)

    suspend fun updateMealFoodItem(item: MealFoodItem) {
        mealDao.insertMealFoodItem(item) // Uses REPLACE strategy
    }
}
