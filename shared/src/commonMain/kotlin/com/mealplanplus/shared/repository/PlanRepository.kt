package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlanRepository(private val database: MealPlanDatabase) {

    private val queries = database.planQueries

    suspend fun getPlanByDate(userId: Long, date: String): Plan? {
        return queries.selectPlanByDate(userId, date).executeAsOneOrNull()?.toPlan()
    }

    fun getPlansForDateRange(userId: Long, startDate: String, endDate: String): Flow<List<Plan>> {
        return queries.selectPlansForDateRange(userId, startDate, endDate).asFlowList().map { list ->
            list.map { it.toPlan() }
        }
    }

    fun getPlansWithDietName(userId: Long, startDate: String, endDate: String): Flow<List<PlanWithDietName>> {
        return queries.selectPlansWithDietName(userId, startDate, endDate).asFlowList().map { list ->
            list.map {
                PlanWithDietName(
                    userId = it.userId,
                    date = it.date,
                    dietId = it.dietId,
                    isCompleted = it.isCompleted == 1L,
                    notes = it.notes,
                    dietName = it.dietName
                )
            }
        }
    }

    suspend fun insertOrUpdatePlan(plan: Plan) {
        queries.insertPlan(
            userId = plan.userId,
            date = plan.date,
            dietId = plan.dietId,
            notes = plan.notes,
            isCompleted = if (plan.isCompleted) 1L else 0L
        )
    }

    suspend fun updatePlanDiet(userId: Long, date: String, dietId: Long?) {
        queries.updatePlanDiet(dietId, userId, date)
    }

    suspend fun updatePlanCompleted(userId: Long, date: String, isCompleted: Boolean) {
        queries.updatePlanCompleted(if (isCompleted) 1L else 0L, userId, date)
    }

    suspend fun updatePlanNotes(userId: Long, date: String, notes: String?) {
        queries.updatePlanNotes(notes, userId, date)
    }

    suspend fun deletePlan(userId: Long, date: String) {
        queries.deletePlan(userId, date)
    }

    suspend fun deletePlansForDateRange(userId: Long, startDate: String, endDate: String) {
        queries.deletePlansForDateRange(userId, startDate, endDate)
    }

    // MARK: - Snapshot functions for iOS
    suspend fun getPlansWithDietNameSnapshot(userId: Long, startDate: String, endDate: String): List<PlanWithDietName> {
        return queries.selectPlansWithDietName(userId, startDate, endDate).executeAsList().map {
            PlanWithDietName(
                userId = it.userId,
                date = it.date,
                dietId = it.dietId,
                isCompleted = it.isCompleted == 1L,
                notes = it.notes,
                dietName = it.dietName
            )
        }
    }

    private fun com.mealplanplus.shared.db.Plans.toPlan(): Plan {
        return Plan(
            userId = userId,
            date = date,
            dietId = dietId,
            notes = notes,
            isCompleted = isCompleted == 1L
        )
    }
}
