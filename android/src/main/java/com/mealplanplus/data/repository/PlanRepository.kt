package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.model.PlanWithDietName
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
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

    fun getPlansInRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Plan>> =
        planDao.getPlansInRange(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    fun getPlansWithDietNames(startDate: LocalDate, endDate: LocalDate): Flow<List<PlanWithDietName>> =
        planDao.getPlansWithDietNames(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    fun getPlansByUser(): Flow<List<Plan>> = planDao.getPlansByUser(getCurrentUserId())

    suspend fun getPlanForDate(date: LocalDate): Plan? =
        planDao.getPlanForDate(getCurrentUserId(), date.toEpochMs())

    suspend fun getDietForDate(date: LocalDate): Diet? =
        planDao.getDietForDate(getCurrentUserId(), date.toEpochMs())

    suspend fun setPlanForDate(date: LocalDate, dietId: Long?, notes: String? = null) {
        planDao.upsertPlan(Plan(userId = getCurrentUserId(), date = date.toEpochMs(), dietId = dietId, notes = notes))
    }

    suspend fun removePlan(date: LocalDate) = planDao.deletePlan(getCurrentUserId(), date.toEpochMs())

    suspend fun copyPlanToDate(fromDate: LocalDate, toDate: LocalDate) {
        val userId = getCurrentUserId()
        val sourcePlan = planDao.getPlanForDate(userId, fromDate.toEpochMs())
        sourcePlan?.let {
            planDao.upsertPlan(Plan(userId = userId, date = toDate.toEpochMs(), dietId = it.dietId, notes = it.notes))
        }
    }

    suspend fun completePlan(date: LocalDate) {
        val userId = getCurrentUserId()
        val plan = planDao.getPlanForDate(userId, date.toEpochMs())
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = true))
        }
    }

    suspend fun uncompletePlan(date: LocalDate) {
        val userId = getCurrentUserId()
        val plan = planDao.getPlanForDate(userId, date.toEpochMs())
        plan?.let {
            planDao.upsertPlan(it.copy(isCompleted = false))
        }
    }
}
