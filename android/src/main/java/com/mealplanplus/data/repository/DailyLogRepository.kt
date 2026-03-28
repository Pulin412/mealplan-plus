package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getLogsByUser(): Flow<List<DailyLog>> = dailyLogDao.getLogsByUser(getCurrentUserId())

    suspend fun getLogByDate(date: LocalDate): DailyLog? =
        dailyLogDao.getLogByDate(getCurrentUserId(), date.format(dateFormatter))

    fun getLogByDateFlow(date: LocalDate): Flow<DailyLog?> =
        dailyLogDao.getLogByDateFlow(getCurrentUserId(), date.format(dateFormatter))

    fun getLogWithFoods(date: LocalDate): Flow<DailyLogWithFoods?> {
        val userId = getCurrentUserId()
        val dateStr = date.format(dateFormatter)
        return combine(
            dailyLogDao.getLogByDateFlow(userId, dateStr),
            dailyLogDao.getLoggedFoods(userId, dateStr)
        ) { log, loggedFoods ->
            if (log == null && loggedFoods.isEmpty()) return@combine null

            val actualLog = log ?: DailyLog(userId = userId, date = dateStr)
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
        val dateStr = date.format(dateFormatter)
        val existing = dailyLogDao.getLogByDate(userId, dateStr)
        if (existing != null) {
            dailyLogDao.updateLog(existing.copy(plannedDietId = plannedDietId, notes = notes))
        } else {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateStr, plannedDietId = plannedDietId, notes = notes))
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
        val dateStr = date.format(dateFormatter)
        if (dailyLogDao.getLogByDate(userId, dateStr) == null) {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateStr))
        }
        return dailyLogDao.insertLoggedFood(
            LoggedFood(
                userId = userId,
                logDate = dateStr,
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
        dailyLogDao.clearLoggedFoodsForSlot(getCurrentUserId(), date.format(dateFormatter), slotType)
    }

    /** Returns true if any food has been logged for [slotType] on [date]. Queries the DB directly (never stale). */
    suspend fun isSlotLogged(date: LocalDate, slotType: String): Boolean {
        val userId = getCurrentUserId()
        val dateStr = date.format(dateFormatter)
        return dailyLogDao.getLoggedFoodsForSlot(userId, dateStr, slotType).isNotEmpty()
    }

    suspend fun clearAllFoodsForDate(date: LocalDate) {
        dailyLogDao.clearLoggedFoods(getCurrentUserId(), date.format(dateFormatter))
    }

    fun todayDate(): LocalDate = LocalDate.now()

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, dateFormatter)

    // Chart data
    fun getDailyMacroTotals(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getDailyMacroTotals(getCurrentUserId(), startDate.format(dateFormatter), endDate.format(dateFormatter))

    fun getCompletedDaysCalories(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getCompletedDaysCalories(getCurrentUserId(), startDate.format(dateFormatter), endDate.format(dateFormatter))

    /**
     * Apply a full diet to a day — logs each food individually into logged_foods.
     */
    suspend fun applyDiet(date: LocalDate, dietWithMeals: DietWithMeals) {
        val userId = getCurrentUserId()
        val dateStr = date.format(dateFormatter)
        val timestamp = System.currentTimeMillis()

        if (dailyLogDao.getLogByDate(userId, dateStr) == null) {
            dailyLogDao.insertLog(DailyLog(userId = userId, date = dateStr, plannedDietId = dietWithMeals.diet.id))
        }

        dietWithMeals.meals.forEach { (slotType, mealWithFoods) ->
            // Clear this slot first so re-applying the same diet never creates duplicates
            dailyLogDao.clearLoggedFoodsForSlot(userId, dateStr, slotType)
            mealWithFoods?.items?.forEach { foodItem ->
                dailyLogDao.insertLoggedFood(
                    LoggedFood(
                        userId = userId,
                        logDate = dateStr,
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
