package com.mealplanplus.data.repository

import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.model.PlanWithDietName
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao
) {
    fun getPlansInRange(startDate: String, endDate: String): Flow<List<Plan>> =
        planDao.getPlansInRange(startDate, endDate)

    fun getPlansWithDietNames(startDate: String, endDate: String): Flow<List<PlanWithDietName>> =
        planDao.getPlansWithDietNames(startDate, endDate)

    fun getAllPlans(): Flow<List<Plan>> = planDao.getAllPlans()

    suspend fun getPlanForDate(date: String): Plan? = planDao.getPlanForDate(date)

    suspend fun getDietForDate(date: String): Diet? = planDao.getDietForDate(date)

    suspend fun setPlanForDate(date: String, dietId: Long?, notes: String? = null) {
        planDao.upsertPlan(Plan(date = date, dietId = dietId, notes = notes))
    }

    suspend fun removePlan(date: String) = planDao.deletePlan(date)

    suspend fun copyPlanToDate(fromDate: String, toDate: String) {
        val sourcePlan = planDao.getPlanForDate(fromDate)
        sourcePlan?.let {
            planDao.upsertPlan(Plan(date = toDate, dietId = it.dietId, notes = it.notes))
        }
    }

    suspend fun completePlan(date: String) {
        val plan = planDao.getPlanForDate(date)
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = true))
        }
    }

    suspend fun uncompletePlan(date: String) {
        val plan = planDao.getPlanForDate(date)
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = false))
        }
    }
}
