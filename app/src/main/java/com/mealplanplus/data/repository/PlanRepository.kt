package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.model.PlanWithDietName
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepository @Inject constructor(
    private val planDao: PlanDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getPlansInRange(startDate: String, endDate: String): Flow<List<Plan>> =
        planDao.getPlansInRange(getCurrentUserId(), startDate, endDate)

    fun getPlansWithDietNames(startDate: String, endDate: String): Flow<List<PlanWithDietName>> =
        planDao.getPlansWithDietNames(getCurrentUserId(), startDate, endDate)

    fun getPlansByUser(): Flow<List<Plan>> = planDao.getPlansByUser(getCurrentUserId())

    suspend fun getPlanForDate(date: String): Plan? = planDao.getPlanForDate(getCurrentUserId(), date)

    suspend fun getDietForDate(date: String): Diet? = planDao.getDietForDate(getCurrentUserId(), date)

    suspend fun setPlanForDate(date: String, dietId: Long?, notes: String? = null) {
        planDao.upsertPlan(Plan(userId = getCurrentUserId(), date = date, dietId = dietId, notes = notes))
    }

    suspend fun removePlan(date: String) = planDao.deletePlan(getCurrentUserId(), date)

    suspend fun copyPlanToDate(fromDate: String, toDate: String) {
        val userId = getCurrentUserId()
        val sourcePlan = planDao.getPlanForDate(userId, fromDate)
        sourcePlan?.let {
            planDao.upsertPlan(Plan(userId = userId, date = toDate, dietId = it.dietId, notes = it.notes))
        }
    }

    suspend fun completePlan(date: String) {
        val userId = getCurrentUserId()
        val plan = planDao.getPlanForDate(userId, date)
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = true))
        }
    }

    suspend fun uncompletePlan(date: String) {
        val userId = getCurrentUserId()
        val plan = planDao.getPlanForDate(userId, date)
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = false))
        }
    }
}
