package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.MealFoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMeals(): Flow<List<Meal>>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): Meal?

    @Query("SELECT * FROM meals WHERE slotType = :slotType ORDER BY name ASC")
    fun getMealsBySlot(slotType: String): Flow<List<Meal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    // Meal food items
    @Query("SELECT * FROM meal_food_items WHERE mealId = :mealId")
    suspend fun getMealFoodItems(mealId: Long): List<MealFoodItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodItem(item: MealFoodItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodItems(items: List<MealFoodItem>)

    @Delete
    suspend fun deleteMealFoodItem(item: MealFoodItem)

    @Query("DELETE FROM meal_food_items WHERE mealId = :mealId")
    suspend fun clearMealFoodItems(mealId: Long)

    @Query("DELETE FROM meal_food_items WHERE mealId = :mealId AND foodId = :foodId")
    suspend fun removeFoodFromMeal(mealId: Long, foodId: Long)

    @Query("DELETE FROM meal_food_items")
    suspend fun deleteAllMealFoodItems()

    @Query("DELETE FROM meals")
    suspend fun deleteAllMeals()
}
