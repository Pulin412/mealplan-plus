package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans WHERE date = :date")
    suspend fun getPlanForDate(date: String): Plan?

    @Query("SELECT * FROM plans WHERE date BETWEEN :startDate AND :endDate ORDER BY date")
    fun getPlansInRange(startDate: String, endDate: String): Flow<List<Plan>>

    @Query("""
        SELECT d.* FROM diets d
        INNER JOIN plans p ON p.dietId = d.id
        WHERE p.date = :date
    """)
    suspend fun getDietForDate(date: String): Diet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: Plan)

    @Query("DELETE FROM plans WHERE date = :date")
    suspend fun deletePlan(date: String)

    @Query("SELECT * FROM plans ORDER BY date DESC")
    fun getAllPlans(): Flow<List<Plan>>
}
