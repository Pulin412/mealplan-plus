package com.mealplanplus.data.repository

import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val foodDao: FoodDao,
    private val dietRepository: DietRepository
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun getAllLogs(): Flow<List<DailyLog>> = dailyLogDao.getAllLogs()

    suspend fun getLogByDate(date: LocalDate): DailyLog? =
        dailyLogDao.getLogByDate(date.format(dateFormatter))

    fun getLogByDateFlow(date: LocalDate): Flow<DailyLog?> =
        dailyLogDao.getLogByDateFlow(date.format(dateFormatter))

    fun getLogWithFoods(date: LocalDate): Flow<DailyLogWithFoods?> {
        val dateStr = date.format(dateFormatter)
        return combine(
            dailyLogDao.getLogByDateFlow(dateStr),
            dailyLogDao.getLoggedFoods(dateStr)
        ) { log, loggedFoods ->
            if (log == null && loggedFoods.isEmpty()) return@combine null

            val actualLog = log ?: DailyLog(date = dateStr)
            val foodsWithDetails = loggedFoods.mapNotNull { lf ->
                foodDao.getFoodById(lf.foodId)?.let { food ->
                    LoggedFoodWithDetails(lf, food)
                }
            }
            DailyLogWithFoods(actualLog, foodsWithDetails)
        }
    }

    suspend fun createOrUpdateLog(date: LocalDate, plannedDietId: Long? = null, notes: String? = null) {
        val dateStr = date.format(dateFormatter)
        val existing = dailyLogDao.getLogByDate(dateStr)
        if (existing != null) {
            dailyLogDao.updateLog(existing.copy(plannedDietId = plannedDietId, notes = notes))
        } else {
            dailyLogDao.insertLog(DailyLog(dateStr, plannedDietId, notes))
        }
    }

    suspend fun logFood(
        date: LocalDate,
        foodId: Long,
        quantity: Double,
        slotType: String,
        timestamp: Long? = null,
        notes: String? = null
    ): Long {
        val dateStr = date.format(dateFormatter)
        // Ensure log exists
        if (dailyLogDao.getLogByDate(dateStr) == null) {
            dailyLogDao.insertLog(DailyLog(dateStr))
        }
        return dailyLogDao.insertLoggedFood(
            LoggedFood(
                logDate = dateStr,
                foodId = foodId,
                quantity = quantity,
                slotType = slotType,
                timestamp = timestamp,
                notes = notes
            )
        )
    }

    suspend fun updateLoggedFood(loggedFood: LoggedFood) = dailyLogDao.updateLoggedFood(loggedFood)

    suspend fun deleteLoggedFood(id: Long) = dailyLogDao.deleteLoggedFoodById(id)

    suspend fun clearSlot(date: LocalDate, slotType: String) {
        dailyLogDao.clearLoggedFoodsForSlot(date.format(dateFormatter), slotType)
    }

    fun todayDate(): LocalDate = LocalDate.now()

    fun formatDate(date: LocalDate): String = date.format(dateFormatter)

    fun parseDate(dateStr: String): LocalDate = LocalDate.parse(dateStr, dateFormatter)
}
