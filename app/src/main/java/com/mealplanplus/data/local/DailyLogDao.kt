package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.DailyLog
import com.mealplanplus.data.model.DailyLogSlotOverride
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.LoggedFood
import com.mealplanplus.data.model.LoggedMeal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLogDao {
    @Query("SELECT * FROM daily_logs WHERE userId = :userId ORDER BY date DESC")
    fun getLogsByUser(userId: Long): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date")
    suspend fun getLogByDate(userId: Long, date: String): DailyLog?

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date = :date")
    fun getLogByDateFlow(userId: Long, date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getLogsBetweenDates(userId: Long, startDate: String, endDate: String): Flow<List<DailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Update
    suspend fun updateLog(log: DailyLog)

    @Delete
    suspend fun deleteLog(log: DailyLog)

    // Logged foods
    @Query("SELECT * FROM logged_foods WHERE userId = :userId AND logDate = :date ORDER BY slotType, timestamp")
    fun getLoggedFoods(userId: Long, date: String): Flow<List<LoggedFood>>

    @Query("SELECT * FROM logged_foods WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun getLoggedFoodsForSlot(userId: Long, date: String, slotType: String): List<LoggedFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedFood(food: LoggedFood): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedFoods(foods: List<LoggedFood>)

    @Update
    suspend fun updateLoggedFood(food: LoggedFood)

    @Delete
    suspend fun deleteLoggedFood(food: LoggedFood)

    @Query("DELETE FROM logged_foods WHERE id = :id")
    suspend fun deleteLoggedFoodById(id: Long)

    @Query("DELETE FROM logged_foods WHERE userId = :userId AND logDate = :date")
    suspend fun clearLoggedFoods(userId: Long, date: String)

    @Query("DELETE FROM logged_foods WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun clearLoggedFoodsForSlot(userId: Long, date: String, slotType: String)

    // Logged meals
    @Query("SELECT * FROM logged_meals WHERE userId = :userId AND logDate = :date ORDER BY slotType, timestamp")
    fun getLoggedMeals(userId: Long, date: String): Flow<List<LoggedMeal>>

    @Query("SELECT * FROM logged_meals WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun getLoggedMealsForSlot(userId: Long, date: String, slotType: String): List<LoggedMeal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedMeal(meal: LoggedMeal): Long

    @Update
    suspend fun updateLoggedMeal(meal: LoggedMeal)

    @Delete
    suspend fun deleteLoggedMeal(meal: LoggedMeal)

    @Query("DELETE FROM logged_meals WHERE id = :id")
    suspend fun deleteLoggedMealById(id: Long)

    @Query("DELETE FROM logged_meals WHERE userId = :userId AND logDate = :date")
    suspend fun clearLoggedMeals(userId: Long, date: String)

    @Query("DELETE FROM logged_meals WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun clearLoggedMealsForSlot(userId: Long, date: String, slotType: String)

    // Slot overrides
    @Query("SELECT * FROM daily_log_slot_overrides WHERE userId = :userId AND logDate = :date")
    fun getSlotOverrides(userId: Long, date: String): Flow<List<DailyLogSlotOverride>>

    @Query("SELECT * FROM daily_log_slot_overrides WHERE userId = :userId AND logDate = :date")
    suspend fun getSlotOverridesList(userId: Long, date: String): List<DailyLogSlotOverride>

    @Query("SELECT * FROM daily_log_slot_overrides WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun getSlotOverride(userId: Long, date: String, slotType: String): DailyLogSlotOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlotOverride(override: DailyLogSlotOverride)

    @Delete
    suspend fun deleteSlotOverride(override: DailyLogSlotOverride)

    @Query("DELETE FROM daily_log_slot_overrides WHERE userId = :userId AND logDate = :date AND slotType = :slotType")
    suspend fun deleteSlotOverrideBySlot(userId: Long, date: String, slotType: String)

    @Query("DELETE FROM daily_log_slot_overrides WHERE userId = :userId AND logDate = :date")
    suspend fun clearSlotOverrides(userId: Long, date: String)

    // Chart data - daily macro totals
    @Query("""
        SELECT
            lm.logDate as date,
            COALESCE(SUM(
                (f.caloriesPer100 / 100.0) *
                CASE mfi.unit
                    WHEN 'GRAM' THEN mfi.quantity
                    WHEN 'PIECE' THEN mfi.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN mfi.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN mfi.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN mfi.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE mfi.quantity
                END * lm.quantity
            ), 0) as calories,
            COALESCE(SUM(
                (f.proteinPer100 / 100.0) *
                CASE mfi.unit
                    WHEN 'GRAM' THEN mfi.quantity
                    WHEN 'PIECE' THEN mfi.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN mfi.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN mfi.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN mfi.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE mfi.quantity
                END * lm.quantity
            ), 0) as protein,
            COALESCE(SUM(
                (f.carbsPer100 / 100.0) *
                CASE mfi.unit
                    WHEN 'GRAM' THEN mfi.quantity
                    WHEN 'PIECE' THEN mfi.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN mfi.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN mfi.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN mfi.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE mfi.quantity
                END * lm.quantity
            ), 0) as carbs,
            COALESCE(SUM(
                (f.fatPer100 / 100.0) *
                CASE mfi.unit
                    WHEN 'GRAM' THEN mfi.quantity
                    WHEN 'PIECE' THEN mfi.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN mfi.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN mfi.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN mfi.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE mfi.quantity
                END * lm.quantity
            ), 0) as fat
        FROM logged_meals lm
        LEFT JOIN meal_food_items mfi ON lm.mealId = mfi.mealId
        LEFT JOIN food_items f ON mfi.foodId = f.id
        WHERE lm.userId = :userId AND lm.logDate BETWEEN :startDate AND :endDate
        GROUP BY lm.logDate
        ORDER BY lm.logDate
    """)
    fun getDailyMacroTotals(userId: Long, startDate: String, endDate: String): Flow<List<DailyMacroSummary>>

    // Chart data for completed plans only (weekly calories for home screen)
    @Query("""
        SELECT
            p.date as date,
            COALESCE(SUM(
                (f.caloriesPer100 / 100.0) *
                CASE mfi.unit
                    WHEN 'GRAM' THEN mfi.quantity
                    WHEN 'PIECE' THEN mfi.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN mfi.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN mfi.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN mfi.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE mfi.quantity
                END * lm.quantity
            ), 0) as calories,
            0.0 as protein,
            0.0 as carbs,
            0.0 as fat
        FROM plans p
        LEFT JOIN logged_meals lm ON lm.logDate = p.date AND lm.userId = p.userId
        LEFT JOIN meal_food_items mfi ON lm.mealId = mfi.mealId
        LEFT JOIN food_items f ON mfi.foodId = f.id
        WHERE p.userId = :userId AND p.date BETWEEN :startDate AND :endDate AND p.isCompleted = 1
        GROUP BY p.date
        ORDER BY p.date
    """)
    fun getCompletedDaysCalories(userId: Long, startDate: String, endDate: String): Flow<List<DailyMacroSummary>>
}
