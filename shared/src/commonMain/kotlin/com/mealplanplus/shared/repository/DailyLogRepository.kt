package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DailyLogRepository(private val database: MealPlanDatabase) {

    private val queries = database.dailyLogQueries

    suspend fun getDailyLog(userId: Long, date: String): DailyLog? {
        return queries.selectDailyLog(userId, date).executeAsOneOrNull()?.toDailyLog()
    }

    fun getDailyLogsForRange(userId: Long, startDate: String, endDate: String): Flow<List<DailyLog>> {
        return queries.selectDailyLogsForRange(userId, startDate, endDate).asFlowList().map { list ->
            list.map { it.toDailyLog() }
        }
    }

    suspend fun insertOrUpdateDailyLog(log: DailyLog) {
        queries.insertDailyLog(
            userId = log.userId,
            date = log.date,
            plannedDietId = log.plannedDietId,
            notes = log.notes,
            createdAt = log.createdAt
        )
    }

    suspend fun updateDailyLogDiet(userId: Long, date: String, dietId: Long?) {
        queries.updateDailyLogDiet(dietId, userId, date)
    }

    suspend fun deleteDailyLog(userId: Long, date: String) {
        queries.deleteDailyLog(userId, date)
    }

    // Slot overrides
    suspend fun getSlotOverrides(userId: Long, date: String): List<DailyLogSlotOverride> {
        return queries.selectSlotOverrides(userId, date).executeAsList().map {
            DailyLogSlotOverride(
                userId = it.userId,
                logDate = it.logDate,
                slotType = it.slotType,
                overrideMealId = it.overrideMealId,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }
    }

    suspend fun insertSlotOverride(override: DailyLogSlotOverride) {
        queries.insertSlotOverride(
            userId = override.userId,
            logDate = override.logDate,
            slotType = override.slotType,
            overrideMealId = override.overrideMealId,
            notes = override.notes,
            createdAt = override.createdAt
        )
    }

    suspend fun deleteSlotOverride(userId: Long, date: String, slotType: String) {
        queries.deleteSlotOverride(userId, date, slotType)
    }

    // Logged foods
    fun getLoggedFoods(userId: Long, date: String): Flow<List<LoggedFoodWithDetails>> {
        return queries.selectLoggedFoods(userId, date).asFlowList().map { list ->
            list.map { row ->
                LoggedFoodWithDetails(
                    loggedFood = LoggedFood(
                        id = row.id,
                        userId = row.userId,
                        logDate = row.logDate,
                        foodId = row.foodId,
                        quantity = row.quantity,
                        unit = FoodUnit.fromString(row.unit),
                        slotType = row.slotType,
                        timestamp = row.timestamp,
                        notes = row.notes
                    ),
                    food = FoodItem(
                        id = row.id_,
                        name = row.name,
                        brand = row.brand,
                        barcode = row.barcode,
                        caloriesPer100 = row.caloriesPer100,
                        proteinPer100 = row.proteinPer100,
                        carbsPer100 = row.carbsPer100,
                        fatPer100 = row.fatPer100,
                        gramsPerPiece = row.gramsPerPiece,
                        gramsPerCup = row.gramsPerCup,
                        gramsPerTbsp = row.gramsPerTbsp,
                        gramsPerTsp = row.gramsPerTsp,
                        glycemicIndex = row.glycemicIndex?.toInt(),
                        isFavorite = row.isFavorite == 1L,
                        lastUsed = row.lastUsed,
                        createdAt = row.createdAt,
                        isSystemFood = row.isSystemFood == 1L
                    )
                )
            }
        }
    }

    suspend fun insertLoggedFood(loggedFood: LoggedFood): Long {
        queries.insertLoggedFood(
            userId = loggedFood.userId,
            logDate = loggedFood.logDate,
            foodId = loggedFood.foodId,
            quantity = loggedFood.quantity,
            unit = loggedFood.unit.name,
            slotType = loggedFood.slotType,
            timestamp = loggedFood.timestamp,
            notes = loggedFood.notes
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateLoggedFood(loggedFood: LoggedFood) {
        queries.updateLoggedFood(
            quantity = loggedFood.quantity,
            unit = loggedFood.unit.name,
            slotType = loggedFood.slotType,
            timestamp = loggedFood.timestamp,
            notes = loggedFood.notes,
            id = loggedFood.id
        )
    }

    suspend fun deleteLoggedFood(id: Long) {
        queries.deleteLoggedFood(id)
    }

    // Logged meals
    suspend fun insertLoggedMeal(loggedMeal: LoggedMeal): Long {
        queries.insertLoggedMeal(
            userId = loggedMeal.userId,
            logDate = loggedMeal.logDate,
            mealId = loggedMeal.mealId,
            slotType = loggedMeal.slotType,
            quantity = loggedMeal.quantity,
            timestamp = loggedMeal.timestamp,
            notes = loggedMeal.notes
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateLoggedMeal(loggedMeal: LoggedMeal) {
        queries.updateLoggedMeal(
            quantity = loggedMeal.quantity,
            slotType = loggedMeal.slotType,
            timestamp = loggedMeal.timestamp,
            notes = loggedMeal.notes,
            id = loggedMeal.id
        )
    }

    suspend fun deleteLoggedMeal(id: Long) {
        queries.deleteLoggedMeal(id)
    }

    // Daily macro summary
    fun getDailyMacroSummary(userId: Long, startDate: String, endDate: String): Flow<List<DailyMacroSummary>> {
        return queries.selectDailyMacroSummary(userId, startDate, endDate).asFlowList().map { list ->
            list.map {
                DailyMacroSummary(
                    date = it.date,
                    calories = it.calories ?: 0.0,
                    protein = it.protein ?: 0.0,
                    carbs = it.carbs ?: 0.0,
                    fat = it.fat ?: 0.0
                )
            }
        }
    }

    private fun com.mealplanplus.shared.db.Daily_logs.toDailyLog(): DailyLog {
        return DailyLog(
            userId = userId,
            date = date,
            plannedDietId = plannedDietId,
            notes = notes,
            createdAt = createdAt
        )
    }
}
