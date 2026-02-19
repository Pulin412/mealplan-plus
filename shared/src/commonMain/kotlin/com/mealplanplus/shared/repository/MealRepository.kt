package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MealRepository(private val database: MealPlanDatabase) {

    private val queries = database.mealQueries

    fun getAllMeals(userId: Long): Flow<List<Meal>> {
        return queries.selectAllMeals(userId).asFlowList().map { list ->
            list.map { it.toMeal() }
        }
    }

    suspend fun getMealById(id: Long): Meal? {
        return queries.selectMealById(id).executeAsOneOrNull()?.toMeal()
    }

    fun getMealsBySlot(userId: Long, slotType: String): Flow<List<Meal>> {
        return queries.selectMealsBySlot(userId, slotType).asFlowList().map { list ->
            list.map { it.toMeal() }
        }
    }

    suspend fun getMealWithFoods(mealId: Long): MealWithFoods? {
        val meal = getMealById(mealId) ?: return null
        val items = queries.selectMealFoodItems(mealId).executeAsList().map { row ->
            MealFoodItemWithDetails(
                mealFoodItem = MealFoodItem(
                    mealId = row.mealId,
                    foodId = row.foodId,
                    quantity = row.quantity,
                    unit = FoodUnit.fromString(row.unit),
                    notes = row.notes
                ),
                food = FoodItem(
                    id = row.id,
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
        return MealWithFoods(meal, items)
    }

    suspend fun insertMeal(meal: Meal): Long {
        queries.insertMeal(
            userId = meal.userId,
            name = meal.name,
            description = meal.description,
            slotType = meal.slotType,
            customSlotId = meal.customSlotId,
            createdAt = meal.createdAt
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateMeal(meal: Meal) {
        queries.updateMeal(
            name = meal.name,
            description = meal.description,
            slotType = meal.slotType,
            customSlotId = meal.customSlotId,
            id = meal.id
        )
    }

    suspend fun deleteMeal(id: Long) {
        queries.deleteMealById(id)
    }

    suspend fun addFoodToMeal(mealId: Long, foodId: Long, quantity: Double, unit: FoodUnit, notes: String? = null) {
        queries.insertMealFoodItem(mealId, foodId, quantity, unit.name, notes)
    }

    suspend fun removeFoodFromMeal(mealId: Long, foodId: Long) {
        queries.deleteMealFoodItem(mealId, foodId)
    }

    suspend fun clearMealFoods(mealId: Long) {
        queries.deleteMealFoodItemsByMealId(mealId)
    }

    // Custom meal slots
    fun getCustomSlots(userId: Long): Flow<List<CustomMealSlot>> {
        return queries.selectCustomSlots(userId).asFlowList().map { list ->
            list.map { CustomMealSlot(it.id, it.userId, it.name, it.orderNum.toInt()) }
        }
    }

    suspend fun insertCustomSlot(userId: Long, name: String, order: Int): Long {
        queries.insertCustomSlot(userId, name, order.toLong())
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun deleteCustomSlot(id: Long) {
        queries.deleteCustomSlot(id)
    }

    private fun com.mealplanplus.shared.db.Meals.toMeal(): Meal {
        return Meal(
            id = id,
            userId = userId,
            name = name,
            description = description,
            slotType = slotType,
            customSlotId = customSlotId,
            createdAt = createdAt
        )
    }
}
