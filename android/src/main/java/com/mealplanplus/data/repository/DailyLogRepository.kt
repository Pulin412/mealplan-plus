package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val foodDao: FoodDao,
    private val dietRepository: DietRepository,
    private val planDao: PlanDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getLogsByUser(): Flow<List<DailyLog>> = dailyLogDao.getLogsByUser(getCurrentUserId())

    suspend fun getLogByDate(date: LocalDate): DailyLog? =
        dailyLogDao.getLogByDate(getCurrentUserId(), date.toEpochMs())

    fun getLogByDateFlow(date: LocalDate): Flow<DailyLog?> =
        dailyLogDao.getLogByDateFlow(getCurrentUserId(), date.toEpochMs())

    fun getLogWithFoods(date: LocalDate): Flow<DailyLogWithFoods?> {
        val userId = getCurrentUserId()
        val dateMs = date.toEpochMs()
        return combine(
            dailyLogDao.getLogByDateFlow(userId, dateMs),
            dailyLogDao.getLoggedFoods(userId, dateMs)
        ) { log, loggedFoods ->
            if (log == null && loggedFoods.isEmpty()) return@combine null

            val actualLog = log ?: DailyLog(userId = userId, date = dateMs)
            val foodsWithDetails = loggedFoods.mapNotNull { lf ->
                foodDao.getFoodById(lf.foodId)?.let { food ->
                    LoggedFoodWithDetails(lf, food)
                }
            }
            DailyLogWithFoods(actualLog, foodsWithDetails)
        }
    }

    suspend fun createOrUpdateLog(date: LocalDate, plannedDietId: Long? = null, notes: String? = null) {
        val userId = getCurrentUserId()
        val dateMs = date.toEpochMs()
        val existing = dailyLogDao.getLogByDate(userId, dateMs)
        if (existing != null) {
            dailyLogDao.updateLog(existing.copy(plannedDietId = plannedDietId, notes = notes))
        } else {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateMs, plannedDietId = plannedDietId, notes = notes))
        }
    }

    suspend fun logFood(
        date: LocalDate,
        foodId: Long,
        quantity: Double,
        slotType: String,
        unit: FoodUnit = FoodUnit.GRAM,
        timestamp: Long? = null,
        notes: String? = null
    ): Long {
        val userId = getCurrentUserId()
        val dateMs = date.toEpochMs()
        if (dailyLogDao.getLogByDate(userId, dateMs) == null) {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateMs))
        }
        return dailyLogDao.insertLoggedFood(
            LoggedFood(
                userId = userId,
                logDate = dateMs,
                foodId = foodId,
                quantity = quantity,
                unit = unit,
                slotType = slotType,
                timestamp = timestamp,
                notes = notes
            )
        )
    }

    suspend fun updateLoggedFood(loggedFood: LoggedFood) = dailyLogDao.updateLoggedFood(loggedFood)

    suspend fun deleteLoggedFood(id: Long) = dailyLogDao.deleteLoggedFoodById(id)

    suspend fun clearSlot(date: LocalDate, slotType: String) {
        dailyLogDao.clearLoggedFoodsForSlot(getCurrentUserId(), date.toEpochMs(), slotType)
    }

    /** Returns true if any food has been logged for [slotType] on [date]. Queries the DB directly (never stale). */
    suspend fun isSlotLogged(date: LocalDate, slotType: String): Boolean {
        val userId = getCurrentUserId()
        return dailyLogDao.getLoggedFoodsForSlot(userId, date.toEpochMs(), slotType).isNotEmpty()
    }

    suspend fun clearAllFoodsForDate(date: LocalDate) {
        dailyLogDao.clearLoggedFoods(getCurrentUserId(), date.toEpochMs())
    }

    fun todayDate(): LocalDate = LocalDate.now()

    // Chart data
    fun getDailyMacroTotals(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getDailyMacroTotals(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    fun getCompletedDaysCalories(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getCompletedDaysCalories(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    /** Returns dates where any food was logged — used for accurate streak calculation. */
    fun getLoggedDatesForStreak(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getLoggedDates(getCurrentUserId(), startDate.toEpochMs(), endDate.toEpochMs())

    /**
     * Apply a full diet to a day — logs each food individually into logged_foods.
     */
    suspend fun applyDiet(date: LocalDate, dietWithMeals: DietWithMeals) {
        val userId = getCurrentUserId()
        val dateMs = date.toEpochMs()
        val timestamp = System.currentTimeMillis()

        if (dailyLogDao.getLogByDate(userId, dateMs) == null) {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateMs, plannedDietId = dietWithMeals.diet.id))
        }

        dietWithMeals.meals.forEach { (slotType, mealWithFoods) ->
            dailyLogDao.clearLoggedFoodsForSlot(userId, dateMs, slotType)
            mealWithFoods?.items?.forEach { foodItem ->
                dailyLogDao.insertLoggedFood(
                    LoggedFood(
                        userId = userId,
                        logDate = dateMs,
                        foodId = foodItem.mealFoodItem.foodId,
                        quantity = foodItem.mealFoodItem.quantity,
                        unit = foodItem.mealFoodItem.unit,
                        slotType = slotType,
                        timestamp = timestamp
                    )
                )
            }
        }
    }
}
