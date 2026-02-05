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

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavorites(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE lastUsed IS NOT NULL ORDER BY lastUsed DESC LIMIT :limit")
    fun getRecentFoods(limit: Int = 20): Flow<List<FoodItem>>

    @Query("UPDATE food_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE food_items SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long

    @Update
    suspend fun updateFood(food: FoodItem)

    @Delete
    suspend fun deleteFood(food: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteFoodById(id: Long)
}
