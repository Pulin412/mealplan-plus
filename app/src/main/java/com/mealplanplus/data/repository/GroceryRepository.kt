package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroceryRepository @Inject constructor(
    private val groceryDao: GroceryDao,
    private val planDao: PlanDao,
    private val dietRepository: DietRepository,
    @ApplicationContext private val context: Context
) {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    // ===== List operations =====

    fun getListsByUser(): Flow<List<GroceryList>> = groceryDao.getListsByUser(getCurrentUserId())

    fun getListWithItems(listId: Long): Flow<GroceryListWithItems?> = groceryDao.getListWithItems(listId)

    suspend fun getListWithItemsOnce(listId: Long): GroceryListWithItems? = groceryDao.getListWithItemsOnce(listId)

    suspend fun createEmptyList(name: String): Long {
        val list = GroceryList(
            userId = getCurrentUserId(),
            name = name
        )
        return groceryDao.insertList(list)
    }

    suspend fun deleteList(list: GroceryList) = groceryDao.deleteList(list)

    suspend fun deleteListById(listId: Long) = groceryDao.deleteListById(listId)

    // ===== Item operations =====

    suspend fun toggleItemChecked(itemId: Long, checked: Boolean) {
        groceryDao.setItemChecked(itemId, checked)
    }

    suspend fun uncheckAllItems(listId: Long) {
        groceryDao.uncheckAllItems(listId)
    }

    suspend fun deleteItem(itemId: Long) {
        groceryDao.deleteItemById(itemId)
    }

    suspend fun updateItemQuantity(itemId: Long, quantity: Double) {
        groceryDao.updateItemQuantity(itemId, quantity)
    }

    suspend fun addCustomItem(listId: Long, name: String, quantity: Double, unit: FoodUnit): Long {
        val item = GroceryItem(
            listId = listId,
            customName = name,
            quantity = quantity,
            unit = unit
        )
        return groceryDao.insertItem(item)
    }

    // ===== Generate from date range =====

    /**
     * Generate grocery list from planned meals for given dates
     * Combines quantities for same food items
     */
    suspend fun generateFromDateRange(
        name: String,
        dates: List<LocalDate>
    ): Long {
        val userId = getCurrentUserId()

        // Get date strings
        val dateStrings = dates.map { it.format(dateFormatter) }
        val startDate = dates.minOrNull()?.format(dateFormatter)
        val endDate = dates.maxOrNull()?.format(dateFormatter)

        // Create the list first
        val list = GroceryList(
            userId = userId,
            name = name,
            startDate = startDate,
            endDate = endDate
        )
        val listId = groceryDao.insertList(list)

        // Collect all food items from planned diets
        val aggregatedFoods = mutableMapOf<AggregationKey, AggregatedFood>()

        for (dateStr in dateStrings) {
            val plan = planDao.getPlanForDate(userId, dateStr)
            val dietId = plan?.dietId ?: continue

            val dietWithMeals = dietRepository.getDietWithMeals(dietId) ?: continue

            // Process each meal in the diet
            for ((_, mealWithFoods) in dietWithMeals.meals) {
                if (mealWithFoods == null) continue

                for (item in mealWithFoods.items) {
                    val key = AggregationKey(item.food.id, item.mealFoodItem.unit)
                    val existing = aggregatedFoods[key]

                    if (existing != null) {
                        existing.quantity += item.mealFoodItem.quantity
                    } else {
                        aggregatedFoods[key] = AggregatedFood(
                            foodId = item.food.id,
                            foodName = item.food.name,
                            quantity = item.mealFoodItem.quantity,
                            unit = item.mealFoodItem.unit
                        )
                    }
                }
            }
        }

        // Convert to GroceryItems and insert
        val groceryItems = aggregatedFoods.values.mapIndexed { index, agg ->
            GroceryItem(
                listId = listId,
                foodId = agg.foodId,
                quantity = agg.quantity,
                unit = agg.unit,
                sortOrder = index
            )
        }.sortedBy { aggregatedFoods.values.find { agg -> agg.foodId == it.foodId }?.foodName?.lowercase() }

        if (groceryItems.isNotEmpty()) {
            groceryDao.insertItems(groceryItems)
        }

        return listId
    }

    /**
     * Generate grocery list from a single diet (for preview or direct use)
     */
    suspend fun generateFromDiet(name: String, dietId: Long): Long {
        val userId = getCurrentUserId()

        val dietWithMeals = dietRepository.getDietWithMeals(dietId)
            ?: throw IllegalArgumentException("Diet not found")

        // Create the list
        val list = GroceryList(
            userId = userId,
            name = name
        )
        val listId = groceryDao.insertList(list)

        // Aggregate foods
        val aggregatedFoods = mutableMapOf<AggregationKey, AggregatedFood>()

        for ((_, mealWithFoods) in dietWithMeals.meals) {
            if (mealWithFoods == null) continue

            for (item in mealWithFoods.items) {
                val key = AggregationKey(item.food.id, item.mealFoodItem.unit)
                val existing = aggregatedFoods[key]

                if (existing != null) {
                    existing.quantity += item.mealFoodItem.quantity
                } else {
                    aggregatedFoods[key] = AggregatedFood(
                        foodId = item.food.id,
                        foodName = item.food.name,
                        quantity = item.mealFoodItem.quantity,
                        unit = item.mealFoodItem.unit
                    )
                }
            }
        }

        // Convert and insert
        val groceryItems = aggregatedFoods.values.mapIndexed { index, agg ->
            GroceryItem(
                listId = listId,
                foodId = agg.foodId,
                quantity = agg.quantity,
                unit = agg.unit,
                sortOrder = index
            )
        }.sortedBy { aggregatedFoods.values.find { agg -> agg.foodId == it.foodId }?.foodName?.lowercase() }

        if (groceryItems.isNotEmpty()) {
            groceryDao.insertItems(groceryItems)
        }

        return listId
    }

    // ===== Helper classes =====

    private data class AggregationKey(
        val foodId: Long,
        val unit: FoodUnit
    )

    private data class AggregatedFood(
        val foodId: Long,
        val foodName: String,
        var quantity: Double,
        val unit: FoodUnit
    )
}
