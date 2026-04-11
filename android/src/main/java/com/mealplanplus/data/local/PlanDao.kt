package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.model.PlanWithDietName
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans WHERE userId = :userId AND date = :date")
    suspend fun getPlanForDate(userId: Long, date: Long): Plan?

    @Query("SELECT * FROM plans WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getPlansInRange(userId: Long, startDate: Long, endDate: Long): Flow<List<Plan>>

    @Query("""
        SELECT p.userId, p.date, p.dietId, p.isCompleted, p.notes, d.name as dietName
        FROM plans p
        LEFT JOIN diets d ON p.dietId = d.id
        WHERE p.userId = :userId AND p.date BETWEEN :startDate AND :endDate
        ORDER BY p.date
    """)
    fun getPlansWithDietNames(userId: Long, startDate: Long, endDate: Long): Flow<List<PlanWithDietName>>

    @Query("""
        SELECT d.* FROM diets d
        INNER JOIN plans p ON p.dietId = d.id
        WHERE p.userId = :userId AND p.date = :date
    """)
    suspend fun getDietForDate(userId: Long, date: Long): Diet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: Plan)

    @Query("DELETE FROM plans WHERE userId = :userId AND date = :date")
    suspend fun deletePlan(userId: Long, date: Long)

    @Query("SELECT * FROM plans WHERE userId = :userId ORDER BY date DESC")
    fun getPlansByUser(userId: Long): Flow<List<Plan>>

    @Query("DELETE FROM plans")
    suspend fun deleteAllPlans()
}
