package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface DietDao {
    @Query("SELECT * FROM diets ORDER BY name ASC")
    fun getAllDiets(): Flow<List<Diet>>

    @Query("SELECT * FROM diets WHERE id = :id")
    suspend fun getDietById(id: Long): Diet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiet(diet: Diet): Long

    @Update
    suspend fun updateDiet(diet: Diet)

    @Delete
    suspend fun deleteDiet(diet: Diet)

    // Diet meals
    @Query("SELECT * FROM diet_meals WHERE dietId = :dietId")
    suspend fun getDietMeals(dietId: Long): List<DietMeal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietMeal(dietMeal: DietMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietMeals(dietMeals: List<DietMeal>)

    @Query("DELETE FROM diet_meals WHERE dietId = :dietId")
    suspend fun clearDietMeals(dietId: Long)

    @Query("DELETE FROM diet_meals WHERE dietId = :dietId AND slotType = :slotType")
    suspend fun removeMealFromDiet(dietId: Long, slotType: String)

    @Query("UPDATE diet_meals SET mealId = :mealId WHERE dietId = :dietId AND slotType = :slotType")
    suspend fun updateDietMeal(dietId: Long, slotType: String, mealId: Long?)

    @Query("DELETE FROM diet_meals")
    suspend fun deleteAllDietMeals()

    @Query("DELETE FROM diets")
    suspend fun deleteAllDiets()

    @Query("SELECT COUNT(*) FROM diets")
    suspend fun getDietCount(): Int
}
