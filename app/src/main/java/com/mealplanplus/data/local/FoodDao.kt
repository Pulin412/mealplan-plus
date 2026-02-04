package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodById(id: Long): FoodItem?

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%'")
    fun searchFoods(query: String): Flow<List<FoodItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodById(id: Long)
}
