package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietFullSummary
import com.mealplanplus.data.model.DietMeal
import kotlinx.coroutines.flow.Flow

/**
 * Flat row returned by [DietDao.getFoodNamesForDiets].
 * Carries `dietId` so a single batch query can cover all diets at once.
 */
data class DietFoodNameRow(val dietId: Long, val foodName: String)

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

    // Diet slots (formerly diet_meals)
    @Query("SELECT * FROM diet_slots WHERE dietId = :dietId")
    suspend fun getDietMeals(dietId: Long): List<DietMeal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietMeal(dietMeal: DietMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietMeals(dietMeals: List<DietMeal>)

    @Query("DELETE FROM diet_slots WHERE dietId = :dietId")
    suspend fun clearDietMeals(dietId: Long)

    @Query("DELETE FROM diet_slots WHERE dietId = :dietId AND slotType = :slotType")
    suspend fun removeMealFromDiet(dietId: Long, slotType: String)

    @Query("UPDATE diet_slots SET mealId = :mealId WHERE dietId = :dietId AND slotType = :slotType")
    suspend fun updateDietMeal(dietId: Long, slotType: String, mealId: Long?)

    @Query("SELECT * FROM diet_slots WHERE dietId = :dietId AND slotType = :slotType LIMIT 1")
    suspend fun getDietMeal(dietId: Long, slotType: String): DietMeal?

    @Query("UPDATE diet_slots SET instructions = :instructions WHERE dietId = :dietId AND slotType = :slotType")
    suspend fun updateDietMealInstructions(dietId: Long, slotType: String, instructions: String?)

    @Query("DELETE FROM diet_slots")
    suspend fun deleteAllDietMeals()

    @Query("DELETE FROM diets")
    suspend fun deleteAllDiets()

    @Query("SELECT COUNT(*) FROM diets")
    suspend fun getDietCount(): Int

    @Query("UPDATE diets SET isFavourite = :isFavourite WHERE id = :id")
    suspend fun setFavourite(id: Long, isFavourite: Boolean)

    @Query("SELECT * FROM diets WHERE isFavourite = 1 ORDER BY name ASC")
    fun getFavouriteDiets(): Flow<List<Diet>>

    /**
     * All diets with full macro summary in a single query.
     */
    @Query("""
        SELECT
            d.id, d.name, d.description, d.createdAt,
            COUNT(DISTINCT ds.slotType) as mealCount,
            COALESCE(SUM(
                CASE WHEN mfi.unit = 'GRAM' THEN f.caloriesPer100 * mfi.quantity / 100
                     WHEN mfi.unit = 'PIECE' THEN f.caloriesPer100 * COALESCE(f.gramsPerPiece, 100) * mfi.quantity / 100
                     WHEN mfi.unit = 'CUP' THEN f.caloriesPer100 * COALESCE(f.gramsPerCup, 240) * mfi.quantity / 100
                     WHEN mfi.unit = 'TBSP' THEN f.caloriesPer100 * COALESCE(f.gramsPerTbsp, 15) * mfi.quantity / 100
                     WHEN mfi.unit = 'TSP' THEN f.caloriesPer100 * COALESCE(f.gramsPerTsp, 5) * mfi.quantity / 100
                     ELSE f.caloriesPer100 * mfi.quantity / 100
                END
            ), 0) as totalCalories,
            COALESCE(SUM(
                CASE WHEN mfi.unit = 'GRAM' THEN f.proteinPer100 * mfi.quantity / 100
                     WHEN mfi.unit = 'PIECE' THEN f.proteinPer100 * COALESCE(f.gramsPerPiece, 100) * mfi.quantity / 100
                     WHEN mfi.unit = 'CUP' THEN f.proteinPer100 * COALESCE(f.gramsPerCup, 240) * mfi.quantity / 100
                     WHEN mfi.unit = 'TBSP' THEN f.proteinPer100 * COALESCE(f.gramsPerTbsp, 15) * mfi.quantity / 100
                     WHEN mfi.unit = 'TSP' THEN f.proteinPer100 * COALESCE(f.gramsPerTsp, 5) * mfi.quantity / 100
                     ELSE f.proteinPer100 * mfi.quantity / 100
                END
            ), 0) as totalProtein,
            COALESCE(SUM(
                CASE WHEN mfi.unit = 'GRAM' THEN f.carbsPer100 * mfi.quantity / 100
                     WHEN mfi.unit = 'PIECE' THEN f.carbsPer100 * COALESCE(f.gramsPerPiece, 100) * mfi.quantity / 100
                     WHEN mfi.unit = 'CUP' THEN f.carbsPer100 * COALESCE(f.gramsPerCup, 240) * mfi.quantity / 100
                     WHEN mfi.unit = 'TBSP' THEN f.carbsPer100 * COALESCE(f.gramsPerTbsp, 15) * mfi.quantity / 100
                     WHEN mfi.unit = 'TSP' THEN f.carbsPer100 * COALESCE(f.gramsPerTsp, 5) * mfi.quantity / 100
                     ELSE f.carbsPer100 * mfi.quantity / 100
                END
            ), 0) as totalCarbs,
            COALESCE(SUM(
                CASE WHEN mfi.unit = 'GRAM' THEN f.fatPer100 * mfi.quantity / 100
                     WHEN mfi.unit = 'PIECE' THEN f.fatPer100 * COALESCE(f.gramsPerPiece, 100) * mfi.quantity / 100
                     WHEN mfi.unit = 'CUP' THEN f.fatPer100 * COALESCE(f.gramsPerCup, 240) * mfi.quantity / 100
                     WHEN mfi.unit = 'TBSP' THEN f.fatPer100 * COALESCE(f.gramsPerTbsp, 15) * mfi.quantity / 100
                     WHEN mfi.unit = 'TSP' THEN f.fatPer100 * COALESCE(f.gramsPerTsp, 5) * mfi.quantity / 100
                     ELSE f.fatPer100 * mfi.quantity / 100
                END
            ), 0) as totalFat,
            SUM(CASE WHEN f.glycemicIndex IS NOT NULL THEN
                f.glycemicIndex * (
                    CASE WHEN mfi.unit = 'GRAM' THEN f.carbsPer100 * mfi.quantity / 100.0
                         WHEN mfi.unit = 'PIECE' THEN f.carbsPer100 * COALESCE(f.gramsPerPiece, 100) * mfi.quantity / 100.0
                         WHEN mfi.unit = 'CUP' THEN f.carbsPer100 * COALESCE(f.gramsPerCup, 240) * mfi.quantity / 100.0
                         WHEN mfi.unit = 'TBSP' THEN f.carbsPer100 * COALESCE(f.gramsPerTbsp, 15) * mfi.quantity / 100.0
                         WHEN mfi.unit = 'TSP' THEN f.carbsPer100 * COALESCE(f.gramsPerTsp, 5) * mfi.quantity / 100.0
                         ELSE f.carbsPer100 * mfi.quantity / 100.0
                    END
                ) / 100.0
            ELSE NULL END) as totalGlycemicLoad,
            d.isFavourite
        FROM diets d
        LEFT JOIN diet_slots ds ON d.id = ds.dietId
        LEFT JOIN meals m ON ds.mealId = m.id
        LEFT JOIN meal_food_items mfi ON m.id = mfi.mealId
        LEFT JOIN food_items f ON mfi.foodId = f.id
        GROUP BY d.id
        ORDER BY d.name
    """)
    fun getDietsWithFullSummary(): Flow<List<DietFullSummary>>

    // ─── Batch enrichment helpers ────────────────────────────────────────────

    /**
     * Returns every food name that appears in any meal slot of the given diets.
     * One query instead of one per diet.
     */
    @Query("""
        SELECT ds.dietId, f.name as foodName
        FROM diet_slots ds
        JOIN meals m ON ds.mealId = m.id
        JOIN meal_food_items mfi ON m.id = mfi.mealId
        JOIN food_items f ON mfi.foodId = f.id
        WHERE ds.dietId IN (:dietIds)
    """)
    suspend fun getFoodNamesForDiets(dietIds: List<Long>): List<DietFoodNameRow>

    /**
     * Returns all diet_slot rows for the given diets in a single query.
     */
    @Query("SELECT * FROM diet_slots WHERE dietId IN (:dietIds)")
    suspend fun getDietMealsForDiets(dietIds: List<Long>): List<DietMeal>

    // Sync helpers
    @Query("SELECT * FROM diets WHERE syncedAt IS NULL OR updatedAt > syncedAt")
    suspend fun getUnsyncedDiets(): List<Diet>

    @Query("SELECT * FROM diets WHERE serverId = :serverId LIMIT 1")
    suspend fun getDietByServerId(serverId: String): Diet?
}
