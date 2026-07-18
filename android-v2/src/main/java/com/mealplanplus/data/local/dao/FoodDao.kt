package com.mealplanplus.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mealplanplus.data.model.Food
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {

    @Query("SELECT * FROM foods ORDER BY updatedAt DESC")
    fun getAllFoods(): Flow<List<Food>>

    @Query(
        "SELECT * FROM foods WHERE " +
        "name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' " +
        "ORDER BY updatedAt DESC"
    )
    fun searchFoods(query: String): Flow<List<Food>>

    @Upsert
    suspend fun upsertAll(foods: List<Food>)

    @Upsert
    suspend fun upsert(food: Food): Long

    @Delete
    suspend fun delete(food: Food)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE foods SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)
}
