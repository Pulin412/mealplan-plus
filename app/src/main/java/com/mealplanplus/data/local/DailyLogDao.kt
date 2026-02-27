package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.DailyLog
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.LoggedFood
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

    // Chart data - daily macro totals
    @Query("""
        SELECT
            lf.logDate as date,
            COALESCE(SUM(
                (f.caloriesPer100 / 100.0) *
                CASE lf.unit
                    WHEN 'GRAM' THEN lf.quantity
                    WHEN 'PIECE' THEN lf.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN lf.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN lf.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN lf.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE lf.quantity
                END
            ), 0) as calories,
            COALESCE(SUM(
                (f.proteinPer100 / 100.0) *
                CASE lf.unit
                    WHEN 'GRAM' THEN lf.quantity
                    WHEN 'PIECE' THEN lf.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN lf.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN lf.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN lf.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE lf.quantity
                END
            ), 0) as protein,
            COALESCE(SUM(
                (f.carbsPer100 / 100.0) *
                CASE lf.unit
                    WHEN 'GRAM' THEN lf.quantity
                    WHEN 'PIECE' THEN lf.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN lf.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN lf.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN lf.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE lf.quantity
                END
            ), 0) as carbs,
            COALESCE(SUM(
                (f.fatPer100 / 100.0) *
                CASE lf.unit
                    WHEN 'GRAM' THEN lf.quantity
                    WHEN 'PIECE' THEN lf.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN lf.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN lf.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN lf.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE lf.quantity
                END
            ), 0) as fat
        FROM logged_foods lf
        LEFT JOIN food_items f ON lf.foodId = f.id
        WHERE lf.userId = :userId AND lf.logDate BETWEEN :startDate AND :endDate
        GROUP BY lf.logDate
        ORDER BY lf.logDate
    """)
    fun getDailyMacroTotals(userId: Long, startDate: String, endDate: String): Flow<List<DailyMacroSummary>>

    // Chart data for completed plans only (weekly calories for home screen)
    @Query("""
        SELECT
            p.date as date,
            COALESCE(SUM(
                (f.caloriesPer100 / 100.0) *
                CASE lf.unit
                    WHEN 'GRAM' THEN lf.quantity
                    WHEN 'PIECE' THEN lf.quantity * COALESCE(f.gramsPerPiece, 100)
                    WHEN 'CUP' THEN lf.quantity * COALESCE(f.gramsPerCup, 240)
                    WHEN 'TBSP' THEN lf.quantity * COALESCE(f.gramsPerTbsp, 15)
                    WHEN 'TSP' THEN lf.quantity * COALESCE(f.gramsPerTsp, 5)
                    ELSE lf.quantity
                END
            ), 0) as calories,
            0.0 as protein,
            0.0 as carbs,
            0.0 as fat
        FROM plans p
        LEFT JOIN logged_foods lf ON lf.logDate = p.date AND lf.userId = p.userId
        LEFT JOIN food_items f ON lf.foodId = f.id
        WHERE p.userId = :userId AND p.date BETWEEN :startDate AND :endDate AND p.isCompleted = 1
        GROUP BY p.date
        ORDER BY p.date
    """)
    fun getCompletedDaysCalories(userId: Long, startDate: String, endDate: String): Flow<List<DailyMacroSummary>>
}
