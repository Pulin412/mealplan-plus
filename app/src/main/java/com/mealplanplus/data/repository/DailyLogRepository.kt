package com.mealplanplus.data.repository

import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyLogRepository @Inject constructor(
    private val dailyLogDao: DailyLogDao,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietRepository: DietRepository,
    private val planDao: PlanDao
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

    // Meal logging
    fun getLogWithMeals(date: LocalDate): Flow<DailyLogWithMeals?> {
        val dateStr = date.format(dateFormatter)
        return combine(
            dailyLogDao.getLogByDateFlow(dateStr),
            dailyLogDao.getLoggedMeals(dateStr)
        ) { log, loggedMeals ->
            // Return empty state if nothing logged yet
            if (log == null && loggedMeals.isEmpty()) {
                return@combine DailyLogWithMeals(DailyLog(date = dateStr), emptyList())
            }

            val actualLog = log ?: DailyLog(date = dateStr)
            val mealsWithDetails = loggedMeals.mapNotNull { lm ->
                mealDao.getMealById(lm.mealId)?.let { meal ->
                    val mealFoodItems = mealDao.getMealFoodItems(meal.id)
                    val foodsWithDetails = mealFoodItems.mapNotNull { mfi ->
                        foodDao.getFoodById(mfi.foodId)?.let { food ->
                            MealFoodItemWithDetails(mfi, food)
                        }
                    }
                    LoggedMealWithDetails(lm, meal, foodsWithDetails)
                }
            }
            DailyLogWithMeals(actualLog, mealsWithDetails)
        }
    }

    suspend fun logMeal(
        date: LocalDate,
        mealId: Long,
        slotType: String,
        quantity: Double = 1.0,
        timestamp: Long? = null,
        notes: String? = null
    ): Long {
        val dateStr = date.format(dateFormatter)
        // Ensure log exists
        if (dailyLogDao.getLogByDate(dateStr) == null) {
            dailyLogDao.insertLog(DailyLog(dateStr))
        }
        return dailyLogDao.insertLoggedMeal(
            LoggedMeal(
                logDate = dateStr,
                mealId = mealId,
                slotType = slotType,
                quantity = quantity,
                timestamp = timestamp,
                notes = notes
            )
        )
    }

    suspend fun updateLoggedMeal(loggedMeal: LoggedMeal) = dailyLogDao.updateLoggedMeal(loggedMeal)

    suspend fun deleteLoggedMeal(id: Long) = dailyLogDao.deleteLoggedMealById(id)

    suspend fun clearMealsForSlot(date: LocalDate, slotType: String) {
        dailyLogDao.clearLoggedMealsForSlot(date.format(dateFormatter), slotType)
    }

    suspend fun clearLoggedMeals(date: LocalDate) {
        dailyLogDao.clearLoggedMeals(date.format(dateFormatter))
    }

    // Chart data
    fun getDailyMacroTotals(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getDailyMacroTotals(startDate.format(dateFormatter), endDate.format(dateFormatter))

    fun getCompletedDaysCalories(startDate: LocalDate, endDate: LocalDate): Flow<List<DailyMacroSummary>> =
        dailyLogDao.getCompletedDaysCalories(startDate.format(dateFormatter), endDate.format(dateFormatter))

    /**
     * Apply a full diet to a day - logs all meals from the diet to their respective slots
     * Used for manual logging when no plan exists
     */
    suspend fun applyDiet(date: LocalDate, dietWithMeals: DietWithMeals) {
        val dateStr = date.format(dateFormatter)
        val timestamp = System.currentTimeMillis()

        // Ensure log exists
        if (dailyLogDao.getLogByDate(dateStr) == null) {
            dailyLogDao.insertLog(DailyLog(dateStr, plannedDietId = dietWithMeals.diet.id))
        }

        // Log each meal to its slot
        dietWithMeals.meals.forEach { (slotType, mealWithFoods) ->
            mealWithFoods?.let { mwf ->
                dailyLogDao.insertLoggedMeal(
                    LoggedMeal(
                        logDate = dateStr,
                        mealId = mwf.meal.id,
                        slotType = slotType,
                        quantity = 1.0,
                        timestamp = timestamp
                    )
                )
            }
        }
    }
}
