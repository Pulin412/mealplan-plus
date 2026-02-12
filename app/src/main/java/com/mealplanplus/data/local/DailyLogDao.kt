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
    @Query("SELECT * FROM daily_logs ORDER BY date DESC")
    fun getAllLogs(): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    suspend fun getLogByDate(date: String): DailyLog?

    @Query("SELECT * FROM daily_logs WHERE date = :date")
    fun getLogByDateFlow(date: String): Flow<DailyLog?>

    @Query("SELECT * FROM daily_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DailyLog)

    @Update
    suspend fun updateLog(log: DailyLog)

    @Delete
    suspend fun deleteLog(log: DailyLog)

    // Logged foods
    @Query("SELECT * FROM logged_foods WHERE logDate = :date ORDER BY slotType, timestamp")
    fun getLoggedFoods(date: String): Flow<List<LoggedFood>>

    @Query("SELECT * FROM logged_foods WHERE logDate = :date AND slotType = :slotType")
    suspend fun getLoggedFoodsForSlot(date: String, slotType: String): List<LoggedFood>

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

    @Query("DELETE FROM logged_foods WHERE logDate = :date")
    suspend fun clearLoggedFoods(date: String)

    @Query("DELETE FROM logged_foods WHERE logDate = :date AND slotType = :slotType")
    suspend fun clearLoggedFoodsForSlot(date: String, slotType: String)

    // Logged meals
    @Query("SELECT * FROM logged_meals WHERE logDate = :date ORDER BY slotType, timestamp")
    fun getLoggedMeals(date: String): Flow<List<LoggedMeal>>

    @Query("SELECT * FROM logged_meals WHERE logDate = :date AND slotType = :slotType")
    suspend fun getLoggedMealsForSlot(date: String, slotType: String): List<LoggedMeal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoggedMeal(meal: LoggedMeal): Long

    @Update
    suspend fun updateLoggedMeal(meal: LoggedMeal)

    @Delete
    suspend fun deleteLoggedMeal(meal: LoggedMeal)

    @Query("DELETE FROM logged_meals WHERE id = :id")
    suspend fun deleteLoggedMealById(id: Long)

    @Query("DELETE FROM logged_meals WHERE logDate = :date")
    suspend fun clearLoggedMeals(date: String)

    @Query("DELETE FROM logged_meals WHERE logDate = :date AND slotType = :slotType")
    suspend fun clearLoggedMealsForSlot(date: String, slotType: String)

    // Slot overrides
    @Query("SELECT * FROM daily_log_slot_overrides WHERE logDate = :date")
    fun getSlotOverrides(date: String): Flow<List<DailyLogSlotOverride>>

    @Query("SELECT * FROM daily_log_slot_overrides WHERE logDate = :date")
    suspend fun getSlotOverridesList(date: String): List<DailyLogSlotOverride>

    @Query("SELECT * FROM daily_log_slot_overrides WHERE logDate = :date AND slotType = :slotType")
    suspend fun getSlotOverride(date: String, slotType: String): DailyLogSlotOverride?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlotOverride(override: DailyLogSlotOverride)

    @Delete
    suspend fun deleteSlotOverride(override: DailyLogSlotOverride)

    @Query("DELETE FROM daily_log_slot_overrides WHERE logDate = :date AND slotType = :slotType")
    suspend fun deleteSlotOverrideBySlot(date: String, slotType: String)

    @Query("DELETE FROM daily_log_slot_overrides WHERE logDate = :date")
    suspend fun clearSlotOverrides(date: String)

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
        WHERE lm.logDate BETWEEN :startDate AND :endDate
        GROUP BY lm.logDate
        ORDER BY lm.logDate
    """)
    fun getDailyMacroTotals(startDate: String, endDate: String): Flow<List<DailyMacroSummary>>

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
        LEFT JOIN logged_meals lm ON lm.logDate = p.date
        LEFT JOIN meal_food_items mfi ON lm.mealId = mfi.mealId
        LEFT JOIN food_items f ON mfi.foodId = f.id
        WHERE p.date BETWEEN :startDate AND :endDate AND p.isCompleted = 1
        GROUP BY p.date
        ORDER BY p.date
    """)
    fun getCompletedDaysCalories(startDate: String, endDate: String): Flow<List<DailyMacroSummary>>
}
